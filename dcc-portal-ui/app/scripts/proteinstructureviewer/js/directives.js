/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

(function () {
  'use strict';

  angular.module('proteinstructureviewer', ['proteinstructureviewer.directives']);

  var module = angular.module('proteinstructureviewer.directives', ['proteinstructureviewer.chartmaker']);

// The core proteinstructure directive -- this is used with a (possible) mutation to display
// the structure. But the core value is the gene identifier.
  module.directive('proteinstructure', function (chartmaker, $location, Protein, LocationService, 
    $window, gettextCatalog) {

    // This might look n**2, but it's not supposed to be.
    function transformDomains(domains) {
      var d, next, thisDomain, nextDomain, thisSize, nextSize, sortedDomains, domainCount, transformedDomains,
        domain;

      sortedDomains = domains.sort(function (a, b) {
        return a.start - b.start;
      });
      domainCount = sortedDomains.length;
      d = 0;

      while (d < domainCount) {
        next = d + 1;

        // Simple case, no more domains. Stop.
        if (next === domainCount) {
          break;
        }
        thisDomain = sortedDomains[d];
        nextDomain = sortedDomains[next];

        // Simple case, next domain starts after this one, no overlap
        if (thisDomain.end < nextDomain.start) {
          d++;
          continue;
        }

        // The domains do overlap. Keep the biggest. Note we splice the sortedDomains
        // array and adjust count so we can continue.
        thisSize = thisDomain.end - thisDomain.start;
        nextSize = nextDomain.end - nextDomain.start;
        if (thisSize >= nextSize) {

          // Next is smaller, remove it.
          sortedDomains.splice(next, 1);
          domainCount--;
        } else {
          // This is smaller. Juggle them
          sortedDomains.splice(d, 2, nextDomain);
          domainCount--;
        }
      }

      transformedDomains = [];
      domainCount = sortedDomains.length;
      for (d = 0; d < domainCount; d++) {
        domain = sortedDomains[d];
        transformedDomains.push({id: domain.hitName, start: domain.start, stop: domain.end,
          description: domain.description});
      }

      return transformedDomains;
    }

    function getOverallFunctionalImpact(mutation) {
      var result = 'unknown', fi = mutation.functionalImpact;
      if (fi) {
        if (fi === 'High') {
          result = 'high';
        } else if (fi === 'Medium') {
          result = 'medium';
        } else if (fi === 'Low') {
          result = 'low';
        } else {
          result = 'unknown';
        }
      } else {
        result = 'unknown';
      }
      return result;
    }

    /**
     * Transforms mutation hits to results for protein vewier. 
     */
    function transformMutations(mutations, transcriptId) {
      var filters = LocationService.filters();
      var result = _(mutations.hits)
        .map(hit => ({
            mutation: hit, 
            transcript: _.find(hit.transcripts, {id: transcriptId})
          }))
        .filter( m => m.transcript !== undefined && m.transcript !== null )
        .map( m => mapToResult(m) )
        .filter( m => filterResults(m, filters) ) 
        .value(); // Needed for lodash 3.x

      return result;
    }

     /**
     * Maps to object for protein viewer
     */
    function mapToResult(d) {
      var start = d.transcript.consequence.aaMutation.replace(/[^\d]/g, '');
        var m = {
          id: d.transcript.consequence.aaMutation,
          position: start,
          value: d.mutation.affectedDonorCountTotal,
          ref: d.mutation.id,
          functionalImpact: d.transcript.functionalImpact || 'Unknown'
        };
      
      return m;
    }

    /** 
     * Filters results based on valid start and criteria of filter
     */
    function filterResults(m, filters) {
      if (m.start < 0) return false;

      var fiFilter = _.get(filters, 'mutation.functionalImpact.is');
      if (fiFilter) {
        if (fiFilter.indexOf(m.functionalImpact) >= 0) {
          return true;
        }
      } else {
        return true;
      }
      return false; 
    }

    return {
      restrict: 'E',
      replace: true,
      scope: {'highlightMarker': '&', 'transcript': '='},
      template: '<div class="protein-structure-viewer-diagram"></div>',
      link: function (scope, iElement) {
        var options, selectedMutation;

        options = iElement.data();

        selectedMutation = scope.$eval('highlightMarker');
        if (selectedMutation) {
          selectedMutation = selectedMutation();
        }

        function refresh(transcript) {
          var element, chartData;

          if (transcript && transcript.id) {

            Protein.init(transcript.id).get().then(function (mutations) {
              scope.mutations = mutations;


              element = jQuery(iElement).get()[0];

              chartData = {};
              chartData.start = 1;
              chartData.stop = transcript.lengthAminoAcid;

              // Ideally, we need to merge domains that overlap. This is based on pfam curation
              // rules that indicate only domains in the same family can overlap. Our strategy is
              // simple: when we find two domains that overlap, at all, we replace them by the larger
              // until we are done.
              chartData.domains = transformDomains(transcript.domains);

              // Now reformat the mutations as required. Yes, it would be better to provide an
              // iterator function for these, and for the domains too, for that matter, but this
              // will do for now.
              chartData.mutations = transformMutations(mutations, transcript.id);

              options.tooltipShowFunc = function(elem, d, options, isDomain) {
                var getLabel = function(){

                  if(isDomain) {
                    return d.id + ': ' + d.description;
                  }

                  var FI = getOverallFunctionalImpact(d);
                  return gettextCatalog.getString('Mutation ID') + ': ' + d.ref + '<br>' +
                         gettextCatalog.getString('Number of donors') + ': ' + d.value + '<br>' +
                         gettextCatalog.getString('Amino acid change') + ': ' + d.id + '<br>' +
                         gettextCatalog.getString('Functional Impact') + ': ' + FI;
                };

                var position = null;

                if(isDomain){
                  // The domain svg element consits of a group of text and rect. Since the text
                  // can extend the rect which we care about it, get the first child (rect)
                  var actualElement = angular.element(angular.element(elem).context.firstChild);

                  // Use the width/height of the rect and the CTM of the svg container plus the
                  // x and y position of the rect within the svg container to find the left/right values
                  position = {
                    width: actualElement.prop('width').baseVal.value,
                    height: actualElement.prop('height').baseVal.value,
                    left: elem.getScreenCTM().e + actualElement.prop('x').baseVal.value,
                    top: elem.getScreenCTM().f + $window.pageYOffset + actualElement.prop('y').baseVal.value
                  };

                }

                scope.$emit('tooltip::show', {
                  element: angular.element(elem),
                  text: getLabel(),
                  placement: options.placement,
                  elementPosition: isDomain?position:null
                });
              };

              options.tooltipHideFunc = function() {
                scope.$emit('tooltip::hide');
              };

              options.markerClassFn = function(d) {
                var style;
                style = getOverallFunctionalImpact(d);
                if (selectedMutation) {
                  if( d.ref === selectedMutation) {
                    style = style + ' selected';
                  } else {
                    style = style + ' fade';
                  }
                }
                return style;
              };

              options.markerUrlFn = function (d) {
                $location.path('/mutations/' + d.ref);
              };

             options.displayWidth = jQuery('.protein-structure-viewer-diagram').width() - 100;

              var chart = chartmaker.chart(options, chartData);
              if (chartData.mutations.length > 0) {
                chart.display(element);
              } else {
                chart.displayError(element, gettextCatalog.getString('No Mutation occurs in coding region of this Gene.'));
              }
            });
          }
        }


        scope.$on('$locationChangeSuccess', function () {
          if (scope.transcript) {
            refresh(scope.transcript);
          }
        });

        scope.$watch('transcript', function (transcript) {
          scope.transcript = transcript;
          refresh(transcript);
        });
        
        jQuery(window).resize(function() {
          refresh(scope.transcript);
        });
        
        scope.$on('$destroy', function () {
          jQuery(window).unbind('resize');
        });

      }
    };
  });
})();
