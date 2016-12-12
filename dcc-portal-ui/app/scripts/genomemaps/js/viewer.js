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

'use strict';

/* globals CellBaseManager:false, CellBaseAdapter:false, FeatureTrack:false, IcgcGeneAdapter: false */
/* globals GeneRenderer: false, FeatureRenderer: false, IcgcMutationTrack: false, IcgcMutationAdapter: false  */
/* globals GENE_BIOTYPE_COLORS: false, GENE_BIOTYPE_COLORS: false */

angular.module('icgc.modules.genomeviewer', ['icgc.modules.genomeviewer.header', 'icgc.modules.genomeviewer.service'])
  .constant('gvConstants',  {
    CHROMOSOME_LIMIT_MAP: {
      1: 249250621,
      2: 243199373,
      3: 198022430,
      4: 191154276,
      5: 180915260,
      6: 171115067,
      7: 159138663,
      8: 146364022,
      9: 141213431,
      10: 135534747,
      11: 135006516,
      12: 133851895,
      13: 115169878,
      14: 107349540,
      15: 102531392,
      16: 90354753,
      17: 81195210,
      18: 78077248,
      19: 59128983,
      20: 63025520,
      21: 48129895,
      22: 51304566,
      X: 155270560,
      Y: 59373566,
      MT: 16569
    }
  });

angular.module('icgc.modules.genomeviewer').controller('GenomeViewerController', function ($scope, GMService) {
  var _controller = this;


  _controller.getSpecies = function (callback) {
    CellBaseManager.get({
      host: GMService.getConfiguration().cellBaseHost,
      category: 'meta',
      subCategory: 'species',
      success: function (r) {
        var taxonomies = r.response[0].result[0];

        for (var taxonomy in taxonomies) {

          if (! taxonomies.hasOwnProperty(taxonomy)) {
            continue;
          }

          var newSpecies = [];

          for (var i = 0; i < taxonomies[taxonomy].length; i++) {

            var species = taxonomies[taxonomy][i];

            if (! species.hasOwnProperty('assemblies')) {
              continue;
            }

            for (var j = 0; j < species.assemblies.length; j++) {

              var s = Utils.clone(species);

              s.assembly = species.assemblies[j];
              delete s.assemblies;
              newSpecies.push(s);

            }
          }
          taxonomies[taxonomy] = newSpecies;
        }
        callback(taxonomies);
      }
    });
  };


  // Assume this is a single use call i.e. this is not an idempotent function so don't call this more than once!
  _controller.initFullScreenHandler = function(genomeViewer) {

    if (! document.addEventListener) {
      return false;
    }

    var _fullscreenHandler = function() {
      if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
        jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-small');
        jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-full');
      } else {
        jQuery('#gv-fullscreen-ctrl').removeClass('icon-resize-full');
        jQuery('#gv-fullscreen-ctrl').addClass('icon-resize-small');
      }

      setTimeout( function() {
        // Ensure our setWidth triggers after the genome viewers interal resize callbacks
        genomeViewer.setWidth(jQuery('.t_gv__navbar').outerWidth());
      }, 1000);


    };

    document.addEventListener('webkitfullscreenchange', _fullscreenHandler);
    document.addEventListener('mozfullscreenchange', _fullscreenHandler);
    document.addEventListener('fullscreenchange', _fullscreenHandler);

    $scope.$on('$destroy', function() {
      document.removeEventListener('webkitfullscreenchange', _fullscreenHandler);
      document.removeEventListener('mozfullscreenchange', _fullscreenHandler);
      document.removeEventListener('fullscreenchange', _fullscreenHandler);
    });

  };

});

angular.module('icgc.modules.genomeviewer').directive('genomeViewer', function (GMService, 
  $location, gvConstants, gettextCatalog) {
  return {
    restrict: 'A',
    template: '<div id="genome-viewer" style="border:1px solid #d3d3d3;border-left:none;border-top-width: 0px;"></div>',
    replace: true,
    controller: 'GenomeViewerController',
    link: function (scope, element, attrs, GenomeViewerController) {
      require.ensure([], require => {
        require('~/scripts/genome-viewer.js');
      console.log(GenomeViewerController);
      var genomeViewer, navigationBar, tracks = {};
      var availableSpecies;
        var regionObj = new Region({chromosome: 1, start: 1, end: 1}),
        done = false; 

      function setup() {
        regionObj.start = parseInt(regionObj.start, 10);
        regionObj.end = parseInt(regionObj.end, 10);
        var species = availableSpecies.vertebrates[0];
        genomeViewer = genomeViewer || new GenomeViewer({
            cellBaseHost: GMService.getConfiguration().cellBaseHost,
            cellBaseVersion: 'v3',
            target: 'genome-viewer',
            width: 1135,
            availableSpecies: availableSpecies,
            species: species,
            region: regionObj,
            defaultRegion: regionObj,
            sidePanel: false,
            drawNavigationBar: false,
            navigationBarConfig: {
              componentsConfig: {
                restoreDefaultRegionButton:false,
                regionHistoryButton:false,
                speciesButton:false,
                chromosomesButton:false,
                windowSizeControl:false,
                positionControl:false,
                searchControl:false
              }
            },
            drawKaryotypePanel: true,
            drawChromosomePanel: true,
            drawRegionOverviewPanel: true,
            karyotypePanelConfig: {
              hidden:true,
              collapsed: false,
              collapsible: false
            },
            chromosomePanelConfig: {
              hidden:true,
              collapsed: false,
              collapsible: false
            },
            version: 'Powered by ' +
            '<a target="_blank" href="http://www.genomemaps.org/">Genome Maps</a>'
          });
        window.gv = genomeViewer;

        /** Set Navigation bar **/
        navigationBar = new IcgcNavigationBar({
          zoom: genomeViewer.zoom,
          handlers: {
            'zoom:change': function (event) {
              genomeViewer._zoomChangeHandler(event);
            },
            'region:change': function (event) {
              genomeViewer._regionChangeHandler(event);
            },
            'region:move': function (event) {
              genomeViewer._regionMoveHandler(event);
            },
          }
        });
        genomeViewer.on('region:change', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.setRegion(event.region);
          }
        });
        genomeViewer.on('region:move', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.moveRegion(event.region);
          }
        });
        genomeViewer.setNavigationBar(navigationBar);

        /** Add tracks **/
        tracks.sequence = new FeatureTrack({
          title: 'Sequence',
          height: 30,
          visibleRegionSize: 200,
          renderer: new SequenceRenderer({tooltipContainerID: '#genomic'}),
          dataAdapter: new CellBaseAdapter({
            category: 'genomic',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            subCategory: 'region',
            resource: 'sequence',
            params: {},
            species: genomeViewer.species,
            cacheConfig: {
              chunkSize: 1000
            }
          })
        });
        genomeViewer.addTrack(tracks.sequence);

        var icgcGeneOverviewRenderer = new FeatureRenderer(FEATURE_TYPES.gene);
        icgcGeneOverviewRenderer.on({
          'feature:click': function (e) {
            scope.$apply(function () {
              $location.path('/genes/' + e.feature.id).search({});
            });
          }
        });

        tracks.icgcGeneOverviewTrack = new IcgcGeneTrack({
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          height: 100,
          autoHeight: false,
          renderer: icgcGeneOverviewRenderer,
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            multiRegions: true,
            histogramMultiRegions: false,
            /* TODO: use BASE_URL */
            //uriTemplate: 'https://dcc.icgc.org/api/browser/gene?segment={region}&resource=gene',
            cacheConfig: {
              chunkSize: 100000
            }
          })
        });
        genomeViewer.addOverviewTrack(tracks.icgcGeneOverviewTrack);


        tracks.icgcGeneTrack = new IcgcGeneTrack({
          title: 'ICGC Genes',
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          minTranscriptRegionSize: 300000,
          height: 100,
          renderer: new GeneRenderer({
            tooltipContainerID: '#genomic',
            handlers: {
              'feature:click': function (e) {
                var path = '/genes/' + e.feature[e.featureType === 'gene' ? 'id' : 'geneId'];
                scope.$apply(function () {
                  $location.path(path).search({}).search({});
                });
              }
            }
          }),
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            multiRegions: true,
            histogramMultiRegions: false,
            /* TODO: use BASE_URL */
            cacheConfig: {
              chunkSize: 100000
            }
          })
        });

        genomeViewer.addTrack(tracks.icgcGeneTrack);

        tracks.icgcMutationsTrack = new IcgcMutationTrack({
          title: 'ICGC Mutations',
          minHistogramRegionSize: 10000,
          maxLabelRegionSize: 3000,
          height: 100,
          renderer: new FeatureRenderer({
            tooltipContainerID: '#genomic',
          label: function (f) {
            return f.id;
          },
          tooltipTitle: function (f) {
              return '<span class="gmtitle">' + gettextCatalog.getString('ICGC mutation') + ' - ' + f.id + '</span>';
          },
          tooltipText: function (f) {
            var consequences = GMService.tooltipConsequences(f.consequences), fi;
            fi = (f.functionalImpact && _.contains(f.functionalImpact, 'High')) ? 'High' : 'Low';
            return '<span class="gmkeys">' + gettextCatalog.getString('mutation') + ':&nbsp;</span>' + 
            f.mutation + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('reference allele') + ':&nbsp;</span>' + 
              f.refGenAllele + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('mutation type') + ':&nbsp;</span>' + 
              f.mutationType + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('project info') + ':</span><br>' + 
              f.projectInfo.join('<br>') + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('consequences') + ':<br></span>' + 
              consequences + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('source') + ':&nbsp;</span>ICGC<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('start-end') + ':&nbsp;</span>' + 
              f.start + '-' + f.end + '<br>' +
              '<span class="gmkeys">' + gettextCatalog.getString('functional impact') + ':&nbsp;</span>' + fi;
          },
          color: function (feat) {
            switch (feat.mutationType) {
              case 'single base substitution':
                return 'Chartreuse';
              case 'insertion of <=200bp':
                return 'orange';
              case 'deletion of <=200bp':
                return 'red';
              case 'multiple base substitution (>=2bp and <=200bp)':
                return 'lightblue';
              default:
                return 'black';
            }
          },
          infoWidgetId: 'mutationCds',
          height: 8,
          histogramColor: 'orange',
          handlers: {
            'feature:click': function (e) {
              scope.$apply(function () {
                $location.path('/mutations/' + e.feature.id).search({});
              });
            }
          }
        }),
          dataAdapter: new IcgcMutationAdapter({
            resource: 'mutation',
            multiRegions: true,
            histogramMultiRegions: false,
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            featureCache: {
              chunkSize: 10000
            }
          })
        });

        genomeViewer.addTrack(tracks.icgcMutationsTrack);
        /** End add tracks **/

        genomeViewer.draw();

        genomeViewer.karyotypePanel.hide();
        genomeViewer.chromosomePanel.hide();
        GenomeViewerController.initFullScreenHandler(genomeViewer);
      }

      function update() {
        var {genes, mutations, tab} = scope;

        if (!done) {
          if (tab === 'genes' &&
            genes.hasOwnProperty('data') &&
            genes.data.hasOwnProperty('hits') &&
            genes.data.hits.length) {
            done = true;
            var gene = genes.data.hits[0];
            regionObj = new Region({chromosome: gene.chromosome, start: gene.start - 1500, end: gene.end + 1500});
            if (! availableSpecies) {
              GenomeViewerController.getSpecies(function (s) {
                availableSpecies = s;
                setup();
              });
            } else {
              setup();
            }
          }
          else if (tab === 'mutations' &&
            mutations.hasOwnProperty('data') &&
            mutations.data.hasOwnProperty('hits') &&
            mutations.data.hits.length) {
            done = true;
            var mutation = mutations.data.hits[0];
            regionObj = new Region({
              chromosome: mutation.chromosome,
              start: mutation.start,
              end: mutation.start
            });

            if (! availableSpecies) {
              GenomeViewerController.getSpecies(function (s) {
                availableSpecies = s;
                setup();
              });
            } else {
              setup();
            }
          }

        }
      }
      scope.$watch('[genes, mutations, tab]', update, true);

      scope.$on('gv:set:region', function (e, params) {
        if (genomeViewer) {
          genomeViewer.setRegion(params);
          navigationBar._handleZoomSlider(genomeViewer.zoom);
        }
      });
      scope.$on('gv:toggle:panel', function (e, params) {
        var action = params.active ? 'show' : 'hide';
        genomeViewer[params.panel + 'Panel'][action]();
      });
      scope.$on('gv:zoom:set', function (e, val) {
        navigationBar._handleZoomSlider(val);
      });
      scope.$on('gv:reset', function (e) {
        navigationBar._handleRestoreDefaultRegion(e);
      });
      scope.$on('gv:autofit', function () {
        genomeViewer.toggleAutoHeight(true);
      });
      scope.$on('$destroy', function () {
        if (genomeViewer) {
          genomeViewer.destroy();
        }
      });

      update();
      });

    }
  };
});

angular.module('icgc.modules.genomeviewer').directive('gvembed', function (GMService, $location,
  LocationService, gvConstants, gettextCatalog) {
  return {
    restrict: 'E',
    replace: true,
    transclude: true,
    controller: 'GenomeViewerController',
    template: '<div id="gv-application" style="border:1px solid #d3d3d3;border-top-width: 0px;"></div>',

    link: function (scope, element, attrs, GenomeViewerController) {
      var genomeViewer, navigationBar, tracks = {};
      var availableSpecies;
      require.ensure([], require => {
        require('~/scripts/genome-viewer.js');
      function setup(regionObj) {
        regionObj.start = parseInt(regionObj.start, 10);
        regionObj.end = parseInt(regionObj.end, 10);
        var species = availableSpecies.vertebrates[0];
        genomeViewer = genomeViewer || new GenomeViewer({
            cellBaseHost: GMService.getConfiguration().cellBaseHost,
            cellBaseVersion: 'v3',
            target: 'gv-application',
            width: 1135,
            availableSpecies: availableSpecies,
            species: species,
            region: regionObj,
            defaultRegion: regionObj,
            sidePanel: false,
            drawNavigationBar: false,
            navigationBarConfig: {
            componentsConfig: {
              restoreDefaultRegionButton:false,
              regionHistoryButton:false,
              speciesButton:false,
              chromosomesButton:false,
              windowSizeControl:false,
              positionControl:false,
              searchControl:false
            }
          },
            drawKaryotypePanel: false,
            drawChromosomePanel: false,
            drawRegionOverviewPanel: true,
            karyotypePanelConfig: {
              collapsed: false,
              collapsible: false
            },
            chromosomePanelConfig: {
              collapsed: false,
              collapsible: false
            },
            version: gettextCatalog.getString('Powered by' +
              ' <a target="_blank" href="http://www.genomemaps.org/">Genome Maps</a>')
          });
        window.gv = genomeViewer;

        /** Set Navigation bar **/
        navigationBar = new IcgcNavigationBar({
          zoom: genomeViewer.zoom,
          handlers: {
            'zoom:change': function (event) {
              genomeViewer._zoomChangeHandler(event);
            },
            'region:change': function (event) {
              genomeViewer._regionChangeHandler(event);
            },
            'restoreDefaultRegion:click': function (event) {
              Utils.setMinRegion(genomeViewer.defaultRegion, genomeViewer.getSVGCanvasWidth());
              event.region = genomeViewer.defaultRegion;
              genomeViewer.trigger('region:change', event);
            }
          }
        });
        genomeViewer.on('region:change', function (event) {
          navigationBar.setRegion(event.region, genomeViewer.zoom);
        });
        genomeViewer.on('region:move', function (event) {
          if (event.sender !== navigationBar) {
            navigationBar.moveRegion(event.region);
          }
        });
        genomeViewer.setNavigationBar(navigationBar);

        /** Add tracks **/
        tracks.sequence = new FeatureTrack({
          title: 'Sequence',
          height: 30,
          visibleRegionSize: 200,
          renderer: new SequenceRenderer({tooltipContainerID: '#genomic'}),
          dataAdapter: new CellBaseAdapter({
            category: 'genomic',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            subCategory: 'region',
            resource: 'sequence',
            params: {},
            species: genomeViewer.species,
            cacheConfig: {
              chunkSize: 100
            }
          })
        });
        genomeViewer.addTrack(tracks.sequence);

        var featureGeneRendererParams = {
          tooltipContainerID: '#genomic',
          label: function (f) {
            var str = '';
            str += (f.strand < 0 || f.strand === '-') ? '<' : '';
            str += (f.strand > 0 || f.strand === '+') ? '>' : '';
            str += ' ' + f.externalName + ' ';
            str += (f.biotype !== null && f.biotype !== '') ? ' [' + f.biotype + ']' : '';
            return str;
          },
          tooltipTitle: function (f) {
            return 'ICGC Gene - <span style="color:#283e5d">' + f.externalName + '</span>';
          },
          tooltipText: function (f) {
            var color = GENE_BIOTYPE_COLORS[f.biotype];
            var str = '';
            str += 'id:&nbsp;<span style="color:#166aa2">' + f.stableId + '</span><br>';
            str += 'biotype:&nbsp;<span style="font-weight: bold;color:' + color + ';">' + f.biotype + '</span><br>';
            str += FEATURE_TYPES.getTipCommons(f);
            str += '<br>description:&nbsp;<span style="font-weight: bold;">' +
                     (f.description ? f.description : 'none') + '</span><br>';

            return str;
          },
          color: function (f) {
            return GENE_BIOTYPE_COLORS[f.biotype];
          },
          infoWidgetId: 'stableId',
          height: 4,
          histogramColor: 'lightblue',
          handlers: {
            'feature:click': function (e) {
              var path = '/genes/' + e.feature[e.featureType === 'gene' ? 'id' : 'geneId'];
              scope.$apply(function () {
                $location.path(path).search({});
              });
            }
          },
        };

        var icgcGeneOverviewRenderer = new FeatureRenderer(featureGeneRendererParams);

        icgcGeneOverviewRenderer.on({
          'feature:click': function (e) {
            scope.$apply(function () {
              $location.path('/genes/' + e.feature.id).search({});
            });
          }
        });
        tracks.icgcGeneOverviewTrack = new IcgcGeneTrack({
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          height: 100,
          autoHeight: false,
          functional_impact: _.get(LocationService.filters(), 'mutation.functionalImpact.is', ''),
          renderer: icgcGeneOverviewRenderer,
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            featureCache: {
              chunkSize: 100000
            }
          })
        });

        genomeViewer.addOverviewTrack(tracks.icgcGeneOverviewTrack);

        tracks.icgcGeneTrack = new IcgcGeneTrack({
          title: 'ICGC Genes',
          minHistogramRegionSize: 20000000,
          maxLabelRegionSize: 10000000,
          minTranscriptRegionSize: 300000,
          height: 100,
          functional_impact: _.get(LocationService.filters(), 'mutation.functionalImpact.is', ''),
          renderer: new GeneRenderer({
          tooltipContainerID: '#genomic',
            handlers: {
              'feature:click': function (e) {
                var path = '/genes/' + e.feature[e.featureType === 'gene' ? 'id' : 'geneId'];
                scope.$apply(function () {
                  $location.path(path).search({}).search({});
                });
              }
            }
          }),
          dataAdapter: new IcgcGeneAdapter({
            resource: 'gene',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            multiRegions: true,
            histogramMultiRegions: false,
            featureCache: {
              chunkSize: 100000
            }
          })

        });
        genomeViewer.addTrack(tracks.icgcGeneTrack);

        tracks.icgcMutationsTrack = new IcgcMutationTrack({
          title: 'ICGC Mutations',
          minHistogramRegionSize: 10000,
          maxLabelRegionSize: 3000,
          height: 100,
          functional_impact: _.get(LocationService.filters(), 'mutation.functionalImpact.is', ''),
          renderer: new FeatureRenderer({
            tooltipContainerID: '#genomic',
            label: function (f) {
              return f.id;
            },
            tooltipTitle: function (f) {
              return '<span class="gmtitle">' + gettextCatalog.getString('ICGC mutation') + ' - ' + f.id + '</span>';
            },
            tooltipText: function (f) {
              var consequences = GMService.tooltipConsequences(f.consequences), fi;
              fi = (f.functionalImpact && _.contains(f.functionalImpact, 'High')) ? 'High' : 'Low';

              return '<span class="gmkeys">' + gettextCatalog.getString('mutation:') + '&nbsp;</span>' + 
                f.mutation + '<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('reference allele:') + '&nbsp;</span>' + 
                f.refGenAllele + '<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('mutation type:') + '&nbsp;</span>' + 
                f.mutationType + '<br>'+
                '<span class="gmkeys">' + gettextCatalog.getString('project info:') + '</span><br>' + 
                f.projectInfo.join('<br>')+ '<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('consequences:') + '<br></span>' + 
                consequences + '<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('source:') + '&nbsp;</span>ICGC<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('start-end:') + '&nbsp;</span>' + 
                f.start + '-' + f.end + '<br>' +
                '<span class="gmkeys">' + gettextCatalog.getString('functional impact:') + '&nbsp;</span>' + fi;
            },
            color: function (feat) {
              switch (feat.mutationType) {
                case 'single base substitution':
                  return 'Chartreuse';
                case 'insertion of <=200bp':
                  return 'orange';
                case 'deletion of <=200bp':
                  return 'red';
                case 'multiple base substitution (>=2bp and <=200bp)':
                  return 'lightblue';
                default:
                  return 'black';
              }
            },
            infoWidgetId: 'mutationCds',
            height: 8,
            histogramColor: 'orange',
            handlers: {
              //'feature:mouseover': function (e) {
              //},
              'feature:click': function (e) {
                if (! e.feature.id) {
                  return;
                }
                scope.$apply(function () {
                  $location.path('/mutations/' + e.feature.id).search({});
                });
              }
            }
          }),
          dataAdapter: new IcgcMutationAdapter({
            resource: 'mutation',
            chromosomeLimitMap: gvConstants.CHROMOSOME_LIMIT_MAP,
            multiRegions: true,
            histogramMultiRegions: false,
            featureCache: {
              chunkSize: 10000
            }
          })
        });

        genomeViewer.addTrack(tracks.icgcMutationsTrack);
        /** End add tracks **/

        genomeViewer.draw();

        GenomeViewerController.initFullScreenHandler(genomeViewer);

        scope.$on('$locationChangeSuccess', function () {
          tracks.icgcMutationsTrack.functional_impact = _.get(LocationService.filters(),
            'mutation.functionalImpact.is', '');
          tracks.icgcMutationsTrack.functional_impact = _.get(LocationService.filters(),
            'mutation.functionalImpact.not', tracks.icgcMutationsTrack.functional_impact);
          tracks.icgcMutationsTrack.dataAdapter.clearData();
          tracks.icgcMutationsTrack.draw();
          
          tracks.icgcGeneTrack.functional_impact = _.get(LocationService.filters(),
            'mutation.functionalImpact.is', '');
          tracks.icgcGeneTrack.functional_impact = _.get(LocationService.filters(),
            'mutation.functionalImpact.not', tracks.icgcMutationsTrack.functional_impact);
          tracks.icgcGeneTrack.dataAdapter.clearData();
          tracks.icgcGeneTrack.draw();
        });
        scope.$on('gv:set:region', function (e, params) {
          genomeViewer.setRegion(params);
        });
        scope.$on('gv:zoom:set', function (e, val) {
          navigationBar._handleZoomSlider(val);
        });
        scope.$on('gv:reset', function (e) {
          navigationBar._handleRestoreDefaultRegion(e);
        });
        scope.$on('gv:autofit', function () {
          genomeViewer.toggleAutoHeight(true);
        });
        scope.$on('$destroy', function () {
          if (genomeViewer) {
            genomeViewer.destroy();
          }
        });
      }

      function update() {
        var region = attrs.region;
        if (!region) {
          return;
        }

        var regionObj = new Region();
        regionObj.parse(region);

        scope.isValidChromosome = GMService.isValidChromosome(regionObj.chromosome);
        if (!scope.isValidChromosome) {
          return;
        }

        var offset = (regionObj.end - regionObj.start) * 0.05;
        regionObj.start -= offset;
        regionObj.end += offset;

        if (! availableSpecies) {
          GenomeViewerController.getSpecies(function (s) {
            availableSpecies = s;
            setup(regionObj);
          });
        } else {
          setup(regionObj);
        }
      }

      attrs.$observe('region', update);
      update();

      });
      
    }
  };
});
