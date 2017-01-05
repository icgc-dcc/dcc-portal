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

'use strict';

////////////////////////////////////////////////////////////////////////
// Primary Repository Module
////////////////////////////////////////////////////////////////////////
angular.module('icgc.repositories', ['icgc.repositories.controllers', 'icgc.repositories.services'])
   .config(function ($stateProvider, $urlMatcherFactoryProvider) {

      // Make UI-Router ignore trailing slashes
      $urlMatcherFactoryProvider.strictMode(false);
    
      function _normalizeRepoCode(repoCode) {
         return repoCode.toLowerCase().replace(/[^\w]+/i, '.');
      }
    
      $stateProvider
        .state('ICGCcloud', {
          url: '/icgc-in-the-cloud',
          template: function () {

            var _templateStr =  '<div data-ui-view="home"></div>' +
                                '<div data-ui-view="cloud-repo-content" class="cloud-repository-container"></div>';

            return _templateStr;
          },
          abstract: true
        })
        .state('ICGCcloud.home', {
          url: '',
          views: {
            'home': {
              templateUrl: 'scripts/repositories/views/home.html',
              controller: 'RepositoriesHomeController'
            }
          }
        })
        .state('ICGCcloud.repositories', {
          url: '/:repoAlias',
          // UI-Router only instantiates RepositoriesController once
          // which is good for us in this context
          views:{
            
            'cloud-repo-content': {
              templateUrl: 'scripts/repositories/views/repos/repos.html',
              controller: 'RepositoriesController as repositoryCtrl',
              resolve: {
                // Use the explicit dependency injection
                // declaration so the code doesn't blow up
                // after its minified/uglified.
                repoMap: ['ExternalRepoService', function (ExternalRepoService) {
                  return ExternalRepoService.refreshRepoMap();
                }]
              }
            },
            'bodyContent@ICGCcloud.repositories': {
              templateUrl: function () {

                var stateParams = arguments[0],
                    context = stateParams.repoAlias.toLowerCase();

                return 'scripts/repositories/views/repos/repos.' +
                       _normalizeRepoCode(context) + '.content.html';
              },
              controller: 'RepositoriesContentController as repositoryContentCtrl'
            }
          }
        });
    

   });



////////////////////////////////////////////////////////////////////////
// Controller Declaration
////////////////////////////////////////////////////////////////////////
angular.module('icgc.repositories.controllers', [])
   .constant('REPO_GUIDE_ALIAS_CONSTANT', 'guide')
   .constant('repoAliasMapConstants', {
         'collaboratory': 'collaboratory',
         'aws': 'aws-virginia'
    })
   .controller('RepositoriesHomeController', function($scope, Page, gettextCatalog) {
      Page.stopWork();
      Page.setPage('entity');
      Page.setTitle(gettextCatalog.getString('ICGC in the Cloud'));
   })
   .controller('RepositoriesGuideController', function($scope, Page, gettextCatalog) {
      Page.stopWork();
      Page.setPage('entity');
      Page.setTitle(gettextCatalog.getString('User Guide'));
   })
  .controller('RepositoriesController', function ($scope, Page, repoAliasMapConstants,
    RepositorySearchService, RepositoryService, $stateParams, gettextCatalog) {
     var _ctrl = this,
       _repoAlias = $stateParams.repoAlias.toLowerCase(),
       _repoContext = _.get(repoAliasMapConstants, _repoAlias, null),
       _repoDataCollectionManager = null,
       _repoCreationDate = null,
       _filterQueryStr = null,
       _repoSummaryData = null,
       _repoStats = {};

      function _capitalizeWords(str) {
         return str.replace(/[^\s]+/g, function(word) {
            return word.replace(/^[a-z]/i, function(firstLetter) {
               return firstLetter.toUpperCase();
            });
         });
      }

      function _refreshData() {
         _repoDataCollectionManager.getFileSummary()
            .then(function(repoSummaryData) {
               _repoSummaryData = repoSummaryData;
            });

         _repoDataCollectionManager.getFileStats().then(function(repoStats) {

            var chartProvider = RepositorySearchService.getChartProvider();
            _repoStats.repoDataTypes = _repoDataCollectionManager.orderDataTypes(repoStats.stats);
              _repoStats.primarySites = chartProvider.getSiteProjectDonorChart(repoStats.donorPrimarySite);
         });


         // Get index creation time
         _repoDataCollectionManager.getMetaData().then(function(repoMetaData) {
            if (angular.isObject(repoMetaData) && angular.isDefined(repoMetaData)) {
               _repoCreationDate = repoMetaData.creation_date;
            }
            else {
               console.warn('Expected a repository object with a creation date but got: ', repoMetaData);
            }
         });

         _filterQueryStr = JSON.stringify(_repoDataCollectionManager.buildRepoFilter());
      }


      function _init() {
         Page.stopWork();
         Page.setPage('entity');
         /// ${repoContext} would be a noun
         Page.setTitle(_.template(gettextCatalog.getString('ICGC in the Cloud - ${repoContext} Repository'))({
            repoContext : _capitalizeWords(_repoContext)
         }));
        //  Page.setTitle(gettextCatalog.getString('ICGC in the Cloud')+ ' - ' + 
        //  _capitalizeWords(_repoContext) + ' ' + gettextCatalog.getString('Repository'));


         // In this case we are querying by the repo name which is indexed.
         // We currently cannot search by Repo Code and there
         // are no relevant filter facets in the UI to represent Repo Code
         // queries.
         try {
            _repoDataCollectionManager = RepositorySearchService.getRepoDataCollectionManagerFactory(_repoContext);
         }
         catch (e) {
            console.error(e, '\nAborting data refresh...');
            return;
         }


         _refreshData();
      }
     
        // Initialize the controller
        _init();  

      //



      ////////////////////////////////////////////////////////////////////////
      // Controller Public API
      ////////////////////////////////////////////////////////////////////////
      _ctrl.getRepoDataTypes = function() {
         return _repoStats.repoDataTypes || null;
      };

      _ctrl.getPrimarySites = function() {
         return _repoStats.primarySites || null;
      };

      _ctrl.getRepoContextID = function() {
         return _repoContext;
      };

      _ctrl.getRepoSummaryData = function() {
         return _repoSummaryData;
      };

      _ctrl.hasSummaryData = function() {
         return _repoSummaryData !== null;
      };

      _ctrl.getRepoCreationDate = function() {
         return    _repoCreationDate;
      };

      _ctrl.getFilterQueryStr = function() {
         return _filterQueryStr;
      };

      _ctrl.getRepoName = () => {
        if(_.contains(_repoContext, 'aws')){
          return 'AWS - Virginia';
        } else if(_.contains(_repoContext, 'collaboratory')) {
          return 'Collaboratory - Toronto';
        }
      };
});


////////////////////////////////////////////////////////////////////////
// Services Declaration
////////////////////////////////////////////////////////////////////////
angular.module('icgc.repositories.services', [])
   .constant('RepositoryServiceConstants', {
      DATA_TARGET_TYPE: {
         REPO: 'repoName',
         STUDY: 'study'
      }
   })
   .service('RepositorySearchService', function(   PCAWG, ExternalRepoService, Restangular,
                                    HighchartsService, RepositoryServiceConstants) {
      var _srv = this,
         _chartProvider = null;
         // constants used by the service

      var _repoServiceURLs = {
            // Absolute Path: /api/v1/repository/files/repo/stats/{{repoCode}}
            GET_REPO_STATS: 'repository/files/repo/stats',

            // uses GET param filters={'file': {'repoName':{ is: ['<repo_context>'] } } }
            GET_REPO_FILE_SUMMARY_STATS: 'repository/files/summary',

            // Absolute Path: /api/v1/repository/files/metadata
            GET_REPO_METADATA: 'repository/files/metadata'
         };


      function RepoCollectionDataManager(repoID, targettedType) {
         var _self = this,
            _repoID = repoID,
            // if null it's target is the whole repo study
            _targettedType = targettedType || RepositoryServiceConstants.DATA_TARGET_TYPE.REPO;

         function _init() {
            var repoName = ExternalRepoService.getRepoNameFromCode(_repoID);

            if (repoName === null) {
               throw new Error('Could not find repository name with the repo code: ' + _repoID + '\nAborting...');
            }

            _self._repoID = _repoID;
            _self._repoName = repoName;
            _self._targettedType = _targettedType;
         }


         _init();

         return this;
      }

      RepoCollectionDataManager.prototype.buildRepoFilter = function(datatype) {
         var filter = { file: {} };

         filter.file[this._targettedType] =  {
               is: [this._repoName]
            };

         if (angular.isDefined(datatype)) {
            filter.file.dataType = {
               is: [datatype]
            };
         }

         return filter;
      };


      /**
      * Reorder for UI, the top 5 items are fixed, the remining are appended to the end
      * on a first-come-first-serve basis.
      */
      RepoCollectionDataManager.prototype.orderDataTypes = function(data) {
         var _precedence = PCAWG.precedence(),
            _list = [],
            _self = this;


         // Scrub
         var dataTypeData = Restangular.stripRestangular(data);

         // Flatten and normalize for display
         Object.keys(dataTypeData).forEach(function(key) {
            _list.push({
               name: key,
               uiName: PCAWG.translate(key),
               donorCount: +data[key].donorCount,
               fileCount: +data[key].fileCount,
               fileSize: +data[key].fileSize,
               fileFormat: data[key].dataFormat,
               filters: JSON.stringify(_self.buildRepoFilter(key))
            });
         });

         // Sort
         return _.sortBy(_list, function(d) {
            return _precedence.indexOf(d.name);
         });

      };


      // set up prototypes so we don't duplicate logic unnecessarily

      /**
      * Get pancancer statistics - This uses ICGC's external repository end point
      * datatype
      *   - # donors
      *   - # files
      *   - file size
      */
      RepoCollectionDataManager.prototype.getFileStats = function() {
      return Restangular.one(_repoServiceURLs.GET_REPO_STATS, this._repoID).get();
      };

      RepoCollectionDataManager.prototype.getFileSummary = function() {
         var params = { filters: this.buildRepoFilter() };

         return    Restangular
                  .one(_repoServiceURLs.GET_REPO_FILE_SUMMARY_STATS)
                  .get(params);
      };

      RepoCollectionDataManager.prototype.getMetaData = function() {
         return Restangular
               .one(_repoServiceURLs.GET_REPO_METADATA)
               .get();
      };


      function ChartProvider() {
         var _self = this;

         _self.getSiteProjectDonorChart = function(data) {
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

               bar.stack.sort(function(a, b) {
                     return b.count - a.count;
                  })
                  .forEach(function(p) {
                     p.y0 = bar.total;
                     p.y1 = bar.total + p.count;
                     bar.total += p.count;
                  });

                  list.push(bar);
               });

               // Sorted
               return list.sort(function(a, b) { return b.total - a.total });
         };

         _self.getPrimarySiteDonorChart = function(data) {
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

      }

      // Public API for Repositories Service
      _srv.getRepoDataCollectionManagerFactory = function(repoID, targettedType) {
         return new RepoCollectionDataManager(repoID, targettedType);
      };

      _srv.getChartProvider = function() {
         // lazy initialization
         if (_chartProvider === null) {
            _chartProvider = new ChartProvider();
         }

         return _chartProvider;
      };

   })
   .service('RepositoryService', require('./repository-service'))
   .controller('RepositoriesContentController', function() {});

