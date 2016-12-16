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

import './entityset.persistence.dropdown/entityset.persistence.dropdown.js';
import './entityset.persistence.modals';

angular.module('icgc.advanced', [
  'icgc.advanced.controllers',
  'ui.router',
  'entityset.persistence.dropdown',
  'entityset.persistence.modals',
  ])
  .config(function ($stateProvider) {
    $stateProvider.state('advanced', {
      url: '/search?filters',
      data: {
        tab: 'donor', isAdvancedSearch: true
      },
      reloadOnSearch: false,
      templateUrl: '/scripts/advanced/views/advanced.html',
      controller: 'AdvancedCtrl as AdvancedCtrl'
    });
    $stateProvider.state('advanced.gene', {
      url: '/g',
      reloadOnSearch: false,
      data: {tab: 'gene', isAdvancedSearch: true}
    });
    $stateProvider.state('advanced.mutation', {
      url: '/m',
      reloadOnSearch: false,
      data: {tab: 'mutation', subTab: 'mutation', isAdvancedSearch: true}
    });
    $stateProvider.state('advanced.mutation.occurrence', {
      url: '/o',
      reloadOnSearch: false,
      data: {subTab: 'occurrence', isAdvancedSearch: true}
    });
  });


(function () {

  var _locationFilterCache = null;

angular.module('icgc.advanced.controllers', [
    'icgc.advanced.services', 'icgc.sets.services', 'icgc.facets'])
    .controller('AdvancedCtrl',
    function ($scope, $rootScope, $state, $modal, Page, AdvancedSearchTabs, LocationService, AdvancedDonorService, // jshint ignore:line
              AdvancedGeneService, AdvancedMutationService, SetService, CodeTable, Restangular, FilterService,
              RouteInfoService, FacetConstants, Extensions, SurvivalAnalysisLaunchService, gettextCatalog) {

      var _controller = this,
          dataRepoRouteInfo = RouteInfoService.get ('dataRepositories'),
          dataRepoUrl = dataRepoRouteInfo.href,
          _serviceMap = {},
          _filterService = LocationService.getFilterService();

      _locationFilterCache = _filterService.getCachedFiltersFactory();

      var _isInAdvancedSearchCtrl = true;
      
      // NOTE: using IDs instead of storing references to entities b/c pagination results in new objects  
      this.selectedEntityIdsMap = {};
      this.toggleSelectedEntity = (entityType, entity) => { this.selectedEntityIdsMap[entityType] = _.xor((this.selectedEntityIdsMap[entityType] || []), [entity.id]) };
      this.isEntitySelected = (entityType, entity) =>  this.selectedEntityIdsMap[entityType] && this.selectedEntityIdsMap[entityType].includes(entity.id);
      this.handleOperationSuccess = (entityType) => { this.selectedEntityIdsMap[entityType] = [] };

      _controller.donorSets = _.cloneDeep(SetService.getAllDonorSets());
      _controller.geneSets = _.cloneDeep(SetService.getAllGeneSets());
      _controller.mutationSets = _.cloneDeep(SetService.getAllMutationSets());

      // to check if a set was previously selected and if its still in effect
      const updateSetSelection = (entity, entitySets) => {
        let filters = _locationFilterCache.filters();

        entitySets.forEach( (set) =>
          set.selected = filters[entity] && filters[entity].id &&  _.includes(filters[entity].id.is, `ES:${set.id}`)
        );
      };

      function _refresh() {
        var filters = _locationFilterCache.filters(),
            _services = [
              { 'service': _controller.Donor, id: 'donor', startRunTime: null },
              { 'service': _controller.Gene, id: 'gene', startRunTime: null },
              { 'service': _controller.Mutation, id: 'mutation', startRunTime: null }
            ],
            refreshOrder = [];

        // Based on the tab we are on make sure we exec
        // our refreshes in the correct order.
        switch (_controller.getActiveTab()) {
          case 'mutation':
            refreshOrder = _services.reverse();
            break;
          case 'gene':
            refreshOrder = [
              _services[1],
              _services[0],
              _services[2]
            ];
            break;
          default: // donor
            refreshOrder = _services;
            break;
        }
        // Set the first controller request to be fired so we can use it in later contexts
        refreshOrder[0].controllerUIActive = true;

        // Handy function used to perform our refresh requests
        // and unblock when certain conditions are met
        function _execRefresh(serviceObj) {

          var service = serviceObj.service;

          if (_.get(service, 'isFacetsInitialized', false)) {
            return true;
          }


          serviceObj.startRunTime = new Date().getTime();

          var refreshPromise = service.init.apply(_controller);

          serviceObj.promiseCount = ++_promiseCount;

          refreshPromise.then(
            function () {

              var nowTime = new Date().getTime(),
                  timeDelta = nowTime - _workStartTime;

              _totalMSElapsed += timeDelta;

              // If we have resolved all our promises in under _MAX_REFRESH_BLOCK_TIME
              // or we have waitied at least _MAX_REFRESH_BLOCK_TIME before
              // the first resolve then unblock...
              if ( ( _pageUnblockedTime === null &&
                    (_promiseCount === _refreshServicesLength ||
                    _totalMSElapsed >= _MAX_REFRESH_BLOCK_TIME)
                  ) ) {

                _pageUnblockedTime = nowTime;
              }

              serviceObj.service.isFacetsInitialized = true;

              // Prevent rendering the main tab if the hits (i.e. it has been rendered previously)
              // this can occur if one of our current tab watchers fire before this _refresh service
              // is invoked. The tab watcher fires the render function but the tab data (i.e. facets) may not
              // yet be fully initialized yet.
              if (serviceObj.controllerUIActive && ! _.get(serviceObj, 'service.isHitsInitialized', false)) {
                _renderTab(_controller.getActiveTab());
                _controller.loadingFacet = false;
              }

            });

          return refreshPromise;
        }

        var _workStartTime = null,
            _promiseCount = 0,
            _totalMSElapsed = 0,
            _pageUnblockedTime = null,
            _MAX_REFRESH_BLOCK_TIME = 500, // Block for 500ms max
            _nonRefreshedServices = _.filter(refreshOrder, function(serviceObj) {
              return _.get(serviceObj, 'service.isFacetsInitialized', false) === false ;
            }),
          _refreshServicesLength = _nonRefreshedServices.length;

        // Reset our refresh variables before executing the refresh
        _pageUnblockedTime = null;
        _totalMSElapsed = 0;
        _promiseCount = 0;
        _workStartTime = new Date().getTime();


        if (_refreshServicesLength > 0) {
          _.forEach(_nonRefreshedServices, function (refreshServiceObj) {
            _execRefresh(refreshServiceObj);
          });
        }

        _controller.hasGeneFilter = angular.isObject(filters) ?  filters.hasOwnProperty('gene') : false;
      }

      function _renderTab(tab, forceFullRefresh) {
        var service = null;

        switch(tab) {
          case 'donor':
          case 'gene':
            service = _serviceMap[tab];
            break;
          default:
            service = _serviceMap.mutation;
            break;
        }

        if (service.isHitsInitialized === true && forceFullRefresh !== true) {
          return;
        }

        // Force a facet pull and rerender in the case that this method was being called before the _refresh
        // function has had a chance to initialize the facets.
        if (forceFullRefresh === true || ! _.get(service, 'isFacetsInitialized' , false)) {
          _resetService(service);
          _refresh();
        }
        else if (service.isFacetsInitialized) {
          service.isHitsInitialized = true;
          service.renderBodyTab();
        }

      }

      function _resetService(service) {
        service.isFacetsInitialized = false;
        service.isHitsInitialized = false;
      }

      function _resetServices() {
        var serviceIds = _.keys(_serviceMap);

        for (var i = 0; i < serviceIds.length; i++) {
          _resetService(_serviceMap[serviceIds[i]]);
        }

      }


      function _init() {

        Page.setTitle(gettextCatalog.getString('Advanced Search'));
        Page.setPage('advanced');

        // Force a refresh of the tabs for good measure!
        // Useful if we are being directed to the Advanced Search
        // form a different part of the application
        _resetServices();
        _refresh();

        // Setup
        _controller.setActiveTab($state.current.data.tab);
        _controller.setSubTab($state.current.data.subTab);

        $scope.$watch(function() {
          var queryParams = LocationService.search(),
              pagingTypes = ['donors', 'genes', 'mutations', 'occurrences'];

          var str = _.reduce(pagingTypes, function(concatStr, type) {
            return concatStr + (queryParams[type] ?  (type + queryParams[type]) : '');
          }, '');

          return str;
        },
        function(newVal, oldVal) {

          if (newVal !== oldVal) {
            _renderTab(_controller.getActiveTab(), true);
          }

        });

        $scope.$on(_filterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {
          if (filterObj.currentPath.indexOf('/search') < 0) {
            // Unfortunately this event fired before a state change notification is posted so this
            // provides a better why to determine if we need to abort requests that have been made.
            return;
          }

          _locationFilterCache.updateCache();
          updateSetSelection('donor', _controller.donorSets);
          updateSetSelection('gene', _controller.geneSets);
          updateSetSelection('mutation', _controller.mutationSets);
          _resetServices();
          _refresh();
        });

        $rootScope.$on('$stateChangeStart', function(e, toState) {

          _isInAdvancedSearchCtrl = _.get(toState, 'data.isAdvancedSearch', false) ? true : false;

          if (_isInAdvancedSearchCtrl) {
            _locationFilterCache.updateCache();
          }
        });

        $rootScope.$on(FacetConstants.EVENTS.FACET_STATUS_CHANGE, function(event, facetStatus) {
          if (! facetStatus.isActive) {
            return;
          }

          _controller.loadingFacet = true;

        });

        $rootScope.$on(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, () => {
          _controller.donorSets = _.cloneDeep(SetService.getAllDonorSets());
          _controller.geneSets = _.cloneDeep(SetService.getAllGeneSets());
          _controller.mutationSets = _.cloneDeep(SetService.getAllMutationSets());
        });

        // Tabs need to update when using browser buttons
        // Shouldn't have to worry about refreshing data here
        // since you cannot change tabs and filters in one movement
        // ... actually you can by clicking on counts in the tables
        // $scope.$on('$locationChangeSuccess') should take care of that anyway
        $scope.$watch(function () {
            var stateData = angular.isDefined($state.current.data) ? $state.current.data : null;

            if (! stateData ||
                ! angular.isDefined(stateData.isAdvancedSearch) ||
                ! angular.isDefined(stateData.tab)) {
              return null;
            }

            return stateData.tab;
          },
          function (tab) {
             if (tab !== null) {
               _controller.setActiveTab(tab);
             }
          });

        $scope.$watch(function () {
          var stateData = angular.isDefined($state.current.data) ? $state.current.data : null;

          if (! stateData ||
              ! angular.isDefined(stateData.isAdvancedSearch) ||
              ! angular.isDefined(stateData.subTab)) {
            return null;
          }

          return stateData.subTab;
        },
        function (subTab) {
          if (subTab !== null) {
            _controller.setSubTab(subTab);
          }
        });

        updateSetSelection('donor', _controller.donorSets);
        updateSetSelection('gene', _controller.geneSets);
        updateSetSelection('mutation', _controller.mutationSets);
      }

      /////////////////////////////////////////////////////////////////
      // Advanced Search Public API
      /////////////////////////////////////////////////////////////////

      _controller.Page = Page;
      _controller.state = AdvancedSearchTabs;

      _controller.dataRepoTitle = dataRepoRouteInfo.title;

      _controller.Donor = AdvancedDonorService;
      _controller.Gene = AdvancedGeneService;
      _controller.Mutation = AdvancedMutationService;

      _serviceMap = {
        donor: _controller.Donor,
        gene: _controller.Gene,
        mutation: _controller.Mutation
      };

      _controller.Location = LocationService;

      //

      _controller.downloadDonorData = function() {
        $modal.open({
          templateUrl: '/scripts/downloader/views/request.html',
          controller: 'DownloadRequestController',
          resolve: {
            filters: function() { return undefined }
          }
        });
      };

      _controller.saveSet = function(type, limit) {
        _controller.setLimit = limit;
        _controller.setType = type;

        $modal.open({
          templateUrl: '/scripts/sets/views/sets.upload.html',
          controller: 'SetUploadController',
          resolve: {
            setType: function() {
              return _controller.setType;
            },
            setLimit: function() {
              return _controller.setLimit;
            },
            setUnion: function() {
              return undefined;
            },
            selectedIds: function() {
              return undefined;
            }
          }
        });
      };

      _controller.viewExternalDonors = function(limit) {
        var params = {
          filters: _locationFilterCache.filters(),
          size: limit,
          isTransient: true,
          name: 'Input donor set',
          sortBy: 'ssmAffectedGenes',
          sortOrder: 'DESCENDING',
        };

        // Ensure scope is destroyed as there may be unreferenced watchers on the filter. (see: facets/tags.js)
        $scope.$destroy();

        SetService.createEntitySet('donor', params)
          .then(function (set) {
            invariant(set.id, 'Response from SetService.createEntitySet did not include an id!');
            var newFilter = JSON.stringify({file: {donorId: {is: [Extensions.ENTITY_PREFIX + set.id]}}});
            LocationService.goToPath(dataRepoUrl, {filters: newFilter});
          });
      };

      /**
       * Create new enrichment analysis
       */
      _controller.enrichmentAnalysis = function(limit) {
        $modal.open({
          templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
          controller: 'EnrichmentUploadController',
          resolve: {
            geneLimit: function() {
              return limit;
            },
            filters: function() {
              return undefined;
            }
          }
        });
      };

      _controller.oncogridAnalysis = function(){
        $modal.open({
          templateUrl: '/scripts/oncogrid/views/oncogrid.upload.html',
          controller: 'OncoGridUploadController',
          resolve: {
            donorsLimit: () => _controller.Donor.donors.pagination.total,
            genesLimit: () => _controller.Gene.genes.pagination.total,
            filters: () => LocationService.filters()
          }
        });
      };

      _controller.setActiveTab = function (tab) {
        _controller.state.setTab(tab);
        _renderTab(tab);
      };

      _controller.getActiveTab = function() {
        return _controller.state.getTab();
      };

      _controller.setSubTab = function (tab) {
        _controller.state.setSubTab(tab);
      };

      /**
       * View observation/experimental details
       */
      _controller.viewObservationDetail = function(observation) {
        $modal.open({
          templateUrl: '/scripts/advanced/views/advanced.observation.popup.html',
          controller: 'ObservationDetailController',
          resolve: {
            observation: function() {
              return observation;
            }
          }
        });
      };

        /**
       * Run Survival/Phenotype analysis
       */
      _controller.launchSurvivalAnalysis = (entityType, entityId, entitySymbol) => {
        var filters = LocationService.filters();
        SurvivalAnalysisLaunchService.launchSurvivalAnalysis(entityType, entityId, entitySymbol, filters);
      };

      _init();
    })
   // Container to observation popup
  .controller('ObservationDetailController', function($scope, $modalInstance, observation) {
    $scope.observation = observation;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  })
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // AdvancedDonorService, AdvancedGeneService & AdvancedMutationService are essentially Strategy Pattern
  // (without the proper language level enforcement (thanks ES5 :S) objects that have a common
  // interface for facet initialization via <service>.init(), and hits initialization via <service>.renderBodyTab()
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  .service('AdvancedDonorService', // Advanced Donor Service
    function(Page, LocationService, HighchartsService, Donors, AdvancedSearchTabs, Extensions, $q) {

      var _ASDonorService = this;

      function _initFacets(facets) {
        _ASDonorService.pieProjectId = HighchartsService.pie({
          type: 'donor',
          facet: 'projectId',
          facets: facets
        });
        _ASDonorService.piePrimarySite = HighchartsService.pie({
          type: 'donor',
          facet: 'primarySite',
          facets: facets
        });
        _ASDonorService.pieGender = HighchartsService.pie({
          type: 'donor',
          facet: 'gender',
          facets: facets
        });
        _ASDonorService.pieTumourStage = HighchartsService.pie({
          type: 'donor',
          facet: 'tumourStageAtDiagnosis',
          facets: facets
        });
        _ASDonorService.pieVitalStatus = HighchartsService.pie({
          type: 'donor',
          facet: 'vitalStatus',
          facets: facets
        });
        _ASDonorService.pieStatusFollowup = HighchartsService.pie({
          type: 'donor',
          facet: 'diseaseStatusLastFollowup',
          facets: facets
        });
        _ASDonorService.pieRelapseType = HighchartsService.pie({
          type: 'donor',
          facet: 'relapseType',
          facets: facets
        });
        _ASDonorService.pieAge = HighchartsService.pie({
          type: 'donor',
          facet: 'ageAtDiagnosisGroup',
          facets: facets
        });
        _ASDonorService.pieDataTypes = HighchartsService.pie({
          type: 'donor',
          facet: 'availableDataTypes',
          facets: facets
        });
        _ASDonorService.pieAnalysisTypes = HighchartsService.pie({
          type: 'donor',
          facet: 'analysisTypes',
          facets: facets
        });
      }

      function _processDonorHits() {

        if (! _.get(_ASDonorService, 'donors.hits.length', false)) {
          return;
        }

        _ASDonorService.mutationCounts = null;

        var filters = _locationFilterCache.filters();
        if (_.has(filters, 'donor.id')) {
          delete filters.donor.id;
        }
        Donors
          .one(_.pluck(_ASDonorService.donors.hits, 'id').join(','))
          .handler
          .one('mutations', 'counts')
          .get({filters: filters})
          .then(function (counts) {
            _ASDonorService.mutationCounts = counts;
          });

      }

      function _initDonors() {
        var params = LocationService.getJqlParam('donors'),
            filters = _locationFilterCache.filters() || {},
            deferred = $q.defer();

        params.filters = filters;
        _ASDonorService.isLoading = true;


        Donors.getList(params).then(function(hitsDonorList) {
          _ASDonorService.donors.hits = hitsDonorList.hits;
          _ASDonorService.donors.pagination = hitsDonorList.pagination;
          _ASDonorService.hitsLoaded = true;


          _ASDonorService.donors.hits.forEach(function (donor) {
            donor.embedQuery = LocationService.merge(filters, {donor: {id: {is: [donor.id]}}}, 'facet');

            // Remove donor entity set because donor id is the key
            if (donor.embedQuery.hasOwnProperty('donor')) {
              var donorFilters = donor.embedQuery.donor;
              delete donorFilters[Extensions.ENTITY];
            }

            // Proper encode
            donor.embedQuery = encodeURIComponent(JSON.stringify(donor.embedQuery));

          });
        })
        .finally(function() {
          _ASDonorService.isLoading = false;
          deferred.resolve();
        });

      return deferred.promise;
    }

    ///////////////////////////////////////////////////////////////////////
    // Donor Public API
    ///////////////////////////////////////////////////////////////////////
    _ASDonorService.init = function () {

      var deferred = $q.defer();

      _ASDonorService.isLoading = true;

      if (angular.isDefined(_ASDonorService.donors)) {
        _ASDonorService.donors.hits = [];
        _ASDonorService.hitsLoaded = false;
      }

      var params = LocationService.getJqlParam('donors');

      params.include = 'facets';
      params.facetsOnly = true;
      params.filters = _locationFilterCache.filters();

      Donors.getList(params).then(function (facetDonorList) {
        _ASDonorService.isLoading = false;

        _initFacets(facetDonorList.facets);

        _ASDonorService.donors = facetDonorList.plain(); // Build the partial object

        deferred.resolve();
      });

      return deferred.promise;
    };



    _ASDonorService.renderBodyTab = function () {
        _initDonors().then(_processDonorHits);
    };

  })
  .service('AdvancedGeneService', // Advanced Donor Service
    function(Page, LocationService, Genes, Projects, Donors, AdvancedSearchTabs,
             FiltersUtil, Extensions, ProjectCache, $q) {

      var _ASGeneService = this;

      _ASGeneService.projectGeneQuery = function(projectId, geneId) {
        var filters = _locationFilterCache.filters() || {};

        if (filters.hasOwnProperty('gene')) {
          delete filters.gene.id;
          delete filters.gene[Extensions.ENTITY];
        }
        if (filters.hasOwnProperty('donor')) {
          delete filters.donor.projectId;
        }

        if (filters.hasOwnProperty('gene') === false) {
          filters.gene = {};
        }

        if (filters.hasOwnProperty('donor') === false) {
          filters.donor = {};
        }

        filters.gene.id = { is: [geneId] };
        filters.donor.projectId = { is: [projectId] };

        return encodeURIComponent(JSON.stringify(filters));
      };


    function _processGeneHits() {

      if (! _.get(_ASGeneService, 'genes.hits.length', false)) {
        return;
      }

      _ASGeneService.mutationCounts = null;

      var geneIds = _.pluck(_ASGeneService.genes.hits, 'id').join(',');
      var projectCachePromise = ProjectCache.getData();


      var filters = _locationFilterCache.filters();
      if (_.has(filters, 'gene.id')) {
        delete filters.gene.id;
      }
      // Get Mutations counts
      Genes.one(geneIds).handler
        .one('mutations', 'counts')
        .get({filters: filters})
        .then(function (data) {
          _ASGeneService.mutationCounts = data;
        });


      // Need to get SSM Test Donor counts from projects
      Projects.getList().then(function (projects) {
        _ASGeneService.genes.hits.forEach(function (gene) {

          var geneFilter = _locationFilterCache.filters();
          if (geneFilter.hasOwnProperty('gene')) {
            delete geneFilter.gene[ Extensions.ENTITY ];
            delete geneFilter.gene.id;
            geneFilter.gene.id = {
              is: [gene.id]
            };
          } else {
            geneFilter.gene = {
              id: {
                is: [gene.id]
              }
            };
          }

          Donors
            .getList({
              size: 0,
              include: 'facets',
              filters: geneFilter
            })
            .then(function (data) {
              gene.uiDonors = [];
              if (data.facets.projectId.terms) {

                var _f = _locationFilterCache.filters();
                if (_f.hasOwnProperty('donor')) {
                  delete _f.donor.projectId;
                  if (_.isEmpty(_f.donor)) {
                    delete _f.donor;
                  }
                }
                if (_f.hasOwnProperty('gene')) {
                  delete _f.gene[ Extensions.ENTITY ];
                  if (_.isEmpty(_f.gene)) {
                    delete _f.gene;
                  }
                }


                gene.uiDonorsLink = LocationService.toURLParam(
                  LocationService.merge(_f, {gene: {id: {is: [gene.id]}}}, 'facet')
                );

                gene.uiDonors = data.facets.projectId.terms;
                gene.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  projectCachePromise.then(function(lookup) {
                    facet.projectName = lookup[facet.term] || facet.term;
                  });

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });

                // This is just used for gene CSV export, it is unwieldly to do it in the view
                gene.uiDonorsExportString = gene.uiDonors.map(function(d) {
                  return d.term + ':' + d.count + '/' +  d.countTotal;
                }).join('|');
              }
            });
        });
      });
    }

    function _initGenes() {

      var params = LocationService.getJqlParam('genes'),
          filters = _locationFilterCache.filters() || {},
          deferred = $q.defer();

      _ASGeneService.isLoading = true;

      Genes.getList(params).then(function(hitsGenesList) {

        _ASGeneService.genes.hits = hitsGenesList.hits;
        _ASGeneService.genes.pagination = hitsGenesList.pagination;
        _ASGeneService.hitsLoaded = true;

        _ASGeneService.genes.hits.forEach(function (gene) {

          gene.embedQuery = LocationService.merge(filters, {gene: {id: {is: [gene.id]}}}, 'facet');

          // Remove gene entity set because gene id is the key
          if (gene.embedQuery.hasOwnProperty('gene')) {
            var geneFilters = gene.embedQuery.gene;
            delete geneFilters[Extensions.ENTITY];
          }

          // Proper encode
          gene.embedQuery = encodeURIComponent(JSON.stringify(gene.embedQuery));

        });

      })
      .finally(function() {
        _ASGeneService.isLoading = false;
        deferred.resolve();
      });

      return deferred.promise;
    }

      ///////////////////////////////////////////////////////////////////////
      // Genes Public API
      ///////////////////////////////////////////////////////////////////////
      _ASGeneService.init = function () {

        var deferred = $q.defer();

        _ASGeneService.isLoading = true;

        if (angular.isDefined(_ASGeneService.genes)) {
          _ASGeneService.genes.hits = [];
          _ASGeneService.hitsLoaded = false;
        }

        var params = LocationService.getJqlParam('genes');

        params.include = 'facets';
        params.facetsOnly = true;

        Genes.getList(params).then(function (facetGeneList) {
          _ASGeneService.isLoading = false;
          _ASGeneService.genes = facetGeneList.plain(); // Build the partial object

          deferred.resolve();
        });

        return deferred.promise;
    };

      _ASGeneService.renderBodyTab = function () {
          _initGenes().then(_processGeneHits);
      };
  })
  .service('AdvancedMutationService', function (Page, LocationService, HighchartsService, Mutations,
    Occurrences, Projects, Donors, AdvancedSearchTabs, Extensions, ProjectCache, $q) {

    var _ASMutationService = this,
        _projectCachePromise = ProjectCache.getData();




    function _initOccurrences(occurrences) {
        occurrences.hits.forEach(function(occurrence) {
          _projectCachePromise.then(function(lookup) {
            occurrence.projectName = lookup[occurrence.projectId] || occurrence.projectId;
          });
        });

      _ASMutationService.occurrences = occurrences;
    }

    function _initFacets(facets) {
      _ASMutationService.pieConsequences = HighchartsService.pie({
        type: 'mutation',
        facet: 'consequenceType',
        facets: facets
      });
      _ASMutationService.piePlatform = HighchartsService.pie({
        type: 'mutation',
        facet: 'platform',
        facets: facets
      });
      _ASMutationService.pieVerificationStatus = HighchartsService.pie({
        type: 'mutation',
        facet: 'verificationStatus',
        facets: facets
      });
      _ASMutationService.pieType = HighchartsService.pie({
        type: 'mutation',
        facet: 'type',
        facets: facets
      });
    }

    function _processMutationHits() {
      if (! _.get(_ASMutationService, 'mutations.hits.length', false)) {
        return;
      }

      // Need to get SSM Test Donor counts from projects
      Projects.getList().then(function (projects) {
        _ASMutationService.mutations.hits.forEach(function (mutation) {

          var mutationFilter = _locationFilterCache.filters();
          if (mutationFilter.hasOwnProperty('mutation')) {
            delete mutationFilter.mutation[ Extensions.ENTITY ];
            delete mutationFilter.mutation.id;
            mutationFilter.mutation.id = {
              is: [mutation.id]
            };
          } else {
            mutationFilter.mutation = {
              id: {
                is: [mutation.id]
              }
            };
          }

          Donors.getList({
            size: 0,
            include: 'facets',
            filters: mutationFilter
          }).then(function (data) {
            mutation.uiDonors = [];
            if (data.facets.projectId.terms) {
              var _f = _locationFilterCache.filters();
              if (_f.hasOwnProperty('donor')) {
                delete _f.donor.projectId;
                if (_.isEmpty(_f.donor)) {
                  delete _f.donor;
                }
              }
              if (_f.hasOwnProperty('mutation')) {
                delete _f.mutation[ Extensions.ENTITY ];
                if (_.isEmpty(_f.mutation)) {
                  delete _f.mutation;
                }
              }

              mutation.uiDonorsLink = LocationService.toURLParam(
                LocationService.merge(_f, {mutation: {id: {is: [mutation.id]}}}, 'facet')
              );
              mutation.uiDonors = data.facets.projectId.terms;
              mutation.uiDonors.forEach(function (facet) {
                var p = _.find(projects.hits, function (item) {
                  return item.id === facet.term;
                });

                _projectCachePromise.then(function(lookup) {
                  facet.projectName = lookup[facet.term] || facet.term;
                });

                facet.countTotal = p.ssmTestedDonorCount;
                facet.percentage = facet.count / p.ssmTestedDonorCount;
              });

              // This is just used for mutation CSV export, it is unwieldly to do it in the view
              mutation.uiDonorsExportString = mutation.uiDonors.map(function(d) {
                return d.term + ':' + d.count + '/' + d.countTotal;
              }).join('|');
            }
          });
        });
      });
    }

    function _initMutations() {
      var params = LocationService.getJqlParam('mutations'),
        filters = _locationFilterCache.filters() || {},
        deferred = $q.defer();

      _ASMutationService.isLoading = true;

      params.include = ['consequences'];
      params.filters = filters;


      Mutations.getList(params)
        .then(function(hitsMutationsList) {
          _ASMutationService.mutations.hits = hitsMutationsList.hits;
          _ASMutationService.mutations.pagination = hitsMutationsList.pagination;
          _ASMutationService.hitsLoaded = true;

          _ASMutationService.mutations.hits.forEach(function (mutation) {
            mutation.embedQuery = LocationService.merge(filters, {mutation: {id: {is: [mutation.id]}}}, 'facet');

            // Remove mutation entity set because mutation id is the key
            if (mutation.embedQuery.hasOwnProperty('mutation')) {
              var mutationFilters = mutation.embedQuery.mutation;
              delete mutationFilters[Extensions.ENTITY];
            }

            // Proper encode
            mutation.embedQuery = encodeURIComponent(JSON.stringify(mutation.embedQuery));

          });
        })
        .finally(function() {
          _ASMutationService.isLoading = false;
          deferred.resolve();
        });

      var occurrencesFilters = LocationService.getJqlParam('occurrences') || {};

      _.assign(occurrencesFilters, filters);

      Occurrences.getList(occurrencesFilters)
        .then(function(occurrencesList) {
          _initOccurrences(occurrencesList);
        });

      return deferred.promise;
    }



      ///////////////////////////////////////////////////////////////////////
      // Mutations Public API
      ///////////////////////////////////////////////////////////////////////
      _ASMutationService.init = function () {

        var deferred = $q.defer();

        _ASMutationService.isLoading = true;

        if (angular.isDefined(_ASMutationService.genes)) {
          _ASMutationService.mutations.hits = [];
          _ASMutationService.hitsLoaded = false;
        }

        var mParams = LocationService.getJqlParam('mutations');

        mParams.include = ['facets', 'consequences'];
        mParams.facetsOnly = true;
        mParams.filters = _locationFilterCache.filters() || {};

        Mutations.getList(mParams)
          .then(function (mutationsFacetsList) {
            _ASMutationService.isLoading = false;
            _initFacets(mutationsFacetsList.facets);

            _ASMutationService.mutations = mutationsFacetsList.plain(); // Build partial object

            deferred.resolve();
          });

        return deferred.promise;
      };

      _ASMutationService.renderBodyTab = function() {
        _initMutations().then(_processMutationHits);
      };

      _ASMutationService.projectMutationQuery = function(projectId, mutationId) {

        var filters = _locationFilterCache.filters();

        if (filters.hasOwnProperty('mutation')) {
          delete filters.mutation.id;
          delete filters.mutation[Extensions.ENTITY];
        }
        if (filters.hasOwnProperty('donor')) {
          delete filters.donor.projectId;
        }

        if (filters.hasOwnProperty('mutation') === false) {
          filters.mutation = {};
        }

        if (filters.hasOwnProperty('donor') === false) {
          filters.donor = {};
        }

        filters.mutation.id = { is: [mutationId] };
        filters.donor.projectId = { is: [projectId] };
        return encodeURIComponent(JSON.stringify(filters));
      };

  });
})();

angular.module('icgc.advanced.services', [])
  .service('AdvancedSearchTabs', function () {
    this.isLoading = false;
    this.visitedTab = {};
    this.visitedFacet = {};

    this.setTab = function (tab) {
      this.tab = tab;
      this.facetTab = tab;
      if (tab === 'mutation') {
        this.subTab = tab;
      }
      this.visitedTab[tab] = true;
      this.visitedFacet[tab] = true;
    };
    this.getTab = function () {
      return this.tab;
    };
    this.isTab = function (tab) {
      return this.tab === tab;
    };

    this.hasVisitedTab = function(tab) {
      return this.visitedTab[tab];
    };
    this.hasVisitedFacet = function(tab) {
      return this.visitedFacet[tab];
    };

    this.setFacetTab = function (tab) {
      this.facetTab = tab;
      this.visitedFacet[tab] = true;
    };
    this.getFacetTab = function () {
      return this.facetTab;
    };
    this.isFacetTab = function (tab) {
      return this.facetTab === tab;
    };

    this.setSubTab = function (tab) {
      this.subTab = tab;
    };
    this.getSubTab = function () {
      return this.subTab;
    };
    this.isSubTab = function (tab) {
      return this.subTab === tab;
    };
  });

