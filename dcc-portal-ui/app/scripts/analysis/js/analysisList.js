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

  /**
   * Controls list of existing analysis (bench)
   */
  module.controller('AnalysisListController', function($window, $location, AnalysisService, gettextCatalog) {
    var _this = this;

    var REMOVE_ONE = gettextCatalog.getString('Are you sure you want to remove this analysis?');
    var REMOVE_ALL = gettextCatalog.getString('Are you sure you want to remove all analysis?');

    _this.newAnalysis = function() {
      $location.path('analysis').search({});
    };

    _this.getAnalysis = function(id, type) {
      var routeType = type;

      if (type === 'union') {
        routeType = 'set';
      }

      if (id) {
        $location.path('analysis/view/' + routeType + '/' + id);
      } else {
        $location.path('analysis');
      }
    };

    _this.removeAllAnalyses = function() {
      var confirmRemove;
      confirmRemove  = $window.confirm(REMOVE_ALL);
      if (confirmRemove) {
        AnalysisService.removeAll();
        _this.analysisList = AnalysisService.getAll();
        $location.path('analysis');
      }
    };

    _this.analysisName = AnalysisService.analysisName;


    _this.remove = function(id, selectedId) {
      var confirmRemove = window.confirm(REMOVE_ONE);
      if (! confirmRemove) {
        return;
      }

      // 1) Deletion of item that is not currently selected
      if (id !== selectedId) {
        AnalysisService.remove(id);
        return;
      }


      // 2) Delete of item we are on, need to find out where to move next
      var hasNext = _this.analysisList.length > 1? true : false;

      if (hasNext === true) {
        var currentIndex = _.findIndex(_this.analysisList, function(analysis) {
          return analysis.id === id;
        });
        var max = _this.analysisList.length - 1;
        var nextIndex = currentIndex <  max ? currentIndex+1 : currentIndex-1;
        var nextAnalysis = _this.analysisList[nextIndex];
        if (AnalysisService.remove(id) === true) {
          $location.path('analysis/view/' + nextAnalysis.type + '/' + nextAnalysis.id);
        }

      } else {
        if (AnalysisService.remove(id) === true) {
          $location.path('analysis');
        }
      }
    };

    _this.analysisList = AnalysisService.getAll();
  });

})();


