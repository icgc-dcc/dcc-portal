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

  var module = angular.module('icgc.sets.services', ['icgc.common.filters']);

  module.service('SetOperationService', function() {
    var shortHandPrefix = 'S';

    /**
     * Check set/list equality
     */
    this.isEqual = function(s1, s2) {
      return (_.difference(s1, s2).length === 0 && _.difference(s2, s1).length === 0);
    };


    /**
     *  Pull out the unique sets involved in the set operations analysis result
     */
    this.extractUniqueSets = function(items) {
      var result = [];
      items.forEach(function(set) {
        set.intersection.forEach(function(id) {
          if (_.contains(result, id) === false) {
            result.push(id);
          }
        });
      });
      return result;
    };


    /**
     * Sort set operations analysis results
     */
    this.sortData = function(items) {
      items.forEach(function(subset) {
        subset.intersection.sort();
        subset.exclusions.sort();
      });
      return _.sortBy(items, function(subset) {
          var secondary = subset.exclusions.length > 0 ? subset.exclusions[0] : '';
          return subset.intersection.length + '' + secondary;
        }).reverse();
    };

    /**
     * Transform data array to be consumed by venn-diagram visualization
     */
    this.transform = function(data) {
      var result = [];

      data.forEach(function(set) {
        var subset = [];
        set.intersection.forEach(function(sid) {
          subset.push({
            id: sid,
            count: set.count
          });
        });
        result.push(subset);
      });
      return result;
    };

    function _getSetShortHand(setId, setList) {
      if (setList) {
        return shortHandPrefix + '<sub>' + (setList.indexOf(setId) + 1) + '</sub>';
      }
      return setId;
    }

    this.getSetShortHand = _getSetShortHand;


  });



  /**
   * Abstracts CRUD operations on entity lists (gene, donor, mutation)
   */
  module.service('SetService',
    function ($window, $location, $q, $timeout, Restangular, RestangularNoCache, API,
              localStorageService, toaster, Extensions, Page, FilterService, RouteInfoService, gettextCatalog) {

    var LIST_ENTITY = 'entity';
    var dataRepoUrl = RouteInfoService.get ('dataRepositories').href;
    var _service = this;

    // For application/json format
    function params2JSON(type, params, derived) {
      var data = {};
      
      if (typeof derived === 'undefined' || !derived) {
        data.filters = encodeURI(JSON.stringify(params.filters));
        data.size = params.size || 0;
      }
      
      data.type = type.toUpperCase();
      data.name = params.name;
      data.description = params.description || '';

      if (params.isTransient) {
        data.isTransient = params.isTransient;
      }

      // Set default sort values if necessary
      if (angular.isDefined(params.filters) && !angular.isDefined(params.sortBy)) {
        if (type === 'donor') {
          data.sortBy = 'ssmAffectedGenes';
        } else if (type === 'gene') {
          data.sortBy = 'affectedDonorCountFiltered';
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

    _service.createAdvLink = function(set) {
      var type = set.type.toLowerCase(), filters = {};
      filters[type] = {};
      filters[type].id = {is: [Extensions.ENTITY_PREFIX+set.id]};

      if (['gene', 'mutation'].indexOf(type) >= 0) {
        return '/search/' + type.charAt(0) + '?filters=' + angular.toJson(filters);
      } else {
        return '/search?filters=' + angular.toJson(filters);
      }
    };

    _service.createRepoLink = function(set) {
      var filters = {};
    	var type = 'file';
      filters[type] = {};
      filters[type] = {donorId: {is: [Extensions.ENTITY_PREFIX + set.id]}};

      return dataRepoUrl + '?filters=' + angular.toJson(filters);
    };

    _service.materialize = function(type, params) {
      var data = params2JSON(type, params, true);
      return Restangular.one('entityset').post('union', data, {}, {'Content-Type': 'application/json'});
    };

    /**
    * We want to materialize a new set in a synchronous request.
    */
    _service.materializeSync = function(type, params) {
      var data = params2JSON(type, params, true);
      return Restangular.one('entityset')
        .customPOST(data, 'union', {async:false}, {'Content-Type': 'application/json'});
    };


    /**
    * params.filters
    * params.sort
    * params.order
    * params.name
    * params.description - optional
    * params.count - limit (max 1000) ???
    *
    * Create a new set from
    */
    _service.addSet = function(type, params) {
      var promise = null;
      var data = params2JSON(type, params);
      var addSetSaving;

      if(! data.isTransient){
        addSetSaving = _service.savingToaster(data.name);
      }      

      promise = Restangular.one('entityset')
                .post(undefined, data, {async: 'false'}, {'Content-Type': 'application/json'});

      promise.then(function(data) {        
        
        if (! data.id) {
          return;
        }

        // If flagged as transient, don't save to local storage
        if (data.subtype === 'TRANSIENT') { 
          return;
        }

        data.type = data.type.toLowerCase();

        setList.unshift(data);
        _service.refreshList();

        localStorageService.set(LIST_ENTITY, setList);
        
        _service.saveSuccessToaster(data.name);
        toaster.clear(addSetSaving);

      });
      
      return promise;
    };

    _service.addExternalSet = function(type, params) {
      var promise = null;
      var data = params2JSON(type, params);
      var addSetSaving;

      if(! data.isTransient){
        addSetSaving = _service.savingToaster(data.name);
      }

      promise = Restangular.one('entityset').one('external')
        .post(undefined, data, {}, {'Content-Type': 'application/json'});

      promise.then(function(data) {
        if (! data.id) {
          return;
        }

        // If flagged as transient, don't save to local storage
        if (data.subtype === 'TRANSIENT') {
          return;
        }

        data.type = data.type.toLowerCase();

        setList.unshift(data);
        _service.refreshList();

        localStorageService.set(LIST_ENTITY, setList);

        _service.saveSuccessToaster(data.name);
        toaster.clear(addSetSaving);
      });

      return promise;
    };

    _service.createFileSet = function (params) {
      params.name = 'File Set';
      params.description = 'File Set for Manifest';
      params.sortBy = 'id';
      params.sortOrder = 'DESCENDING';
      var promise = null;
      var data = params2JSON('FILE', params);
      promise = Restangular.one('entityset').one('file')
        .post(undefined, data, {}, {'Content-Type': 'application/json'});

      return promise;
    };

    _service.createEntitySet = function (type, params) {
      invariant(params.name, 'Params is missing required property "name"');
      var data = params2JSON(type, params);
      return Page.startWork(Restangular.one('entityset')
        .customPOST(data, undefined, {async:'false'}, {'Content-Type': 'application/json'}));
    };

    _service.getTransientSet = function(type, params) {
      var data = params2JSON(type, params);
      return Restangular.one('entityset')
        .post(undefined, data, {async: 'false'}, {'Content-Type': 'application/json'});
    };

    _service.createForwardRepositorySet = function(type, params, forwardUrl) {
      Page.startWork();
      params.name = 'Input donor set';
      params.description = '';
      params.sortBy = 'fileName';
      params.sortOrder = 'DESCENDING';
      
      var data = params2JSON(type, params);      
      var promise = Restangular.one('entityset').one('external')
      .post(undefined, data, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        Page.stopWork();
        var newFilter = JSON.stringify({donor: {id: {is: [Extensions.ENTITY_PREFIX+data.id]}}});
        FilterService.filters(newFilter);
        $location.path(forwardUrl).search('filters', newFilter);
      });
      return promise;
    };

    _service.createSetFromSingleFile = function (fileId, size) {
      Page.startWork();
      var params = {
        name: fileId + ' donors',
        description: '',
        sortBy: 'fileName',
        sortOrder: 'DESCENDING',
        filters: {
          file: {
            id: {
              is: [fileId]
            }
          }
        },
        isTranient: true,
        size: size
      };

      var data = params2JSON('donor', params);
      var promise = Restangular.one('entityset').one('external')
        .post(undefined, data, {}, { 'Content-Type': 'application/json' });
      promise.then(function (data) {
        Page.stopWork();
        var newFilter = JSON.stringify({ donor: { id: { is: [Extensions.ENTITY_PREFIX + data.id] } } });
        FilterService.filters(newFilter);
        $location.path('/search').search('filters', newFilter);
      });
      return promise;
    };


    /**
     * params.union
     * params.name
     * params.description - optional
     *
     * Create a new set from the union of various subsets of the same type
     */
    _service.addDerivedSet = function(type, params) {
      var promise = null;
      var data = params2JSON(type, params, true);
      var addSetSaving;

      if(! data.isTransient){
        addSetSaving = _service.savingToaster(data.name);
      }

      promise = Restangular.one('entityset')
                .post('union', data, {async: 'false'}, {'Content-Type': 'application/json'});

      promise.then(function(data) {
        if (! data.id) {
          console.log('there is an error in creating derived set');
          return;
        }

        data.type = data.type.toLowerCase();

        setList.unshift(data);
        _service.refreshList();

        localStorageService.set(LIST_ENTITY, setList);

        _service.saveSuccessToaster(data.name);
        toaster.clear(addSetSaving);
      });

      return promise;
    };

    _service.refreshList = function() {
      setList.forEach(function(set) {
        var filters = {};
        filters[set.type] = {};
        filters[set.type].id = {is: [Extensions.ENTITY_PREFIX+set.id]};

        // Grab the type (base advanced search router and corresponding filters)
        set.advType = ''; // default route is donors or just route is '/search';
        set.advFilters = filters;
        
        if (['gene', 'mutation'].indexOf(set.type) !== -1) {
          // Grab the type (base advanced search router and corresponding filters)
          set.advType = set.type.charAt(0); // route is '/search' plus either '/g' or '/m'
          set.advLink = '/search/' +  set.advType + '?filters=' + JSON.stringify(filters);
        } else if (['file'].indexOf(set.type) !== -1) {
          set.repoLink = dataRepoUrl + '?filters=' + JSON.stringify(filters);
        } else {
          set.advLink = '/search?filters=' + JSON.stringify(filters);
          var fileFilters = {};
          fileFilters.file = {donorId: {is: [Extensions.ENTITY_PREFIX + set.id]}};
          set.repoLink = dataRepoUrl + '?filters=' + JSON.stringify(fileFilters);
        }
      });
    };

    _service.getMetaData = function( ids ) {
      return RestangularNoCache.several('entityset/sets', ids).get('', {});
    };

    _service.lookupTable = function(metaData) {
      var map = {};
      metaData.forEach(function(d) {
        map[d.id] = d.name;
      });
      return map;
    };

    _service.exportSet = function(id) {
      $window.location.href = API.BASE_URL + '/entityset/' + id + '/export';
    };


    /****** Local storage related API ******/
    _service.getAll = function() {
      return setList;
    };

    _service.getAllGeneSets = function() {
      return _.filter(setList, function(s) {
        return s.type === 'gene';
      });
    };

    _service.initService = function() {

      setList = localStorageService.get(LIST_ENTITY) || [];
      _service.refreshList();

      return setList;
    };

    _service.removeSeveral = function(ids) {
      _.remove(setList, function(list) {
        return ids.indexOf(list.id) >= 0;
      });
      localStorageService.set(LIST_ENTITY, setList);
      return true;
    };

    _service.remove = function(id) {
      _.remove(setList, function(list) {
        return list.id === id;
      });
      localStorageService.set(LIST_ENTITY, setList);
      return true;
    };

    // Wait for sets to getResolved
    function ResolveSet(Ids, waitMS, numTries) {
      var _Ids = Ids,
          _waitMS = waitMS || 1500,
          _numTries = numTries || 10,
          _originalNumTries = _numTries,
          _retrievalTargetPromiseFunction = _defaultRetrievalTargetPromiseFunction,
          _resolvedEntityFunction = _defaultSetResolvedFunction,
          _resolvePromise = null,
          _this = this;

      _this.resolve = _resolve;
      _this.reset = _reset;

      _this.setRetrievalEntityFunction = function(fn) {

        if (! angular.isFunction(fn)) {
          throw new Error('Expected Function for setRetrievalEntityFunction method!');
        }

        _retrievalTargetPromiseFunction = fn;
        return _this;
      };

      _this.setResolvedEntityFunction = function(fn) {

        if (! angular.isFunction(fn)) {
          throw new Error('Expected Function for setResolvedEntityFunction method!');
        }

        _resolvedEntityFunction = fn;
        return _this;
      };

      function _defaultRetrievalTargetPromiseFunction() {
        return _service.getMetaData(_Ids);
      }

      function _defaultSetResolvedFunction(data) {

        var resolvedSets = _.filter(data, function (entityStatusData) {
          return entityStatusData.state.toUpperCase() === 'FINISHED';
        });

        return resolvedSets.length === _Ids.length;
      }

      function _reset() {
        _numTries = _originalNumTries;
        _resolvePromise = null;
        return _this;
      }

      function _resolve() {

        if (_resolvePromise === null) {
          _resolvePromise = $q.defer();
        }

        if (numTries <= 0) {
          return _resolvePromise.reject();
        }

        _retrievalTargetPromiseFunction()
          .then(function(data) {

            var entityData = data.plain();

            if (_resolvedEntityFunction(entityData)) {
              _resolvePromise.resolve(entityData);
            }
            else {
              setTimeout(function() {
                _numTries--;
                _resolve();
              }, _waitMS);
            }

          });

        return _resolvePromise.promise;
      }


    }

    _service.pollingResolveSetFactory = function(Ids, waitMS, numTries) {
      var _Ids = [];

      if (angular.isArray(Ids)) {
        _Ids = Ids;
      }
      else if (angular.isDefined(Ids)) {
        _Ids.push(Ids);
      }
      else {
        throw new Error('Expected an entity ID, or array of entity IDs but got: ', Ids);
      }

      return new ResolveSet(_Ids, waitMS, numTries);
    };

    _service.getAnalysis = function (id) {
      return RestangularNoCache.one('analysis/union/' + id).get()
        .then(function (wrappedResponse) {
          var response = wrappedResponse.plain();
          return response.state === 'IN_PROGRESS' ?
            $timeout(_service.getAnalysis.bind(null, id), 800) : response;
        });
    };

    _service.createAnalysis = function (setIds, setType) {
      return Restangular.one('analysis').post('union', {
        lists: setIds,
        type: setType
      }, {}, { 'Content-Type': 'application/json' })
        .then(function (response) {
          var id = response.plain().id;
          return _service.getAnalysis(id);
        });
    };

    _service.savingToaster = function(setName){
      /// ${savedSet} is a noun
      return toaster.pop('warning', _.template(gettextCatalog.getString('Saving ${savedSet}'))({savedSet: setName}), 
      /// 
        gettextCatalog.getString('Please wait'), 0, 'trustedHtml');
    };

    _service.saveSuccessToaster = function(setName){
      /// ${savedSet} is a noun
      return toaster.pop('success', _.template(gettextCatalog.getString('${savedSet} Saved'))({savedSet : setName}),
      /// 
        gettextCatalog.getString('View in <a href="/analysis/sets">Data Analysis</a>'), 4000, 'trustedHtml');
    };

    // Initialize
    var setList = _service.initService();
    _service.refreshList();
  });

})();
