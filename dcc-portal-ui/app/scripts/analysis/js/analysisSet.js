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

  var module = angular.module('icgc.analysis.controllers');

  module.controller('SavedSetController', function($scope, $rootScope, $window, $location, $timeout, $modal,
    SetService, LocationService, RouteInfoService, Extensions, gettextCatalog) {

    var _this = this;
    _this.entitySets = SetService.getAll();
    _this.selectedSets = [];
    _this.checkAll = false;
    _this.dataRepoTitle = RouteInfoService.get ('dataRepositories').title;

    // Selected sets
    _this.update = function() {
      _this.selectedSets = [];
      _this.entitySets.forEach(function(set) {
        if (set.checked === true) {
          _this.selectedSets.push(set);
        }
      });
    };

    _this.newAnalysis = function() {
      $location.path('analysis');
    };

    /* Select all / de-select all */
    _this.toggleSelectAll = function() {
      _this.checkAll = !_this.checkAll;

      _this.entitySets.forEach(function(set) {
        set.checked = _this.checkAll;
      });
    };

    _this.getEntitySetShareParams = function(item) {
      // Only do this assignment operation once - otherwise return the
      // cached value.
      if (angular.isDefined(item.entitySetShareParams)) {
        return item.entitySetShareParams;
      }


      var base = item.type === 'file' ? 'repositories' : 'search';
      var shareParams = {
        url: LocationService.buildURLFromPath(base +
                                              (item.advType !== '' ? ('/' +  item.advType) : '')),
        filters: JSON.stringify(item.advFilters)
      };

      item.entitySetShareParams = shareParams;

      return item.entitySetShareParams;
    };

    _this.exportSet = function(id) {
      SetService.exportSet(id);
    };

    _this.downloadDonorData = function(id) {
      $modal.open({
        templateUrl: '/scripts/downloader/views/request.html',
        controller: 'DownloadRequestController',
        resolve: {
          filters: function() { return {donor:{id:{is:[Extensions.ENTITY_PREFIX + id]}}} }
        }
      });
    };


    _this.addCustomGeneSet = function() {
      $modal.open({
        templateUrl: '/scripts/genelist/views/upload.html',
        controller: 'GeneListController'
      });
    };


    /* To remove unretrievable sets from local store*/
    _this.removeInvalidSets = function() {
      var toRemove = _.filter(_this.entitySets, function(set) {
        return set.state !== 'FINISHED';
      });
      if (toRemove.length > 0) {
        SetService.removeSeveral(_.pluck(toRemove, 'id'));
        _this.checkAll = false; // reset
      }
    };


    /* User inititated set removal */
    _this.removeSets = function() {
      var confirmRemove = $window.confirm(gettextCatalog.getString('Are you sure you want to remove selected sets?'));
      if (!confirmRemove || confirmRemove === false) {
        return;
      }

      var toRemove = _this.selectedSets;
      if (toRemove.length > 0) {
        SetService.removeSeveral(_.pluck(toRemove, 'id'));
        _this.checkAll = false; // reset
      }
    };

    _this.renameSet = SetService.renameSet;

    $rootScope.$on(SetService.setServiceConstants.SET_EVENTS.SET_CHANGE_EVENT, () => {
      _this.entitySets = SetService.getAll();
    });

  });


})();
