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

(function() {
  'use strict';

  var module = angular.module('icgc.visualization.stackedarea', []);
  var releaseDates = {
      4:'May-11',
      5:'Jun-11',
      6:'Jul-11',
      7:'Dec-11',
      8:'Mar-12',
      9:'Aug-12',
      10:'Nov-12',
      11:'Dec-12',
      12:'Mar-13',
      13:'Jul-13',
      14:'Sep-13',
      15:'Jan-14',
      16:'May-14',
      17:'Sep-14',
      18:'Feb-15',
      19:'Jun-15',
      20: 'Nov-15',
      21: 'May-16',
      22: 'August-16',
      23: 'Dec-16',
      24: 'May-17',
      25: 'June-17',
      26: 'Nov-17'
    };

  module.directive('donorHistory', function ($location, HighchartsService, gettextCatalog) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        items: '=',
        selected: '=',
        selectedProjectCount: '='
      },
      templateUrl: '/scripts/stackedareachart/views/stackedareachart.html',
      link: function ($scope, $element) {
        var chart;
        var filterProjects = function(data, includedProjects){
          if(!includedProjects){
            return data;
          }

          var result = [];
          data.forEach(function (elem) {
            if(includedProjects.indexOf(elem.group) >= 0){
              result.push(elem);
            }
          });

          return result;
        };

        $scope.showPlot = false;
        $scope.defaultGraphHeight = 600;
        $scope.defaultGraphTitle = gettextCatalog.getString('Cumulative Count of Project Donors with Molecular Data in DCC by Release');

        var config = {
          margin:{top: 10, right: 40, bottom: 60, left: 40},
          height: $scope.defaultGraphHeight,
          width: 1000,
          colours: HighchartsService.primarySiteColours,
          yaxis:{label: gettextCatalog.getString('# of Donors'),ticks:8},
          xaxis: {
            label: gettextCatalog.getString('Release'),
            ticksValueRange: [4, 26],
            secondaryLabel: function(data){return releaseDates[data]}
          },
          onClick: function(project){
            $scope.$emit('tooltip::hide');
            $location.path('/projects/' + project).search({});
            $scope.$apply();
          },
          tooltipShowFunc: function(elem, project, currentDonors,release) {
            function getLabel() {
              return '<strong>'+project+'</strong><br>' + gettextCatalog.getString('Release') + ':' + 
              release + ' <br>' + gettextCatalog.getString('# of Donors') + ' ' + currentDonors;
            }

            $scope.$emit('tooltip::show', {
              element: angular.element(elem),
              text: getLabel(),
              placement: 'top',
              sticky:true
            });
          },
          tooltipHideFunc: function() {
            $scope.$emit('tooltip::hide');
          },
          graphTitles: [
            $scope.defaultGraphTitle,
            gettextCatalog.getString('Individual Count of Project Donors with Molecular Data in DCC by Release')],
          offset: 'zero'
        };

        function renderChart (chart) {
          var svgMountPoint = $element.find ('.canvas')[0];
          chart.render (svgMountPoint);
        }

        function shouldShowPlot (history) {
          var projectsWithHistory = _.map (history, 'group');
          var selectedProjects = $scope.selected;
          var selectedProjectsWithHistory = _.intersection (projectsWithHistory, selectedProjects);
          return ! _.isEmpty (selectedProjectsWithHistory);
        }

        $scope.$watch('selected', function (newValue){
            if(newValue && $scope.items){
              if(chart){
                chart.destroy();
              }

              $scope.selected = newValue;
              var showPlot = shouldShowPlot ($scope.items);
              $scope.showPlot = showPlot;
              if (! showPlot) { return }

              chart = new dcc.StackedAreaChart (filterProjects ($scope.items, $scope.selected), config);
              renderChart (chart);
            }
          },true);

        $scope.$watch('items', function (newValue) {
          var showPlot = shouldShowPlot (newValue);
          $scope.showPlot = showPlot;
          if (! showPlot) { return }

          if (!chart && newValue) {
            chart = new dcc.StackedAreaChart(filterProjects($scope.items,$scope.selected),config);
            renderChart (chart);
          } else if (newValue) {
            $scope.items = newValue;
          }
        }, true);

        $scope.$on('$destroy', function () {
          if (chart) {
            chart.destroy();
          }
        });

      }
    };
  });
})();
