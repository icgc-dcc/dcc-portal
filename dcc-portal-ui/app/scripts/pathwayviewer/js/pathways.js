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

  var module = angular.module('icgc.pathways', [
    'icgc.enrichment.directives', 'icgc.sets.services'
  ]);
  
  module
    .constant('PathwaysConstants', {
      EVENTS: {
        MODEL_READY_EVENT: 'icgc.pathways.ready.event'
      }
    })
    .config(function ($stateProvider) {
      $stateProvider.state('pathways', {
        url: '/pathways/:id',
        templateUrl: '/scripts/pathwayviewer/views/pathways.html',
        controller: 'PathwaysController',
        resolve: {
          EnrichmentData: ['$q', '$stateParams', 'Restangular',
            
            function ($q, $stateParams, Restangular) {
              var entityID = $stateParams.id,
                deferred = $q.defer();
              
              Restangular.one('analysis/enrichment', entityID).get()
                .then(function (rectangularEnrichmentData) {
                    return deferred.resolve(rectangularEnrichmentData.plain());
                  },
                  function (response) {
                    return deferred.reject(response);
                  }
                );
              
              return deferred.promise;
            }]
        }
      });
    });
  
  
  module.controller('PathwaysController', function ($scope, $q, Page, EnrichmentData, Restangular,
                                                    GeneSetService, GeneSetHierarchy, GeneSets,
                                                    TooltipText, EnrichmentService, SetService, 
                                                    PathwaysConstants, RestangularNoCache, PathwayDataService, 
                                                    gettextCatalog) {


    var _selectedPathway = null;

    function _resolveEnrichmentData(Id) {
      $scope.analysis.isLoading = true;

      SetService.pollingResolveSetFactory(Id, 2000, 10)
        .setRetrievalEntityFunction(function () {
          return RestangularNoCache.one('analysis/enrichment', Id).get();
        })
        .setResolvedEntityFunction(function (entityData) {
          return entityData.state.toUpperCase() === 'FINISHED';
        })
        .resolve()
        .then(function (entityData) {
          _initAnalysisScope(entityData);

          if (!_selectedPathway) {
            return;
          }

          var _updateSelectedPathway = _.first(
            _.filter(entityData.results, function (pathway) {
              return pathway.geneSetId === _selectedPathway.geneSetId;
            })
          );

          _.assign(_selectedPathway, _updateSelectedPathway);
          $scope.analysis.isLoading = false;

        });
    }

    function _initAnalysisScope(entityRecords) {
      $scope.pathways = entityRecords.results;
      $scope.analysis = {
        getID: function () {
          return entityRecords.id;
        },
        getData: function () {
          return entityRecords;
        },
        getContext: function () {
          return 'pathways';
        }
      };
    }

    function _init() {
      Page.stopWork();
      Page.setPage('entity');
      Page.setTitle(gettextCatalog.getString('Enrichment Analysis Pathway Viewer'));

      $scope.TooltipText = TooltipText;

      _initAnalysisScope(EnrichmentData);
      $scope.analysis.isLoading = true;

      // Select the first gene set in the pathway as the
      // default value if one exists...
      var firstGenesetPathway = _.first($scope.pathways);

      if (firstGenesetPathway) {
        $scope.setSelectedPathway(firstGenesetPathway);
      }

      if (EnrichmentData.state !== 'FINISHED') {
        _resolveEnrichmentData(EnrichmentData.id);
      }
      else {
        $scope.analysis.isLoading = false;
      }

    }

    function _addFilters(pathway) {

      if (_.get(pathway, 'geneSetFilters', false)) {
        return;
      }

      pathway.geneSetFilters = EnrichmentService.geneSetFilters(EnrichmentData, pathway);
      pathway.geneSetOverlapFilters = EnrichmentService.geneSetOverlapFilters(EnrichmentData, pathway);

    }

    $scope.getSelectedPathway = function () {
      return _selectedPathway;
    };

    $scope.setSelectedPathway = function (pathway) {
      $scope.pathway = {};

      _addFilters(pathway);
      _selectedPathway = pathway;

      PathwayDataService.getPathwayData(pathway.geneSetId, pathway.geneSetOverlapFilters)
        .then(function (pathwayData) {
          $scope.geneSet = pathwayData.geneSet;
          $scope.pathway = _.pick(pathwayData, 'xml', 'zooms', 'mutationHighlights', 'drugHighlights', 'overlaps');
          $scope.uiParentPathways = pathwayData.uiParentPathways;

          setTimeout(function () {
            $scope.$broadcast(PathwaysConstants.EVENTS.MODEL_READY_EVENT, {});
          }, 100);
        })
        .catch(function () {
          $scope.pathway = {
            xml: '',
            zooms: [''],
            mutationHighlights: [],
            drugHighlights: []
          };
        });
    };
    // Initialize our controller and it's corresponding scope.
    _init();
  });
})();