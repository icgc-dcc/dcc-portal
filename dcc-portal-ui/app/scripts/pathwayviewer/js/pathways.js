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
        MODEL_READY_EVENT:'icgc.pathways.ready.event'
      }
    })
    .config(function($stateProvider) {
		$stateProvider.state('pathways', {
			url: '/pathways/:entityID',
			templateUrl: '/scripts/pathwayviewer/views/pathways.html',
			controller: 'PathwaysController',
			resolve: {
                EnrichmentData: ['$q', '$stateParams', 'Restangular',

                function($q, $stateParams, Restangular) {
                    var entityID = $stateParams.entityID,
                        deferred = $q.defer();

                    Restangular.one('analysis/enrichment', entityID).get()
                      .then(function(rectangularEnrichmentData) {
                            return deferred.resolve(rectangularEnrichmentData.plain());
                          },
                          function(response) {
                            return deferred.reject(response);
                          }
						          );

                    return deferred.promise;
                }]
            }
		});
	});
	

	module.controller('PathwaysController', function($scope, $q, Page, EnrichmentData, Restangular,
		GeneSetService, GeneSetHierarchy, GeneSets, GeneSetVerificationService, TooltipText, LocationService,
    EnrichmentService, SetService, PathwaysConstants, RestangularNoCache) {
				
		
		var _selectedPathway = null;

    function _resolveEnrichmentData(Id) {
      $scope.analysis.isLoading = true;

      SetService.pollingResolveSetFactory(Id, 2000, 10)
        .setRetrievalEntityFunction(function() {
          return RestangularNoCache.one('analysis/enrichment', Id).get();
        })
        .setResolvedEntityFunction(function(entityData){
          return entityData.state.toUpperCase() === 'FINISHED';
        })
        .resolve()
        .then(function (entityData) {

          _initAnalysisScope(entityData);

          if (! _selectedPathway) {
            return;
          }

          var _updateSelectedPathway = _.first(
            _.filter(entityData.results, function(pathway) {
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
        getID: function() {
          return entityRecords.id;
        },
        getData: function() {
          return entityRecords;
        },
        getContext: function() {
          return 'pathways';
        }
      };
    }
	
		function _init() {
			Page.stopWork();
			Page.setPage('entity');
			Page.setTitle('Enrichment Analysis Pathway Viewer');

      $scope.TooltipText = TooltipText;

      _initAnalysisScope(EnrichmentData);
      $scope.analysis.isLoading = true;

      // Select the first gene set in the pathway as the
      // default value if one exists...
      var firstGenesetPathway = _.first($scope.pathways);

      if ( firstGenesetPathway ) {
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
		
		$scope.getSelectedPathway = function() {
      return _selectedPathway;
    };

		$scope.setSelectedPathway = function(pathway) {			
			$scope.pathway = {};
      
      _addFilters(pathway);
      
			_selectedPathway = pathway; 
     
      var id = pathway.geneSetId;
      
      
      var _geneSet = null,
        _pathwayId = null,
        _parentPathwayId = null,
        _uiParentPathways = null,
        _uniprotIds = null,
        _xml = null,
        _zooms = [''],
        _pathwayHighlights = [],
        _geneOverlapExistsHash = {};
        
        
        
        
       Restangular.one('genesets').one(id).get()
       .then(function(geneSet){
          _geneSet = geneSet;
          _uiParentPathways = GeneSetHierarchy.uiPathwayHierarchy(_geneSet.hierarchy, _geneSet);
          _geneSet.showPathway = true;
          _pathwayId = _uiParentPathways[0].diagramId;
          _parentPathwayId= _uiParentPathways[0].geneSetId;
       })
       .then(function() {
         var deferred = $q.defer();
        
         GeneSetService.getPathwayXML(_pathwayId)  
          .then(function(xml) {
              _xml = xml;
              deferred.resolve();
          }).catch(function() {
            $scope.pathway = {xml: '', zooms: [''], highlights: [] };
          });
          
         return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
          
         // If the diagram itself isnt the one being diagrammed, get list of stuff to zoom in on
          if(_pathwayId !== _parentPathwayId) {
            GeneSetService.getPathwayZoom(_parentPathwayId).then(function(data) {
                _zooms = data;
                 deferred.resolve();
            });
          } 
          else {
            _zooms = [''];
            deferred.resolve();
          }
          
          return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
         
   
          GeneSetService.getPathwayProteinMap(_parentPathwayId, []).then(function(map) {
            
              // Normalize into array
                  _.forEach(map,function(value,id) {

                    if(value && value.dbIds) {
                      var dbIds = value.dbIds.split(',');

                      _pathwayHighlights.push({
                        uniprotId:id,
                        dbIds: dbIds,
                        value:value.value
                      });
                    }
                  });

              //console.log(_geneOverlapExistsHash);
              // Get ensembl ids for all the genes so we can link to advSearch page
              _uniprotIds = _.pluck(_pathwayHighlights, 'uniprotId');
              deferred.resolve();
          });
           return deferred.promise;
       })
       .then(function(){
         var deferred = $q.defer();

         Restangular.one('genes').get({filters: pathway.geneSetOverlapFilters})
           .then(function(geneListData) {

             // Check if there are hits (i.e. any gene overlap) - otherwise resolve the promise
             if (! _.get(geneListData,'hits[0]')) {
               deferred.resolve();
               return;
             }

             var pathwayGeneID = 'externalDbIds.uniprotkb_swissprot',
               geneList = [];

             _.forEach(geneListData.hits, function(gene) {
               var overlapGeneExternalIDs = _.get(gene, pathwayGeneID, false); /*, overlapGene = {},*/

               // We can't join by this ID (with the pathway viewer) so throw away
               if( ! overlapGeneExternalIDs) {
                 return;
               }

               // Create auxiliary gene object that we can later reference
               /*var  overlapGene = {
                geneId: gene.id,
                name: gene.name,
                geneSymbol: gene['symbol']
                };

               _.forEach(overlapGeneExternalIDs, function(dbID) {
                 _geneOverlapExistsHash[dbID] = overlapGene;
               });*/
                //console.log(overlapGene);
               geneList.push.apply(geneList, overlapGeneExternalIDs);

             });

             _.forEach(geneList, function(g) {
               _geneOverlapExistsHash[g] = true;
             });



             //console.log('Overlap genes returned: ' +
             // _.keys(_geneOverlapExistsHash).length, '\nProtein Map Intersect Overlap Length = ' +
            //  (_.intersection(_.values(_uniprotIds), _.keys(_geneOverlapExistsHash)).length) );
             deferred.resolve();

           });
         return deferred.promise;
       })
       .then(function() {
          var deferred = $q.defer();
          
          GeneSetVerificationService.verify(_uniprotIds.join(',') )
          .then(function(data) {

            var geneCount = 0;

            _.forEach(_pathwayHighlights,function(n){
                var geneKey = 'external_db_ids.uniprotkb_swissprot';
                if (! data.validGenes[geneKey]) {
                  return;
                }
      
                var uniprotObj = data.validGenes[geneKey][n.uniprotId];
                if(!uniprotObj){
                  return;
                }
                var ensemblId = uniprotObj[0].id;
                n.advQuery =  LocationService.mergeIntoFilters({
                  gene: {
                    id:  {is: [ensemblId]}
                  }
                });
                n.geneSymbol = uniprotObj[0].symbol;
                n.geneId = ensemblId;

              if (angular.isDefined(_geneOverlapExistsHash[n.uniprotId])) {
                geneCount++;

                _.forEach(n.dbIds, function(dbID) {
                  // Swap in Reactome keys but maintain the id we use this to determine overlaps in O(1)
                  // later... The dbID is used as a reference to the reactome SVG nodes...
                  _geneOverlapExistsHash[dbID] = {id: n.uniprotId, geneId: n.geneId, geneSymbol: n.geneSymbol};
                });
                delete _geneOverlapExistsHash[n.uniprotId];
              }
            });
           console.log(geneCount + ' Overlapped genes validated! ');
            deferred.resolve();
          });
          
          return deferred.promise;
       })
       .then(function(){
         $scope.geneSet = _geneSet;
         $scope.pathway = {xml: _xml, zooms: _zooms, highlights: _pathwayHighlights, overlaps: _geneOverlapExistsHash};
         $scope.uiParentPathways = _uiParentPathways;

         setTimeout(function() {
           $scope.$broadcast(PathwaysConstants.EVENTS.MODEL_READY_EVENT, {});
         }, 100);

         //console.log($scope.pathway);
       });
 
		};	
		
		// Initialize our controller and it's corresponding scope.
		_init();
		
	});
})();