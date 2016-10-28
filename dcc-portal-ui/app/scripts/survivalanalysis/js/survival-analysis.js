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

  var module = angular.module('icgc.survival', ['icgc.survival.services', 'icgc.donors.models']);

  function renderChart (params) {
    var svg = params.svg;
    var container = params.container;
    var dataSets = params.dataSets;
    var disabledDataSets = params.disabledDataSets;
    var onMouseEnterDonor = params.onMouseEnterDonor || _.noop;
    var onMouseLeaveDonor = params.onMouseLeaveDonor || _.noop;
    var onClickDonor = params.onClickDonor || _.noop;
    var palette = params.palette;
    var markerType = params.markerType || 'circle';

    var containerBounds = container.getBoundingClientRect();

    var yAxisLabel = 'Survival Rate';
    var xAxisLabel = 'Duration ' + ' (days)';

    var margin = {
      top: 20,
      right: 20,
      bottom: 46,
      left: 60
    };
    var outerWidth = containerBounds.width;
    var outerHeight = params.height || outerWidth * 0.5;

    var axisWidth = outerWidth - margin.left - margin.right;
    var axisHeight = outerHeight - margin.top - margin.bottom;

    var longestDuration = _.max(dataSets
        .filter(function (data) {
          return !_.includes(disabledDataSets, data) && data.donors.length;
        })
        .map(function (data) {
          return data.donors.slice(-1)[0].time;
        }));
    
    var xDomain = params.xDomain || [0, longestDuration];
    var onDomainChange = params.onDomainChange;

    var x = d3.scale.linear()
      .range([0, axisWidth]);

    var y = d3.scale.linear()
      .range([axisHeight, 0]);

    var xAxis = d3.svg.axis()
      .scale(x)
      .orient('bottom');

    var yAxis = d3.svg.axis()
      .scale(y)
      .orient('left');

    svg
      .attr('width', outerWidth)
      .attr('height', outerHeight);

    var wrapperFragment = document.createDocumentFragment();

    var wrapper = d3.select(wrapperFragment).append('svg:g')
        .attr('class', 'wrapper')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    x.domain([xDomain[0], xDomain[1]]);
    y.domain([0, 1]);

    // Draw x axis
    wrapper.append('svg:g')
      .attr('class', 'x axis')
      .attr('transform', 'translate( 0,' + axisHeight + ')')
      .call(xAxis)
      .append('svg:text')
        .attr('class', 'axis-label')
        .attr('dy', 30)
        .attr('x', axisWidth / 2)
        .style('text-anchor', 'end')
        .text(xAxisLabel);

    // Draw y axis
    wrapper.append('svg:g')
      .attr('class', 'y axis')
      .call(yAxis)
      .append('svg:text')
        .attr('class', 'axis-label')
        .attr('transform', 'rotate(-90)')
        .attr('y', -40)
        .attr('x', - (margin.top + axisHeight / 2))
        .text(yAxisLabel);
    
    function brushend() {
      var extent = brush.extent();
      svg.select('.brush').call(brush.clear());
      if (extent[1] - extent[0] > 1) {
        onDomainChange(extent);
      }
    }
    var brush = d3.svg.brush()
      .x(x)
      .on('brushend', brushend);

    wrapper.append('svg:g')
      .attr('class', 'brush')
      .call(brush)
      .selectAll('rect')
      .attr('height', axisHeight);

    var maskName = 'mask_' + _.uniqueId();

    svg.append('svg:clipPath')
      .attr('id', maskName)
      .append('svg:rect')
        .attr('x', 0)
        .attr('y', -10)
        .attr('width', axisWidth)
        .attr('height', axisHeight + margin.top);

    dataSets.forEach(function (data, i) {
      if (_.includes(disabledDataSets, data)) {
        return;
      }
      var line = d3.svg.area()
        .interpolate('step-before')
        .x(function(p) { return x(p.x); })
        .y(function(p) { return y(p.y); });

      var setGroup = wrapper.append('svg:g')
        .attr('class', 'serie')
        .attr('set-id', data.meta.id)
        .attr('clip-path', 'url(' + window.location.href + '#' + maskName + ')');

      var setColor = palette[i % palette.length];

 
      var donorsInRange = data.donors.filter(function (donor, i, arr) {
        return _.inRange(donor.time, xDomain[0], xDomain[1] + 1) ||
          ( arr[i - 1] && donor.time >= xDomain[1] && arr[i - 1].time <= xDomain[1] ) ||
          ( arr[i + 1] && donor.time <= xDomain[0] && arr[i + 1].time >= xDomain[0] );
      });

      // Draw the data as an svg path
      setGroup.append('svg:path')
        .datum(donorsInRange
          .map(function (d) { return {x: d.time, y: d.survivalEstimate}; }))
        .attr('class', 'line')
        .attr('d', line)
        .attr('stroke', setColor);

      // Draw the data points as circles
      var markers = setGroup.selectAll('circle')
        .data(donorsInRange)
        .enter();

      if (markerType === 'line') {
        markers = markers.append('svg:line')
          .attr('class', 'point-line')
          .attr('status', function (d) { return d.status; })
          .attr('x1', function(d) { return x(d.time); })
          .attr('y1', function(d) { return y(d.survivalEstimate); })
          .attr('x2', function(d) { return x(d.time); })
          .attr('y2', function(d) { return y(d.survivalEstimate) + (d.status === 'deceased' ? 10 : -5); })
          .attr('stroke', setColor);
      } else {
        markers = markers.append('svg:circle')
          .attr('class', 'point')
          .attr('status', function (d) { return d.status; })
          .attr('cx', function(d) { return x(d.time); })
          .attr('cy', function(d) { return y(d.survivalEstimate); })
          .attr('fill', setColor );
      }

      markers
        .on('mouseover', function (d) {
          onMouseEnterDonor(d3.event, d);
        })
        .on('mouseout', function (d) {
          onMouseLeaveDonor(d3.event, d);
        })
        .on('click', function (d) {
          onClickDonor(d3.event, d);
        });
    });
    
    svg.node().appendChild(wrapperFragment);

    return svg;
  }

  var survivalAnalysisController = function (
      $scope,
      $element,
      FullScreenService
    ) {
      var ctrl = this;
      var graphContainer = $element.find('.survival-graph').get(0);
      var svg = d3.select(graphContainer).append('svg');
      var tipTemplate = _.template($element.find('.survival-tip-template').html());
      var stateStack = [];
      var state = {
        xDomain: undefined,
        disabledDataSets: undefined
      };

      var isFullScreen = function () {
        return _.includes([
          document.fullscreenElement,
          document.webkitFullscreenElement,
          document.mozFullscreenElement
          ], $element.get(0));
      };

      var update = function (params) {
        if (!ctrl.dataSets) {
          return;
        }

        svg.selectAll('*').remove();
        renderChart(_.defaults({
          svg: svg, 
          container: graphContainer, 
          dataSets: ctrl.dataSets,
          disabledDataSets: state.disabledDataSets,
          palette: ctrl.palette,
          markerType: 'line',
          xDomain: state.xDomain,
          height: isFullScreen() && ( window.innerHeight - 100 ),
          onMouseEnterDonor: function (event, donor) {
            $scope.$apply(function () {
              ctrl.tooltipParams = {
                isVisible: true,
                element: event.target,
                text: tipTemplate({
                  donor: _.extend(
                    { isCensored: _.includes(ctrl.censoredStatuses,donor.status) },
                    donor
                  ),
                  labels: ctrl.tipLabels,
                  unit: 'days'
                }),
                placement: 'right',
                sticky: true
              };
            });
          },
          onMouseLeaveDonor: function () {
            $scope.$apply(function () {
              ctrl.tooltipParams = {
                isVisible: false
              };
            });
          },
          onClickDonor: function (e, donor) {
            window.open('/donors/'+donor.id, '_blank');
          },
          onDomainChange: function (newXDomain) {
            $scope.$apply(function () {
              updateState({xDomain: newXDomain});
            });
          }
        }, params));
      };

      var updateState = function (newState) {
        stateStack = stateStack.concat(state);
        state = _.extend({}, state, newState);
        update();
      };

      window.addEventListener('resize', update);
      update();

      this.getStateStack = function () {
        return stateStack;
      };

      this.canUndo = function () {
        return stateStack.length > 1;
      };

      this.handleClickReset = function () {
        updateState(stateStack[0]);
        stateStack = [stateStack[0]];
      };

      this.handleClickUndo = function () {
        state = _.last(stateStack);
        stateStack = _.without(stateStack, state);
        update();
      };

      this.isFullScreen = isFullScreen;

      this.handleClickEnterFullScreen = function () {
        FullScreenService.enterFullScreen($element.get(0));
      };

      this.handleClickExitFullScreen = FullScreenService.exitFullScreen;

      this.isDataSetDisabled = function (dataSet) {
        return _.includes(state.disabledDataSets, dataSet);
      };

      this.toggleDataSet = function (dataSet) {
        updateState({disabledDataSets: _.xor(state.disabledDataSets, [dataSet])});
      };

      this.$onInit = function () {
        updateState({disabledDataSets: ctrl.initialDisabledDataSets});
      };

      this.$onChanges = function (changes) {
        if (changes.dataSets) {
          setTimeout(update);
        }
      };

      this.$onDestroy = function () {
        window.removeEventListener('resize', update);
      };

  };

  module.component('survivalAnalysisGraph', {
    templateUrl: '/scripts/survivalanalysis/views/survival-analysis.html',
    bindings: {
      dataSets: '<',
      initialDisabledDataSets: '<',
      tipLabels: '<',
      censoredStatuses: '<',
      palette: '<',
      title: '<',
      pvalue: '<'
    },
    controller: survivalAnalysisController,
    controllerAs: 'ctrl'
  });

})();