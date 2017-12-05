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

  var module = angular.module('icgc.common.location', []);
  // TODO: Refactor out all filter logic here to use only FilterService.
  // Currently Advanced search and the facets module makes use of
  // the new FilterService
  module
    .factory('LocationService', function ($location, FilterService, $anchorScroll, Notify, Extensions) {

    return {
      path: function () {
        return $location.path();
      },
      search: function () {
        return $location.search();
      },
      clear: function () {
        return $location.search({});
      },
      buildURLFromPath: function(path) {
        var port = window.location.port ? ':' +  window.location.port : '',
            URL = window.location.protocol + '//' + window.location.hostname +
                port + (('/' + path) || window.location.pathname);

        return URL;
      },
      // Introduced to temporarily support older logic
      getFilterService: function() {
        return FilterService;
      },
      filters: function () {
        return FilterService.filters.apply(FilterService, arguments);
      },
      setFilters: function (filters) {
        // TODO: Factor this into FacetService
        console.info('@Deprecated - Location Service setFilters() being used.', filters);
        var search = this.search(),
            filtersJSON = (_.isObject(filters) && !_.isEmpty(filters)) ? angular.toJson(filters) : '{}';

        // Clear all 'from' params. Pagination should be reset if filters change
        delete search.from;
        ['donors', 'genes', 'mutations', 'occurrences', 'files'].forEach(function (type) {
          var soType = search[type];
          if (soType) {
            var so = angular.fromJson(soType);
            delete so.from;
            if (_.isEmpty(so)) {
              delete search[type];
            } else {
              search[type] = angular.toJson(so);
            }

          }
        });

        if (filtersJSON) {
          search.filters = filtersJSON;
        }

        $location.search(search);
      },
      removeFilters: function () {
        FilterService.removeFilters.apply(FilterService, arguments);
      },
      mergeIntoFilters: function () {
        return FilterService.mergeIntoFilters.apply(FilterService, arguments);
      },
      overwriteFilters: function () {
        return FilterService.overwriteFiltersAtObjectLevel.apply(FilterService, arguments);
      },
      merge: function () {
        return FilterService.merge.apply(FilterService, arguments);
      },
      toURLParam: function(filterParamObj) {
        var filterStr = '';
        
        if (filterParamObj === null) {
          return filterStr;  
        }
        
        switch(typeof filterParamObj) {
          case 'object':
            filterStr = JSON.stringify(filterParamObj);
          break;
          default: 
            filterStr = filterParamObj;
          break;
        }
        
        return encodeURIComponent(filterStr);
      },
      goToInlineAnchor: function (inlineAnchorID) {
        $location.hash(inlineAnchorID);
        $anchorScroll(inlineAnchorID);
      },
      getJqlParam: function (param) {
        
        try {
          return angular.fromJson(this.search()[param]) || {};
        } catch (e) {
          Notify.setMessage('Cannot parse: ' + param + '=' + this.search()[param] );
          Notify.showErrors();
        }
      },
      setJsonParam: function (param, data) {
        var s = $location.search();
        s[param] = angular.toJson(data);
        $location.search(s);
      },
      getParam: function (param) {
        return this.search()[param];
      },
      setParam: function (param, data) {
        var s = $location.search();
        s[param] = data;
        $location.search(s);
      },
      removeParam: function (param) {
        var s = this.search();
        delete s[param];
        $location.search(s);
      },
      getPaginationParams: function(dataType){
        return _.defaults({}, this.getJqlParam(dataType), {from: 1, size: 10});
      },
      goToFirstPage: function(param) {
        this.setJsonParam(param, _.extend(this.getJqlParam(param), {from: 1}));
      },
      goToPath: function(path, search, hash) {
        var searchParams = search || {},
            hashParam = hash || '';

        $location.path(path).search(searchParams).hash(hashParam);
      },
      // Extract UUIDs from filters
      extractSetIds: function(filters) {
        var list = this.extractSetIdsHelper(filters);
        var retList = [];
        _.forEach(list, function(item) {
          if (item.indexOf(Extensions.ENTITY_PREFIX) === 0) {
            retList.push(item.substring(3));
          }
        });
 
        return retList;
      },
      
      extractSetIdsHelper: function(filters) {
        var result = [];
        ['donor', 'gene', 'mutation', 'file'].forEach(function(type) {
          if (filters.hasOwnProperty(type) === false) {
            return;
          }

          if (filters[type].hasOwnProperty('id')) {
            var entityFilters = filters[type].id;
            if (entityFilters.hasOwnProperty('is')) {
              result = result.concat( entityFilters.is );
            } else if (_.has(entityFilters, 'not')) {
              result = result.concat( entityFilters.not );
            }
          } else if (filters[type].hasOwnProperty('donorId')) {
            var donorIdFilters = filters[type].donorId;
            if (donorIdFilters.hasOwnProperty('is')) {
              result = result.concat( donorIdFilters.is );
            } else if (_.has(donorIdFilters, 'not')) {
              result = result.concat( donorIdFilters.not );
            }
          }

        });
        return result;
      }

    };
  });
})();
