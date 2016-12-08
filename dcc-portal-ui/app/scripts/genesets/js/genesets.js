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

  var module = angular.module('icgc.genesets', ['icgc.genesets.controllers', 'ui.router']);
  
  

  module.config(function ($stateProvider) {
    $stateProvider.state('geneset', {
      url: '/genesets/:id',
      templateUrl: 'scripts/genesets/views/geneset.html',
      controller: 'GeneSetCtrl as GeneSetCtrl',
      resolve: {
        geneSet: ['$stateParams', 'GeneSets', 
        function ($stateParams, GeneSets) {
          return GeneSets.one($stateParams.id).get().then(function (geneSet) {
                return geneSet;
            });
          }]
        }
      });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.genesets.controllers', ['icgc.genesets.services', 'icgc.pathways']);
  
  /*jshint -W072 */
  module.controller('GeneSetCtrl',
    function ($scope, $timeout, $state, LocationService, HighchartsService, Page, GeneSetHierarchy, GeneSetService,
      GeneSetVerificationService, FiltersUtil, ExternalLinks, geneSet, PathwaysConstants, PathwayDataService, $filter) {


      var _ctrl = this, 
      geneSetFilter = {}; // Build adv query based on type

      // Defaults for client side pagination 
      _ctrl.tableFilter = {};
      _ctrl.currentCancerPage = 1;
      _ctrl.defaultCancerRowLimit = 10;
      _ctrl.rowSizes = [10, 25, 50];

      Page.setTitle(geneSet.id);
      Page.setPage('entity');

      _ctrl.geneSet = geneSet;
      _ctrl.geneSet.queryType = FiltersUtil.getGeneSetQueryType(_ctrl.geneSet.type);
      geneSetFilter[_ctrl.geneSet.queryType] = {is:[_ctrl.geneSet.id]};

      _ctrl.ExternalLinks = ExternalLinks;

      /**
       * Our function for keeping the page on the current section. 
       */
      $scope.fixScroll = function () {
        var current = jQuery('.current').children('a').attr('href');
        // We do not want to immediately scroll away from the controls on page load. 
        // Timeout of zero is used to ensure scroll happens after render. 
        $timeout(function() {
          if (current !== '#summary') {
            jQuery('body,html').stop(true, true);
            var offset = jQuery(current).offset();
            var to = offset.top - 40;
            jQuery('body,html').animate({scrollTop: to}, 200); 
          } 
        },0);
      };

      // Builds the project-donor distribution based on thie gene set
      // 1) Create embedded search queries
      // 2) Project-donor breakdown
      // 3) Project-gene breakdwon
      // 4) Build reactome pathways, if applicable
      function refresh() {
        $scope.fixScroll();
        var mergedGeneSetFilter = LocationService.mergeIntoFilters({gene:geneSetFilter});
        _ctrl.baseAdvQuery = mergedGeneSetFilter;

        _ctrl.uiParentPathways = GeneSetHierarchy.uiPathwayHierarchy(geneSet.hierarchy, _ctrl.geneSet);
        _ctrl.uiInferredTree = GeneSetHierarchy.uiInferredTree(geneSet.inferredTree);

        GeneSetService.getMutationCounts(mergedGeneSetFilter).then(function(count) {
          _ctrl.totalMutations = count;
        });

        GeneSetService.getGeneCounts(mergedGeneSetFilter).then(function(count) {
          _ctrl.totalGenes = count;
        });


        // Find out which projects are affected by this gene set, this data is used to generate cancer distribution
        // 1) Find the impacted projects: genesetId -> {projectIds} -> {projects}
        var geneSetProjectPromise = GeneSetService.getProjects(mergedGeneSetFilter);


        // 2) Add mutation counts
        geneSetProjectPromise.then(function(projects) {
          var ids, mutationPromise;
          if (! projects.hits || projects.hits.length === 0) {
            return;
          }

          ids = _.pluck(projects.hits, 'id');
          mutationPromise = GeneSetService.getProjectMutations(ids, mergedGeneSetFilter);

          mutationPromise.then(function(projectMutations) {
            projects.hits.forEach(function(proj) {
              proj.mutationCount = projectMutations[proj.id];
              proj.advQuery = LocationService.mergeIntoFilters({
                gene: geneSetFilter,
                donor: {projectId:{is:[proj.id]}, availableDataTypes:{is:['ssm']}}
              });
            });
          }).then(function(){
            _ctrl.geneSet.uiProjects = getUiProjectsJSON(projects.hits);
          });
        });


        // 3) Add donor counts, gene counts
        geneSetProjectPromise.then(function(projects) {
          var ids, donorPromise, genePromise;
          if (! projects.hits || projects.hits.length === 0) {
            return;
          }

          ids = _.pluck(projects.hits, 'id');

          donorPromise = GeneSetService.getProjectDonors(ids, mergedGeneSetFilter);
          genePromise = GeneSetService.getProjectGenes(ids, mergedGeneSetFilter);

          _ctrl.totalDonors = 0;

          donorPromise.then(function(projectDonors) {
            projects.hits.forEach(function(proj) {
              proj.affectedDonorCount = projectDonors[proj.id];
              proj.uiAffectedDonorPercentage = proj.affectedDonorCount / proj.ssmTestedDonorCount;
              _ctrl.totalDonors += proj.affectedDonorCount;
            });

            _ctrl.donorBar = HighchartsService.bar({
              hits: _.take(_.sortBy(projects.hits, function (p) {
                return -p.uiAffectedDonorPercentage;
              }), 10),
              xAxis: 'id',
              yValue: 'uiAffectedDonorPercentage'
            });
          });

          genePromise.then(function(projectGenes) {
            projects.hits.forEach(function(proj) {
              proj.geneCount = projectGenes[proj.id];
              proj.uiAffectedGenePercentage = proj.geneCount / _ctrl.geneSet.geneCount;
            });

            _ctrl.geneBar = HighchartsService.bar({
              hits: _.take(_.sortBy(projects.hits, function (p) {
                return -p.uiAffectedGenePercentage;
              }),10),
              xAxis: 'id',
              yValue: 'uiAffectedGenePercentage'
            });
          });

        });
  
        // 4) if it's a reactome pathway, get diagram
        if(_ctrl.geneSet.source === 'Reactome' && _ctrl.uiParentPathways[0]) {
          _ctrl.pathway = {}; // initialize pathway object

          PathwayDataService.getPathwayData(geneSet.id, null)
            .then(function (pathwayData) {
              _ctrl.pathway = _.pick(pathwayData, 'xml', 'zooms', 'mutationHighlights', 'drugHighlights');

              // Wait before rendering legend, 
              // Same approach taken in the pathway viewer page. 
              setTimeout(function () {
                $scope.$broadcast(PathwaysConstants.EVENTS.MODEL_READY_EVENT, {});
              }, 100);
          }).catch( function() {
            _ctrl.pathway = {
              xml: '',
              zooms: [''],
              mutationHighlights: [],
              drugHighlights: []
            };
          });
        }

        // Assign projects to controller so it can be rendered in the view
        geneSetProjectPromise.then(function(projects) {
          _ctrl.geneSet.projects = projects.hits || [];
        }).then(function(){
          _ctrl.geneSet.uiProjects = getUiProjectsJSON(_ctrl.geneSet.projects);
        });

        GeneSetService.getMutationImpactFacet(mergedGeneSetFilter).then(function(d) {
          _ctrl.mutationFacets = d.facets;
        });   
      }

      function getUiProjectsJSON(projects){
        return projects.map(function(project){
          return _.extend({}, {
            uiId: project.id,
            uiName: project.name,
            uiPrimarySite: project.primarySite,
            uiTumourType: project.tumourType,
            uiTumourSubtype: project.tumourSubtype,
            uiAffectedDonorPercentage: $filter('number')(project.uiAffectedDonorPercentage*100, 2),
            uiAdvQuery: project.advQuery,
            uiAffectedDonorCount: $filter('number')(project.affectedDonorCount),
            uiSSMTestedDonorCount: $filter('number')(project.ssmTestedDonorCount),
            uiMutationCount: $filter('number')(project.mutationCount),
            uiGeneCount: $filter('number')(project.geneCount),
            uiGeneSetCount: _ctrl.geneSet.geneCount,
            uiQueryType: _ctrl.geneSet.queryType,
            uiGeneSetId: _ctrl.geneSet.id,
            uiAffectedGenePercentage: $filter('number')(project.uiAffectedGenePercentage*100, 2)
          });
        });
      }

      $scope.$on('$locationChangeSuccess', function (event, dest) {
        if (dest.indexOf('genesets') !== -1) {
          refresh();
        }
      });

      refresh();
    });


  module.controller('GeneSetGenesCtrl', function ($scope, $timeout, LocationService, Genes, GeneSets, FiltersUtil) {
    var _ctrl = this, _geneSet = '', mergedGeneSetFilter = {};

    function success(genes) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(_geneSet.type);

      if (genes.hasOwnProperty('hits')) {
        _ctrl.genes = genes;
        if (_.isEmpty(_ctrl.genes.hits)) {
          return;
        }

        Genes.one(_.pluck(_ctrl.genes.hits, 'id').join(',')).handler.one('mutations',
          'counts').get({filters: mergedGeneSetFilter}).then(function (data) {
            _ctrl.genes.hits.forEach(function (g) {

              var geneFilter = { id:{is:[g.id]}};
              geneFilter[geneSetQueryType] = {is:[_geneSet.id]};

              g.mutationCount = data[g.id];
              g.advQuery = LocationService.mergeIntoFilters({
                gene: geneFilter
              });
            });
            // Timeout so that our scroll function gets called after render. 
            $timeout(function() {$scope.fixScroll()},0);
          });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('genesets');
        
      GeneSets.one().get().then(function (geneSet) {
        _geneSet = geneSet;
        mergedGeneSetFilter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: [geneSet.id]}}});
        Genes.getList({
          from: params.from,
          size: params.size,
          filters: mergedGeneSetFilter
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('GeneSetMutationsCtrl',
    function ($scope, $timeout, Mutations, GeneSets, Projects, LocationService, Donors, FiltersUtil, ProjectCache) {

    var _ctrl = this, geneSet;

    function success(mutations) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(geneSet.type);
      var geneFilter = {};
      geneFilter[geneSetQueryType] = {is:[geneSet.id]};

      if (mutations.hasOwnProperty('hits')) {
        var projectCachePromise = ProjectCache.getData();

        _ctrl.mutations = mutations;

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {
            Donors.getList({
              size: 0,
              include: 'facets',
              filters: LocationService.mergeIntoFilters({
                mutation: {id: {is: mutation.id}},
                gene: {geneSetId: {is: [geneSet.id] }}
              })
            }).then(function (data) {

              mutation.uiDonors = data.facets.projectId.terms;
              mutation.advQuery = LocationService.mergeIntoFilters({
                mutation: {id: {is: [mutation.id] }},
                gene: geneFilter
              });

              if (mutation.uiDonors) {
                mutation.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                    mutation: {id: {is: [mutation.id]}},
                    donor: {projectId: {is: [facet.term]}},
                    gene: geneFilter
                  });

                  projectCachePromise.then(function(lookup) {
                    facet.projectName = lookup[facet.term] || facet.term;
                  });

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });
              }
              // Timeout so that our scroll function gets called after render. 
              $timeout(function() {$scope.fixScroll()},0);            
            });
          });
        });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('mutationset');

      GeneSets.one().get().then(function (p) {
        geneSet = p;

        Mutations.getList({
          include: 'consequences',
          from: params.from,
          size: params.size,
          filters: LocationService.mergeIntoFilters({
            gene: {geneSetId: {is: [geneSet.id]}}
          })
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('GeneSetDonorsCtrl', function ($scope, LocationService, Donors, GeneSets, FiltersUtil) {
    var _ctrl = this, _geneSet, mergedGeneSetFilter;

    function success(donors) {
      var geneSetQueryType = FiltersUtil.getGeneSetQueryType(_geneSet.type);
      var geneFilter = {};
      geneFilter[geneSetQueryType] = {is:[_geneSet.id]};

      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;

        if (_.isEmpty(_ctrl.donors.hits)) {
          return;
        }

        Donors.one(_.pluck(_ctrl.donors.hits, 'id').join(',')).handler.one('mutations', 'counts').get({
          filters: mergedGeneSetFilter
        }).then(function (data) {
          _ctrl.donors.hits.forEach(function (d) {
            d.mutationCount = data[d.id];
            d.advQuery = LocationService.mergeIntoFilters({
              gene: geneFilter,
              donor: {id:{is:[d.id]}}
            });
          });
        });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('affectedDonors');

      GeneSets.one().get().then(function (geneSet) {
        _geneSet = geneSet;
        mergedGeneSetFilter = LocationService.mergeIntoFilters({gene: {geneSetId: {is: [geneSet.id]}}});
        Donors.getList({
          from: params.from,
          size: params.size,
          filters: mergedGeneSetFilter
        }).then(success);
      });
    }

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('genesets') !== -1) {
        refresh();
      }
    });

    refresh();
  });
})();