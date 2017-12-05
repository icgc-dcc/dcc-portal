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

var module = angular.module('icgc.project.donor', []);

module.controller('ProjectDonorsCtrl', function ($scope, HighchartsService,
  Projects, Donors, LocationService, $stateParams, LoadState) {

  var _ctrl = this,
    _projectId = $stateParams.id || null,
    project = Projects.one(_projectId),
    FilterService = LocationService.getFilterService(),
    loadState = new LoadState({ scope: $scope });

  function success(donors) {
    if (donors.hasOwnProperty('hits')) {
      _ctrl.donors = donors;
      _ctrl.donors.advQuery = LocationService.mergeIntoFilters({ donor: { projectId: { is: [project.id] } } });

      _ctrl.donors.hits.forEach(function (donor) {
        donor.advQuery = LocationService.mergeIntoFilters({ donor: { id: { is: [donor.id] } } });
      });
      Donors
        .one(_.map(donors.hits, 'id').join(',')).handler.all('mutations')
        .one('counts').get({ filters: LocationService.filters() }).then(function (data) {
          _ctrl.mutationCounts = data;
        });

      _ctrl.bar = HighchartsService.bar({
        hits: _ctrl.donors.hits,
        xAxis: 'id',
        yValue: 'ssmAffectedGenes',
        options: {
          linkBase: '/donors/'
        }
      });
    }
  }

  function refresh() {

    var params = LocationService.getPaginationParams('donors');

    loadState.loadWhile(
      Projects.one(_projectId).getDonors({
        from: params.from,
        size: params.size,
        sort: params.sort,
        order: params.order,
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