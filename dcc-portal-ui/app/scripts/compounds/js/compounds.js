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

////////////////////////////////////////////////////////////////////////
// Primary Compound Module
////////////////////////////////////////////////////////////////////////
angular.module('icgc.compounds', ['icgc.compounds.controllers', 'icgc.compounds.services'])
  .config(function ($stateProvider) {
    $stateProvider.state('compound', {
      url: '/compound/:compoundId',
      templateUrl: 'scripts/compounds/views/compound.html',
      controller: 'CompoundCtrl as CompoundCtrl',
      resolve: {
        compoundManager: ['Page', '$stateParams', 'CompoundsService', 'gettextCatalog', 
        function (Page, $stateParams, CompoundsService, gettextCatalog) {

          Page.startWork();
          Page.setTitle(gettextCatalog.getString('Compounds'));
          Page.setPage('entity');
          return CompoundsService.getCompoundManagerFactory($stateParams.compoundId);
        }]
      },
      reloadOnSearch: true
    });
  });

angular.module('icgc.compounds.controllers', ['icgc.compounds.services'])
  .controller('CompoundCtrl', function ($scope, compoundManager, CompoundsService, Page,
                                        FilterService, CompoundsServiceConstants,
                                        $location, $timeout, gettextCatalog, $filter) {

    var _ctrl = this,
        _compound = null,
        _targetedCompoundGenes = [],
        _targetedCompoundGenesResultPerPage = 10,
        _targetGenes = [],
        _targetCompoundResultPage = 0,
        _targetedCompoundIds = [],
        _mutationalImpactFacets = null;

    // Defaults for client side pagination 
    _ctrl.currentGenesPage = 1;
    _ctrl.defaultGenesRowLimit = 10;
    _ctrl.currentClinicalTrailsPage = 1;
    _ctrl.defaultClinicalTrialsRowLimit = 10;
    _ctrl.rowSizes = [10, 25, 50];


    function getMutationImpactFacets() {
      var promise = compoundManager.getMutationImpactFacets();

      promise.then(function(data) {
        _mutationalImpactFacets = data.facets;
      });

      return promise;
    }

    function fixScroll() {
      var current = $location.hash();

      // If we are linked to the page and we have a hash, wait for render to finish and scroll to position. 
      $timeout(function () {
        if (current && current !== '#summary') {
          jQuery('body,html').stop(true, true);
          var offset = jQuery(current).offset();
          var to = offset.top - 40;
          jQuery('body,html').animate({ scrollTop: to }, 200);
        }
      }, 0);
    }

    function _init() {
      compoundManager.filters(FilterService.filters());

      _initCompound();
      /// ${compoundName} compound name
      Page.setTitle(_.template(gettextCatalog.getString('Compounds - ${compoundName}'))({compoundName: _compound.name.toUpperCase()}));
      Page.stopWork();
      fixScroll();
    }

    function _initTargetedCompoundGenes(targetGenes) {
      _targetedCompoundGenes = targetGenes;
      _targetGenes = getUiTargetedCompoundGenesJSON(_targetedCompoundGenes);
    }

    function _initCompound() {
      _compound = compoundManager.getCompound();

      compoundManager.getTargetedCompoundGenes(_targetCompoundResultPage)
        .then(_initTargetedCompoundGenes)
        .then(function() {
          return getMutationImpactFacets();
        })
        .finally(function() {
          $scope.$on(CompoundsServiceConstants.EVENTS.COMPOUND_DATA_NEEDS_RELOAD, _reloadCompound);
        });

    }

    function _reloadCompound() {
      Page.startWork();

      compoundManager.reloadCompoundGenes()
        .then(_initTargetedCompoundGenes)
        .then(function() {
          // We want to ensure the facets are loaded (and checked)
          // before we return control to the user - avoids unecessary confusion
          // as to why the facet is not checked.
          // TODO: We should revist the UI facet design to render the selection (checkmark) right away.
          return getMutationImpactFacets();
        })
        .finally(function() {
          Page.stopWork();
        });
    }

    // Creating a new Object for table filters
    function getUiTargetedCompoundGenesJSON(genes){
      return genes.map(function (gene) {
        return _.extend({}, gene, {
          uiId: gene.id,
          uiName: gene.name,
          uiSymbol: gene.symbol,
          uiLocation: 'chr' + gene.chromosome + ':' + gene.start +'-' + gene.end,
          uiType: $filter('trans')(gene.type),
          uiAffectedDonorCountFilter: gene.affectedDonorCountFilter,
          uiAffectedDonorCountFiltered: $filter('number')(gene.affectedDonorCountFiltered),
          uiAffectedDonorCountTotal: $filter('number')(_ctrl.getAffectedDonorCountTotal()),
          uiAffectedDonorCountTotalPercentage: $filter('number')((gene.affectedDonorCountFiltered/_ctrl.getAffectedDonorCountTotal() * 100), 2) + '%',
          uiMutationCountTotal: $filter('number')(gene.mutationCountTotal),
          uiMutationCountFilter : gene.mutationCountFilter
        });
      });
    }

    _init();


    //////////////////////////////////////////////////////////////////////
    // Controller API
    //////////////////////////////////////////////////////////////////////

    _ctrl.getMutationImpactFacets = function() {
      return _mutationalImpactFacets;
    };

    _ctrl.getTargetedCompoundGenesResultPerPage = function() {
      return Math.min(_targetedCompoundIds.length, _targetedCompoundGenesResultPerPage);
    };

    _ctrl.getPrettyExternalRefName = function(refName) {
      var referenceName = refName.toLowerCase();

      switch(referenceName) {
        case 'chembl':
          referenceName = 'ChEMBL';
          break;
        case 'drugbank':
          referenceName = 'Drugbank';
          break;
        default:
          break;
      }

      return referenceName;
    };

    _ctrl.getTargetedCompoundGenes = function() {
      return _targetGenes;
    };

    _ctrl.getTargetedGeneCount = function() {
      return _compound.genes.length;
    };

    _ctrl.getFilter = function() {
      var filter = compoundManager.getCompoundGenesFilter().filters;
      return filter;
    };

    _ctrl.getAffectedDonorCountTotal = function() {
      return compoundManager.getAffectedDonorCountTotal();
    };

    _ctrl.getAffectedDonorCountTotalFilter = function() {
      return compoundManager.filters();
    };

    _ctrl.getCompound = function() {
      return _compound;
    };

    // Watch to get affected donor count total once genes promise finishes loading
    $scope.$watch(function(){
      return compoundManager.getAffectedDonorCountTotal();
    }, function(){
      _targetGenes = getUiTargetedCompoundGenesJSON(_targetedCompoundGenes);
    });

  });

angular.module('icgc.compounds.services', ['icgc.genes.models'])
  .constant('CompoundsServiceConstants', {
    EVENTS: {
      COMPOUND_DATA_NEEDS_RELOAD: 'compound.service.event.compoundNeedsReload'
    }
  })
  .service('CompoundsService', function($rootScope, $q, Gene, Mutations, Page, FilterService, $location,
                                        Restangular, CompoundsServiceConstants, Extensions, $state) {

    function _arrayOrEmptyArray(arr) {
      return angular.isArray(arr) ?  arr : [];
    }

    function _compoundEntityFactory(compound) {
      var _id = compound.zincId,
          _inchikey = compound.inchikey,
          _name = compound.name,
          _synonyms = _arrayOrEmptyArray(compound.synonyms),
          _externalReferences = compound.externalReferences,
          _imageURL = compound.smallImageUrl || null,
          _drugClass = compound.drugClass || '--',
          _cancerTrialCount = compound.cancerTrialCount || '--',
          _atcCodes = _arrayOrEmptyArray(compound.atcCodes),
          _genes = _.filter(_.pluck(_arrayOrEmptyArray(compound.genes), 'ensemblGeneId'),
            function(id) {
              return id !== null && id.length > 0;
            }),
          _drugGenesLength = _genes.length,
          _trials = _arrayOrEmptyArray(compound.trials),
          _uiTrials = getUiTrialsJSON(compound.trials);

      return {
        id: _id,
        inchiKey: _inchikey,
        name: _name,
        synonyms: _synonyms,
        externalReferences: _externalReferences,
        imageURL: _imageURL,
        drugClass: _drugClass,
        cancerTrialCount: _cancerTrialCount,
        atcCodes: _atcCodes,
        genes: _genes,
        drugGenesLength: _drugGenesLength,
        trials: _trials,
        uiTrials: _uiTrials
      };
    }

    function _geneEntityFactory(geneData) {

      var _id = geneData.id,
          _type = geneData.type,
          _symbol = geneData.symbol,
          _name = geneData.name,
          _chromosome = geneData.chromosome,
          _start = geneData.start,
          _end = geneData.end,
          _affectedDonorCountFiltered = geneData.affectedDonorCountFiltered,
          _affectedDonorCountTotal = geneData.affectedDonorCountTotal;


      return {
        id: _id,
        type: _type,
        symbol: _symbol,
        name: _name,
        chromosome: _chromosome,
        start: _start,
        end: _end,
        affectedDonorCountFiltered: _affectedDonorCountFiltered,
        affectedDonorCountTotal: _affectedDonorCountTotal
      };

    }

    // Creating a new Object for table filters
    function getUiTrialsJSON(trials){
      return _arrayOrEmptyArray(trials.map(function (trial) {
        return _.extend({}, {
          uiCode: trial.code,
          uiDescription: trial.description,
          uiConditions: trial.conditions,
          uiStartDate: trial.startDate,
          uiPhaseName: trial.phaseName.split('/'),
          uiStatusName: trial.statusName
        });
      }));
    }

    var _srv = this;

    function CompoundManager(compoundId) {
      var _self = this,
          _compoundEntity = null,
          _compoundTargetedGenes = [],
          _compoundTargetedGeneIds = [],
          _affectedDonorCountTotal = 0,
          _geneEntityId = null,
          _filters = {};

      // For application/json format
      function _params2JSON(type, params) {
        var data = {};
        data.filters = params.filters;
        data.type = type.toUpperCase();
        data.name = params.name;
        data.description = params.description || '';
        data.size = params.size || 0;

        if (params.isTransient) {
          data.isTransient = params.isTransient;
        }

        // Set default sort values if necessary
        if (angular.isDefined(params.filters) && !angular.isDefined(params.sortBy)) {
          if (type === 'donor') {
            data.sortBy = 'ssmAffectedGenes';
          } else {
            data.sortBy = 'affectedDonorCountFiltered';
          }
          data.sortOrder = 'DESCENDING';
        } else {
          data.sortBy = params.sortBy;
          data.sortOrder = params.sortOrder;
        }
        data.union = params.union;
        return data;
      }

      function _reloadData() {
        var defer = $q.defer(),
          deferPromise = defer.promise;

        Restangular
          .one('drugs', compoundId)
          .get()
          .then(function(compound) {
            _compoundEntity = _compoundEntityFactory(compound.plain());
            defer.resolve(_self);
          }, function(error){
            if(error.status === 404){
              Page.stopWork();
              $state.go('404', {page: 'compound', id: compoundId, url: '/compound/:id'}, {location: false});
            }
          });

        return deferPromise;
      }

      function _createGeneEntitySet(params) {
        var deferred = $q.defer(),
            urlParams = _.extend({}, params),
            compoundGenes = _compoundEntity.genes;

        if (compoundGenes.length === 0 || _geneEntityId !== null) {
          deferred.resolve();
          return deferred.promise;
        }


        urlParams.filters = {
          gene: {
            id: {
              is: compoundGenes
            }
          }
        };

        urlParams.size = compoundGenes.length;
        urlParams.isTransient = true;
        urlParams.name = 'Input gene set';
        urlParams.description = '';
        urlParams.sortBy = 'affectedDonorCountFiltered';
        urlParams.sortOrder = 'DESCENDING';

        var data = _params2JSON('gene', urlParams);

          Restangular.one('entityset')
            .customPOST(data, undefined, {async:'false'}, {'Content-Type': 'application/json'})
            .then(function(data) {

              if (data.id) {
                _geneEntityId = data.id;
              }
              else {
                console.warn('Could not create gene entity set');
              }

              deferred.resolve();
            });

        return deferred.promise;
      }

      function _getResultsCompoundGenesFilter(geneLimit) {

        var filters = _filters;

        if (_geneEntityId) {

       if (! _.has(filters, 'gene')) {
            filters.gene = {
              id: {
                is: [Extensions.ENTITY_PREFIX + _geneEntityId]
              }
            };
       }

        }

       return  {
         from: 1,
         size: (geneLimit || 10),
         filters: filters
       };
      }

      function _reloadCompoundGenes(genePageIndex, geneLimit) {
        var deferred = $q.defer(),
            limit = geneLimit || _compoundEntity.drugGenesLength;

        _self.getCompoundMutations(genePageIndex, limit)
          .then(function (restangularMutationCountData) {
            var mutationCountData = restangularMutationCountData.plain(),
              geneCount = mutationCountData.length,
              mutationGeneValueMap = {};

            _compoundTargetedGeneIds.length = 0;

            if (geneCount === 0) {
              deferred.resolve(_compoundTargetedGenes);
              return;
            }

            for (var i = 0; i < geneCount; i++) {
              var mutationData = mutationCountData[i],
                geneId = _.get(mutationData, 'geneId', false);

              if (geneId) {
                _compoundTargetedGeneIds.push(geneId);
                mutationGeneValueMap[geneId] = +mutationData.mutationCount;
              }
            }

            var params = _getResultsCompoundGenesFilter(limit);

            Restangular
              .one('genes')
              .get(params)
              .then(function (geneList) {
                var geneListResults = _.get(geneList, 'hits', false);

                if (!geneListResults) {
                  deferred.resolve(_compoundTargetedGenes);
                }

                if (geneList.pagination.total !==  _compoundEntity.genes.length) {

                  // Validate genes against current Filters
                  var validGenes = _.pluck(geneListResults, 'id');

                  _compoundEntity.genes = validGenes;
                }

                var geneListResultsLength = geneListResults.length;
                _compoundTargetedGenes.length = 0;

                var geneFilter = _getResultsCompoundGenesFilter().filters;


                for (var i = 0; i < geneListResultsLength; i++) {
                  var gene = _geneEntityFactory(geneListResults[i]);

                  var filter = _.cloneDeep(geneFilter);
                  filter.gene.id  = {is: [gene.id]};

                  gene.mutationCountFilter = filter;

                  gene.affectedDonorCountFilter = filter;

                  gene.mutationCountTotal = mutationGeneValueMap[gene.id];

                  _compoundTargetedGenes.push(gene);
                }

                _compoundTargetedGenes = _.sortByOrder(_compoundTargetedGenes, 'affectedDonorCountFiltered', false);

                deferred.resolve(_compoundTargetedGenes);

                _self.getCompoundDonors();

              });


          });

        return deferred.promise;
      }

      function _getMutationImpactFacets() {
        var params = _getResultsCompoundGenesFilter();

        params.include =  angular.isArray(params.include) ? params.include : [];

        params.include.push('facets');
        params.facetsOnly = true;

        var promise = Mutations.getList(params);

        return promise;
      }

      _self.getResultsCompoundGenesFilter = _getResultsCompoundGenesFilter;

      _self.getCompoundDonors = function(geneLimit) {
        var params = _getResultsCompoundGenesFilter(geneLimit);

        params.include =  angular.isArray(params.include) ? params.include : [];

        params.include.push('facets');
        params.facetsOnly = true;

        return Restangular
          .one('donors')
          .get(params)
          .then(function(restangularDonorsList) {
            _affectedDonorCountTotal = _.get(restangularDonorsList, 'pagination.total', 0);
          });
      };

      _self.getCompoundMutations = function(geneStartIndex, geneLimit) {
        var params = _getResultsCompoundGenesFilter(geneLimit);
        delete params.from;
        
        return Restangular
            .one('drugs')
            .one(compoundId)
            .one('genes')
            .one('mutations')
            .one('counts')
            .get(params);
      };

      _self.getMutationImpactFacets = _getMutationImpactFacets;

      _self.getTargetedCompoundGeneIds = function() {
        return _compoundTargetedGeneIds;
      };

      _self.filters = function(filters) {

        if (arguments.length === 1) {
          _filters = angular.isObject(filters) ? filters : {};
        }

        return _.cloneDeep(_filters);
      };

      /* Reloads only the gene mutation data related to gene mutations - used for refining
      the compound targeted genes using filters */
      _self.reloadCompoundGenes = _reloadCompoundGenes;

      _self.getCompoundGenesFilter = _getResultsCompoundGenesFilter;

      _self.getTargetedCompoundGenes = function(genePageIndex, geneLimit) {

        var deferred = $q.defer();

        _createGeneEntitySet().then(function() {
            return _self.reloadCompoundGenes(genePageIndex, geneLimit);
        })
        .then(function(compoundTargetedGenes) {
           deferred.resolve(compoundTargetedGenes);
        });

        return deferred.promise;
      };

      _self.init = function() {

        $rootScope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {

          if (filterObj.currentPath.indexOf('/compound') >= 0) {

            if (! _.isEmpty(filterObj.currentFilters)) {
              _.assign(_filters, _getResultsCompoundGenesFilter().filters, filterObj.currentFilters);
            }
            else {
              _filters = {};
            }

            $rootScope.$broadcast(CompoundsServiceConstants.EVENTS.COMPOUND_DATA_NEEDS_RELOAD, _filters);
          }
        });

        return _reloadData();
      };

      /* Reloads all compound from scratch */
      _self.reloadData = _reloadData;

      _self.getTargetedGenes = function() {
        return _compoundTargetedGenes;
      };

      _self.getCompound = function() {
        return _compoundEntity;
      };

      _self.getGeneEntityID = function() {
        return _geneEntityId;
      };

      _self.getAffectedDonorCountTotal = function() {
        return _affectedDonorCountTotal;
      };

    }


    _srv.getCompoundManagerFactory = function(id) {
      var _compoundManager = new CompoundManager(id);
      return _compoundManager.init();
    };

    _srv.getCompoundsByGeneId = function (geneId) {
      return Restangular.one ('drugs').one ('genes').one (geneId).get({size: 500});
    };
    
    _srv.getCompoundsByGeneList = function (geneList) {
      return Restangular.all('drugs').one('genes', geneList).getList();
    };
    
    _srv.getCompoundsFromEntitySetId = function (entitySetId) {
      return Restangular.one('drugs').get({
        size: 10000, //TODO: Use paging to retrieve drugs instead of hard coding size limit
        filters: {gene:{id:{is:['ES:' + entitySetId]}}}
      });
    };
    _srv.getCompoundByZincId = function (zincId) {
      return Restangular.one('drugs').one(zincId).get();
    };
  });
