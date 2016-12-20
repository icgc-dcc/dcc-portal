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

  var module = angular.module('icgc.analysis', [
    'icgc.analysis.controllers',
    'icgc.share',
    'ui.router'
  ]);

  module.config(function ($stateProvider) {
    $stateProvider.state('analysis', {
      url: '/analysis',
      reloadOnSearch: false,
      templateUrl: 'scripts/analysis/views/analysis.html',
      controller: 'AnalysisController',
      data: {
        tab: 'analysis'
      }
    });
    $stateProvider.state('analysis.sets', {
      url: '/sets',
      reloadOnSearch: false,
      data : {
        tab: 'sets'
      }
    });

    // We can't seem to have a single state for /x and /x/y
    $stateProvider.state('analysis.viewhome', {
      url: '/view',
      data: {
        tab: 'view'
      }
    });
    $stateProvider.state('analysis.view', {
      url: '/view/:type/:id',
      //reloadOnSearch: false,
      data: {
        tab: 'view'
      }
    });
  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.analysis.controllers', ['icgc.analysis.services']);


  /**
   * Top level set analyses controller
   *
   * AnalysisController: view analysis
   *   - SavedSetController: manage saved sets
   *   - AnalysisListController: manage saved analysis
   *   - NewAnalysisController: creates new analysis
   */
  module
    .constant('DEFAULT_SELECTED_TAB_CONSTANT', 'analysis')
    .controller('AnalysisController', function ($scope, $timeout, $state, $location, Page,
                                                AnalysisService, DEFAULT_SELECTED_TAB_CONSTANT, gettextCatalog) {

    Page.setPage('analysis');
    Page.setTitle(gettextCatalog.getString('Analysis'));


    $scope.currentTab = $state.current.data.tab || DEFAULT_SELECTED_TAB_CONSTANT;
    $scope.savedAnalyses = AnalysisService.getAll();

    $scope.AnalysisService = AnalysisService;

    $scope.$watch(function () {
      return $state.current.data && $state.current.data.tab;
    }, function () {
      $scope.currentTab = $state.current.data.tab || DEFAULT_SELECTED_TAB_CONSTANT;
    });

    $scope.$watch(function() {
      return $state.params;
    }, function() {
      $scope.analysisId = $state.params.id;
      $scope.analysisType = $state.params.type === 'set'? 'union' : $state.params.type;
      init();
    });


    $scope.newAnalysis = function() {

      if ($scope.analysisId !== undefined) {
        $location.path('analysis');
      } else {
        $scope.$broadcast('analysis::reload', {});
      }
    };

    var analysisPromise;
    var pollTimeout;


    function wait(id, type) {
      $scope.error = null;

      var promise = AnalysisService.getAnalysis(id, type);
      promise.then(function(data) {
        var rate = 1000;

        if (data.state !== 'FINISHED') {
          $scope.analysisResult = data;

          if (data.state === 'POST_PROCESSING') {
            rate = 4000;
          }
          pollTimeout = $timeout(function() {
            wait(id, type);
          }, rate);
        } else if (data.state === 'FINISHED') {
          $scope.analysisResult = data;
        }
      }, function() {
        $scope.error = true;
      });
    }

    function init() {
      $timeout.cancel(pollTimeout);
      $scope.error = null;
      $scope.analysisResult = undefined;

      if (! $scope.analysisId || ! $scope.analysisType) {
        return;
      }

      var id = $scope.analysisId, type = $scope.analysisType;
      var promise = AnalysisService.getAnalysis(id, type);

      promise.then(function(data) {
        if (! _.isEmpty(data)) {
          AnalysisService.addAnalysis(data, type);
        } else {
          $scope.error = true;
          return;
        }

        if (data.state === 'FINISHED') {
          $scope.analysisResult = undefined;
          $timeout(function() {
            $scope.analysisResult = data;
          }, 150);
          return;
        }

        // Kick off polling if not finished
        wait(id, type);

      }, function() {
        $scope.error = true;
      });
    }


    $scope.$on('$locationChangeStart', function() {
      // Cancel any remaining polling requests
      $timeout.cancel(pollTimeout);
    });

    // Clea up
    $scope.$on('destroy', function() {
      $timeout.cancel(analysisPromise);
    });

  });

})();



(function () {
  'use strict';

  var module = angular.module('icgc.analysis.services', ['restangular']);

  module.service('AnalysisService', function(RestangularNoCache, localStorageService, gettextCatalog) {
    var ANALYSIS_ENTITY = 'analysis';
    var analysisList = [];
    var analysesStrings = {
      set: {
        name: gettextCatalog.getString('Set Operations'),
        description: gettextCatalog.getString('Display Venn diagram and find out intersection or union,' + 
          ' etc. of your sets of the same type.'),
        demoDescription: gettextCatalog.getString('Compare high impact mutations in brain cancers across' + 
          ' GBM-US, LGG-US, and PCBA-DE.')
      },
      get union () {
        return this.set;
      },
      enrichment: {
        name: gettextCatalog.getString('Enrichment Analysis'),
        description: gettextCatalog.getString('Find out statistically significantly over-represented groups of ' +
          'gene sets (e.g. Reactome pathways) when comparing with your gene set.'),
        demoDescription: gettextCatalog.getString('Perform enrichment analysis on top 50 genes in Cancer Gene Census.')
      },
      phenotype: {
        name: gettextCatalog.getString('Cohort Comparison'),
        description: gettextCatalog.getString('Display the survival analysis of your donor sets and compare some' +
        ' characteristics such as gender, vital status and age at diagnosis between your donor sets.'),
        demoDescription: gettextCatalog.getString('Display survival analysis and compare phenotypes across ' +
          'donors with pancreatic cancer with and without mutations in the gene KRAS.')
      },
      oncogrid: {
        name: gettextCatalog.getString('OncoGrid'),
        description: gettextCatalog.getString('Display OncoGrid diagram to visualize genetic alteration occurrences' + 
          ' affecting a set of donors.'),
        demoDescription: gettextCatalog.getString('Generate an OncoGrid using top 75 donors and genes for PCAWG' +
        ' liver projects.')
      },
      survival: {
        name: gettextCatalog.getString('Survival Analysis'),
        description: gettextCatalog.getString('Display the survival analysis of your donor sets and compare some' +
        ' characteristics such as gender, vital status and age at diagnosis between your donor sets.'),
        demoDescription: 'TBD'
      }
    };

    this.getAnalysis = function(id, type) {
      return RestangularNoCache.one('analysis/' + type , id).get();
    };

    this.getAll = function() {
      return analysisList;
    };

    this.removeAll = function() {
      analysisList = [];
      localStorageService.set(ANALYSIS_ENTITY, analysisList);
    };

    this.analysisName = function(type) {
      return _.get(analysesStrings[type], 'name', '???');
    };

    this.analysisDemoDescription = function(type) {
      return _.get(analysesStrings[type], 'demoDescription', '???dd');
    };

    this.analysisDescription = function(type) {
      return _.get(analysesStrings[type], 'description', '???d');
    };


    /**
     * Add analysis to local storage
     */
    this.addAnalysis = function(analysis, type) {
      var ids = _.pluck(analysisList, 'id');
      if (_.contains(ids, analysis.id) === true) {
        return;
      }

      var payload = {
        id: analysis.id,
        timestamp: analysis.timestamp || '--',
        // Type is used in the UI route, we convert union to set so it makes more logical sense
        type: type === 'union'? 'set' : type
      };

      if (type === 'enrichment') {
        payload.universe = analysis.params.universe;
        payload.maxGeneCount = analysis.params.maxGeneCount;
      } else if (type === 'phenotype') {
        payload.dataType = 'donor';
        payload.inputSetCount = analysis.inputCount || '';
      } else if (type === 'oncogrid') {
        payload.dataType = 'oncogrid';
        payload.geneCount = analysis.geneCount;
        payload.donorCount = analysis.donorCount;
      } else {
        payload.dataType = analysis.type.toLowerCase();
        payload.inputSetCount = analysis.inputCount || '';
      }

      analysisList.unshift( payload );
      localStorageService.set(ANALYSIS_ENTITY, analysisList);
    };

    this.remove = function(id) {
      var ids = _.pluck(analysisList, 'id');

      if (_.contains(ids, id)) {
        var index = ids.indexOf(id);
        analysisList.splice(index, 1);
        localStorageService.set(ANALYSIS_ENTITY, analysisList);
        return true;
      }
      return false;
    };

    // Init service
    analysisList = localStorageService.get(ANALYSIS_ENTITY) || [];

  });

})();
