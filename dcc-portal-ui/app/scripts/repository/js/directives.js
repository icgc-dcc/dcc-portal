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

(function($) {
  'use strict';

  var module = angular.module('icgc.repository.directives', ['cfp.loadingBar']);

  module.directive('bamstats', function($timeout, cfpLoadingBar, gettextCatalog) {
    return {
      replace: true,
      restrict: 'AE',
      scope: {
        bamId: '=',
        onModal: '=',
        bamName: '=',
        bamFileName: '='	
      },
      templateUrl: 'scripts/repository/views/bamiobio.html',
      link: function(scope, element) {

        scope.closing = false;

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

        readCoverageChart.yAxis().tickFormat(function(tickValue) {
          return tickValue * 100 + '%';
        });

        scope.toggleOutliers = function() {
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
        scope.highlightSelectedSeq = function(chrId) {
          scope.chromosomes.forEach(function(chr) {
            if (chr.id === chrId) {
              chr.selected = true;
            } else {
              chr.selected = false;
            }
          });
          scope.setSelectedSeq(chrId);
        };

        scope.setSelectedSeq = function(selectedChrId, start, end) {
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

        scope.sampleMore = function() {
          if (sampleMultiplier >= sampleMultiplierLimit) {
            window.alert(gettextCatalog.getString('You\'ve reached the sampling limit'));
            return;
          }
          sampleMultiplier += 1;
          var options = {
            sequenceNames: [scope.chromosomeId],
            binNumber: binNumber + parseInt(binNumber / 4 * sampleMultiplier, 10),
            binSize: binSize + parseInt(binSize / 4 * sampleMultiplier, 10)
          };
          // Sets new options and samples for new statistics
          var lengthExtent = charts.depthChart.brush().extent();
          if (lengthExtent.length !== 0 &&
            lengthExtent.toString() !== '0,0') {
            options.start = parseInt(lengthExtent[0], 10);
            options.end = parseInt(lengthExtent[1], 10);
          }
          goSampling(options);
        };

        scope.toggleChart = function(event, chartId) {
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

        scope.cancel = function() {
          scope.$parent.cancel();
        };

        function goBam() {
          // Get read depth
          bam.estimateBaiReadDepth(function(id) {
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
              charts.depthChart.on('brushend', function(x, brush) {
                var options = {
                  sequenceNames: [scope.chromosomeId]
                };
                if (!brush.empty()) {
                  options.start = parseInt(brush.extent()[0], 10);
                  options.end = parseInt(brush.extent()[1], 10);
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
            function(key) {
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
            onEnd: function() {
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
          bam.sampleStats(function(data, seq) {
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
                function(prev, curr) {
                  if (prev) {
                    return prev;
                  }
                  if (curr.name === options.sequenceNames[0]) {
                    return curr;
                  }
                  return false;
                }, false).end;
              percentDone = Math.round((data.last_read_position / length) * 100) / 100;
            }
            if (cfpLoadingBar.status < percentDone) {
              cfpLoadingBar.set(percentDone);
            }
            // Update charts, make sure we are still receiving data before attempting to manuplate DOM. 
            try {
              if (!scope.closing && bam.sampleClient._socket.readyState === 1) {
                updatePercentCharts(data, sampleDonutChart);
                updateTotalReads(data.total_reads);
                updateHistogramCharts(data);
              }
            } catch (error) {
              // Gracefully report that an update was in flight during teardown. 
              throw new Error('An error occured during chart update: ' + error);
            }
             
          }, options);
        }

        function updatePercentCharts(stats, donutChart) {
          var pie = d3.layout.pie().sort(null);
          // Update percent charts
          var keys = ['mapped_reads', 'proper_pairs',
            'forward_strands', 'singletons', 'both_mates_mapped',
            'duplicates'];
          keys.forEach(function(key) {
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
            function(i) {
              return histograms.coverage_hist[i] !== '0';
            }).map(function(key) {
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
              function(i) {
                return histograms.frag_hist[i] !== '0';
              }).map(function(key) {
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

        scope.$on('$destroy', function() {
          if (bam.sampleClient !== undefined) {
            bam.sampleClient.close(1000);
          }
          cfpLoadingBar.complete();
        });
        goBam();
      }
    };
  });

  module.directive('vcfstats', function(gettextCatalog) {
    return {
      replace: true,
      restrict: 'AE',
      scope: {
        vcfId: '=',
        onModal: '=',
        vcfName: '=',
        vcfFileName: '='
      },
      templateUrl: 'scripts/repository/views/vcfiobio.html',
      link: function(scope) {

        var vcfiobio;
        var chromosomeChart;
        var variantDensityChart;
        var variantDensityVF;
        var variantDensityRefVF;

        var tstvChart;
        var alleleFreqChart;
        var mutSpectrumChart;
        var varTypeChart;
        var qualDistributionChart;
        var indelLengthChart;

        var densityPanelDimensions = {
          width: 0,
          height: 0,
          padding: 40,
          verticalOffset: 120
        };

        var chromosomeIndex = 0;
        var regionStart = null;
        var regionEnd = null;
        var afData = null;

        var densityOptions = {
          removeSpikes: false,
          maxPoints: 5000,
          epsilonRDP: null
        };
        var densityRegionOptions = {
          removeSpikes: false,
          maxPoints: 1000,
          epsilonRDP: null
        };
        var statsOptions = {
          samplingMultiplier: 1,
          binSize: 80000,
          binNumber: 50,
          start: 1
        };
        var colorSchemeMS = {
          A: [1, 2, 3],
          G: [0, 2, 3],
          C: [0, 1, 3],
          T: [0, 1, 2]
        };
        var colorMS = [
          '#8ca252',
          '#e7ba52',
          '#1f77b4',
          '#ad494a'
        ];
        var lookupNucleotide = {
          A: ['G', 'C', 'T'],
          G: ['A', 'C', 'T'],
          C: ['A', 'G', 'T'],
          T: ['A', 'G', 'C']
        };
        var	samplingMultiplierLimit = 4;
        var colorListVarType = ['#2171b5', '#eff3ff', '#bdd7e7', '#6baed6'];

        function init() {
          d3.selectAll('.vcf-iobio svg').style('visibility', 'hidden');
          d3.selectAll('.svg-alt').style('visibility', 'hidden');
          d3.selectAll('.samplingLoader').style('display', 'block');

          vcfiobio = new Vcfiobio();

          // Get the container dimensions to determine the chart dimensions
          getChartDimensions();

          // Create the chromosome picker chart. Listen for the click event on one of the arcs.
          // This event is dispatched when the user clicks on a particular chromosome.
          chromosomeChart = donutChooserD3()
            .width(220)
            .height(220)
            .options({ showTooltip: false })
            .on('clickslice', function(d) {
              chromosomeIndex = d.idx;
              regionStart = null;
              regionEnd = null;
              onReferenceSelected(d, d.idx);
            })
            .on('clickall', function() {
              chromosomeIndex = -1;
              regionStart = null;
              regionEnd = null;
              onAllReferencesSelected();
            });

          // Create the variant density chart
          variantDensityChart = lineD3()
            .width(densityPanelDimensions.width)
            .height(densityPanelDimensions.height - densityPanelDimensions.verticalOffset)
            .widthPercent('100%')
            .heightPercent('100%')
            .kind('area')
            .margin({ left: 20, right: 20, top: 0, bottom: 20 })
            .showXAxis(true)
            .showYAxis(false)
            .pos(function(d) { return d[0] })
            .depth(function(d) { return d[1] });

          // View finder (area chart) for variant density chart (when a references is selected)
          variantDensityVF = lineD3()
            .width(densityPanelDimensions.width)
            .height(20)
            .widthPercent('100%')
            .heightPercent('100%')
            .kind('area')
            .margin({ left: 20, right: 20, top: 10, bottom: 20 })
            .showYAxis(false)
            .showBrush(true)
            .brushHeight(40)
            .pos(function(d) { return d[0] })
            .depth(function(d) { return d[1] })
            .showGradient(false);

          // View finder (reference as boxes on x-axis) for variant density chart (for all references)
          variantDensityRefVF = barChartAltD3()
            .width(densityPanelDimensions.width)
            .height(20)
            .widthPercent('100%')
            .heightPercent('100%')
            .margin({ left: 20, right: 20, top: 0, bottom: 0 })
            .nameFunction(function(d) { return d.name })
            .valueFunction(function(d) { return d.value })
            .on('clickbar', function(d, i) {
              chromosomeIndex = d.idx;
              chromosomeChart.clickSlice(i);
              onReferenceSelected(d, d.idx);
            });

          // TSTV grouped barchart (to show ratio)
          tstvChart = groupedBarD3();
          var tstvCategories = ['TS', 'TV'];
          tstvChart.width($('#tstv-ratio').width() - 20)
            .height($('#tstv-ratio').height() - 130)
            .widthPercent('100%')
            .heightPercent('100%')
            .margin({ left: 10, right: 10, top: 30, bottom: 10 })
            .showXAxis(true)
            .showYAxis(false)
            .showXTicks(false)
            .showTooltip(false)
            .categories(tstvCategories)
            .categoryPadding(0.4)
            .showBarLabel(true)
            .barLabel(function(d, i) {
              return tstvCategories[i];
            });

          // Allele freq chart
          alleleFreqChart = histogramD3VCF()
            .width(355)
            .height(120)
            .margin({ left: 45, right: 0, top: 10, bottom: 30 })
            .xValue(function(d) { return d[0] })
            .yValue(function(d) { return Math.log(d[1]) })
            .yAxisLabel('log(frequency)');

          alleleFreqChart.formatXTick(function(d) {
            return (d * 2) + '%';
          });
          alleleFreqChart.tooltipText(function(d, i) {
            var value = afData[i][1];
            return d3.round(value) + ' variants with ' + (d[0] * 2) + '%' + ' AF ';
          });

          // Mutation spectrum grouped barchart
          mutSpectrumChart = groupedBarD3();
          mutSpectrumChart.width(355)
            .height(120)
            .widthPercent('95%')
            .heightPercent('85%')
            .margin({ left: 45, right: 0, top: 15, bottom: 30 })
            .categories(['1', '2', '3'])
            .categoryPadding(0.5)
            .fill(function(d, i) {
              var colorScheme = colorSchemeMS[d.category];
              var colorIdx = colorScheme[i];
              return colorMS[colorIdx];
            })
            .barLabel(function(d) {
              var nucleotide = lookupNucleotide[d.category];
              return nucleotide[+d.name - 1];
            })
            .xAxisLabel('Reference Base');

          // var type barchart (to show ratio)
          varTypeChart = groupedBarD3();
          var varTypeCategories = ['SNP', 'Ins', 'Del', 'Other'];
          varTypeChart.width(150)
            .height(90)
            .margin({ left: 40, right: 10, top: 30, bottom: 30 })
            .showXAxis(true)
            .showYAxis(true)
            .showXTicks(false)
            .categories(varTypeCategories)
            .categoryPadding(0.1)
            .colorList(colorListVarType)
            .showBarLabel(true)
            .barLabel(function(d, i) {
              return varTypeCategories[i];
            });

          // Indel length chart
          indelLengthChart = histogramD3VCF();
          indelLengthChart.width($('#indel-length').width())
            .height($('#indel-length').height() - 25)
            .widthPercent('100%')
            .heightPercent('100%')
            .margin({ left: 40, right: 0, bottom: 30, top: 20 })
            .xValue(function(d) { return d[0] })
            .yValue(function(d) { return d[1] })
            .xAxisLabel(function() { return 'Deletions: x < 0, Insertions: x > 0' });
            indelLengthChart.tooltipText(function(d) {
              return d[1] + ' variants with a ' + Math.abs(d[0]) + ' bp ' + (d[0] < 0 ? 'deletion' : 'insertion');
            });

          // QC score histogram chart
          qualDistributionChart = histogramD3VCF();
          qualDistributionChart.width($('#qual-distribution').width())
            .height($('#qual-distribution').height() - 20)
            .widthPercent('100%')
            .heightPercent('100%')
            .margin({ left: 40, right: 0, bottom: 30, top: 20 })
            .xValue(function(d) { return d[0] })
            .yValue(function(d) { return d[1] })
            .xAxisLabel('Variant Quality Score');
          qualDistributionChart.tooltipText(function(d) {
            return d3.round(d[1]) + ' variants with VQ of ' + d[0];
          });
          
          _loadVcfFromUrl(scope.vcfId);
        }

        function _loadVcfFromUrl(url) {
          vcfiobio.openVcfUrl(url);
          d3.select('#vcf_file').text(url);
          d3.select('#selectData').style('visibility', 'hidden').style('display', 'none');
          vcfiobio.loadRemoteIndex(url, checkReferences);
        }

        function checkReferences(refData) {
          if(refData.length > 0){
            onReferencesLoaded(refData);
            d3.select('#loadingData').transition().style('display','none');
            d3.select('#showData').transition().style('visibility', 'visible');
          } else {
            d3.select('#loadingData').transition().style('display','none');
            d3.select('#showData').transition().style('display','none').style('visibility', 'hidden');
            d3.select('#noData').transition().style('display','block');
          }
          
        }

        function onReferencesLoaded(refData) {
          d3.selectAll('section#top svg').style('display', 'block');
          d3.selectAll('section#top .svg-alt').style('display', 'block');
          d3.selectAll('section#top .samplingLoader').style('display', 'none');

          var otherRefData = null;
          var pieChartRefData = null;
          pieChartRefData = vcfiobio.getReferences(0.005, 1);

          chromosomeChart(d3.select('#primary-references').datum(pieChartRefData));

          // Show "ALL" references as first view
          chromosomeChart.clickAllSlices(pieChartRefData);
          otherRefData = vcfiobio.getReferences(0, 0.005);

          if (otherRefData.length > 0) {
            var dropdown = d3.select('#other-references-dropdown');

            dropdown.on('change', function() {
              chromosomeIndex = this.value;
              onReferenceSelected(refData[chromosomeIndex], chromosomeIndex);
            });

            dropdown.selectAll('option')
              .data(otherRefData)
              .enter()
              .append('option')
              .attr('value', function(d) { return d.idx })
              .text(function(d) { return d.name });

            d3.select('#other-references').style('display', 'block');
          } else {
            d3.select('#other-references').style('display', 'none');
          }
        }

        function getChartDimensions() {
          densityPanelDimensions.width = $('#variant-density-panel').width() - densityPanelDimensions.padding;
          densityPanelDimensions.height = $('#variant-density-panel').height();
        }

        function onAllReferencesSelected() {
          d3.select('#reference_selected').text(gettextCatalog.getString('All References'));
          d3.select('#region_selected').text('');
          d3.select('#variant-density-panel').select('.hint')
            .text(gettextCatalog.getString('(click bottom chart to select a reference)'));

          loadGenomeVariantDensityData();
          loadStats(chromosomeIndex);
        }

        function loadGenomeVariantDensityData() {
          d3.select('#variant-density-vf').style('display', 'none');
          d3.select('#variant-density-ref-vf').style('display', 'block');
          d3.selectAll('section#top .svg-alt').style('visibility', 'visible');

          // Calculate the width and height of the panel as it may have changed since initialization
          getChartDimensions();
          variantDensityChart.width(densityPanelDimensions.width);
          variantDensityChart.height(densityPanelDimensions.height - densityPanelDimensions.verticalOffset);
          variantDensityRefVF.width(densityPanelDimensions.width);

          var data = vcfiobio.getGenomeEstimatedDensity(false, densityOptions.removeSpikes,
            densityOptions.maxPoints, densityOptions.epsilonRDP);

          // Load the variant density chart with the data
          variantDensityChart.showXAxis(false);
          variantDensityChart(d3.select('#variant-density').datum(data), onVariantDensityChartRendered);
          variantDensityRefVF(d3.select('#variant-density-ref-vf').datum(vcfiobio.getReferences(0.005, 1)));
        }

        function onVariantDensityChartRendered() {
          // TODO: REMOVE THIS!
        }

        function onVariantDensityVFChartRendered() {
          // TODO: REMOVE THIS!
        }

        function loadStats(i) {
          d3.select('#total-reads').select('#value').text(0);
          d3.selectAll('section#middle svg').style('visibility', 'hidden');
          d3.selectAll('section#middle .svg-alt').style('visibility', 'hidden');
          d3.selectAll('section#middle .samplingLoader').style('display', 'block');
          d3.selectAll('section#bottom svg').style('visibility', 'hidden');
          d3.selectAll('section#bottom .svg-alt').style('visibility', 'hidden');
          d3.selectAll('section#bottom .samplingLoader').style('display', 'block');

          var options = JSON.parse(JSON.stringify(statsOptions));
          var refs = [];
          refs.length = 0;
          // If we are getting stats by sampling all references,
          // we will divide the bins across the references.
          // Otherwise, the user has selected a particular reference
          // (and optionally selected a region of the reference)
          // and we will sample across the reference or a region
          // of the reference.
          if (i === -1) {
            var numReferences = vcfiobio.getReferences(0.005, 1).length;
            for (var x = 0; x < numReferences; x++) {
              refs.push(x);
            }
            options.binNumber = d3.round(statsOptions.binNumber / numReferences);
          } else {
            refs.push(i);
            options.start = regionStart;
            options.end = regionEnd;
          }

          // Increase the bin size by the sampling multiplier, which
          // captures the number of times the "sample more" button
          // has been pressed by the user
          options.binNumber = options.binNumber * statsOptions.samplingMultiplier;

          vcfiobio.getStats(refs, options, function(data) {
            renderStats(data);
          });
        }

        function renderWarning() {
          $('#vcf-messages').show();
        }

        function renderStats(stats) {
          $('#vcf-messages').hide();
          d3.selectAll('section#middle svg').style('visibility', 'visible');
          d3.selectAll('section#middle .svg-alt').style('visibility', 'visible');
          d3.selectAll('section#middle .samplingLoader').style('display', 'none');

          d3.selectAll('section#bottom svg').style('visibility', 'visible');
          d3.selectAll('section#bottom .svg-alt').style('visibility', 'visible');
          d3.selectAll('section#bottom .samplingLoader').style('display', 'none');

          if (stats.TotalRecords === 0) {
            renderWarning();
          }

          // # of Variants sampled	
          var readParts = shortenNumber(stats.TotalRecords);
          d3.select('#total-reads')
            .select('#value')
            .text(readParts[0] || 0);
          d3.select('#total-reads')
            .select('#number')
            .text(readParts[1] || '\xa0');

          // TsTv Ratio
          var tstvRatio = stats.TsTvRatio;
          if (tstvRatio === undefined || tstvRatio === null) {
            tstvRatio = 0;
          }
          d3.select('#tstv-ratio')
            .select('#ratio-value')
            .text(tstvRatio.toFixed(2));

          var tstvData = [
            { category: '', values: [tstvRatio, 1] }
          ];
          // This is the parent object for the chart
          var tstvSelection = d3.select('#ratio-panel').datum(tstvData);
          // Render the mutation spectrum chart with the data
          tstvChart(tstvSelection);

          // Var types
          var varTypeArray = vcfiobio.jsonToValueArray(stats.var_type);
          var varTypeData = [
            { category: '', values: varTypeArray }
          ];
          // This is the parent object for the chart
          var varTypeSelection = d3.select('#var-type').datum(varTypeData);
          // Render the var type data with the data
          varTypeChart(varTypeSelection);

          // Alelle Frequency
          var afObj = stats.af_hist;
          afData = vcfiobio.jsonToArray2D(afObj);
          if (afData.length > 0 ) {
            var afSelection = d3.select('#allele-freq-histogram')
              .datum(afData);
            alleleFreqChart(afSelection, { outliers: true, averageLine: false });
          } else {
            var emptySelection = d3.select('#allele-freq-histogram')
              .datum([]);
            alleleFreqChart(emptySelection, { outliers: true, averageLine: false });
          }

          // Mutation Spectrum
          var msObj = stats.mut_spec;
          var msArray = vcfiobio.jsonToArray(msObj, 'category', 'values');
          // Exclude the 0 value as this is the base that that represents the
          // "category"  Example:  For mutations for A, keep values for G, C, T,
          // but exclude 0 value for A.
          msArray.forEach(function(d) {
            d.values = d.values.filter(function(val) {
              if (val === undefined || isNaN(val) || val === 0) {
                return false;
              } else {
                return true;
              }
            });
          });
          // This is the parent object for the chart
          var msSelection = d3.select('#mut-spectrum').datum(msArray);
          // Render the mutation spectrum chart with the data
          mutSpectrumChart(msSelection);


          // QC distribution
          var qualPoints = vcfiobio.jsonToArray2D(stats.qual_dist.regularBins);
          var factor = 2;
          var qualReducedPoints = 
            vcfiobio.reducePoints(qualPoints, factor, function(d) { return d[0] }, function(d) { return d[1] });

          var qualSelection = d3.select('#qual-distribution-histogram')
            .datum(qualReducedPoints);
          var qualOptions = { outliers: true, averageLine: true };
          qualDistributionChart(qualSelection, qualOptions);

          // Indel length distribution
          var indelData = vcfiobio.jsonToArray2D(stats.indel_size);
          var indelSelection = d3.select('#indel-length-histogram')
            .datum(indelData);
          var indelOptions = { outliers: true, averageLine: false };
          indelLengthChart(indelSelection, indelOptions);

          // Reset the sampling multiplier back to one
          // so that next time we get stats, we start
          // with the default sampling size that can
          // be increased as needed
          statsOptions.samplingMultiplier = 1;
        }

        function shortenNumber(num) {
          if (num.toString().length <= 3) {
            return [num];
          } else if (num.toString().length <= 6) {
            return [Math.round(num / 1000), 'thousand'];
          } else {
            return [Math.round(num / 1000000), 'million'];
          }
        }

        function onReferenceSelected(ref, i) {
          d3.select('#reference_selected').text(gettextCatalog.getString('Reference') + ' ' + ref.name);
          d3.select('#region_selected').text('0 - ' + d3.format(',')(ref.value));
          d3.select('#variant-density-panel').select('.hint')
            .text(gettextCatalog.getString('(drag bottom chart to select a region)'));

          loadVariantDensityData(ref);
          loadStats(chromosomeIndex);
        }

        function loadVariantDensityData(ref) {
          d3.select('#variant-density-vf').style('display', 'block');
          d3.select('#variant-density-ref-vf').style('display', 'none');
          d3.selectAll('section#top .svg-alt').style('visibility', 'visible');

          // Get the point data (the estimated density)
          var data = vcfiobio.getEstimatedDensity(ref.name,
            false, densityOptions.removeSpikes, densityOptions.maxPoints, densityOptions.epsilonRDP);


          // Calculate the width and height of the panel as it may have changed since initialization
          getChartDimensions();
          variantDensityChart.width(densityPanelDimensions.width);
          variantDensityChart.height(densityPanelDimensions.height / 2);
          variantDensityVF.width(densityPanelDimensions.width);

          // Load the variant density chart with the data
          variantDensityChart.showXAxis(true);
          variantDensityChart(d3.select('#variant-density').datum(data), onVariantDensityChartRendered);
          variantDensityVF(d3.select('#variant-density-vf').datum(data), onVariantDensityVFChartRendered);

          // Listen for the brush event.  This will select a subsection of the x-axis on the variant
          // density chart, allowing the user to zoom in to a particular region to sample that specific
          // region rather than the entire chromosome.
          variantDensityVF.on('d3brush', function(brush) {
            if (!brush.empty()) {

              // These are the region coordinates
              regionStart = d3.round(brush.extent()[0]);
              regionEnd = d3.round(brush.extent()[1]);

              d3.select('#region_selected')
                .text(d3.format(',')(regionStart) + ' - ' + d3.format(',')(regionEnd));

              // Get the estimated density for the reference (already in memory)
              var data = vcfiobio.getEstimatedDensity(ref.name,
                false, densityRegionOptions.removeSpikes, null, densityRegionOptions.epsilonRDP);

              // Now filter the estimated density data to only include the points that fall within the selected
              // region
              var filteredData = data.filter(function(d) {
                return (d[0] >= regionStart && d[0] <= regionEnd);
              });

              // Now let's aggregate to show in 900 px space
              var factor = d3.round(filteredData.length / 900);
              filteredData = 
                vcfiobio.reducePoints(filteredData, factor, function(d) { return d[0] }, function(d) { return d[1] });

              // Show the variant density for the selected region
              variantDensityChart(d3.select('#variant-density').datum(filteredData), onVariantDensityChartRendered);

              // Load the stats based on the selected region
              loadStats(chromosomeIndex);
            }
          });

          // Listen for finished event which is dispatched after line is drawn.  If chart has
          // transitions, event is dispatched after transitions have occurred.
          variantDensityChart.on('d3rendered', onVariantDensityChartRendered);
        }

        scope.increaseSampling = function() {
          if (statsOptions.samplingMultiplier >= samplingMultiplierLimit) {
            window.alert(gettextCatalog.getString('You have reached the sampling limit'));
            return;
          }
          statsOptions.samplingMultiplier += 1;
          loadStats(chromosomeIndex);
        };
        
        scope.$on('$destroy', function() {
          scope.closing = true;
          if (vcfiobio.sampleClient !== undefined) {
            vcfiobio.sampleClient.close(1000);
          }
        });
        
        scope.cancel = function() {
          scope.$parent.cancel();
        };

        // GO!
        init();
      }
    };
  });

})(jQuery);