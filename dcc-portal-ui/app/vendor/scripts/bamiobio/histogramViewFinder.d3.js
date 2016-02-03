/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Based on work from iobio: https://github.com/iobio
 *
 * This file incorporates work covered by the following copyright and permission notice:
 *
 *    The MIT License (MIT)
 *
 *    Copyright (c) <2014>
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *    associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *    and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 *    subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included
 *    in all copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
 */

function histogramViewFinderD3() {
  var margin = {top: 25, right: 20, bottom: 20, left: 50},
      width = 200,
      height = 100,
      focalHeightRatio = 0.83,
      globalHeightRatio = 0.12,
      xValue = function(d) { return d[0]; },
      yValue = function(d) { return d[1]; },
      x = d3.scale.linear(),
      y = d3.scale.linear(),
      xAxis = d3.svg.axis().scale(x).orient("bottom"),
      yAxis = d3.svg.axis().scale(y).orient("left"),
      focalChart = histogramD3().margin({top:0, right:0, bottom:20, left:0}),
      globalChart = histogramD3().margin({top:5, right:0, bottom:5, left:0}).yAxis(function(){}); // empty function to remove y axis
      var globalChartOptions = {averageLine:false};
      
      yAxis = focalChart.yAxis();
      xAxis = focalChart.xAxis();
      globalChart.xAxis(xAxis);
      
  function chart(selection, options) {
     var innerHeight = height - margin.top - margin.bottom;
     var innerWidth = width - margin.left - margin.right;
     $.extend(globalChartOptions, options);
     selection.each(function(data) {      
        // Select the g element, if it exists.
        var g = selection.selectAll("g").data([0]);

        // Otherwise, create the g element
        var gEnter = g.enter().append("g");
        
        // Update the margin dimensions
        g.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        // // Remove outliers
        // if ( !options.outliers )
        //  data = removeOutliers(data);
        
        // Select the focal panel if it exits
        var gFocus = g.selectAll("g.focal-panel").data([data])
        
        // Otherwise create it
        gFocus.enter().append("g").attr("class", "focal-panel");
        
        // Update focal chart dimensions
        focalChart.height(innerHeight*focalHeightRatio).width(innerWidth)
        
        // Call focal panel chart
        focalChart(gFocus, options);
        
        // Select the global panel if it exits
        var gGlobal = g.selectAll("g.global-panel").data([data])
        
        // Otherwise create it
        gGlobal.enter()
           .append("g").attr("class", "global-panel").attr("transform", "translate(0," + parseInt(innerHeight-(innerHeight*globalHeightRatio)-8) + ")")
           .append("text")
              .attr("dy", innerHeight*globalHeightRatio + 25)
              .attr("dx", innerWidth/2 )
              .attr("text-anchor", 'middle')
              .attr("class", "instruction")
              .text("(drag to select region)");
        
        // Update global chart dimensions
        globalChart.height(innerHeight*globalHeightRatio).width(innerWidth)
        
        // Call global panel chart
        globalChart(gGlobal,globalChartOptions);
        
        // Setup brush for globalChart
        globalChart.brush().on("brush", function() { 
           var x2 = globalChart.x(), brush = globalChart.brush();
           var x = brush.empty() ? x2.domain() : brush.extent();
           var datum = gGlobal.datum().filter(function(d) { return (d[0] >= x[0] && d[0] <= x[1]) });
           if (_.isEmpty(datum)) {
             datum.push([x[0], 0]);
           }
           focalChart(gFocus.datum(datum));
        });             
     })
  }

  chart.margin = function(_) {
    if (!arguments.length) return margin;
    margin = _;
    return chart;
  };

  chart.width = function(_) {
    if (!arguments.length) return width;
    width = _;
    return chart;
  };

  chart.height = function(_) {
    if (!arguments.length) return height;
    height = _;
    return chart;
  };
  
  chart.xValue = function(_) {
    if (!arguments.length) return xValue;
    xValue = _;
    return chart;
  };

  chart.yValue = function(_) {
    if (!arguments.length) return yValue;
    yValue = _;
    return chart;
  };

  chart.x = function(_) {
    if (!arguments.length) return x;
    x = _;
    return chart;
  };

  chart.y = function(_) {
    if (!arguments.length) return y;
    y = _;
    return chart;
  };
  
  chart.xAxis = function(_) {
    if (!arguments.length) return xAxis;
    xAxis = _;
    return chart; 
  };

  chart.yAxis = function(_) {
    if (!arguments.length) return yAxis;
    yAxis = _;
    return chart; 
  };  
  
  chart.setBrush = function(range) {
     var brush = globalChart.brush();
     // set brush region
     d3.select(".global-panel .brush").call(brush.extent(range));
     // trigger brush event
     brush.on('brush')();         
  }
  
  chart.focalChart = function(_) {
    if (!arguments.length) return focalChart;
    focalChart = _;
    return chart; 
  };
  
  chart.globalChart = function(_) {
    if (!arguments.length) return globalChart;
    globalChart = _;
    return chart; 
  };  

  return chart;
}