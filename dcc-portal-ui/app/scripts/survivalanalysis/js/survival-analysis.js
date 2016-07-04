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

  function noop () {}

  function renderChart (params) {
    var svg = params.svg;
    var container = params.container;
    var dataSets = params.dataSets;
    var onMouseEnterDonor = params.onMouseEnterDonor || noop;
    var onMouseLeaveDonor = params.onMouseLeaveDonor || noop;
    var onClickDonor = params.onClickDonor || noop;
    var palette = params.palette;

    var containerBounds = container.getBoundingClientRect();

    var yAxisLabel = 'Survival Rate';
    var xAxisLabel = 'Duration ' + ' (days)';

    var margin = {
      top: 20,
      right: 20,
      bottom: 36,
      left: 44
    };
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
        return data.donors.slice(-1)[0].survivalTime;
      }));

    x.domain([0, longestDuration]);
    y.domain([0, 1]);

    // draw x axis
    svg.append('g')
      .attr('class', 'x axis')
      .attr('transform', 'translate(' + margin.left + ',' + (axisHeight + margin.top) + ')')
      .call(xAxis)
      .append('text')
        .attr('class', 'axis-label')
        .attr('dy', 30)
        .attr('x', axisWidth / 2)
        .style('text-anchor', 'end')
        .text(xAxisLabel);

    // draw y axis
    svg.append('g')
      .attr('class', 'y axis')
      .attr('transform', 'translate(' + margin.left + ', ' + margin.top + ')')
      .call(yAxis)
      .append('text')
        .attr('class', 'axis-label')
        .attr('transform', 'rotate(-90)')
        .attr('y', -30)
        .attr('x', - (margin.top + axisHeight / 2))
        .text(yAxisLabel);

    dataSets.forEach(function (data, i) {
      var line = d3.svg.area()
        .interpolate('step-after')
        .x(function(p) { return x(p.x); })
        .y(function(p) { return y(p.y); });

      var setGroup = svg.append('g')
        .attr('class', 'serie')
        .attr('set-id', data.meta.id)
        .attr('transform', 'translate(' + margin.left + ', ' + margin.top + ')');
      var setColor = palette[i % palette.length];

      // draw the data as an svg path
      setGroup.append('path')
        .datum(data.donors.map(function (d) { return {x: d.survivalTime, y: d.survivalEstimate}; }))
        .attr('class', 'line')
        .attr('d', line)
        .attr('stroke', setColor);

      // draw the data points as circles
      setGroup.selectAll('circle')
        .data(data.donors)
        .enter()
        .append('svg:circle')
          .attr('class', 'point')
          .attr('status', function (d) { return d.status; })
          .attr('cx', function(d) { return x(d.survivalTime); })
          .attr('cy', function(d) { return y(d.survivalEstimate); })
          .attr('fill', setColor )
          .on('mouseover', function (d) {
            onMouseEnterDonor(d3.event, d);
          })
          .on('mouseout', function (d) {
            onMouseLeaveDonor(d3.event, d);
          })
          .on('click', function (d) {
            onClickDonor(d3.event, d);
          })
        ;
    });

    return svg;
  }

  var survivalAnalysisController = [
    '$scope',
    '$element',
    function (
      $scope,
      $element
    ) {
      var ctrl = this;
      var graphContainer = $element.find('.survival-graph').get(0);
      var svg = d3.select(graphContainer).append('svg');
      var tipTemplate = _.template($element.find('.survival-tip-template').html());

      var update = function () {
        if (!ctrl.dataSets) {
          return;
        }
        svg.selectAll('*').remove();
        renderChart({
          svg: svg, 
          container: graphContainer, 
          dataSets: ctrl.dataSets,
          palette: ctrl.palette,
          onMouseEnterDonor: function (event, donor) {
            $scope.$emit('tooltip::show', {
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
            });
          },
          onMouseLeaveDonor: function () {
            $scope.$emit('tooltip::hide');
          },
          onClickDonor: function (e, donor) {
            window.open('/donors/'+donor.id, '_blank');
          }
        });
      };

      window.addEventListener('resize', update);
      update();

      this.$onChanges = function (changes) {
        if (changes.dataSets) {
          setTimeout(update);
        }
      };

      this.$onDestroy = function () {
        window.removeEventListener('resize', update);
      };

  }];

  module.component('survivalAnalysisGraph', {
    templateUrl: '/scripts/survivalanalysis/views/survival-analysis.html',
    bindings: {
      dataSets: '<',
      tipLabels: '<',
      censoredStatuses: '<',
      palette: '<',
    },
    controller: survivalAnalysisController

  });

  function processResponses(responses) {
    var survivalData = responses.survivalData.plain().results;
    var setsMeta = responses.setsMeta.plain();

    return survivalData.map(function (dataSet) {
      var donors = _.flatten(dataSet.intervals.map(function (interval) {
        return interval.donors.map(function (donor) {
          return _.extend({}, donor, {
            survivalEstimate: interval.cumulativeSurvival
          });
        });
      }));

      return {
        meta: _.find(setsMeta, {id: dataSet.id}),
        donors: donors
      };
    });
  }

  module
    .service('SurvivalAnalysisService', [
      '$q',
      'Restangular',
      'SetService',
      function(
        $q,
        Restangular,
        SetService
      ) {

      function fetchOverallSurvival (setIds) {
        var data = setIds;
        

        var fetchSurvival = Restangular
          .one('analysis')
          .post('survival', data, {}, {'Content-Type': 'application/json'})
          .then(function (response) {
            return Restangular.one('analysis/survival/' + response.id).get();
          });

        var fetchSetsMeta = SetService.getMetaData(setIds);

        return $q.all({
          survivalData: fetchSurvival,
          setsMeta: fetchSetsMeta,
        })
          .then(function (responses) {
            return processResponses(responses);
          })
        ;

      }

      _.extend(this, {
        fetchOverallSurvival: fetchOverallSurvival
      });

    }]);

})();