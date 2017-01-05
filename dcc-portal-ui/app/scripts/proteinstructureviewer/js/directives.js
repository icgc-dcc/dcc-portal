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
import lolliplot from '@oncojs/lolliplot/dist/lib';

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
        transformedDomains.push({
          id: domain.hitName,
          start: domain.start,
          end: domain.end,
          description: domain.description
        });
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
          consequence: d.transcript.consequence.aaMutation,
          x: start,
          donors: d.mutation.affectedDonorCountTotal,
          id: d.mutation.id,
          impact: d.transcript.functionalImpact || 'Unknown'
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

          if (!transcript || !transcript.id) {
            console.warn('Aborting refresh due to missing transcript');
            return;
          }

            const drawChart = (mutations) => {
              scope.mutations = mutations;


              element = jQuery(iElement).get()[0];

              chartData = {};
              chartData.start = 1;
              chartData.stop = transcript.lengthAminoAcid;

              // Ideally, we need to merge domains that overlap. This is based on pfam curation
              // rules that indicate only domains in the same family can overlap. Our strategy is
              // simple: when we find two domains that overlap, at all, we replace them by the larger
              // until we are done.
              chartData.proteins = transformDomains(transcript.domains);

              // Now reformat the mutations as required. Yes, it would be better to provide an
              // iterator function for these, and for the domains too, for that matter, but this
              // will do for now.
              chartData.mutations = transformMutations(mutations, transcript.id);

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

              const hideTooltip = () => scope.$emit('tooltip::hide');

              var chart = lolliplot({
                d3: require('d3'),
                ...options,
                width: jQuery('.protein-structure-viewer-diagram').width(),
                element,
                animate: true,
                data: chartData,
                hideStats: true,
                onMutationClick: (data) => {
                  $location.path('/mutations/' + data.id);
                },
                onMutationMouseover: (data, event) => {
                  scope.$emit('tooltip::show', {
                    element: angular.element(event.target),
                    text: gettextCatalog.getString('Mutation ID') + ': ' + data.id + '<br>' +
                      gettextCatalog.getString('Number of donors') + ': ' + data.donors + '<br>' +
                      gettextCatalog.getString('Amino acid change') + ': ' + data.consequence + '<br>' +
                      gettextCatalog.getString('Functional Impact') + ': ' + data.impact,
                    placement: 'top',
                  });
                },
                onMutationMouseout: hideTooltip,
                onProteinMouseover: (data, event) => {
                  scope.$emit('tooltip::show', {
                    elementPosition: {
                      width: 0,
                      height: 0,
                      top: event.target.getBoundingClientRect().top + document.body.scrollTop,
                      left: event.pageX,
                    },
                    text: `${data.id} : ${data.description}`,
                    placement: 'top'
                  });
                },
                onProteinMouseout: hideTooltip,
              });
            }
            Protein.init(transcript.id).get().then(drawChart);;
          
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
          // refresh(scope.transcript);
        });
        
        scope.$on('$destroy', function () {
          jQuery(window).unbind('resize');
        });

      }
    };
  });
})();
