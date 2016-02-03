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

(function ($) {
  'use strict';

  var module = angular.module('icgc.repository.directives', ['cfp.loadingBar']);

  module.directive('bamstats', function ($timeout, cfpLoadingBar) {
    return {
      replace: true,
      restrict: 'AE',
      scope: {
        bamId: '=',
        onModal: '=',
        bamName: '='
      },
      templateUrl: 'scripts/repository/views/bamiobio.html',
      link: function (scope, element) {

        // iobio team wanted analytics of when their app is being loaded
        if (iobioGoogleAnalytics !== undefined) {
          iobioGoogleAnalytics('send', 'pageview');
        }

        // Initialize charts
        // Get height width of histogram charts and set viewboxes
        var readCoverageSvg = $('#read-coverage-distribution-chart');
        var width = readCoverageSvg.width();
        var height = readCoverageSvg.height();

        // Viewboxes
        var dists = $('.focus', element);

        // Need to be set at runtime to get accurate height/width
        for (var i = 0; i < dists.length; i++) {
          dists[i].setAttribute('viewBox', '0 0 ' + width + ' ' + height);
        }

        // Setup donut chart
        var sampleDonutChart = donutD3().radius(100).klass(
          'sampleArc');

        // Hold onto stats
        var sampleStats;

        // Default sampling values
        var binNumber = 20;
        var binSize = 40000;
        var sampleMultiplier = 1;
        var sampleMultiplierLimit = 4;

        var bam = new Bam(scope.bamId);

        var charts = {};

        // Setup main window read depth chart
        charts.depthChart = movingLineD3('#read-depth-container');

        // Setup read coverage histogram chart
        var readCoverageChart = histogramViewFinderD3().width(width).height(height);

        readCoverageChart.yAxis().tickFormat(function (tickValue) {
          return tickValue * 100 + '%';
        });

        scope.toggleOutliers = function () {
          scope.showOutliers = !scope.showOutliers;
          var histDataObject = sampleStats[scope.lengthChartDataId],
            histData = pivot(histDataObject),
            selection = d3.select('#length-distribution svg');
          selection.datum(histData);
          charts.lengthChart(selection, {
            'outliers': scope.showOutliers
          });
        };

        // Highlights the selected chromosome button
        scope.highlightSelectedSeq = function (chrId) {
          scope.chromosomes.forEach(function (chr) {
            if (chr.id === chrId) {
              chr.selected = true;
            } else {
              chr.selected = false;
            }
          });
          scope.setSelectedSeq(chrId);
        };

        scope.setSelectedSeq = function (selectedChrId, start, end) {
          scope.chromosomeId = selectedChrId;
          charts.depthChart(bam.readDepth[scope.chromosomeId]);
          // Reset brush
          resetBrush();
          // Start sampling
          if (start !== undefined && end !== undefined) {
            goSampling({
              sequenceNames: [scope.chromosomeId],
              'start': start,
              'end': end
            });
            var brush = charts.depthChart.brush();
            // Set brush region
            d3.select('#depth-distribution .brush').call(
              brush.extent([start, end]));
          } else {
            goSampling({
              sequenceNames: [scope.chromosomeId]
            });
          }
        };

        scope.sampleMore = function () {
          if (sampleMultiplier >= sampleMultiplierLimit) {
            window.alert('You\'ve reached the sampling limit');
            return;
          }
          sampleMultiplier += 1;
          var options = {
            sequenceNames: [scope.chromosomeId],
            binNumber: binNumber + parseInt(binNumber / 4 * sampleMultiplier),
            binSize: binSize + parseInt(binSize / 4 * sampleMultiplier)
          };
          // Sets new options and samples for new statistics
          var lengthExtent = charts.depthChart.brush().extent();
          if (lengthExtent.length !== 0 &&
            lengthExtent.toString() !== '0,0') {
            options.start = parseInt(lengthExtent[0]);
            options.end = parseInt(lengthExtent[1]);
          }
          goSampling(options);
        };

        scope.toggleChart = function (event, chartId) {
          var elem = event.target;
          if ($(elem).hasClass('selected')) {
            return;
          }
          // Toggle selected
          var pair = [elem, $(elem).siblings()[0]];
          $(pair).toggleClass('selected');

          // Redraw chart
          var histDataObject;
          if (elem.getAttribute('data-id') === 'frag_hist' || elem.getAttribute('data-id') === 'length_hist') {
            scope.lengthChartDataId = elem.getAttribute('data-id');
            histDataObject = sampleStats[scope.lengthChartDataId];
          } else {
            scope.qualityChartDataId = elem.getAttribute('data-id');
            histDataObject = sampleStats[scope.qualityChartDataId];
          }
          var histData = pivot(histDataObject);
          var selection = d3.select($(elem).parent().parent().parent()
            .find('svg')[0]);
          selection.datum(histData);
          charts[chartId](selection);
        };

        scope.cancel = function () {
          scope.$parent.cancel();
        };

        function goBam() {
          // Get read depth
          bam.estimateBaiReadDepth(function (id) {
            scope.chromosomes.push({
              id: id,
              selected: false
            });
            // Setup first time and sample
            if ($('.seq-buttons').length === 0) {
              // Turn off read depth loading msg
              $('#ref-label').css('visibility', 'visible');
              $('#total-reads #sample-more').css('visibility', 'visible');
              $('#readDepthLoadingMsg').css('display', 'none');
              // Turn on sampling message
              $('.samplingLoader').css('display', 'block');

              // Update depth distribution
              charts.depthChart.on('brushend', function (x, brush) {
                var options = {
                  sequenceNames: [scope.chromosomeId]
                };
                if (!brush.empty()) {
                  options.start = parseInt(brush.extent()[0]);
                  options.end = parseInt(brush.extent()[1]);
                  scope.region = {
                    chr: scope.chromosomeId,
                    'start': options.start,
                    'end': options.end
                  };
                }

                goSampling(options);
              });
            }
            $timeout($.noop, 0);
          });
        }

        function pivot(hist) {
          var histData = Object.keys(hist).map(
            function (key) {
              return [+key, +hist[key]];
            });
          return histData;
        }

        function resetBrush() {
          var brush = charts.depthChart.brush();
          var g = d3.selectAll('#depth-distribution .brush');
          brush.clear();
          brush(g);
        }

        // Determines the format of the current total reads sampled and shortens if necessary
        function updateTotalReads(totalReads) {
          var numOfReadDigits = totalReads.toString().length;
          if (numOfReadDigits <= 3) {
            scope.readsSampled.value = totalReads;
            scope.readsSampled.units = '';
          } else if (numOfReadDigits <= 6) {
            scope.readsSampled.value = Math.round(totalReads / 1000);
            scope.readsSampled.units = 'thousand';
          } else {
            scope.readsSampled.value = Math.round(totalReads / 1000000);
            scope.readsSampled.units = 'million';
          }
          // Need to trigger a digest cycle if one is not already in progress
          // Timeout needs a function as a parameter, passed in empty function
          $timeout($.noop, 0);
        }

        function goSampling(options) {
          // Add default options
          options = $.extend({
            bed: window.bed,
            onEnd: function () {
              cfpLoadingBar.complete();
            }
          }, options);
          // Turn on sampling message and off svg
          $('section#middle svg').css('display', 'none');
          $('.samplingLoader').css('display', 'block');
          updateTotalReads(0);
          cfpLoadingBar.start();
          // Sets progress bar to 0 because of existing progress bar on page
          cfpLoadingBar.set(0);
          // Update selected stats
          bam.sampleStats(function (data, seq) {
            if (scope.chromosomeId !== seq) {
              return;
            }
            // Turn off sampling message
            $('.samplingLoader').css('display', 'none');
            $('section#middle svg').css('display', 'block');
            $('section#middle svg').css('margin', 'auto');
            sampleStats = data;
            // Update progress bar
            var length, percentDone;
            if (options.start !== null && options.end !== null) {
              length = options.end - options.start;
              percentDone = Math.round(((data.last_read_position - options.start) / length) * 100) / 100;
            } else {
              length = bam.header.sq.reduce(
                function (prev, curr) {
                  if (prev) {
                    return prev;
                  }
                  if (curr.name === options.sequenceNames[0]) {
                    return curr;
                  }
                }, false).end;
              percentDone = Math.round((data.last_read_position / length) * 100) / 100;
            }
            if (cfpLoadingBar.status < percentDone) {
              cfpLoadingBar.set(percentDone);
            }
            // Update charts
            updatePercentCharts(data, sampleDonutChart);
            updateTotalReads(data.total_reads);
            updateHistogramCharts(data);
          }, options);
        }

        function updatePercentCharts(stats, donutChart) {
          var pie = d3.layout.pie().sort(null);
          // Update percent charts
          var keys = ['mapped_reads', 'proper_pairs',
                      'forward_strands', 'singletons', 'both_mates_mapped',
                      'duplicates'];
          keys.forEach(function (key) {
            var stat = stats[key];
            var data = [stat, stats.total_reads - stat];
            var arc = d3.select('#' + key + ' svg').selectAll('.arc')
              .data(pie(data));
            donutChart(arc);
          });
        }

        function updateHistogramCharts(histograms) {
          // Check if coverage is zero
          if (Object.keys(histograms.coverage_hist).length === 0) {
            histograms.coverage_hist[0] = '1.0';
          }
          // Update read coverage histogram
          var histData, selection;
          histData = Object.keys(histograms.coverage_hist).filter(
            function (i) {
              return histograms.coverage_hist[i] !== '0';
            }).map(function (key) {
            return [+key, +histograms.coverage_hist[key]];
          });

          selection = d3.select('#read-coverage-distribution-chart').datum(histData);
          readCoverageChart(selection);
          if (histograms.coverage_hist[0] > 0.65) {
            // Exclude <5 values b\c they are not informative dominating the chart
            var min = 5;
            var max = readCoverageChart.globalChart().x()
              .domain()[1];
            readCoverageChart.setBrush([min, max]);
          }

          // Update read length distribution
          if (scope.lengthChartDataId === 'frag_hist') {
            histData = Object.keys(histograms.frag_hist).filter(
              function (i) {
                return histograms.frag_hist[i] !== '0';
              }).map(function (key) {
              return [+key, +histograms.frag_hist[key]];
            });
          } else {
            histData = pivot(histograms.length_hist);
          }
          // Remove outliers if outliers checkbox isn't explicity checked
          selection = d3.select('#length-distribution-chart').datum(histData);
          charts.lengthChart(selection, {
            'outliers': scope.showOutliers
          });

          // Update map quality distribution
          if (scope.qualityChartDataId === 'mapq_hist') {
            histData = pivot(histograms.mapq_hist);
          } else {
            histData = pivot(histograms.baseq_hist);
          }
          selection = d3.select('#mapping-quality-distribution-chart').datum(histData);
          charts.qualityChart(selection);
        }

        function tickFormatter(tickValue) {
          if ((tickValue / 1000000) >= 1) {
            tickValue = tickValue / 1000000 + 'M';
          } else if ((tickValue / 1000) >= 1) {
            tickValue = tickValue / 1000 + 'K';
          }
          return tickValue;
        }

        // Setup length histrogram chart
        charts.lengthChart = histogramViewFinderD3().width(width)
          .height(height);
        charts.lengthChart.xAxis().tickFormat(tickFormatter);
        charts.lengthChart.yAxis().tickFormat(tickFormatter);

        // Setup quality histogram chart
        charts.qualityChart = histogramD3().width(width).height(height);
        charts.qualityChart.xAxis().tickFormat(tickFormatter);
        charts.qualityChart.yAxis().tickFormat(tickFormatter);

        scope.chromosomes = [];
        scope.showOutliers = false;
        scope.readsSampled = {};
        scope.lengthChartDataId = 'frag_hist';
        scope.qualityChartDataId = 'mapq_hist';

        scope.$on('$destroy', function () {
          if (bam.sampleClient !== undefined) {
            bam.sampleClient.close(1000);
          }
          cfpLoadingBar.complete();
        });
        goBam();
      }
    };
  });
})(jQuery);