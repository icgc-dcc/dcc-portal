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

var module = angular.module('icgc.project.mutation', []);

module.controller('ProjectMutationsCtrl', function ($scope, HighchartsService,
  Projects, Donors, LocationService, ProjectCache, $stateParams, LoadState) {

  var _ctrl = this,
    _projectId = $stateParams.id || null,
    project = Projects.one(_projectId),
    FilterService = LocationService.getFilterService();

  var loadState = new LoadState({ scope: $scope });

  function success(mutations) {
    if (mutations.hasOwnProperty('hits')) {
      var projectCachePromise = ProjectCache.getData();

      _ctrl.mutations = mutations;

      if (_.isEmpty(_ctrl.mutations.hits)) {
        return;
      }

      mutations.advQuery = LocationService.mergeIntoFilters({ donor: { projectId: { is: [project.id] } } });

      // Need to get SSM Test Donor counts from projects
      Projects.getList().then(function (projects) {
        _ctrl.mutations.hits.forEach(function (mutation) {

          mutation.advQuery = LocationService.mergeIntoFilters({
            donor: { projectId: { is: [project.id] } },
            mutation: { id: { is: [mutation.id] } }
          });

          mutation.advQueryAll = LocationService.mergeIntoFilters({ mutation: { id: { is: [mutation.id] } } });

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
                donor: { projectId: { is: [facet.term] } },
                mutation: { id: { is: [mutation.id] } }
              });

              projectCachePromise.then(function (lookup) {
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
        yValue: 'affectedDonorCountFiltered',
        options: {
          linkBase: '/mutations/'
        }
      });
    }
  }

  function refresh() {

    var params = LocationService.getPaginationParams('mutations');

    loadState.loadWhile(
      project.getMutations({
        from: params.from,
        size: params.size,
        sort: params.sort,
        order: params.order,
        include: 'consequences',
        filters: LocationService.filters()
      }).then(success)
    );
  }

  $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function (e, filterObj) {

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