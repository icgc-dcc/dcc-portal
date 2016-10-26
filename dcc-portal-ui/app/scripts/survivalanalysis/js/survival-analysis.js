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

  const linearScale = d3.scale ? d3.scale.linear : d3.scaleLinear

const defaultOptions = {
  onMouseEnterDonor: _.noop,
  onMouseLeaveDonor: _.noop,
  onClickDonor: _.noop,
  palette: ['#0e6402', '#c20127', '#00005d'],
  xAxisLabel: 'Survival Rate',
  yAxisLabel: 'Duration (days)',
  margins: {
    top: 20,
    right: 20,
    bottom: 46,
    left: 60,
  },
}

function renderPlot (params) {
  const {
    svg,
    container,
    dataSets,
    disabledDataSets,
    onMouseEnterDonor,
    onMouseLeaveDonor,
    onClickDonor,
    palette,
    xAxisLabel,
    yAxisLabel,
    margins,
    getSetSymbol,
  } = _.defaultsDeep({}, params, defaultOptions)


  const containerBounds = container.getBoundingClientRect()

  var outerWidth = containerBounds.width
  var outerHeight = params.height || outerWidth * 0.5

  var axisWidth = outerWidth - margins.left - margins.right
  var axisHeight = outerHeight - margins.top - margins.bottom

  var longestDuration = _.max(dataSets
      .filter(function (data) {
        return !_.includes(disabledDataSets, data) && data.donors.length
      })
      .map(function (data) {
        return data.donors.slice(-1)[0].time
      }))
  
  var xDomain = params.xDomain || [0, longestDuration]
  var onDomainChange = params.onDomainChange

  var x = linearScale()
    .range([0, axisWidth])

  var y = linearScale()
    .range([axisHeight, 0])

  var xAxis = d3.svg
      ? d3.svg.axis().scale(x).orient('bottom')
      : d3.axisBottom().scale(x)

  var yAxis = d3.svg
    ? d3.svg.axis().scale(y).ticks(5).tickSize(axisWidth).orient('right')
    : d3.axisLeft().scale(y)

  svg
    .attr('width', outerWidth)
    .attr('height', outerHeight)

  var wrapperFragment = document.createDocumentFragment()

  var wrapper = d3.select(wrapperFragment).append('svg:g')
      .attr('class', 'wrapper')
      .attr('transform', 'translate(' + margins.left + ',' + margins.top + ')')

  x.domain([xDomain[0], xDomain[1]])
  y.domain([0, 1])

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
      .text(xAxisLabel)

  // Draw y axis
  var gy = wrapper.append('svg:g')
    .attr('class', 'y axis')
    .call(yAxis)
  gy.selectAll('g')
    .filter(d => d)
    .classed('minor', true)
  gy.selectAll('text')
      .attr('x', -20)
  gy.append('svg:text')
      .attr('class', 'axis-label')
      .attr('transform', 'rotate(-90)')
      .attr('y', -40)
      .attr('x', - (margins.top + axisHeight / 2))
      .text(yAxisLabel)

  var brush = d3.svg
    ? d3.svg.brush().x(x)
    : d3.brushX()

  brush.on('brushend', function brushend() {
    var extent = brush.extent()
    svg.select('.brush').call(brush.clear())
    if (extent[1] - extent[0] > 1) {
      onDomainChange(extent)
    }
  })

  wrapper.append('svg:g')
    .attr('class', 'brush')
    .call(brush)
    .selectAll('rect')
    .attr('height', axisHeight)

  var maskName = 'mask_' + _.uniqueId()

  svg.append('svg:clipPath')
    .attr('id', maskName)
    .append('svg:rect')
      .attr('x', 0)
      .attr('y', -10)
      .attr('width', axisWidth)
      .attr('height', axisHeight + margins.top)

  dataSets.forEach(function (data, i) {
    if (_.includes(disabledDataSets, data)) {
      return
    }
    var line = d3.svg.area()
      .interpolate('step-before')
      .x(function(p) { return x(p.x) })
      .y(function(p) { return y(p.y) })

    var setGroup = wrapper.append('svg:g')
      .attr('class', 'serie')
      .attr('set-id', data.meta.id)
      .attr('clip-path', 'url(' + window.location.href + '#' + maskName + ')')

    var setColor = palette[i % palette.length]


    var donorsInRange = data.donors.filter(function (donor, i, arr) {
      return _.inRange(donor.time, xDomain[0], xDomain[1] + 1) ||
        ( arr[i - 1] && donor.time >= xDomain[1] && arr[i - 1].time <= xDomain[1] ) ||
        ( arr[i + 1] && donor.time <= xDomain[0] && arr[i + 1].time >= xDomain[0] )
    })

    // Draw the data as an svg path
    setGroup.append('svg:path')
      .datum(donorsInRange
        .map(function (d) { return {x: d.time, y: d.survivalEstimate} }))
      .attr('class', 'line')
      .attr('d', line)
      .attr('stroke', setColor)

    // Draw the data points as circles
    var markers = setGroup.selectAll('circle')
      .data(donorsInRange)
      .enter()

    markers = markers.append('svg:line')
      .attr('class', 'point-line')
      .attr('status', function (d) { return d.status })
      .attr('x1', function(d) { return x(d.time) })
      .attr('y1', function(d) { return y(d.survivalEstimate) })
      .attr('x2', function(d) { return x(d.time) })
      .attr('y2', function(d) { return y(d.survivalEstimate) + (d.status === 'deceased' ? 10 : -5) })
      .attr('stroke', setColor)

    markers
      .on('mouseover', function (d) {
        onMouseEnterDonor(d3.event, d)
      })
      .on('mouseout', function (d) {
        onMouseLeaveDonor(d3.event, d)
      })
      .on('click', function (d) {
        onClickDonor(d3.event, d)
      })

    if (getSetSymbol) {
      setGroup.selectAll('circle')
        .data(donorsInRange.slice(-1))
        .enter()
        .append('svg:text')
          .attr('x', d => x(d.time))
          .attr('y', d => y(d.survivalEstimate))
          .attr('dy', '-0.5em')
          .attr('text-anchor', 'end')
          .attr('fill', setColor)
          .append('svg:tspan')
            .html(getSetSymbol(data, dataSets))
    }
  })
  
  svg.node().appendChild(wrapperFragment)

  return svg
}

  var survivalAnalysisController = function (
      $scope,
      $element,
      FullScreenService,
      SetOperationService,
      ExportService,
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
        renderPlot(_.defaults({
          svg: svg, 
          container: graphContainer, 
          dataSets: ctrl.dataSets,
          disabledDataSets: state.disabledDataSets,
          palette: ctrl.palette,
          markerType: 'line',
          xDomain: state.xDomain,
          height: isFullScreen() && ( window.innerHeight - 100 ),
          getSetSymbol: SetOperationService.getSetShortHandSVG,
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

      this.handleClickExportSvg = () => ExportService.exportData('survivalplot.svg', `
        <svg
          xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:cc="http://creativecommons.org/ns#"
          xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
          xmlns:svg="http://www.w3.org/2000/svg"
          xmlns="http://www.w3.org/2000/svg"
        >
          <style>
            .domain {
              fill-opacity: 0;
            }
          </style>
        ${svg.html()}
        </svg>`
      );

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

      this.SetOperationService = SetOperationService;
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
      pvalue: '<',
      onClickExportCsv: '&',
    },
    controller: survivalAnalysisController,
    controllerAs: 'ctrl'
  });

  function processResponses(responses) {
    var survivalData = responses.survivalData.plain().results;
    var setsMeta = responses.setsMeta.plain();

    var processGraphData = function (graphType) {
      return survivalData.map(function (dataSet) {
        var donors = _.flatten(dataSet[graphType].map(function (interval) {
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
    };
    var overallStats = isNaN(responses.survivalData.overallStats.pvalue) ? 
      undefined : responses.survivalData.overallStats;
    var diseaseFreeStats = isNaN(responses.survivalData.diseaseFreeStats.pvalue) ? 
      undefined : responses.survivalData.diseaseFreeStats;
    return {
      overall: processGraphData('overall'),
      overallStats: overallStats, 
      diseaseFree: processGraphData('diseaseFree'),
      diseaseFreeStats: diseaseFreeStats
    };
  }

  module
    .service('SurvivalAnalysisService', function(
        $q,
        Restangular,
        SetService
      ) {

      function fetchSurvivalData (setIds) {
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
          });
      }

      var defaultHeadingMap = {
        setName: 'donor_set_name',
        id: 'donor_id',
        time: 'time',
        status: 'donor_vital_status',
        survivalEstimate: 'survival_estimate',
      };

      function dataSetToTSV (dataSet, headingMap) {
        var headings = _({})
          .defaults(defaultHeadingMap, headingMap)
          .values()
          .join('\t');

        _.defaults({}, defaultHeadingMap, headingMap);

        var contents = _(dataSet)
          .map(function (set) {
            return set.donors.map(function (donor) {
              return [set.meta.name, donor.id, donor.time, donor.status, donor.survivalEstimate].join('\t');
            });
          })
          .flatten()
          .join('\n');

        var tsv = headings + '\n' + contents;
        return tsv;
      }

      _.extend(this, {
        fetchSurvivalData: fetchSurvivalData,
        dataSetToTSV: dataSetToTSV
      });

    });

})();