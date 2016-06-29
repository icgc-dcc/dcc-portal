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

  var module = angular.module('icgc.downloader', []);

  /**
   * Requesting dynamic download
   */
  module.controller('DownloadRequestController', function($scope, $location, $window, $modalInstance,
    $filter, API, Donors, LocationService, DownloaderService, DataType, filters, RouteInfoService) {

    var _isSendingRequest = false;

    $scope.params = {};
    $scope.params.processing = false;
    $scope.params.dataTypes = [];

    $scope.totalSize = 0;
    $scope.dataReleasesRouteInfo = RouteInfoService.get ('dataReleases');

    if (! filters) {
      filters = LocationService.filters();
    }

    function sum(active, size) {
      if (active) {
        $scope.dlTotal += size;
        $scope.dlFile++;
      } else {
        $scope.dlTotal -= size;
        $scope.dlFile--;
      }
    }

    function reset() {
      $scope.dlTotal = 0;
      $scope.dlFile = 0;
      $scope.overallSize = 0;
    }

    function sortFunc(dataType) {
      var index = DataType.precedence().indexOf(dataType.label);
      if (index === -1) {
        return DataType.precedence().length + 1;
      }
      return index;
    }

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.toggle = function (datatype) {
      if (datatype.sizes > 0) {
        datatype.active = !datatype.active;
        sum(datatype.active, datatype.sizes);
      }
    };

    $scope.isSendingRequest = function() {
      return _isSendingRequest;
    };

    $scope.download = function() {
      var i, item, actives, linkURL;

      // Prevent sending the request multiple times
      if (_isSendingRequest) {
        return;
      }

      _isSendingRequest = true;

      actives = [];
      for (i = 0; i < $scope.params.dataTypes.length; ++i) {
        item = $scope.params.dataTypes[i];
        if (item.active) {
          actives.push({key: item.label, value: 'TSV'});
        }
      }
      linkURL = $location.protocol() + '://' + $location.host() + ':' + $location.port() + '/download';

      DownloaderService
        .requestDownloadJob(filters, actives, null,
          linkURL, JSON.stringify(filters)).then(function (job) {
            $window.location.assign(API.BASE_URL + '/download/' + job.downloadId);
            $modalInstance.dismiss('cancel');
        })
        .finally(function() {
          _isSendingRequest = false;
        });
    };

    $scope.calculateSize = function () {
      $scope.params.processing = true;
      reset();

      $scope.dlFile = 0;
      $scope.dlTotal = 0;

      // Compute the total number of donors
      Donors.handler.get('count', {filters: filters}).then(function (data) {
        $scope.totalDonor = data;
      });

      DownloaderService.getSizes(filters).then(function (response) {
        $scope.params.dataTypes = response.fileSize;
        $scope.params.dataTypes.forEach(function (dataType) {
          dataType.active = false;
          dataType.uiLabel = dataType.label;
          dataType.uiLabel = DataType.tooltip(dataType.label);
          $scope.overallSize += dataType.sizes;
        });

        // Re-order it based on importance
        $scope.params.dataTypes = $filter('orderBy')($scope.params.dataTypes, sortFunc);

        $scope.params.processing = false;
      });
    };


    $scope.calculateSize();
  });
})();
