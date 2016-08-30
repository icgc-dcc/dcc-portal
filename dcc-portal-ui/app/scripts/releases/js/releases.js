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

  var module = angular.module('icgc.releases', ['icgc.portalfeature','icgc.releases.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('home', {
      url: '/',
      templateUrl: '/scripts/releases/views/home.html',
      controller: 'ReleaseCtrl as ReleaseCtrl',
      resolve: {
        release: ['Releases', function (Releases) {
          return Releases.getCurrent();
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.releases.controllers', ['icgc.releases.models']);

  module.controller('ReleaseCtrl', function (Page, HighchartsService, Releases, Projects, Settings, gettextCatalog) {
    var _ctrl = this;
    Page.setTitle(gettextCatalog.getString('Welcome'));
    Page.setPage('home');

    _ctrl.routeToProjectPageWithLiveDonorStateFilter = function () {
      var liveDonorStateFilter = {
        project: {
          state: {
            is: ['live']
          }
        }
      };

      return '/projects?filters=' + angular.toJson (liveDonorStateFilter);
    };
    _ctrl.routeToAdvancedSearchPageWithLiveDonorStateFilter = function () {
      var liveDonorStateFilter = {
        donor: {
          state: {
            is: ['live']
          }
        }
      };

      return '/search?filters=' + angular.toJson (liveDonorStateFilter);
    };

    function successP(projects) {
      _ctrl.donut = HighchartsService.donut({
        data: projects.hits,
        type: 'project',
        innerFacet: 'primarySite',
        outerFacet: 'id',
        countBy: 'totalDonorCount'
      });
    }

    function successR(release) {
      _ctrl.release = release;
    }

    function successSettings(settings) {
      // Date in the settings file
      _ctrl.applicationReleaseDate = settings.releaseDate;
    }

    Releases.getCurrent().then(successR);
    Projects.getList().then(successP);
    Settings.get().then(successSettings);
  });
})();

(function () {
  'use strict';

  var module = angular.module('icgc.releases.models', []);

  module.service('Releases', function (Restangular, LocationService, Release) {
    this.handler = Restangular.all('releases');

    this.getList = function (params) {
      var defaults = {
        size: 10,
        from: 1,
        filters: LocationService.filters()
      };

      return this.handler.getList(angular.extend(defaults, params));
    };

    this.getCurrent = function (params) {
      return this.handler.one('current').get(params);
    };

    this.one = function (id) {
      return id ? Release.init(id) : Release;
    };
  });

  module.service('Release', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.handler = Restangular.one('releases', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };
  });
})();
