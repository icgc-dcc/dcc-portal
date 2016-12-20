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

import {ensureArray, ensureString, partiallyContainsIgnoringCase} from '../../common/js/ensure-input';

(function () {
  'use strict';

  var module = angular.module('icgc.keyword', ['icgc.keyword.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('q', {
      url: '/q?q',
      reloadOnSearch: false,
      templateUrl: '/scripts/keyword/views/results.html',
      controller: 'KeywordController'
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.keyword.controllers', ['icgc.keyword.models', 'icgc.common.text.utils']);

  module.controller('KeywordController',
    function ($scope, Page, LocationService, debounce, Keyword, RouteInfoService, Abridger, gettextCatalog) {
      var pageSize;

      $scope.from = 1;
      pageSize = 20;

      $scope.query = LocationService.getParam('q') || '';
      $scope.type = LocationService.getParam('type') || 'all';
      $scope.isBusy = false;
      $scope.isFinished = false;
      $scope.dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;
      $scope.compoundEntityUrl = RouteInfoService.get ('drugCompound').href;

      /// ${query} would be a search query/keyword
      Page.setTitle(_.template(gettextCatalog.getString('Results for ${query}'))({query : $scope.query}));
      Page.setPage('q');

      $scope.clear = function () {
        $scope.query = '';
        LocationService.clear();
        $scope.quickFn();
      };

      $scope.next = function () {
        if ($scope.isBusy || $scope.isFinished || !$scope.results.hits.length) {
          return;
        }

        $scope.from += pageSize;
        getResults({scroll: true});
      };


      $scope.badgeStyleClass = function (type) {
        var definedType = _.contains (['pathway', 'go_term', 'curated_set'], type) ? 'geneset' : type;
        return 't_badge t_badge__' + definedType;
      };

      var maxConcat = 3;

      $scope.concatMatches = function (array) {
        var target = $scope.query;
        var matches = _(ensureArray (array))
          .filter (function (element) {
            return partiallyContainsIgnoringCase (ensureString (element), target);
          })
          .take (maxConcat);

        return matches.join (', ');
      };

      var maxAbrigementLength = 120;
      var abridger = Abridger.of (maxAbrigementLength);

      $scope.abridge = function (array) {
        var target = $scope.query;

        return abridger.abridge (array, target);
      };

      $scope.quickFn = function () {
        $scope.from = 1;

        if ($scope.query && $scope.query.length >= 2) {
          LocationService.setParam('q', $scope.query);

          /// ${query} would be a search query/keyword
          Page.setTitle(_.template(gettextCatalog.getString('Results for ${query}'))({query : $scope.query}));
          getResults();
        } else {
          $scope.results = null;
        }
      };

      $scope.quick = debounce($scope.quickFn, 200, false);

      function getResults(settings) {
        var saved = $scope.query;

        settings = settings || {};
        $scope.isBusy = true;

        Keyword.getList({q: $scope.query, type: $scope.type, size: pageSize, from: $scope.from})
          .then(function (response) {
            var pagination = _.get (response, 'pagination', {total: 0});
            $scope.isFinished = (pagination.total < 1) || pagination.total - $scope.from < pageSize;
            $scope.isBusy = false;
            $scope.activeQuery = saved;

            if (settings.scroll) {
              $scope.results.pagination = pagination;
              for (var i = 0; i < response.hits.length; i++) {
                $scope.results.hits.push(response.hits[i]);
              }
            } else {
              $scope.results = response;
            }
          });
      }

      $scope.$watch(function () {
        return LocationService.search();
      }, function (n) {
        if (LocationService.path() === '/q') {
          $scope.query = n.q;
          $scope.type = n.type ? n.type : 'all';
          $scope.quickFn();
        }
      }, true);

      $scope.$watch('from', function (n) {
        $scope.limit = n > 100;
      });

      getResults();
    });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.keyword.models', ['restangular']);

  module.service('Keyword', function (Restangular) {

    this.all = function () {
      return Restangular.all('keywords');
    };

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        type: 'all'
      };

      return this.all().get('', angular.extend(defaults, params));
    };
  });
})();
