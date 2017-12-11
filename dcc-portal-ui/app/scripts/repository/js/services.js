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

(function() {
  'use strict';

  var REPO_API_PATH = 'repository/files';
  var REPOS_PATH = 'repositories';
  var toJson = angular.toJson;
  var uriString = _.flow (toJson, encodeURIComponent);

  var encodeQuery = function (queryMap) {
    return _.map(queryMap, function (val, key) {
      return val ? key + '=' + encodeURIComponent(val) : '';
    }).filter(Boolean).join('&');
  };

  var module = angular.module('icgc.repository.services', []);

  module.service ('ExternalRepoService', function ($window, $q, Restangular, API, HighchartsService, RepositoryService) {

    // Initial values until the call to getRepoMap() returns.
    var _srv = this,
        _repoCodeToName = {}, // This really shouldn't be needed.
        _repoNameToCode = {},
        _repoMapRefreshPromise = null;


    // Private functions....
    function _init() {
      // Force a refresh
      _srv.refreshRepoMap();
      ///////////////////////////
    }

    function _getRepoMap() {
      return Restangular.one(REPOS_PATH).get ({});
    }

    function _concatRepoCodes (repoNames) {
      return _.map (repoNames, function (name) {
        return _.get (_repoNameToCode, name, name);
      }).join();
    }

    function _createFacetPieChart (facets, facetName) {
      return HighchartsService.pie({
        type: 'file',
        facet: facetName,
        facets: facets
      });
    }

    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
    // We need a way to force the refresh of the Repo Map from the server.
    // Returning the promise is useful when it's a dependency.
    _srv.refreshRepoMap = function() {

      // If a refresh promise has been made in the past return it..
      if (_repoMapRefreshPromise !== null) {
        return _repoMapRefreshPromise;
      }

      _repoMapRefreshPromise = _getRepoMap();

      _repoMapRefreshPromise.then(function (restangularMapData) {


        var repoMapData = _.reduce(restangularMapData.plain(), function(result, d) {
          result[d.code] = d.name;
          return result;
        }, {});
        _repoCodeToName = repoMapData;
        _repoNameToCode = _.invert (repoMapData);
        _repoMapRefreshPromise = null;
      });

      return _repoMapRefreshPromise;
    };


    _srv.getManifestSummary = function(params) {
      return Restangular.all(REPO_API_PATH + '/summary/manifest')
        .customPOST(params, undefined, undefined, {'Content-Type': 'application/json'});
    };

    const hydrateFileCopies = (copies, repoCodeMap) => copies.map(copy => ({
      ...copy,
      repo: repoCodeMap[copy.repoCode],
    }));

    _srv.getList = async function (params) {
      var defaults = {
        size: 10,
        from:1
      };

      var precedence = [
          'AWS - Virginia',
          'Collaboratory - Toronto',
          'EGA - Hinxton',
          'GDC - Chicago',
          'PDC - Chicago',
          'PCAWG - Barcelona',
          'PCAWG - Chicago (ICGC)',
          'PCAWG - Chicago (TCGA)',
          'PCAWG - Heidelberg',
          'PCAWG - London',
          'PCAWG - Santa Cruz',
          'PCAWG - Seoul',
          'PCAWG - Tokyo',
          'CGHub - Santa Cruz',
          'TCGA DCC - Bethesda'
      ];

      const filesRequest = Restangular.one(REPO_API_PATH).get(angular.extend (defaults, params)).then((data) => {
        
        if (data.termFacets.hasOwnProperty('repoName') && data.termFacets.repoName.hasOwnProperty('terms')) {
          data.termFacets.repoName.terms = data.termFacets.repoName.terms.sort(function (a, b) {
            return precedence.indexOf(a.term) - precedence.indexOf(b.term);
          });
        }
        
        // Add "No Data" Option to Facets with missing data values
        for (var facet in data.termFacets) {
          if (data.termFacets.hasOwnProperty(facet) && data.termFacets[facet].missing) {
            var f = data.termFacets[facet];
            if (f.hasOwnProperty('terms')) {
              f.terms.push({term: '_missing', count: f.missing});
            } else {
              f.terms = [
                {term: '_missing', count: f.missing}
              ];
            }
          }
        }

        return data;
      });

      const [filesResponse, repoCodeMap] = [
        await filesRequest,
        await RepositoryService.getRepoCodeMap(),
      ];

      return {
        ...filesResponse,
        hits: filesResponse.hits.map(hit => ({
          ...hit,
          fileCopies: hydrateFileCopies(hit.fileCopies, repoCodeMap),
        }))
      };
    };

    _srv.getRelevantRepos = function (filters) {
      var params = {
        size: 0,
        include: 'facets',
        filters: filters || {},
      };

      return $q.all({
        relevantRepoNames: _srv.getList(params)
          .then(Restangular.stripRestangular)
          .then(function (response) {
            var repoNames = _.map(response.termFacets.repoName.terms, 'term');
            return repoNames;
          }),
        repos: _getRepoMap().then(Restangular.stripRestangular),
      }).then(function (responses) {
        var relevantRepoNames = responses.relevantRepoNames;
        var relevantRepos = responses.repos.filter(function (repo) {
          return _.includes(relevantRepoNames, repo.name);
        });
        return relevantRepos;
      });
    };

    _srv.getRepoNameFromCode = function (repoCode) {
      return _.get (_repoCodeToName, repoCode, null);
    };

    _srv.getRepoCodeFromName = function (repoName) {
      return _.get (_repoNameToCode, repoName, null);
    };

    /**
     * Get total donor, file and file size statistics
     */
    // TODO: This is a duplicate function that was organized into
    // the repositories service. Remove this once the dependencies
    // are determined
    _srv.getSummary = function (params) {
      return Restangular.one (REPO_API_PATH + '/summary').get (params);
    };

    _srv.getRepoManifestUrl = function (params) {
      var repoCodes = _.isArray(params.repoCodes) ? params.repoCodes : [params.repoCodes];
      var filters = JSON.stringify(params.filters);
      var format = params.format || 'files';

      var query = encodeQuery({
        repos: repoCodes,
        format: format,
        filters: filters,
        unique: params.unique
      });

      return API.BASE_URL + '/manifests?' + query;
    };

    _srv.downloadSelected = function (ids, repos, unique) {
      $window.location.href = _srv.getManifestUrlByFileIds(ids, repos, unique);
    };

    _srv.export = function (filters, type) {
      $window.location.href = `${API.BASE_URL}/${REPO_API_PATH}/export?type=${type}&filters=${uriString (filters)}`;
    };

    _srv.createManifest = function (params) {
      var data = {};
      if (params.filters) {
         data.filters = JSON.stringify(params.filters);
      }
      if (params.repos) {
         data.repos = params.repos.join(',');
      }
      if (params.format) {
         data.format = params.format;
      }

      return Restangular.service('manifests').post(jQuery.param(data));
    };

    _srv.getManifestUrlByFileIds = function (ids, repos, unique) {
      return _srv.getRepoManifestUrl({
        filters: {file:{id:{is:ids}}},
        repoCodes: _concatRepoCodes (repos),
        format: 'tarball',
        unique: unique
      });
    };

    _srv.getMetaData = function() {
      return Restangular.one (REPO_API_PATH).one('metadata').get ({});
    };

    _srv.getFileInfo = async function (id) {
      const [fileInfo, repoCodeMap] = [
        await Restangular.one(REPO_API_PATH, id).get().then(x => x.plain()),
        await RepositoryService.getRepoCodeMap(),
      ];
      return {
        ...fileInfo,
        fileCopies: hydrateFileCopies(fileInfo.fileCopies, repoCodeMap),
      };
    };

    function _shortenRepoName (name) {
      return name;
    }

    _srv.createFacetCharts = function (facets) {
      var sorted = facets.repoName.terms.sort(function (a, b) {
        return b.count - a.count;
      });
      return {
        repositories: HighchartsService.bar({
          hits: _.map(sorted, function (object) {
            return {
              count: object.count,
              term: _shortenRepoName(object.term)
            };
          }),
          name: 'file',
          xAxis: 'term',
          yValue: 'count'
        }),
        study: _createFacetPieChart(facets, 'study'),
        primarySite: _createFacetPieChart(facets, 'primarySite'),
        dataType: _createFacetPieChart(facets, 'dataType'),
        software: _createFacetPieChart(facets, 'software'),
        experimentalStrategy: _createFacetPieChart(facets, 'experimentalStrategy'),
        fileFormat: _createFacetPieChart(facets, 'fileFormat')
      };
    };


     // Initialize this service
    _init();

  });


  module.service('FileService', function ($filter, RestangularNoCache) {

    this.folder = function (path) {
      return RestangularNoCache.one('download/info' + path)
              .get();
    };

    this.getAllFiles = () => {
      return RestangularNoCache.one('download/info')
        .get({
          fields: 'name,type',
          recursive: true,
          flatten: true,
        });
    };

    this.getStatus = function () {
      return RestangularNoCache.one('download', 'status')
              .get();
    };


    /**
     *  Order the files and folders, see DCC-1648, basically
     * readme > current > others releases > legacy releases
     */
    this.sortFiles = function( files, dirLevel ) {
      var firstSort;

      function logicalSort(file) {
        var pattern, name;
        name = file.name.split('/').pop();

        pattern = /notice\.(txt|md)$/i;
        if (pattern.test(name)) {
          return -4;
        }

        pattern = /readme\.(txt|md)$/i;
        if (pattern.test(name)) {
          return -3;
        }

        pattern = /^current/i;
        if (pattern.test(name)) {
          return -2;
        }

        pattern = /^summary/i;
        if (pattern.test(name)) {
          return -1;
        }

        pattern = /^legacy_data_releases/i;
        if (pattern.test(name)) {
          return files.length + 1;
        }
        return firstSort.indexOf(file.name);
      }

      if (dirLevel > 0) {
        files = $filter('orderBy')(files, 'name');
        firstSort = _.map(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      } else {
        files = $filter('orderBy')(files, 'date', 'desc');
        firstSort = _.map(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      }

      return files;
    };

  });

})();
