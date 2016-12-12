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

// Light angular wrapper for render chart to display highly impacted mutation across projects
(function() {
  'use strict';

  var module = angular.module('icgc.visualization.projectmutationviewer', []);

  module.directive('projectMutationDistribution', function ($location, $filter, $window, gettextCatalog) {
    return {
      restrict: 'E',
      replace: 'true',
      templateUrl: '/scripts/projectmutationviewer/views/projectmutationviewer.html',
      scope: {
        items: '=',
        selected: '=',
        selectedProjectCount: '=',
        projectList: '='
      },
      link: function($scope, $element) {
        var chart, config;

        $scope.showPlot = false;

        $scope.helpText = gettextCatalog.getString('Each dot represents the number of somatic mutations per' +
          ' megabase in a given donor\'s exome.') + 
          gettextCatalog.getString(' Donors are grouped by cancer projects.') + '<br>' +
          gettextCatalog.getString(' Horizontal red lines ' +
          'provide the median number of somatic and exomic mutations within each cancer project.');

        $scope.defaultGraphHeight = 230;

        config = {
          height: $scope.defaultGraphHeight,
          width: 950,
          clickFunc: function(d) {
            $scope.$emit('tooltip::hide');
            $scope.$apply(function() {
              $location.path('/projects/' + d.id).search({});
            });
          },
          tooltipShowFunc: function(elem, data) {
            function getLabel() {

              var numberFilterFn = $filter('number');

              return '<strong>' + data.name + ' [' + data.id + ']</strong><br />' +
                gettextCatalog.getString('Median') + ': ' + numberFilterFn(data.medium) + '<br />' +
                gettextCatalog.getString('# Donors') + ': ' + data.donorCount + '<br />' +
                gettextCatalog.getString('# Mutations') + ': ' + numberFilterFn(data.mutationCount);
            }

            var position = {
              left:elem.getBoundingClientRect().left,
              top:elem.getBoundingClientRect().top + $window.pageYOffset,
              width: elem.getBoundingClientRect().width,
              height: elem.getBoundingClientRect().height
            };
            $scope.$emit('tooltip::show', {
              element: angular.element(elem),
              text: getLabel(),
              placement: 'top',
              elementPosition: position
            });
          },
          tooltipHideFunc: function() {
            $scope.$emit('tooltip::hide');
          }
        };

        function transform(rawData) {
          var chartData = [];

          Object.keys(rawData).forEach(function(projectKey) {
            var projectData = rawData[projectKey];
            var points = [];
            var medium = 0, mean = 0, count = 0;
            var totalMutations = 0;

            Object.keys(projectData).forEach(function(donorKey) {
              var mutationCount = projectData[donorKey];

              mutationCount = mutationCount / 30; // As per equation in DCC-2612
              totalMutations += projectData[donorKey];

              mean += mutationCount;
              points.push( mutationCount );
              count ++;
            });
            mean = mean/count;
            points = points.sort(function(a, b) {
              return a - b;
            });

            if (count % 2 === 0) {
              medium = 0.5 * (points[count/2] + points[(count/2)+1]);
            } else {

              if (count === 1) {
                medium = points[0];
              } else {
                medium = points[Math.floor(count/2) + 1];
              }
            }

            chartData.push({
              id: projectKey,
              name: $scope.projectList[projectKey].name,
              mean: mean,
              medium: medium,
              mutationCount: totalMutations.toFixed(3),
              points: points
            });
          });

          chartData = _.chain(chartData)
            .sortBy( function(d) {
              return d.id;
            })
            .sortBy( function(d) {
              return d.medium;
            })
            .value();

          return chartData;
        }

        $scope.$watch ('items', function (newData) {
          var showPlot = _.some ($scope.selected, function (projectId) {
            return _.has (newData, projectId);
          });

          $scope.showPlot = showPlot;
          if (! showPlot) {return}

          if (newData && !chart) {
            chart = new dcc.ProjectMutationChart (transform ($scope.items), config);
            chart.render( $element.find('.canvas')[0] );

            if (angular.isDefined($scope.selected)) {
              chart.highlight($scope.selected);
            }
          }
        });

        $scope.$watch('selected', function(newData) {
          if (newData && chart) {
            $scope.selected = newData;
            chart.highlight($scope.selected);
          }
        });

        $scope.$on('$destroy', function() {
          $scope.items = null;
          $scope.selected = null;
          if (chart) {
            chart.destroy();
          }
        });
      }
    };
  });
})();
