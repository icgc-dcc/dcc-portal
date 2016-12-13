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

  var module = angular.module('icgc.projects', ['icgc.projects.controllers', 'ui.router']);

  module.config(function ($stateProvider) {
    $stateProvider.state('projects', {
      url: '/projects?filters',
      reloadOnSearch: false,
      templateUrl: 'scripts/projects/views/projects.html',
      controller: 'ProjectsCtrl as ProjectsCtrl',
      data: {tab: 'summary', isProject: true}
    });

    $stateProvider.state('projects.details', {
      url: '/details',
      reloadOnSearch: false,
      data: {tab:'details', isProject: true}
    });

    $stateProvider.state('projects.summary', {
      url: '/summary',
      reloadOnSearch: false,
      data: {tab:'summary', isProject: true}
    });

    $stateProvider.state('projects.history', {
      url: '/history',
      reloadOnSearch: false,
      data: {tab:'history', isProject: true}
    });

    $stateProvider.state('project', {
      url: '/projects/:id',
      templateUrl: 'scripts/projects/views/project.html',
      controller: 'ProjectCtrl as ProjectCtrl',
      resolve: {
        project: ['$stateParams', 'Projects', 
        function ($stateParams, Projects) {
          return Projects.one($stateParams.id).get().then(function(project){
            return project;
          });
        }]
      }
    });
  });
})();

(function () {
  'use strict';

  var toJson = angular.toJson;
  var module = angular.module('icgc.projects.controllers', ['icgc.projects.models']);

  module.controller('ProjectsCtrl',
    function ($q, $scope, $state, $filter, ProjectState, Page, Projects, CodeTable,
               HighchartsService, Donors, Restangular, LocationService, gettextCatalog, $timeout) {

    var _ctrl = this;
    Page.setTitle(gettextCatalog.getString('Cancer Projects'));
    Page.setPage('projects');

    _ctrl.Page = Page;
    _ctrl.state = ProjectState;
    _ctrl.shouldRenderDeferredItems = false;

    _ctrl.setTab = function (tab) {
        _ctrl.state.setTab(tab);
      };
    _ctrl.setTab($state.current.data.tab);


    var deferredTimeout;
    function allowRenderDeferredItems() {
      _ctrl.shouldRenderDeferredItems = true;
    }

    $scope.$watch(function () {
       var currentStateData = angular.isDefined($state.current.data) ? $state.current.data : null;

      if (! currentStateData ||
          ! angular.isDefined(currentStateData.isProject) ||
          ! angular.isDefined(currentStateData.tab)) {
        return null;
      }

        return currentStateData.tab;
      },
      function (currentTab) {
        if (currentTab !== null) {
          _ctrl.setTab(currentTab);
        }

        if ( currentTab === 'details') {
          if (!window.requestIdleCallback) {
            window.clearTimeout(deferredTimeout);
            deferredTimeout = $timeout(allowRenderDeferredItems, 100);
          } else {
            window.requestIdleCallback(() => {
              $scope.$apply(allowRenderDeferredItems);
            });
          }
        }
      });

    $scope.countryCode = CodeTable.countryCode;

    var pathMapping = {
      ids: 'donor.projectId.is',
      datatype: 'donor.availableDataTypes.is',
      state: 'donor.state.is'
    };

    function ensureObject (o) {
      return _.isPlainObject (o) ? o : {};
    }

    var isEmptyObject = _.flow (ensureObject, _.isEmpty);

    function hasQueryFilter() {
      return (! isEmptyObject (LocationService.filters()));
    }

    _ctrl.fieldKeys = [
      'totalLiveDonorCount',
      'totalDonorCount',
      'ssmTestedDonorCount',
      'cnsmTestedDonorCount',
      'stsmTestedDonorCount',
      'sgvTestedDonorCount',
      'methArrayTestedDonorCount',
      'methSeqTestedDonorCount',
      'expArrayTestedDonorCount',
      'expSeqTestedDonorCount',
      'pexpTestedDonorCount',
      'mirnaSeqTestedDonorCount',
      'jcnTestedDonorCount'];

    var fieldMapping = {
      totalLiveDonorCount: {state: 'live'},
      totalDonorCount: {},
      ssmTestedDonorCount: {datatype: 'ssm'},
      cnsmTestedDonorCount: {datatype: 'cnsm'},
      stsmTestedDonorCount: {datatype: 'stsm'},
      sgvTestedDonorCount: {datatype: 'sgv'},
      methArrayTestedDonorCount: {datatype: 'meth_array'},
      methSeqTestedDonorCount: {datatype: 'meth_seq'},
      expArrayTestedDonorCount: {datatype: 'exp_array'},
      expSeqTestedDonorCount: {datatype: 'exp_seq'},
      pexpTestedDonorCount: {datatype: 'pexp'},
      mirnaSeqTestedDonorCount: {datatype: 'mirna_seq'},
      jcnTestedDonorCount: {datatype: 'jcn'}
    };

    function buildFilter (fieldKey, projectIds) {
      var filter = _.mapValues (_.clone (_.get (fieldMapping, fieldKey, {})), function (v) {
        return [v];
      });

      if (_.isArray (projectIds) && (! _.isEmpty (projectIds))) {
        _.assign (filter, {ids: projectIds});
      }

      return _.transform (filter, function (result, value, key) {
        if (_.has (pathMapping, key)) {
          result = _.set(result, pathMapping[key], value);
        }
      });
    }

    $scope.toAdvancedSearch = function (fieldKey, projectIds) {
      var filter = {
        filters: toJson (buildFilter (fieldKey, projectIds))
      };
      return 'advanced (' + toJson (filter) + ')';
    };

    // Transforms data for the stacked bar chart
    function transform(data) {
      var list = [];

      data.forEach(function(gene) {
        var bar = {};
        bar.key = gene.symbol;
        bar.total = 0;
        bar.stack = [];

        gene.uiFIProjects.sort(function(a, b) { return a.count - b.count }).forEach(function(p) {
          bar.stack.push({
            name: p.id,
            y0: bar.total,
            y1: bar.total + p.count,
            link: '/genes/' + gene.id,
            label: p.name,
            key: gene.symbol, // Parent key
            colourKey: p.primarySite
          });
          bar.total += p.count;
        });
        list.push(bar);
      });
      return list.sort(function(a, b) { return b.total - a.total });
    }

    _ctrl.donutChartSubTitle = function () {
      var formatNumber = $filter ('number');
      var pluralizer = function (n, singular) {
        return '' + singular + (n > 1 ?  's' : '');
      };
      var toHumanReadable = function (n, singular) {
        return '' + formatNumber (n) + ' ' + pluralizer (n, singular);
      };

      /// N Donors across N projects
      var subtitle = toHumanReadable (_ctrl.totalDonors, gettextCatalog.getString('Donor'));
      var projects = _.get (_ctrl, 'projects.hits', undefined);

      /// N Donors across N projects
      return subtitle + (_.isArray (projects) ?
          ' ' + gettextCatalog.getString('across') + ' ' + toHumanReadable (projects.length, gettextCatalog.getString('Project')) : '');
     };

    _ctrl.hasDonutData = function () {
      var donutData = _ctrl.donut;
      var lengths = _.map (['inner', 'outer'], function (key) {
        return _.get (donutData, [key, 'length'], 0);
      });
      return _.sum (lengths) > 0;
    };

    _ctrl.numberOfSelectedProjectsInFilter = function () {
      var queryFilter = LocationService.filters();
      return _.get (queryFilter, 'project.id.is.length', 0);
    };

    function noHitsIn (results) {
      return 0 === _.get (results, 'hits.length', 0);
    }

    _ctrl.isLoadingData = false;

    function stopIfNoHits (data) {
      if (noHitsIn (data)) {
        _ctrl.isLoadingData = false;
        _ctrl.stacked = [];
        Page.stopWork();
        return true;
      } else {
        return false;
      }
    }

    var geneDonorCountsRestangular = null;

    function cancelInFlightAggregationAjax () {

      if (geneDonorCountsRestangular) {
        geneDonorCountsRestangular.cancelRequest();
      }

    }

    function success (data) {
      if (data.hasOwnProperty('hits')) {
        var totalDonors = 0, ssmTotalDonors = 0;

        _ctrl.projectIds = _.pluck (data.hits, 'id');

        data.hits.forEach(function (p) {
          totalDonors += p.totalDonorCount;
          ssmTotalDonors += p.ssmTestedDonorCount;
        });

        var totalRowProjectIds = hasQueryFilter() ? _ctrl.projectIds : undefined;
        _ctrl.totals = _.map (_ctrl.fieldKeys, function (fieldKey) {
          return {
            total: _.sum (data.hits, fieldKey),
            sref: $scope.toAdvancedSearch (fieldKey, totalRowProjectIds)
          };
        });

        _ctrl.totalDonors = totalDonors;
        _ctrl.ssmTotalDonors = ssmTotalDonors;

        _ctrl.projects = data;
        _ctrl.donut = HighchartsService.donut({
          data: data.hits,
          type: 'project',
          innerFacet: 'primarySite',
          outerFacet: 'id',
          countBy: 'totalDonorCount'
        });

        // Get project-donor-mutation distribution of exon impacted ssm
        Restangular.one('ui', '').one('search/projects/donor-mutation-counts', '').get({}).then(function(data) {
          // Remove restangular attributes to make data easier to parse
          data = Restangular.stripRestangular(data);
          _ctrl.distribution = data;
        });

        cancelInFlightAggregationAjax();
        if (stopIfNoHits (data)) {return}

        var mutationFilter = {
          mutation: {
            functionalImpact: {is: ['High']}
          }
        };

        Projects.several (_ctrl.projectIds.join()).get ('genes', {
          include: 'projects',
          filters: mutationFilter,
          size: 20
        }).then (function (genes) {
          // About to launch a new ajax getting project aggregation data. Cancel any active call.
          cancelInFlightAggregationAjax();

          if (stopIfNoHits (genes)) {return}

          geneDonorCountsRestangular = Restangular
            .one('ui/search/gene-project-donor-counts/' + _.map(genes.hits, 'id').join(','));

          _ctrl.isLoadingData = true;

          geneDonorCountsRestangular
            .get ({'filters': mutationFilter})
            .then (function (geneProjectFacets) {

              genes.hits.forEach (function (gene) {
                var uiFIProjects = [];

                geneProjectFacets[gene.id].terms.forEach(function (t) {
                  var proj = _.find(data.hits, function (p) {
                    return p.id === t.term;
                  });

                  if (angular.isDefined(proj)) {
                    uiFIProjects.push({
                      id: t.term,
                      name: proj.name,
                      primarySite: proj.primarySite,
                      count: t.count
                    });
                  }
                });
                gene.uiFIProjects = uiFIProjects;
              });

              _ctrl.isLoadingData = false;
              _ctrl.stacked = transform (genes.hits);
              geneDonorCountsRestangular = null;
            });
        });

        // Id to primary site
        var id2site = {};
        _ctrl.projects.hits.forEach(function(h) {
           id2site[h.id] = h.primarySite;
        });

        Restangular.one('projects/history', '').get({}).then(function(data) {
          // Remove restangular attributes to make data easier to parse
          data = Restangular.stripRestangular(data);
          data.forEach(function(dataPoint) {
            dataPoint.colourKey = id2site[dataPoint.group];
          });

          _ctrl.donorData = data;
        });
      }
    }

    function refresh() {
      _ctrl.isLoadingData = true;

      // Needs to first grab every single project for projectIdLookupMap. Otherwise could be missing from map.
      Projects.getList({from: 1, size:100, filters:{}}).then(function(data) {
        _ctrl.projectIDLookupMap = _.mapKeys(data.hits, function(project) {
          return project.id;
        });
        
        Projects.getList({include: 'facets'}).then(success);
      });
    }

    _ctrl.countryIconClass = function (countryName) {
      var defaultValue = '';
      var countryCode = CodeTable.countryCode (countryName);

      return _.isEmpty (countryCode) ? defaultValue : 'flag flag-' + countryCode;
    };

    _ctrl.viewInRepositories = () => {
      LocationService.goToPath('/repositories', `filters={"file":{ "projectCode":{"is":[${ _.map(_ctrl.projects.hits, (project) => `"${project.id}"`, []) }]}}}`);
    };

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.match(new RegExp('^' + window.location.protocol + '//' + window.location.host + '/projects'))) {
        // NOTE: need to defer this call till next tick due to this running before filters are updated
        setTimeout(refresh);
      }
    });

    refresh();
  });

  module.controller('ProjectCtrl', function ($scope, $window, $q, $location, Page, PubMed, project,
    Donors, Mutations, API, ExternalLinks, PCAWG, RouteInfoService, LoadState, SetService, Restangular, 
    LocationService, SurvivalAnalysisLaunchService) {
    var _ctrl = this;

    Page.setTitle(project.id);
    Page.setPage('entity');
    
    var loadState = new LoadState();

    $scope.registerLoadState = loadState.addContributingLoadState;
    $scope.deregisterLoadState = loadState.removeContributingLoadState;

    _ctrl.loadState = loadState;

    var dataRepoRouteInfo = RouteInfoService.get ('dataRepositories');
    var dataRepoUrl = dataRepoRouteInfo.href;

    var dataReleasesRouteInfo = RouteInfoService.get ('dataReleases');

    _ctrl.dataRepoTitle = dataRepoRouteInfo.title;
    _ctrl.dataReleasesTitle = dataReleasesRouteInfo.title;
    _ctrl.dataReleasesUrl = dataReleasesRouteInfo.href;

    _ctrl.hasExp = !_.isEmpty(project.experimentalAnalysisPerformedSampleCounts);
    _ctrl.isPCAWG = PCAWG.isPCAWGStudy;

    _ctrl.project = project;
    _ctrl.ExternalLinks = ExternalLinks;

    _ctrl.isPendingDonor = _.isUndefined (_.get(project, 'primarySite'));

    var projectFilter = {
      file: {
        projectCode: {
          is: [project.id]
        }
      }
    };

    _ctrl.urlToExternalRepository = function () {
      return dataRepoUrl + '?filters=' + angular.toJson (projectFilter);
    };

    if (!_ctrl.project.hasOwnProperty('uiPublicationList')) {
      _ctrl.project.uiPublicationList = [];
    }

    function success(data) {
      _ctrl.project.uiPublicationList.push(data);
    }

    if (_ctrl.project.hasOwnProperty('pubmedIds')) {
      _ctrl.project.pubmedIds.forEach(function (pmid) {
        PubMed.get(pmid).then(success);
      });
    }

    _ctrl.downloadSample = function () {
      $window.location.href = API.BASE_URL + '/projects/' + project.id + '/samples';
    };

    function createSets() {
      var filter = {
        donor: {
          projectId: {
            is: [project.id]
          }
        }
      };

      var donorParams = {
        filters: filter,
        size: 3000,
        isTransient: true,
        name: project.id +  ' Donors',
        sortBy: 'ssmAffectedGenes',
        sortOrder: 'DESCENDING',
      };

      var geneParams = {
        filters: filter,
        size: 50,
        isTransient: true,
        name: 'Top 50 ' + project.id + ' Mutated Genes',
        sortBy: 'affectedDonorCountFiltered',
        sortOrder: 'DESCENDING',
      };

      return {
        donorSet: SetService.createEntitySet('donor', donorParams),
        geneSet: SetService.createEntitySet('gene', geneParams)
      };
    }

    function createOncoGrid(sets) {
      var payload = {
        donorSet: sets.donorSet,
        geneSet: sets.geneSet
      };
      
      return Restangular
        .one('analysis')
        .post('oncogrid', payload, {}, { 'Content-Type': 'application/json' })
        .then(function (data) {
          if (!data.id) {
            throw new Error('Received invalid response from analysis creation');
          }
          $location.path('analysis/view/oncogrid/' + data.id);
        });
    }

    _ctrl.openOncogrid = function() {
      var sets = createSets();
      $q.all(sets).then(function(response) {
        createOncoGrid({donorSet: response.donorSet.id, geneSet: response.geneSet.id});
      });
    };

    function refresh() {
      var params = {
        filters: {donor: {projectId: {is: [project.id]}}},
        size: 0,
        include: ['facets']
      };

      // Get mutation impact for side panel
      var fetchAndUpdateMutations = Mutations.getList(params).then(function (d) {
        _ctrl.mutationFacets = d.facets;
      });

      // Get study facets for summay section
      var fetchAndUpdateStudies = Donors.getList(params).then(function(d) {
        _ctrl.studies = d.facets.studies.terms || [];

        // Remove no-data term
        _.remove(_ctrl.studies, function(t) {
          return t.term === '_missing';
        });

        // Link back to adv page
        _ctrl.studies.forEach(function(t) {
          t.advQuery = {
            donor: {
              projectId: {is: [project.id]},
              studies: {is: [t.term]}
            }
          };

        });
      });

      loadState.loadWhile($q.all([ fetchAndUpdateMutations, fetchAndUpdateStudies ]));
    }

    /**
       * Run Survival/Phenotypw analysis
       */
      _ctrl.launchSurvivalAnalysis = (entityType, entityId, entitySymbol) => {
        var filters = _.merge(_.cloneDeep(LocationService.filters()), {donor: {projectId: {is: [project.id]}}});
        SurvivalAnalysisLaunchService.launchSurvivalAnalysis(entityType, entityId, entitySymbol, filters, project.id);
      };

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('projects') !== -1) {
        refresh();
      }
    });

    $scope.$watch(function () {
      return loadState.isLoading;
    }, function(isLoading){
      if (isLoading === false && $location.hash()) {
        $window.scrollToSelector('#' + $location.hash(), {offset: 30, speed: 800});
      }
    });

    refresh();

  });

  module.controller('ProjectGeneCtrl',
    function($scope, HighchartsService, Projects, Donors, LocationService, ProjectCache, $stateParams, LoadState) {
    var _ctrl = this,
        _projectId = $stateParams.id || null,
        project = Projects.one(_projectId),
        FilterService = LocationService.getFilterService();

    var loadState = new LoadState({scope: $scope});

    function success(genes) {
      if (genes.hasOwnProperty('hits') ) {
        var projectCachePromise = ProjectCache.getData();
        var geneIds = _.pluck(genes.hits, 'id').join(',');
        _ctrl.genes = genes;

        if (_.isEmpty(_ctrl.genes.hits)) {
          return;
        }

        Projects.one(_projectId).get().then(function (data) {
          var project = data;
          genes.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

          // Get Mutations counts
          Projects.one(_projectId).handler
            .one('genes', geneIds)
            .one('mutations', 'counts').get({
              filters: LocationService.filters()
            }).then(function (data) {
              _ctrl.mutationCounts = data;
            });

          // Need to get SSM Test Donor counts from projects
          Projects.getList().then(function (projects) {
            _ctrl.genes.hits.forEach(function (gene) {
              gene.uiAffectedDonorPercentage = gene.affectedDonorCountFiltered / project.ssmTestedDonorCount;

              gene.advQuery =
              LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}, gene: {id: {is: [gene.id]}}});

              gene.advQueryAll = LocationService.mergeIntoFilters({gene: {id: {is: [gene.id]}}});

              Donors.getList({size: 0, include: 'facets', filters: gene.advQueryAll}).then(function (data) {
                gene.uiDonors = data.facets.projectId.terms;
                gene.uiDonors.forEach(function (facet) {
                  var p = _.find(projects.hits, function (item) {
                    return item.id === facet.term;
                  });

                  facet.advQuery = LocationService.mergeIntoFilters({
                      donor: {projectId: {is: [facet.term]}},
                      gene: {id: {is: [gene.id]}}
                    }
                  );

                  projectCachePromise.then(function(lookup) {
                    facet.projectName = lookup[facet.term] || facet.term;
                  });

                  facet.countTotal = p.ssmTestedDonorCount;
                  facet.percentage = facet.count / p.ssmTestedDonorCount;
                });
              });
            });

            _ctrl.bar = HighchartsService.bar({
              hits: _ctrl.genes.hits,
              xAxis: 'symbol',
              yValue: 'uiAffectedDonorPercentage'
            });
          });
        });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('genes');
        
      loadState.loadWhile(
        Projects.one(_projectId).getGenes({
          from: params.from,
          size: params.size,
          filters: LocationService.filters()
        }).then(success)
      );
    }


      $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {

        if (filterObj.currentPath.indexOf('/projects/' + project.id) < 0) {
          return;
        }

        refresh();
      });

      $scope.$on('$locationChangeSuccess', function (event, dest) {
        if (dest.indexOf('/projects/' + project.id) !== -1) {
          refresh();
        }
      });

    refresh();
  });

  module.controller('ProjectMutationsCtrl',
    function ($scope, HighchartsService, Projects, Donors, LocationService, ProjectCache, $stateParams, LoadState) {

    var _ctrl = this,
        _projectId = $stateParams.id || null,
        project = Projects.one(_projectId),
        FilterService = LocationService.getFilterService();

    var loadState = new LoadState({ scope: $scope });

    function success(mutations) {
      if (mutations.hasOwnProperty('hits')) {
        var projectCachePromise = ProjectCache.getData();

        _ctrl.mutations = mutations;

        if ( _.isEmpty(_ctrl.mutations.hits)) {
          return;
        }

        mutations.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

        // Need to get SSM Test Donor counts from projects
        Projects.getList().then(function (projects) {
          _ctrl.mutations.hits.forEach(function (mutation) {

            mutation.advQuery = LocationService.mergeIntoFilters({
              donor: {projectId: {is: [project.id]}},
              mutation: {id: {is: [mutation.id]}}
            });

            mutation.advQueryAll = LocationService.mergeIntoFilters({mutation: {id: {is: [mutation.id]}}});

            Donors.getList({
              size: 0,
              include: 'facets',
              filters: mutation.advQueryAll
            }).then(function (data) {
              mutation.uiDonors = data.facets.projectId.terms;
              mutation.uiDonors.forEach(function (facet) {
                var p = _.find(projects.hits, function (item) {
                  return item.id === facet.term;
                });

                facet.advQuery = LocationService.mergeIntoFilters({
                  donor: {projectId: {is: [facet.term]}},
                  mutation: {id: {is: [mutation.id]}}
                });

                projectCachePromise.then(function(lookup) {
                  facet.projectName = lookup[facet.term] || facet.term;
                });

                facet.countTotal = p.ssmTestedDonorCount;
                facet.percentage = facet.count / p.ssmTestedDonorCount;
              });
            });
          });
        });

        _ctrl.bar = HighchartsService.bar({
          hits: _ctrl.mutations.hits,
          xAxis: 'id',
          yValue: 'affectedDonorCountFiltered'
        });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('mutations');

      loadState.loadWhile(
        project.getMutations({
          from: params.from,
          size: params.size,
          include: 'consequences', 
          filters: LocationService.filters()
        }).then(success)
      );
    }

    $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {

      if (filterObj.currentPath.indexOf('/projects/' + _projectId) < 0) {
        return;
      }

      refresh();
    });

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('/projects/' + project.id) !== -1) {
        refresh();
      }
    });

    refresh();
  });

  module.controller('ProjectDonorsCtrl', function ($scope, HighchartsService, Projects,
                                                   Donors, LocationService, $stateParams, LoadState) {
    var _ctrl = this,
        _projectId = $stateParams.id || null,
        project = Projects.one(_projectId),
        FilterService = LocationService.getFilterService();

    var loadState = new LoadState({ scope: $scope });

    function success(donors) {
      if (donors.hasOwnProperty('hits')) {
        _ctrl.donors = donors;
        _ctrl.donors.advQuery = LocationService.mergeIntoFilters({donor: {projectId: {is: [project.id]}}});

        _ctrl.donors.hits.forEach(function (donor) {
          donor.advQuery = LocationService.mergeIntoFilters({donor: {id: {is: [donor.id]}}});
        });
        Donors
          .one(_.pluck(donors.hits, 'id').join(',')).handler.all('mutations')
          .one('counts').get({filters: LocationService.filters()}).then(function (data) {
            _ctrl.mutationCounts = data;
          });

        _ctrl.bar = HighchartsService.bar({
          hits: _ctrl.donors.hits,
          xAxis: 'id',
          yValue: 'ssmAffectedGenes'
        });
      }
    }

    function refresh() {

      var params = LocationService.getPaginationParams('donors');

      loadState.loadWhile(
        Projects.one(_projectId).getDonors({
          from: params.from,
          size: params.size,
          filters: LocationService.filters()
        }).then(success)
      );
    }

    $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {

      if (filterObj.currentPath.indexOf('/projects/' + _projectId) < 0) {
        return;
      }

      refresh();
    });

    $scope.$on('$locationChangeSuccess', function (event, dest) {
      if (dest.indexOf('/projects/' + project.id) !== -1) {
        refresh();
      }
    });

    refresh();
  });

})();

(function () {
  'use strict';

  var module = angular.module('icgc.projects.models', ['restangular', 'icgc.common.location']);

  module.service('Projects', function (Restangular, LocationService, Project) {
    this.all = function () {
      return Restangular.all('projects');
    };

    this.several = function(list) {
      return Restangular.several('projects', list);
    };


    // Get ALL projects metadata
    this.getMetadata = function() {
      var params = {
        filters: {},
        size: 100
      };

      return this.all().get('', params).then(function(data) {
        return data;
      });
    };


    this.getList = function (params) {
      var defaults = {
        size: 100,
        from: 1,
        filters: LocationService.filters()
      };

      var liveFilters = angular.extend(defaults, _.cloneDeep(params));

      return this.all().get('', liveFilters).then(function (data) {

        if (data.hasOwnProperty('facets') &&
            data.facets.hasOwnProperty('id') &&
            data.facets.id.hasOwnProperty('terms')) {
          data.facets.id.terms = data.facets.id.terms.sort(function (a, b) {
            if (a.term < b.term) {
              return -1;
            }
            if (a.term > b.term) {
              return 1;
            }
            return 0;
          });
        }

        return data;
      });
    };

    this.one = function (id) {
      return id ? Project.init(id) : Project;
    };
  });

  module.service('Project', function (Restangular) {
    var _this = this;
    this.handler = {};

    this.init = function (id) {
      this.id = id;
      this.handler = Restangular.one('projects', id);
      return _this;
    };

    this.get = function (params) {
      var defaults = {};

      return this.handler.get(angular.extend(defaults, params));
    };

    this.getGenes = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };
      return this.handler.one('genes', '').get(angular.extend(defaults, params));
    };

    this.getDonors = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };

      var liveFilters = angular.extend(defaults, _.cloneDeep(params));

      return this.handler.one('donors', '').get(liveFilters);
    };

    this.getMutations = function (params) {
      var defaults = {
        size: 10,
        from: 1
      };
      return this.handler.one('mutations', '').get(angular.extend(defaults, params));
    };

  });

  module.value('X2JS', new X2JS());

  module.service('PubMed', function (Restangular, X2JS) {
    this.handler = Restangular.oneUrl('pubmed', 'https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi');

    function format(xml) {
      var pub = {}, json = X2JS.xml2js(xml).eSummaryResult.DocSum;

      function get(field) {
        return _.find(json.Item, function (o) {
          return o._Name === field;
        }).__text;
      }

      pub.id = json.Id;
      pub.title = get('Title');
      pub.journal = get('FullJournalName');
      pub.issue = get('Issue');
      pub.pubdate = get('PubDate');
      pub.authors = _.pluck(_.find(json.Item, function (o) {
        return o._Name === 'AuthorList';
      }).Item, '__text');
      pub.refCount = parseInt(get('PmcRefCount'), 10);

      return pub;
    }

    this.get = function (id) {
      return this.handler.get({db: 'pubmed', id: id}).then(function (data) {
        return format(data);
      });
    };
  });

  module.service('ProjectState', function () {

    this.visitedTab = {};

    this.hasVisitedTab = function(tab) {
      return this.visitedTab[tab];
    };

    this.setTab = function (tab) {
      this.tab = tab;
      this.visitedTab[tab] = true;
    };
    this.getTab = function () {
      return this.tab;
    };
    this.isTab = function (tab) {
      return this.tab === tab;
    };

  });

})();
