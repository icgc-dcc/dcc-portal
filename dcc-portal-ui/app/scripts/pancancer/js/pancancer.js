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

  var module = angular.module('icgc.pancancer', [
    'icgc.pancancer.controllers',
    'icgc.pancancer.services'
  ]);

  module.config(function($stateProvider) {
    $stateProvider.state('pancancer', {
      url: '/pcawg',
      templateUrl: 'scripts/pancancer/views/pancancer.html',
      controller: 'PancancerController as PancancerController'
    });

    $stateProvider.state('pancancer_ack', {
      url: '/pcawg/nature/commentary/acknowledgements',
      templateUrl: 'scripts/pancancer/views/pancancer.ack.html',
      controller: 'PancancerAcknowledgementController'
    });
  });

})();


(function() {
  'use strict';

  var module = angular.module('icgc.pancancer.controllers', []);

  module.controller('PancancerAcknowledgementController', function($scope, Page) {
    Page.stopWork();
    Page.setPage('entity');
    Page.setTitle('PCAWG');
  }); 
  

  module.controller('PancancerController',
    function($scope, Page, PancancerService, ExternalRepoService, RouteInfoService) {

    Page.stopWork();
    Page.setPage('entity');
    Page.setTitle('PCAWG');

    $scope.dataRepoUrl = RouteInfoService.get ('dataRepositories').href;

    function refresh() {
      // Get stats
      PancancerService.getPancancerStats().then(function(data) {
        $scope.pcawgDatatypes = PancancerService.orderDatatypes(data.stats);
        $scope.primarySites = PancancerService.getSiteProjectDonorChart(data.donorPrimarySite);
      });

      // Get overall summary
      PancancerService.getPancancerSummary().then(function(data) {
        $scope.summary = data;
      });

      // Get index creation time
      ExternalRepoService.getMetaData().then(function(data) {
        $scope.indexDate = data.creation_date || '';
      });
    }

    $scope.filters = PancancerService.buildRepoFilterStr();

    refresh();
  });

})();

(function() {
  'use strict';

  var module = angular.module('icgc.pancancer.services', []);

  module.service('PancancerService', function(Restangular, PCAWG, HighchartsService) {

    function buildRepoFilterStr(datatype) {
      var filters = {
        file: {
          study: {is: ['PCAWG']}
        }
      };

      if (angular.isDefined(datatype)) {
        filters.file.dataType = {
          is: [datatype]
        };
      }

      return JSON.stringify(filters);
    }

    this.buildRepoFilterStr = buildRepoFilterStr;


    this.getSiteProjectDonorChart = function(data) {
      var list = [];

      // Stack friendly format
      Object.keys(data).forEach(function(siteKey) {
        var bar = {};
        bar.total = 0;
        bar.stack = [];
        bar.key = siteKey;

        Object.keys(data[siteKey]).forEach(function(projKey) {
          bar.stack.push({
            name: projKey,
            label: projKey,
            count: data[siteKey][projKey],
            key: siteKey, // parent key
            colourKey: siteKey,
            link: '/projects/' + projKey
          });
        });

        bar.stack.sort(function(a, b) { return b.count - a.count }).forEach(function(p) {
          p.y0 = bar.total;
          p.y1 = bar.total + p.count;
          bar.total += p.count;
        });
        list.push(bar);
      });

      // Sorted
      return list.sort(function(a, b) { return b.total - a.total });
    };


    this.getPrimarySiteDonorChart = function(data) {
      var list = [];

      Object.keys(data).forEach(function(d) {
        list.push({
          id: d,
          count: data[d],
          colour: HighchartsService.getPrimarySiteColourForTerm(d)
        });
      });
      list = _.sortBy(list, function(d) { return -d.count });

      return HighchartsService.bar({
        hits: list,
        xAxis: 'id',
        yValue: 'count'
      });
    };


    /**
     * Reorder for UI, the top 5 items are fixed, the remining are appended to the end
     * on a first-come-first-serve basis.
     */
    this.orderDatatypes = function(data) {
      var precedence = PCAWG.precedence();
      var list = [];


      // Scrub
      data = Restangular.stripRestangular(data);

      // Flatten and normalize for display
      Object.keys(data).forEach(function(key) {
        list.push({
          name: key,
          uiName: PCAWG.translate(key),
          donorCount: +data[key].donorCount,
          fileCount: +data[key].fileCount,
          fileSize: +data[key].fileSize,
          fileFormat: data[key].dataFormat,
          filters: buildRepoFilterStr(key)
        });
      });

      // Sort
      return _.sortBy(list, function(d) {
        return precedence.indexOf(d.name);
      });

    };


    /**
     * Get pancancer statistics - This uses ICGC's external repository end point
     * datatype
     *   - # donors
     *   - # files
     *   - file size
     */
    this.getPancancerStats = function() {
      return Restangular.one('repository/files/pcawg/stats').get({});
    };

    this.getPancancerSummary = function() {
      var param = {
        filters: {file: { study: {is: ['PCAWG']}}}
      };
      return Restangular.one('repository/files/summary').get(param);
    };
  });
})();
