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




(function($) {
  'use strict';

  var module = angular.module('icgc.pathwayviewer', ['icgc.pathways', 'icgc.pathwayviewer.directives.controller']);

  module.directive('pathwayViewer', function ($location, PathwaysConstants) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        items: '=',
        mutationHighlights: '=',
        drugHighlights: '=',
        zooms: '=',
        overlaps: '='
      },
      controller: 'PathwayViewerCtrl',
      templateUrl: 'scripts/pathwayviewer/views/viewer.html',
      link: function ($scope, element, attrs, pathwayViewerCtrl) {
        var showingLegend = false,
            rendered = false,
            zoomedOn = $scope.zooms || [''],
            xml = $scope.items,
            mutationHighlights = $scope.mutationHighlights || [],
            drugHighlights = $scope.drugHighlights || [],
            overlaps = $scope.overlaps || [],
            scrollTimer,
            consequenceFilter = {
              is: [
                      'frameshift_variant',
                      'missense_variant',
                      'start_lost',
                      'initiator_codon_variant',
                      'stop_gained','stop_lost',
                      'exon_loss_variant',
                      'exon_variant',
                      'splice_acceptor_variant',
                      'splice_donor_variant',
                      'splice_region_variant',
                      '5_prime_UTR_premature_start_codon_gain_variant',
                      'disruptive_inframe_deletion',
                      'inframe_deletion',
                      'disruptive_inframe_insertion',
                      'inframe_insertion'
                    ]
            };

        element.bind('mouseenter', function() {
          scrollTimer = setTimeout(function() {
            $('.pathwaysvg').attr('class', 'pathwaysvg');
          }, 500);
        });

        element.bind('mouseleave', function() {
          clearTimeout(scrollTimer);
          $('.pathwaysvg').attr('class', 'pathwaysvg pathway-no-scroll');
        });

        var typeMap = {
          'RenderableComplex': 'Complex',
          'RenderableProtein': 'Protein',
          'RenderableEntitySet': 'EntitySet',
          'RenderableChemical': 'Chemical',
          'RenderableCompartment': 'Compartment',
          'ProcessNode': 'ProcessNode',
          'RenderableMutated Gene(s)': 'Mutated Gene(s)'
        };

        var showLegend = function(){
          $('.pathway-legend').animate({'left': '75%'});
          $('.pathway-legend-controller').addClass('fa-chevron-circle-right').removeClass('fa-question-circle');
          showingLegend = true;
        };

        var showInfo = function(){
          $('.pathway-info-controller').css('visibility','visible');
          $('.pathway-legend-controller').css('visibility','hidden');
          $('.pathway-info').animate({left: '50%'});
        };

        var hideLegend = function(){
          $('.pathway-legend').animate({left: '100%'});
          $('.pathway-legend-controller').addClass('fa-question-circle').removeClass('fa-chevron-circle-right');
          showingLegend = false;
        };

        var hideInfo = function(){
          $('.pathway-info').animate({left: '100%'});
          $('.pathway-info-controller').css('visibility','hidden');
          $('.pathway-legend-controller').css('visibility','visible');
        };

        var renderinfo = function(node, mutationCount, drugCount, isClickableNode){
          $('.pathway-info-svg').html('');

          var padding = 2;
          // Need extra node padding on the top for firefox. Otherwise mutation count gets cut off
          var nodeTopPadding = 10;
          var infoSvg = d3.select('.pathway-info-svg').append('svg')
              .attr('viewBox', '0 0 ' +150+ ' ' +80)
              .attr('preserveAspectRatio', 'xMidYMid')
              .attr('style', 'padding-top:20px')
              .append('g');
          var infoRenderer = new pathwayViewerCtrl.Renderer(infoSvg, {
            onClick: null, urlPath: $location.url(), overlapColor: '#ff9900',
            mutationHighlightColor: '#9b315b', drugHighlightColor: 'navy',
            strokeColor: '#696969'
          });

          node.size={width:120-padding*2,height:60-padding*2};
          node.position={x:padding+15,y:padding+nodeTopPadding};
          infoRenderer.renderNodes([node]);

          if(isClickableNode){
            var model = new pathwayViewerCtrl.PathwayModel();
            model.nodes = [node];
            
            infoRenderer.highlightEntity(
                mutationCount !== 0 ? [{id:node.reactomeId,value:mutationCount}] : [], 
                drugCount !== 0 ? [{id:node.reactomeId,value:drugCount}] : [],
                overlaps, 
                model
            );
          }
        };

        var controllerSettings = {
          width: 500,
          height: 350,
          container: '#pathway-viewer-mini',
          onClick: function (d) {
            var mutationCount = '*',
              drugCount = '*',
              druggableGenesList = [],
              node = $.extend({}, d),
              overlappingGenesMap = {},
              overlappingGenesList = [],
              mutatedGenesList = [],
              annotatedGenesList = [];
            

            // Reset data
            $scope.annotatedGenesList = [];
            $scope.sortField = 'symbol';
            $scope.sortDescendingly = false;
            $scope.entityType = typeMap[d.type];
            $scope.subPathwayId = d.reactomeId;

            hideLegend();
            showInfo();

            if (overlaps && node.isPartOfPathway) {
              _.keys(overlaps).forEach(function(dbId) {
                if (dbId === d.reactomeId) {
                  var overlappingGene = overlaps[dbId];
                  overlappingGenesMap[overlappingGene.geneId] = overlappingGene;
                  overlappingGenesList.push(overlappingGene);
                }
              });

              $scope.overlappingGenesList = overlappingGenesList;
              $scope.overlappingGenesMap = overlappingGenesMap;
            }

            // Create list of uniprot ids if we have any
            if(mutationHighlights && node.isPartOfPathway){
              mutationHighlights.forEach(function (mutationHighlight) {

                if(_.contains(mutationHighlight.dbIds,d.reactomeId)){

                  if(!mutationHighlight.advQuery){
                    return;
                  }

                  _.set(mutationHighlight.advQuery, 'mutation.consequenceType', consequenceFilter);

                  mutatedGenesList.push({
                    symbol: mutationHighlight.geneSymbol,
                    id: mutationHighlight.geneId,
                    value: mutationHighlight.value,
                    advQuery: mutationHighlight.advQuery
                  });
                }
              });

              if(mutatedGenesList.length === 1){
                mutationCount = mutatedGenesList[0].value;
              }

            }
            
            // Create list of uniprot ids if we have any
            if(drugHighlights && node.isPartOfPathway){
              drugHighlights.forEach(function (drugHighlight) {
                if(_.contains(drugHighlight.dbIds,d.reactomeId)){
                  druggableGenesList.push({
                    symbol: drugHighlight.geneSymbol,
                    id: drugHighlight.geneId,
                    drugs: drugHighlight.drugs,
                  });
                }
              });

              if(druggableGenesList.length === 1){
                drugCount = druggableGenesList[0].drugs.length;
              }
            }

            var annotatedGeneIds = _.union(
              _.pluck(mutatedGenesList, 'id'),
              _.pluck(druggableGenesList, 'id')
            );

            annotatedGeneIds.forEach(function (geneId) {
              var mutatedGene = _.find(mutatedGenesList, function(o) { return o.id === geneId });
              var druggableGene = _.find(druggableGenesList, function(o) { return o.id === geneId });
              
              annotatedGenesList.push({
                id: geneId,
                symbol: mutatedGene ? mutatedGene.symbol : druggableGene.symbol,
                value: mutatedGene ? mutatedGene.value : undefined,
                drugs: druggableGene ? druggableGene.drugs : [],
                advQuery: mutatedGene ? mutatedGene.advQuery : []
              });
            });
                        
            $scope.annotatedGenesList = annotatedGenesList;
            
            renderinfo(
                node, 
                mutatedGenesList.length > 0 ? mutationCount : 0,
                druggableGenesList.length > 0 ? drugCount : 0,
                (annotatedGenesList.length > 0 || overlappingGenesList.length > 0)
            );
          },
          urlPath: $location.url(),
          strokeColor: '#696969',
          mutationHighlightColor: '#9b315b',
          drugHighlightColor: 'navy',
          overlapColor: '#ff9900',
          initScaleFactor: 0.90,
          subPathwayColor: 'navy'
        };

        var controller = new pathwayViewerCtrl.ReactomePathway(controllerSettings);


        $('.pathway-info-controller').on('click',function(){
          hideInfo();
        });

        var requestFullScreen = function(element) {
          if (element.requestFullscreen) {
            element.requestFullscreen();
          } else if (element.mozRequestFullScreen) {
            element.mozRequestFullScreen();
          } else if (element.webkitRequestFullScreen) {
            element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
          }
        };

        var exitFullScreen = function() {
          if (document.exitFullscreen) {
              document.exitFullscreen();
            } else if (document.mozCancelFullScreen) {
              document.mozCancelFullScreen();
            } else if (document.webkitExitFullscreen) {
              document.webkitExitFullscreen();
            }
        };

        var fullScreenHandler = function() {
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            $('.pathway-fullscreen-controller').removeClass('fa-compress');
            $('.pathway-fullscreen-controller').addClass('fa-expand');
          } else {
            $('.pathway-fullscreen-controller').removeClass('fa-expand');
            $('.pathway-fullscreen-controller').addClass('fa-compress');
          }
        };

        $('.pathway-fullscreen-controller').on('click', function() {
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            requestFullScreen(document.getElementById('pathway-viewer-mini'));
          } else {
            exitFullScreen();
          }
        });

        if (document.addEventListener){
            document.addEventListener('webkitfullscreenchange', fullScreenHandler);
            document.addEventListener('mozfullscreenchange', fullScreenHandler);
            document.addEventListener('fullscreenchange', fullScreenHandler);
        }

        var fixUrl = function(e,attr) {
          if (e.hasAttribute(attr) && e.getAttribute(attr)!== false) {
            var idMatcher = /(#.*)\'\)/;
            var matches = e.getAttribute(attr).match(idMatcher);
            if (matches !== null) {
              var svgId = matches[1];
              var newPath = window.location.pathname + window.location.search;
              var newUrl = 'url("' + newPath + svgId + '")';

              e.setAttribute(attr, newUrl);
            }
          }
        };

        var fixFilters = function () {
          fixUrl(this, 'filter');
        };

        var fixMarkers = function () {
          fixUrl(this, 'marker-end');
          fixUrl(this, 'marker-start');
        };

        var handleRender = function(){
          if(!xml || !zoomedOn){
            $('.pathwaysvg').remove();
            return;
          }else if(!rendered){
            $('.pathwaysvg').remove();
            controller.render(xml,zoomedOn);
            rendered = true;
          }else{
            hideInfo();
            hideLegend();
          }

          if(mutationHighlights.length || drugHighlights.length || overlaps.length){
            controller.highlight(mutationHighlights, drugHighlights, overlaps);
          }
        };

        $scope.legendClick = function() {
            if(showingLegend){
              hideLegend();
            }else{
              showLegend();
            }
        };

        $scope.$watch('items', function (newValue) {
          rendered = false;
          xml = newValue;
          handleRender();
        });

        $scope.$watch('zooms', function (newValue, oldValue) {
          if (newValue === oldValue) {
            return;
          }

          rendered = false;
          zoomedOn = newValue;
          handleRender();
        });

        $scope.$watch('mutationHighlights', function (newValue, oldValue) {
          if (! newValue || newValue === oldValue) {
            return;
          }

          mutationHighlights = newValue;
          handleRender();
        });
        
        $scope.$watch('drugHighlights', function (newValue, oldValue) {
          if (! newValue || newValue === oldValue) {
            return;
          }
          
          drugHighlights = newValue;
          handleRender();
        });

        $scope.$watch('overlaps', function (newValue, oldValue) {
          if (! newValue || newValue === oldValue) {
            return;
          }

          overlaps = newValue;
          handleRender();
        });

        // Render legend last to ensure all dependencies are initialized. Timeout of 0 does not work in firefox.
        $scope.$on(PathwaysConstants.EVENTS.MODEL_READY_EVENT, function() {
            controller.renderLegend(270, 671);
        });

        // Needed to fix url paths for SVGs on url change due to <base> tag required by angular
        $scope.$on('$locationChangeSuccess', function() {
          jQuery('rect', '#pathway-viewer-mini').map(fixFilters);
          jQuery('polygon', '#pathway-viewer-mini').map(fixFilters);
          jQuery('line', '#pathway-viewer-mini').map(fixMarkers);
        });

        $scope.$on('$destroy', function () {
          element.unbind();
          document.removeEventListener('webkitfullscreenchange', fullScreenHandler);
          document.removeEventListener('mozfullscreenchange', fullScreenHandler);
          document.removeEventListener('fullscreenchange', fullScreenHandler);
        });
      }
    };
  });
})(jQuery);