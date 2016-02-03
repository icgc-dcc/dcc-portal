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
  var toJson = angular.toJson;
  var uriString = _.flow (toJson, encodeURIComponent);

  var module = angular.module('icgc.repository.services', []);

  module.service ('ExternalRepoService', function ($window, Restangular, API) {

    // Initial values until the call to getRepoMap() returns.
    var _srv = this,
        _repoCodeToName = {
          'pcawg-chicago-tcga': 'PCAWG - Chicago (TCGA)',
          'cghub': 'CGHub - Santa Cruz',
          'pcawg-heidelberg': 'PCAWG - Heidelberg',
          'pcawg-tokyo': 'PCAWG - Tokyo',
          'aws-virginia': 'AWS - Virginia',
          'pcawg-barcelona': 'PCAWG - Barcelona',
          'pcawg-cghub': 'PCAWG - Santa Cruz',
          'pcawg-chicago-icgc': 'PCAWG - Chicago (ICGC)',
          'pcawg-london': 'PCAWG - London',
          'tcga': 'TCGA DCC - Bethesda'
        },
        _repoNameToCode = _.invert (_repoCodeToName),
        _repoMapRefreshPromise = null;


    // Private functions....
    function _init() {
      // Force a refresh
      _srv.refreshRepoMap();
      ///////////////////////////
    }

    function _getRepoMap() {
      return Restangular.one (REPO_API_PATH).one('repo_map').get ({});
    }

    function _concatRepoCodes (repoNames) {
      return _.map (repoNames, function (name) {
        return _.get (_repoNameToCode, name, name);
      }).join();
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
        var repoMapData = restangularMapData.plain();
        _repoCodeToName = repoMapData;
        _repoNameToCode = _.invert (repoMapData);
        _repoMapRefreshPromise = null;
      });

      return _repoMapRefreshPromise;
    };


    _srv.getList = function (params) {
      var defaults = {
        size: 10,
        from:1
      };
      
      var precedence = [
          'AWS - Virginia',
          'Collaboratory',
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
      
      return Restangular.one (REPO_API_PATH).get (angular.extend (defaults, params)).then(function (data) {
        if (data.termFacets.hasOwnProperty('repoName') && data.termFacets.repoName.hasOwnProperty('terms')) {
          data.termFacets.repoName.terms = data.termFacets.repoName.terms.sort(function (a, b) {
            return precedence.indexOf(a.term) - precedence.indexOf(b.term);
          });
        }
        
        return data;
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


    _srv.download = function (filters, repos) {
      $window.location.href = API.BASE_URL + '/' + REPO_API_PATH + '/manifest?filters=' +
        uriString (filters) + '&repositories=' + _concatRepoCodes (repos);
    };

    _srv.downloadSelected = function (ids, repos) {
      jQuery('<form method="POST" id="fileDownload" action="' +
              API.BASE_URL + '/' + REPO_API_PATH + '/manifest" style="display:none">' +
             _.map (ids, function (id) {
                return '<input type="hidden" name="fileIds" value="' + id + '"/>';
              }) +
             '<input type="hidden" name="repositories" value="' + _concatRepoCodes (repos) + '"/>' +
             '<input type="submit" value="Submit"/>' +
             '</form>').appendTo('body');

      jQuery('#fileDownload').submit();
      jQuery('#fileDownload').remove();
    };

    _srv.export = function (filters) {
      $window.location.href = API.BASE_URL + '/' + REPO_API_PATH +
        '/export?filters=' + uriString (filters);
    };

    _srv.getMetaData = function() {
      return Restangular.one (REPO_API_PATH).one('metadata').get ({});
    };

    _srv.getFileInfo = function (id) {
      return Restangular.one (REPO_API_PATH, id).get();
    };


     // Initialize this service
    _init();

  });


  module.service('RepositoryService', function ($filter, RestangularNoCache) {

    this.folder = function (path) {
      return RestangularNoCache.one('download', 'info' + path)
              .get();
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
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      } else {
        files = $filter('orderBy')(files, 'date', 'reverse');
        firstSort = _.pluck(files, 'name');
        files = $filter('orderBy')(files, logicalSort);
      }

      return files;
    };

  });

})();
