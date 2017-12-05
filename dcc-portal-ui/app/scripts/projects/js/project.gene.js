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

var module = angular.module('icgc.project.gene', []);

module.controller('ProjectGeneCtrl', function ($scope, HighchartsService,
  Projects, Donors, LocationService, ProjectCache, $stateParams, LoadState) {
  var _ctrl = this,
    _projectId = $stateParams.id || null,
    project = Projects.one(_projectId),
    FilterService = LocationService.getFilterService();

  var loadState = new LoadState({ scope: $scope });

  function success(genes) {
    if (genes.hasOwnProperty('hits')) {
      var projectCachePromise = ProjectCache.getData();
      var geneIds = _.map(genes.hits, 'id').join(',');
      _ctrl.genes = genes;

      if (_.isEmpty(_ctrl.genes.hits)) {
        return;
      }

      Projects.one(_projectId).get().then(function (data) {
        var project = data;
        genes.advQuery = LocationService.mergeIntoFilters({ donor: { projectId: { is: [project.id] } } });

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
              LocationService.mergeIntoFilters({ donor: { projectId: { is: [project.id] } }, gene: { id: { is: [gene.id] } } });

            gene.advQueryAll = LocationService.mergeIntoFilters({ gene: { id: { is: [gene.id] } } });

            Donors.getList({ size: 0, include: 'facets', filters: gene.advQueryAll }).then(function (data) {
              gene.uiDonors = data.facets.projectId.terms;
              gene.uiDonors.forEach(function (facet) {
                var p = _.find(projects.hits, function (item) {
                  return item.id === facet.term;
                });

                facet.advQuery = LocationService.mergeIntoFilters({
                  donor: { projectId: { is: [facet.term] } },
                  gene: { id: { is: [gene.id] } }
                }
                );

                projectCachePromise.then(function (lookup) {
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
            yValue: 'uiAffectedDonorPercentage',
            options: {
              linkBase: '/genes/'
            }
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
        sort: params.sort,
        order: params.order,
        filters: LocationService.filters()
      }).then(success)
    );
  }


  $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function (e, filterObj) {

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