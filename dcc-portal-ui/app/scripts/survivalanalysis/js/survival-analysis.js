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
    '#2196F3', '#f44336', '#FF9800', '#BBCC24', '#9C27B0',
    '#795548', '#3F51B5', '#9E9E9E', '#FFEB3B', '##c0392b'
  ];

  function makeChart (el) {
    return d3.select(el).append('svg');
  }

  function renderChart (chart, el, dataSets) {
    var elRect = el.getBoundingClientRect();

    var yAxisLabel = 'Survival Rate';
    var xAxisLabel = 'Duration';

    var margin = {top: 20, right: 20, bottom: 30, left: 50};
    var width = elRect.width - margin.left - margin.right;
    // var height = elRect.height - margin.top - margin.bottom;
    var height = width * 0.5;

    var x = d3.scale.linear()
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);

    var xAxis = d3.svg.axis()
        .scale(x)
        .orient('bottom');

    var yAxis = d3.svg.axis()
        .scale(y)
        .orient('left');

    chart
        .attr('width', width + margin.left + margin.right)
        .attr('height', height + margin.top + margin.bottom)
        .append('g')
          .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    var longestDuration = _.max(dataSets.map(function (data) {
        return data.slice(-1)[0].end;
      }));

    x.domain([0, longestDuration]);
    y.domain([0, 1]);

    // draw x axis
    chart.append('g')
        .attr('class', 'x axis')
        .attr('transform', 'translate(0,' + height + ')')
        .call(xAxis)
        .append('text')
          .attr('x', width)
          // .attr('dy', '.71em')
          .style('text-anchor', 'end')
          .text(xAxisLabel);

    // draw y axis
    chart.append('g')
        .attr('class', 'y axis')
        .call(yAxis)
        .append('text')
          .attr('transform', 'rotate(-90)')
          .attr('y', 6)
          .attr('dy', '.71em')
          .style('text-anchor', 'end')
          .text(yAxisLabel);

    dataSets.forEach(function (data, i) {
      var line = d3.svg.area()
          .interpolate('step-after')
          .x(function(d) { return x(d.start); })
          .y(function(d) { return y(d.survival); });
      
      // draw the data as an svg path
      chart.append('path')
          .datum(data)
          .attr('class', 'line')
          .attr('d', line)
          .attr('stroke', palette[i % palette.length]);

      // draw the data points as circles
      // chart.selectAll('circle')
      //     .data(data.filter(function (item) {
      //       return item.censored;
      //     }))
      //     .enter().append('svg:circle')
      //     .attr('cx', function(d) { return x(d.start) })
      //     .attr('cy', function(d) { return y(d.survival) })
      //     .attr('stroke-width', 'none')
      //     .attr('fill', 'orange' )
      //     .attr('fill-opacity', .5)
      //     //.attr('visibility', 'hidden')
      //     .attr('r', 3);
    });

    return chart;




  }

  var survivalResultController = [
    '$scope', '$element', 'survivalPlotService',
    function ($scope, $element, survivalPlotService) {
      var ctrl = this;
      var chart, dataSets;
      var el = $element.find('.survival-graph').get(0);

      var update = function () {
        chart.selectAll('*').remove();
        renderChart(chart, el, dataSets);
      };

      this.$onInit = function () {
        console.log('init');
        survivalPlotService.getData(ctrl.analysisId).then(function (ds) {
          chart = makeChart(el);
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

  module.component('survivalResult', {
    templateUrl: '/scripts/survivalanalysis/views/survival-analysis.html',
    bindings: {
      analysisId: '<'
    },
    controller: survivalResultController

  });

  var mockSurvivalData1 = [
    { start: 0, end: 100, died: 1, censored: 2, survival: 1 },
    { start: 100, end: 101, died: 3, censored: 1, survival: 0.9991653 },
    { start: 101, end: 102, died: 2, censored: 1, survival: 0.996659 },
    { start: 102, end: 103, died: 2, censored: 2, survival: 0.9949868 },
    { start: 103, end: 104, died: 1, censored: 5, survival: 0.9933118 },
    { start: 104, end: 108, died: 2, censored: 6, survival: 0.9924707 },
    { start: 108, end: 110, died: 2, censored: 3, survival: 0.9907799 },
    { start: 110, end: 112, died: 1, censored: 1, survival: 0.9890848 },
    { start: 112, end: 114, died: 1, censored: 3, survival: 0.9882366 },
    { start: 114, end: 115, died: 4, censored: 4, survival: 0.9873861 },
    { start: 115, end: 116, died: 3, censored: 1, survival: 0.9839726 },
    { start: 116, end: 117, died: 3, censored: 1, survival: 0.9814101 },
    { start: 117, end: 118, died: 1, censored: 5, survival: 0.9788454 },
    { start: 118, end: 119, died: 3, censored: 2, survival: 0.9779868 },
    { start: 119, end: 121, died: 1, censored: 1, survival: 0.9754063 },
    { start: 121, end: 122, died: 1, censored: 1, survival: 0.9745454 },
    { start: 122, end: 123, died: 1, censored: 0, survival: 0.9736837 },
    { start: 123, end: 124, died: 2, censored: 1, survival: 0.9728221 },
    { start: 124, end: 125, died: 2, censored: 3, survival: 0.9710972 },
    { start: 125, end: 126, died: 3, censored: 1, survival: 0.9693677 },
    { start: 126, end: 127, died: 3, censored: 1, survival: 0.9667713 },
    { start: 127, end: 128, died: 1, censored: 2, survival: 0.9641724 },
    { start: 128, end: 129, died: 3, censored: 0, survival: 0.9633046 },
    { start: 129, end: 130, died: 3, censored: 3, survival: 0.9607011 },
    { start: 130, end: 131, died: 1, censored: 2, survival: 0.9580905 },
    { start: 131, end: 132, died: 2, censored: 3, survival: 0.9572187 },
    { start: 132, end: 133, died: 2, censored: 2, survival: 0.9554704 },
    { start: 133, end: 139, died: 4, censored: 9, survival: 0.9537188 },
    { start: 139, end: 141, died: 1, censored: 0, survival: 0.9501866 },
    { start: 141, end: 142, died: 4, censored: 3, survival: 0.9493035 },
    { start: 142, end: 143, died: 2, censored: 1, survival: 0.9457613 },
    { start: 143, end: 144, died: 1, censored: 1, survival: 0.9439886 },
    { start: 144, end: 145, died: 2, censored: 4, survival: 0.9431014 },
    { start: 145, end: 147, died: 1, censored: 1, survival: 0.9413203 },
    { start: 147, end: 148, died: 2, censored: 0, survival: 0.9404289 },
    { start: 148, end: 149, died: 2, censored: 2, survival: 0.9386461 },
    { start: 149, end: 150, died: 2, censored: 1, survival: 0.9368599 },
    { start: 150, end: 151, died: 4, censored: 2, survival: 0.935072 },
    { start: 151, end: 153, died: 1, censored: 3, survival: 0.9314894 },
    { start: 153, end: 154, died: 2, censored: 1, survival: 0.9305911 },
    { start: 154, end: 157, died: 2, censored: 3, survival: 0.9287928 },
    { start: 157, end: 158, died: 2, censored: 2, survival: 0.9269893 },
    { start: 158, end: 159, died: 2, censored: 0, survival: 0.9251823 },
    { start: 159, end: 160, died: 2, censored: 1, survival: 0.9233754 },
    { start: 160, end: 161, died: 2, censored: 2, survival: 0.9215666 },
    { start: 161, end: 162, died: 3, censored: 2, survival: 0.9197543 },
    { start: 162, end: 163, died: 1, censored: 0, survival: 0.9170304 },
    { start: 163, end: 164, died: 4, censored: 3, survival: 0.9161224 },
    { start: 164, end: 165, died: 1, censored: 1, survival: 0.9124798 },
    { start: 165, end: 166, died: 1, censored: 3, survival: 0.9115682 },
    { start: 166, end: 167, died: 3, censored: 0, survival: 0.9106539 },
    { start: 167, end: 168, died: 1, censored: 2, survival: 0.9079109 },
    { start: 168, end: 170, died: 2, censored: 3, survival: 0.9069948 },
    { start: 170, end: 172, died: 1, censored: 3, survival: 0.9051569 },
    { start: 172, end: 173, died: 3, censored: 2, survival: 0.9042351 },
    { start: 173, end: 174, died: 1, censored: 2, survival: 0.9014642 },
    { start: 174, end: 175, died: 1, censored: 2, survival: 0.9005387 },
    { start: 175, end: 176, died: 1, censored: 3, survival: 0.8996112 },
    { start: 176, end: 178, died: 1, censored: 1, survival: 0.8986809 },
    { start: 178, end: 179, died: 2, censored: 2, survival: 0.8977497 },
    { start: 179, end: 180, died: 2, censored: 0, survival: 0.8958832 },
    { start: 180, end: 182, died: 1, censored: 1, survival: 0.8940167 },
    { start: 182, end: 183, died: 2, censored: 1, survival: 0.8930826 },
    { start: 183, end: 184, died: 1, censored: 1, survival: 0.8912122 },
    { start: 184, end: 185, died: 1, censored: 3, survival: 0.8902761 },
    { start: 185, end: 186, died: 2, censored: 3, survival: 0.889337 },
    { start: 186, end: 187, died: 2, censored: 2, survival: 0.8874528 },
    { start: 187, end: 188, died: 3, censored: 1, survival: 0.8855646 },
    { start: 188, end: 189, died: 2, censored: 1, survival: 0.8827293 },
    { start: 189, end: 191, died: 2, censored: 5, survival: 0.8808371 },
    { start: 191, end: 192, died: 2, censored: 4, survival: 0.8789346 },
    { start: 192, end: 193, died: 3, censored: 3, survival: 0.8770239 },
    { start: 193, end: 194, died: 1, censored: 3, survival: 0.8741484 },
    { start: 194, end: 195, died: 1, censored: 1, survival: 0.8731868 },
    { start: 195, end: 196, died: 1, censored: 2, survival: 0.8722241 },
    { start: 196, end: 197, died: 1, censored: 1, survival: 0.8712592 },
    { start: 197, end: 198, died: 2, censored: 1, survival: 0.8702933 },
    { start: 198, end: 200, died: 4, censored: 6, survival: 0.8683593 },
    { start: 200, end: 201, died: 1, censored: 1, survival: 0.8644654 },
    { start: 201, end: 203, died: 2, censored: 2, survival: 0.8634908 },
    { start: 203, end: 204, died: 3, censored: 1, survival: 0.8615372 },
    { start: 204, end: 205, died: 1, censored: 1, survival: 0.8586034 },
    { start: 205, end: 207, died: 1, censored: 2, survival: 0.8576244 },
    { start: 207, end: 209, died: 2, censored: 4, survival: 0.8566431 },
    { start: 209, end: 211, died: 2, censored: 1, survival: 0.8546715 },
    { start: 211, end: 212, died: 2, censored: 3, survival: 0.8526977 },
    { start: 212, end: 213, died: 4, censored: 1, survival: 0.8507171 },
    { start: 213, end: 214, died: 4, censored: 0, survival: 0.846751 },
    { start: 214, end: 216, died: 2, censored: 2, survival: 0.842785 },
    { start: 216, end: 218, died: 2, censored: 0, survival: 0.8407973 },
    { start: 218, end: 219, died: 2, censored: 1, survival: 0.8388096 },
    { start: 219, end: 221, died: 4, censored: 4, survival: 0.8368195 },
    { start: 221, end: 222, died: 1, censored: 1, survival: 0.8328204 },
    { start: 222, end: 223, died: 1, censored: 2, survival: 0.8318194 },
    { start: 223, end: 224, died: 1, censored: 1, survival: 0.830816 },
    { start: 224, end: 225, died: 1, censored: 0, survival: 0.8298113 },
    { start: 225, end: 226, died: 1, censored: 0, survival: 0.8288068 },
    { start: 226, end: 229, died: 2, censored: 1, survival: 0.8278021 },
    { start: 229, end: 232, died: 5, censored: 4, survival: 0.8257905 },
    { start: 232, end: 233, died: 3, censored: 1, survival: 0.8207367 },
    { start: 233, end: 234, died: 2, censored: 1, survival: 0.8177006 },
    { start: 234, end: 235, died: 1, censored: 1, survival: 0.8156741 },
    { start: 235, end: 237, died: 1, censored: 5, survival: 0.8146596 },
    { start: 237, end: 238, died: 1, censored: 1, survival: 0.8136388 },
    { start: 238, end: 240, died: 1, censored: 0, survival: 0.8126166 },
    { start: 240, end: 241, died: 2, censored: 3, survival: 0.8115944 },
    { start: 241, end: 243, died: 3, censored: 4, survival: 0.8095424 },
    { start: 243, end: 245, died: 2, censored: 5, survival: 0.8064486 },
    { start: 245, end: 246, died: 2, censored: 1, survival: 0.8043728 },
    { start: 246, end: 247, died: 1, censored: 1, survival: 0.8022943 },
    { start: 247, end: 251, died: 4, censored: 10, survival: 0.8012537 },
    { start: 251, end: 252, died: 2, censored: 3, survival: 0.7970366 },
    { start: 252, end: 253, died: 2, censored: 1, survival: 0.7949196 },
    { start: 253, end: 254, died: 1, censored: 1, survival: 0.7927998 },
    { start: 254, end: 255, died: 4, censored: 2, survival: 0.7917386 },
    { start: 255, end: 256, died: 1, censored: 0, survival: 0.7874819 },
    { start: 256, end: 257, died: 3, censored: 2, survival: 0.7864177 },
    { start: 257, end: 259, died: 2, censored: 2, survival: 0.7832165 },
    { start: 259, end: 260, died: 2, censored: 3, survival: 0.7810766 },
    { start: 260, end: 261, died: 1, censored: 1, survival: 0.7789278 },
    { start: 261, end: 263, died: 1, censored: 4, survival: 0.7778519 },
    { start: 263, end: 264, died: 3, censored: 2, survival: 0.7767701 },
    { start: 264, end: 266, died: 3, censored: 1, survival: 0.7735155 },
    { start: 266, end: 267, died: 1, censored: 1, survival: 0.7702562 },
    { start: 267, end: 268, died: 1, censored: 3, survival: 0.7691683 },
    { start: 268, end: 269, died: 1, censored: 3, survival: 0.7680757 },
    { start: 269, end: 271, died: 1, censored: 0, survival: 0.7669785 },
    { start: 271, end: 272, died: 3, censored: 3, survival: 0.7658812 },
    { start: 272, end: 273, died: 1, censored: 1, survival: 0.7625753 },
    { start: 273, end: 274, died: 1, censored: 3, survival: 0.7614717 },
    { start: 274, end: 275, died: 1, censored: 1, survival: 0.7603633 },
    { start: 275, end: 277, died: 2, censored: 0, survival: 0.7592533 },
    { start: 277, end: 278, died: 3, censored: 0, survival: 0.7570332 },
    { start: 278, end: 279, died: 2, censored: 0, survival: 0.7537032 },
    { start: 279, end: 280, died: 2, censored: 1, survival: 0.7514831 },
    { start: 280, end: 281, died: 1, censored: 2, survival: 0.7492598 },
    { start: 281, end: 282, died: 1, censored: 4, survival: 0.7481449 },
    { start: 282, end: 283, died: 1, censored: 1, survival: 0.7470232 },
    { start: 283, end: 284, died: 1, censored: 1, survival: 0.7458999 },
    { start: 284, end: 286, died: 1, censored: 3, survival: 0.7447748 },
    { start: 286, end: 287, died: 2, censored: 2, survival: 0.7436447 },
    { start: 287, end: 289, died: 1, censored: 6, survival: 0.7413775 },
    { start: 289, end: 291, died: 3, censored: 4, survival: 0.7402334 },
    { start: 291, end: 292, died: 2, censored: 0, survival: 0.7367797 },
    { start: 292, end: 293, died: 3, censored: 2, survival: 0.7344772 },
    { start: 293, end: 294, died: 3, censored: 2, survival: 0.7310127 },
    { start: 294, end: 295, died: 2, censored: 1, survival: 0.7275372 },
    { start: 295, end: 296, died: 1, censored: 1, survival: 0.7252165 },
    { start: 296, end: 297, died: 2, censored: 2, survival: 0.7240543 },
    { start: 297, end: 299, died: 1, censored: 3, survival: 0.7217224 },
    { start: 299, end: 301, died: 2, censored: 1, survival: 0.7205507 },
    { start: 301, end: 303, died: 2, censored: 4, survival: 0.7182037 },
    { start: 303, end: 305, died: 2, censored: 7, survival: 0.7158412 },
    { start: 305, end: 306, died: 2, censored: 1, survival: 0.7134511 },
    { start: 306, end: 307, died: 1, censored: 0, survival: 0.711057 },
    { start: 307, end: 308, died: 2, censored: 1, survival: 0.7098599 },
    { start: 308, end: 310, died: 3, censored: 2, survival: 0.7074617 },
    { start: 310, end: 311, died: 2, censored: 2, survival: 0.7038522 },
    { start: 311, end: 312, died: 1, censored: 3, survival: 0.7014377 },
    { start: 312, end: 313, died: 3, censored: 0, survival: 0.7002241 },
    { start: 313, end: 314, died: 1, censored: 2, survival: 0.6965834 },
    { start: 314, end: 315, died: 2, censored: 3, survival: 0.6953656 },
    { start: 315, end: 316, died: 2, censored: 2, survival: 0.6929171 },
    { start: 316, end: 319, died: 1, censored: 4, survival: 0.69046 },
    { start: 319, end: 320, died: 5, censored: 0, survival: 0.6892226 },
    { start: 320, end: 321, died: 3, censored: 1, survival: 0.6830357 },
    { start: 321, end: 323, died: 1, censored: 2, survival: 0.6793168 },
    { start: 323, end: 324, died: 1, censored: 1, survival: 0.6780726 },
    { start: 324, end: 325, died: 1, censored: 1, survival: 0.6768261 },
    { start: 325, end: 326, died: 1, censored: 0, survival: 0.6755774 },
    { start: 326, end: 327, died: 4, censored: 2, survival: 0.6743287 },
    { start: 327, end: 329, died: 2, censored: 5, survival: 0.6693151 },
    { start: 329, end: 331, died: 1, censored: 3, survival: 0.6667846 },
    { start: 331, end: 332, died: 2, censored: 1, survival: 0.6655121 },
    { start: 332, end: 333, died: 1, censored: 2, survival: 0.6629622 },
    { start: 333, end: 334, died: 3, censored: 2, survival: 0.6616824 },
    { start: 334, end: 335, died: 2, censored: 1, survival: 0.6578279 },
    { start: 335, end: 338, died: 1, censored: 0, survival: 0.6552532 },
    { start: 338, end: 339, died: 2, censored: 0, survival: 0.6539659 },
    { start: 339, end: 340, died: 1, censored: 1, survival: 0.6513912 },
    { start: 340, end: 341, died: 3, censored: 0, survival: 0.6501013 },
    { start: 341, end: 342, died: 1, censored: 2, survival: 0.6462317 },
    { start: 342, end: 343, died: 5, censored: 1, survival: 0.6449366 },
    { start: 343, end: 344, died: 4, censored: 0, survival: 0.6384482 },
    { start: 344, end: 345, died: 1, censored: 1, survival: 0.6332576 },
    { start: 345, end: 347, died: 4, censored: 4, survival: 0.6319573 },
    { start: 347, end: 351, died: 1, censored: 8, survival: 0.6267129 },
    { start: 351, end: 352, died: 1, censored: 3, survival: 0.6253794 },
    { start: 352, end: 353, died: 3, censored: 1, survival: 0.6240374 },
    { start: 353, end: 354, died: 2, censored: 0, survival: 0.6200027 },
    { start: 354, end: 355, died: 2, censored: 3, survival: 0.6173129 },
    { start: 355, end: 356, died: 1, censored: 4, survival: 0.6146054 },
    { start: 356, end: 357, died: 3, censored: 1, survival: 0.6132396 },
    { start: 357, end: 358, died: 2, censored: 2, survival: 0.6091331 },
    { start: 358, end: 359, died: 1, censored: 1, survival: 0.606383 },
    { start: 359, end: 360, died: 1, censored: 5, survival: 0.6050049 },
    { start: 360, end: 361, died: 1, censored: 2, survival: 0.6036109 },
    { start: 361, end: 362, died: 2, censored: 2, survival: 0.6022104 },
    { start: 362, end: 363, died: 2, censored: 3, survival: 0.5993964 },
    { start: 363, end: 365, died: 2, censored: 3, survival: 0.5965623 },
    { start: 365, end: 366, died: 1, censored: 1, survival: 0.593708 },
    { start: 366, end: 367, died: 3, censored: 3, survival: 0.5922774 },
    { start: 367, end: 368, died: 1, censored: 0, survival: 0.5879542 },
    { start: 368, end: 369, died: 4, censored: 0, survival: 0.5865131 },
    { start: 369, end: 371, died: 2, censored: 4, survival: 0.5807489 },
    { start: 371, end: 372, died: 2, censored: 2, survival: 0.5778378 },
    { start: 372, end: 373, died: 2, censored: 2, survival: 0.5749121 },
    { start: 373, end: 374, died: 2, censored: 2, survival: 0.5719714 },
    { start: 374, end: 376, died: 1, censored: 1, survival: 0.5690154 },
    { start: 376, end: 377, died: 4, censored: 1, survival: 0.5675336 },
    { start: 377, end: 378, died: 2, censored: 4, survival: 0.5615909 },
    { start: 378, end: 379, died: 2, censored: 0, survival: 0.5585877 },
    { start: 379, end: 381, died: 4, censored: 4, survival: 0.5555845 },
    { start: 381, end: 383, died: 3, censored: 1, survival: 0.5495125 },
    { start: 383, end: 384, died: 2, censored: 2, survival: 0.5449459 },
    { start: 384, end: 385, died: 1, censored: 1, survival: 0.5418844 },
    { start: 385, end: 386, died: 1, censored: 2, survival: 0.5403493 },
    { start: 386, end: 387, died: 1, censored: 0, survival: 0.5388054 },
    { start: 387, end: 388, died: 2, censored: 2, survival: 0.5372616 },
    { start: 388, end: 389, died: 2, censored: 1, survival: 0.534156 },
    { start: 389, end: 390, died: 1, censored: 2, survival: 0.5310414 },
    { start: 390, end: 392, died: 3, censored: 5, survival: 0.529475 },
    { start: 392, end: 393, died: 2, censored: 3, survival: 0.5247049 },
    { start: 393, end: 394, died: 2, censored: 5, survival: 0.5214957 },
    { start: 394, end: 395, died: 2, censored: 5, survival: 0.5182363 },
    { start: 395, end: 396, died: 2, censored: 1, survival: 0.5149249 },
    { start: 396, end: 397, died: 2, censored: 1, survival: 0.5116029 },
    { start: 397, end: 398, died: 3, censored: 2, survival: 0.50827 },
    { start: 398, end: 403, died: 2, censored: 6, survival: 0.5032376 },
    { start: 403, end: 404, died: 2, censored: 1, survival: 0.4998142 },
    { start: 404, end: 405, died: 2, censored: 1, survival: 0.4963791 },
    { start: 405, end: 406, died: 2, censored: 2, survival: 0.492932 },
    { start: 406, end: 408, died: 2, censored: 4, survival: 0.4894607 },
    { start: 408, end: 409, died: 2, censored: 0, survival: 0.4859394 },
    { start: 409, end: 410, died: 3, censored: 2, survival: 0.4824181 },
    { start: 410, end: 412, died: 3, censored: 5, survival: 0.4770973 },
    { start: 412, end: 413, died: 2, censored: 2, survival: 0.4716758 },
    { start: 413, end: 414, died: 2, censored: 1, survival: 0.4680335 },
    { start: 414, end: 415, died: 2, censored: 0, survival: 0.464377 },
    { start: 415, end: 417, died: 1, censored: 1, survival: 0.4607205 },
    { start: 417, end: 418, died: 2, censored: 1, survival: 0.4588849 },
    { start: 418, end: 419, died: 2, censored: 2, survival: 0.4551991 },
    { start: 419, end: 420, died: 1, censored: 5, survival: 0.4514832 },
    { start: 420, end: 421, died: 1, censored: 1, survival: 0.4495862 },
    { start: 421, end: 423, died: 1, censored: 6, survival: 0.4476812 },
    { start: 423, end: 424, died: 1, censored: 2, survival: 0.4457262 },
    { start: 424, end: 425, died: 3, censored: 1, survival: 0.443754 },
    { start: 425, end: 426, died: 1, censored: 0, survival: 0.4378108 },
    { start: 426, end: 428, died: 1, censored: 2, survival: 0.4358298 },
    { start: 428, end: 429, died: 1, censored: 0, survival: 0.4338306 },
    { start: 429, end: 431, died: 2, censored: 3, survival: 0.4318314 },
    { start: 431, end: 432, died: 1, censored: 1, survival: 0.4277766 },
    { start: 432, end: 433, died: 1, censored: 3, survival: 0.4257396 },
    { start: 433, end: 434, died: 4, censored: 3, survival: 0.4236729 },
    { start: 434, end: 435, died: 1, censored: 2, survival: 0.4152833 },
    { start: 435, end: 436, died: 4, censored: 1, survival: 0.4131645 },
    { start: 436, end: 437, died: 2, censored: 0, survival: 0.4046457 },
    { start: 437, end: 438, died: 4, censored: 1, survival: 0.4003862 },
    { start: 438, end: 439, died: 3, censored: 3, survival: 0.3918218 },
    { start: 439, end: 441, died: 4, censored: 2, survival: 0.3852915 },
    { start: 441, end: 442, died: 2, censored: 1, survival: 0.3764848 },
    { start: 442, end: 443, died: 3, censored: 3, survival: 0.3720556 },
    { start: 443, end: 444, died: 4, censored: 1, survival: 0.365291 },
    { start: 444, end: 445, died: 2, censored: 0, survival: 0.3562154 },
    { start: 445, end: 446, died: 2, censored: 1, survival: 0.3516777 },
    { start: 446, end: 448, died: 3, censored: 3, survival: 0.3471104 },
    { start: 448, end: 450, died: 2, censored: 3, survival: 0.3401216 },
    { start: 450, end: 451, died: 2, censored: 0, survival: 0.3353647 },
    { start: 451, end: 452, died: 3, censored: 1, survival: 0.3306077 },
    { start: 452, end: 453, died: 2, censored: 1, survival: 0.3234206 },
    { start: 453, end: 454, died: 2, censored: 5, survival: 0.3185934 },
    { start: 454, end: 456, died: 2, censored: 4, survival: 0.3135762 },
    { start: 456, end: 457, died: 1, censored: 1, survival: 0.3083932 },
    { start: 457, end: 458, died: 1, censored: 1, survival: 0.3057796 },
    { start: 458, end: 459, died: 1, censored: 1, survival: 0.3031436 },
    { start: 459, end: 460, died: 1, censored: 0, survival: 0.3004845 },
    { start: 460, end: 461, died: 3, censored: 1, survival: 0.2978253 },
    { start: 461, end: 462, died: 2, censored: 3, survival: 0.289776 },
    { start: 462, end: 463, died: 1, censored: 0, survival: 0.2842564 },
    { start: 463, end: 464, died: 3, censored: 0, survival: 0.2814966 },
    { start: 464, end: 465, died: 2, censored: 2, survival: 0.2732173 },
    { start: 465, end: 467, died: 1, censored: 3, survival: 0.267584 },
    { start: 467, end: 469, died: 1, censored: 4, survival: 0.2646754 },
    { start: 469, end: 470, died: 1, censored: 1, survival: 0.2616332 },
    { start: 470, end: 472, died: 2, censored: 2, survival: 0.2585551 },
    { start: 472, end: 473, died: 3, censored: 2, survival: 0.2522489 },
    { start: 473, end: 474, died: 3, censored: 3, survival: 0.242547 },
    { start: 474, end: 475, died: 4, censored: 1, survival: 0.2324409 },
    { start: 475, end: 476, died: 2, censored: 0, survival: 0.2187679 },
    { start: 476, end: 477, died: 1, censored: 0, survival: 0.2119314 },
    { start: 477, end: 478, died: 2, censored: 2, survival: 0.2085131 },
    { start: 478, end: 479, died: 2, censored: 2, survival: 0.2014449 },
    { start: 479, end: 480, died: 1, censored: 3, survival: 0.1941196 },
    { start: 480, end: 481, died: 2, censored: 1, survival: 0.1902372 },
    { start: 481, end: 482, died: 2, censored: 2, survival: 0.1823107 },
    { start: 482, end: 485, died: 3, censored: 5, survival: 0.1740238 },
    { start: 485, end: 486, died: 2, censored: 1, survival: 0.1599138 },
    { start: 486, end: 487, died: 3, censored: 2, survival: 0.150222 },
    { start: 487, end: 488, died: 2, censored: 0, survival: 0.1346818 },
    { start: 488, end: 489, died: 1, censored: 2, survival: 0.1243217 },
    { start: 489, end: 491, died: 2, censored: 0, survival: 0.1186707 },
    { start: 491, end: 493, died: 1, censored: 3, survival: 0.1073687 },
    { start: 493, end: 494, died: 1, censored: 2, survival: 0.1006582 },
    { start: 494, end: 496, died: 1, censored: 4, survival: 0.0929152 },
    { start: 496, end: 497, died: 1, censored: 1, survival: 0.0813008 },
    { start: 497, end: 498, died: 1, censored: 0, survival: 0.0677507 },
    { start: 498, end: 500, died: 1, censored: 3, survival: 0.0542006 },
  ];

  var mockSurvivalData2 = JSON.parse(JSON.stringify(mockSurvivalData1)).map(function (item) {
    return _.extend(item, {survival: item.survival - 0.02});
  });

  module
    .service('survivalPlotService', [function () {

      _.extend(this, {
        getData: function () {
          console.log('getting mock survival data');

          return Promise.resolve([mockSurvivalData1, mockSurvivalData2]);
        }
      });

    }]);

})();