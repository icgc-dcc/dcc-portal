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

require('./survival-analysis-service.js');

import { renderPlot } from '@oncojs/survivalplot';
import loadImg from 'load-img';
import getContext from 'get-canvas-context';
import platform from 'platform';

function svgToSvgDataUri (svg) {
  return 'data:image/svg+xml;base64,'+ btoa(unescape(encodeURIComponent(svg)));
}

function svgToPngDataUri(svg, {width, height}) {
  const ctx = getContext('2d', {width, height});
  const svgDataUri = svgToSvgDataUri(svg);

  return new Promise((resolve, reject) => {
    loadImg(svgDataUri, {crossOrigin: true}, (err, image) => {
      if (err) { reject(err) }
      ctx.drawImage(image, 0, 0);
      const pngDataUri = ctx.canvas.toDataURL('image/png');
      resolve(pngDataUri);
    });
  });
}

(function() {
  'use strict';

  var module = angular.module('icgc.survival', ['icgc.survival.services', 'icgc.donors.models']);

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
      var tipTemplate = _.template(require('./survival-tip-template.html'));
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

      this.isEmpty = () => !ctrl.dataSets || _.every(this.dataSets.map(set => set.donors), _.isEmpty);

      var update = function (params) {
        if (ctrl.isEmpty()) {
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

      const getSvgString = ({width, height}={}) => (`
        <svg
          xmlns:svg="http://www.w3.org/2000/svg"
          xmlns="http://www.w3.org/2000/svg"
          class="exported-survival-svg"
          ${
            (width && height) ? `width="${width}" height="${height}"` : ''
          }
          viewBox="0 0 ${svg.attr('width')} ${Number(svg.attr('height')) + 120}"
        >
          <style>
            <![CDATA[
              ${require('!raw!sass!prepend?data=$selection:#edf8ff;!../styles/survival-analysis.scss')}
            ]]>
          </style>
          <foreignObject x="20" y="60" width="400" height="150">
              <div class="legend" xmlns="http://www.w3.org/1999/xhtml">
              ${$element.find('.legend').html()}
              </div>
          </foreignObject>
          <g transform="translate(20, 20)">
            <text x="0" y="0" text-anchor="left" dominant-baseline="hanging">
              <tspan style="font-size: 20px;">
                ${ctrl.title}
              </tspan>

              <tspan x="0" dy="1.5em">
                ${$element.find('.p-value-test').text()}
              </tspan>
            </text>
          </g>
          <g class="survival-graph" transform="translate(0,100)">
            ${
              svg.html()
                .replace(/url\(http\:\/\/(.*?)#/g, 'url(#')
                .replace(/"axis-label" dy="(\d+?)"/, '"axis-label" dy="50"')
            }
          </g>
        </svg>`);

      const exportDimensions = {width: 1200, height: 753};
      this.handleClickExportSvg = () => ExportService.exportData('survivalplot.svg', getSvgString());
      this.handleClickExportPng = async () => {
        const imageDataUri = await svgToPngDataUri(getSvgString(exportDimensions), exportDimensions);
        ExportService.exportDataUri('survivalplot.png', imageDataUri); 
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

      this.SetOperationService = SetOperationService;

      this.doesSupportPngExport = !_.includes(['IE', 'Microsoft Edge'], platform);
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

})();
