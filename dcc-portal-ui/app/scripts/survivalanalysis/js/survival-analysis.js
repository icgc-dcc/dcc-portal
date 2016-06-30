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

  var module = angular.module('icgc.survival', ['icgc.donors.models']);

  var palette = [
    '#6baed6', '#fd8d3c', '#74c476'
    // '#2196F3', '#f44336', '#FF9800', '#BBCC24', '#9C27B0',
    // '#795548', '#3F51B5', '#9E9E9E', '#FFEB3B', '##c0392b'
  ];

  function makeChart (el) {
    return d3.select(el).append('svg');
  }

  function processIntervals (intervals) {
    return [{x: 0, y: 1}].concat(intervals.map(function (interval) {
      return {
        x: interval.end,
        y: interval.cumulativeSurvival
      }
    }));
  }

  function noop () {}

  /*
  dataSets: [
    id: String,
    intervals: [
      {
        censured: Number,
        cumulativeSurvival: Number,
        died: Number,
        end: Number,
        start: Number,
      }
    ]
  ]
  */
  function renderChart (params) {
    var svg = params.svg;
    var container = params.container;
    var dataSets = params.dataSets;
    var onMouseEnterDonor = params.onMouseEnterDonor || noop;
    var onMouseLeaveDonor = params.onMouseLeaveDonor || noop;
    var onClickDonor = params.onClickDonor || noop;

    var containerBounds = container.getBoundingClientRect();

    var yAxisLabel = 'Survival Rate';
    var xAxisLabel = 'Duration';

    var margin = {top: 20, right: 20, bottom: 30, left: 24};
    var outerWidth = containerBounds.width;
    var outerHeight = outerWidth * 0.5;

    var axisWidth = outerWidth - margin.left - margin.right;
    var axisHeight = outerHeight - margin.top - margin.bottom;

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
        .attr('height', outerHeight)
        .append('g')
          .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    var longestDuration = _.max(dataSets.map(function (data) {
        return data.intervals.slice(-1)[0].end;
      }));

    x.domain([0, longestDuration]);
    y.domain([0, 1]);

    dataSets.forEach(function (data, i) {
      var line = d3.svg.area()
          .interpolate('step-before')
          .x(function(p) { return x(p.x); })
          .y(function(p) { return y(p.y); });

      var setGroup = svg.append('g')
            .attr('transform', 'translate(' + margin.left + ', ' + margin.top + ')');
      var setColor = palette[i % palette.length];

      // draw the data as an svg path
      setGroup.append('path')
          .datum(processIntervals(data.intervals))
          .attr('class', 'line')
          .attr('d', line)
          .attr('stroke', setColor);

      // draw the data points as circles
      data.donors.alive && setGroup.selectAll('circle')
          .data(data.donors.alive)
          .enter()
          .append('svg:circle')
            .attr('cx', function(d) { return x(d.survivalTime) })
            .attr('cy', function(d) { return y(d.survivalEstimate) })
            .attr('stroke-width', 'none')
            .attr('fill', setColor )
            .attr('fill-opacity', .5)
            .attr('r', 3)
            .attr('cursor', 'pointer')
            .on('mouseover', function (d) {
              onMouseEnterDonor(d3.event, d);
            })
            .on('mouseout', function (d) {
              onMouseLeaveDonor(d3.event, d);
            })
            .on('click', function (d) {
              onClickDonor(d3.event, d);
            })
    });

    // draw x axis
    svg.append('g')
        .attr('class', 'x axis')
        .attr('transform', 'translate(' + margin.left + ',' + (axisHeight + margin.top) + ')')
        .call(xAxis)
        .append('text')
          .attr('dy', 30)
          .attr('x', axisWidth)
          // .attr('dy', '.71em')
          .style('text-anchor', 'end')
          .text(xAxisLabel);

    // draw y axis
    svg.append('g')
        .attr('class', 'y axis')
        .attr('transform', 'translate(' + margin.left + ', ' + margin.top + ')')
        .call(yAxis)
        .append('text')
          // .attr('transform', 'rotate(-90)')
          .attr('y', -10)
          .attr('x', -24)
          // .attr('dy', '.71em')
          // .style('text-anchor', 'end')
          .text(yAxisLabel);

    return svg;
  }

  var survivalAnalysisController = [
    '$scope', '$element', 'survivalPlotService',
    function ($scope, $element, survivalPlotService) {
      var ctrl = this;
      var svg, dataSets;
      var el = $element.find('.survival-graph').get(0);

      var update = function () {
        svg.selectAll('*').remove();
        renderChart({
          svg: svg, 
          container: el, 
          dataSets: dataSets,
          onMouseEnterDonor: function (event, donor) {
            $scope.$emit('tooltip::show', {
              element: event.target,
              text: donor.id,
              placement: 'right',
              sticky: true
            });
          },
          onMouseLeaveDonor: function () {
            $scope.$emit('tooltip::hide');
          },
          onClickDonor: function (donor) {
            console.log('clicked on', donor)
          }
        });
      };

      this.$onInit = function () {
        var dummySetIds = ["3787d3bd-96f8-41e7-b471-b1250a5cc952", "90e4034b-afb3-43e3-a19f-4b85ba0d6d98", "b211f054-6b79-4878-9638-a33d9711c60d"];
        survivalPlotService.getDataSets(ctrl.setIds || dummySetIds).then(function (ds) {

          svg = makeChart(el);
          dataSets = ds;
          window.addEventListener('resize', update);
          // setTimeout required to avoid weird layout due to container not yet being on screen
          setTimeout(update);
        });
      };

      this.$onDestroy = function () {
        window.removeEventListener('resize', update);
      };

  }];

  module.component('survivalAnalysis', {
    templateUrl: '/scripts/survivalanalysis/views/survival-analysis.html',
    bindings: {
      setIds: '<'
    },
    controller: survivalAnalysisController

  });

  function processSurvivalResponse(response) {
    return response.results.map(function (dataSet) {
      var donors = _.flatten(dataSet.intervals.map(function (interval) {
        return interval.donors.map(function (donor) {
          return _.extend({}, donor, {
            survivalEstimate: interval.cumulativeSurvival
          });
        })
      }));

      return _.extend({}, dataSet, {
        donors: _.groupBy(donors, 'status')
      });
    });
  }

  module
    .service('survivalPlotService', ['Restangular', function (Restangular) {

      _.extend(this, {
        getDataSets: function (setIds) {
          var data = setIds;

          return Restangular
            .one('analysis')
            .post('survival', data, {}, {'Content-Type': 'application/json'})
            .then(function (response) {
              return Restangular.one('analysis/survival/' + response.id).get()
            })
            .then(function (response) {
              return processSurvivalResponse(response.plain());
            })
        }
      });

    }]);

})();