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
  var module = angular.module('icgc.project', ['icgc.project.donor', 'icgc.project.gene', 'icgc.project.mutation']);

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
})();