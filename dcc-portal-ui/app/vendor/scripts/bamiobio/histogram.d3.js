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

function histogramD3() {
  var margin = {top: 30, right: 20, bottom: 20, left: 50},
      width = 200,
      height = 100,
      defaults = {outliers:true,averageLine:true},
      xValue = function(d) { return d[0]; },
      yValue = function(d) { return d[1]; },
      x = d3.scale.linear(),
      y = d3.scale.linear(),
      xAxis = d3.svg.axis().scale(x).orient("bottom"),
      yAxis = d3.svg.axis().scale(y).orient("left").ticks(6),
      brush = d3.svg.brush().x(x);
  
  if (d3.select('#iobio-tooltip')[0][0] !== null) {
    var div = d3.select('#iobio-tooltip');
  } else {
    var div = d3.select('body')
      .append("div")
      .attr('id', 'iobio-tooltip')
      .style('left', '0px')
      .style('top', '0px')
      .style('opacity', 0);
  }

  function chart(selection, options) {
    // merge options and defaults
    options = $.extend(defaults,options);
    var innerHeight = height - margin.top - margin.bottom;
    
    selection.each(function(data) {
       // set svg element
       var svg = d3.select(this);

      // Convert data to standard representation greedily;
      // this is needed for nondeterministic accessors.
      data = data.map(function(d, i) {return [xValue.call(data, d, i), yValue.call(data, d, i)];});
      
      // Remove outliers if wanted.
      if ( !options.outliers )
         data = removeOutliers(data);
         
      // Calculate average.
      var avg = [];
      if (options.averageLine) {
         var totalValue = 0, numValues = 0;
         for (var i=0, len = data.length; i < len; i++) { totalValue += data[i][0]*data[i][1]; numValues += data[i][1]; }
         avg = [totalValue / numValues];
      }

      // Update the x-scale.
      x  .domain(d3.extent(data, function(d) { return d[0]; }));
      x  .range([0, width - margin.left - margin.right]);
      
      // Check for single value x axis.
      if (x.domain()[0] == x.domain()[1]) { var v = x.domain()[0]; x.domain([v-5,v+5]);}

      // Update the y-scale.
      y  .domain([0, d3.max(data, function(d) { return d[1]; })])
      y  .range([innerHeight , 0]);

      // Select the g element, if it exists.
      var g = svg.selectAll("g").data([0]);

      // Otherwise, create the skeletal chart.
      var gEnter = g.enter().append("g");
      gEnter.append("g").attr("class", "x axis").attr("transform", "translate(0," + y.range()[0] + ")");
      gEnter.append("g").attr("class", "y axis");
      gEnter.append("g").attr("class", "x brush")      

      // Update the outer dimensions.
      svg .attr("width", width)
          .attr("height", height);

      // Update the inner dimensions.
      g.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
      
      // Add avg line and text
      var half = x(x.domain()[0]+1) / 2;
      var avgLineG = gEnter.selectAll(".avg")
                .data(avg)
             .enter().append("g")
                .attr("class", "avg")
                .style("z-index", 100)
                .attr("transform", function(d) { return "translate(" + parseInt(x(d)+half) + "," + 0 + ")"; });
      
          avgLineG.append("line")
             .attr("x1", 0)
             .attr("x2", 0)
             .attr("y1", innerHeight)
             .attr("y2", -8);
      
          avgLineG.append("text")
                .text("avg")
                .attr("y", "-10");         
      

      // Add new bars groups.
      var bar = g.selectAll(".bar").data(data)
      var barEnter = bar.enter().append("g")
            .attr("class", "bar")
            .attr("transform", function(d) { return "translate(" + x(d[0]) + "," + innerHeight + ")"; });      
      
      //  Add new bars.
      barEnter.append("rect")
         .attr("x", 1)
         .style("z-index", 5)
         .attr("width", Math.max(x(x.domain()[0]+1),1))
         .attr("height", 0)
         .on("mouseover", function(d) {  
            div.transition()        
               .duration(200)      
               .style("opacity", .9);      
            div.html(d[0] + ", " + d[1])                                 
         .style("left", (d3.event.pageX) + "px") 
         .style("text-align", 'left')    
         .style("top", (d3.event.pageY - 24) + "px");    
         })                  
         .on("mouseout", function(d) {       
            div.transition()        
               .duration(500)      
               .style("opacity", 0);   
         });
         
      // Remove extra bar groups.
      bar.exit().remove();
               
      // Update bars groups.
      bar.transition()
         .duration(200)
         .attr("transform", function(d) { 
            return "translate(" + parseInt(x(d[0])) + "," + Math.floor(y(d[1])) + ")"; 
         });

      // Update bars.
      bar.select("rect").transition()
         .duration(200)
         .attr("width", Math.max(Math.ceil(x(x.domain()[0]+1)),1))
         .attr("height", function(d) { 
            return parseInt(d[0]) >= x.domain()[0] ? innerHeight - parseInt(y(d[1])) : 0; 
         });

      // Update the x-axis.
      g.select(".x.axis").transition()
          .duration(200)
          .call(xAxis);
          
      // Update the y-axis.
      g.select(".y.axis").transition()
         .duration(200)
         .call(yAxis);
         
      // Update avg line and text
      svg.selectAll(".avg").transition()
         .duration(200)
         .attr("transform", function(d) { return "translate(" + parseInt(x(d)+half) + "," + 0 + ")"; })
         .call(moveToFront);
            
      // Update brush if event has been set.
      if( brush.on("brushend") || brush.on("brushstart") || brush.on("brush")) {
         g.select(".x.brush").call(brush).call(moveToFront)
             .selectAll("rect")
               .attr("y", -6)
               .attr("height", innerHeight + 6);      
      }
      
    });
    // moves selection to front of svg
    function moveToFront(selection) {
      return selection.each(function(){
         this.parentNode.appendChild(this);
      });
    }
    
   function removeOutliers(data) {
      var q1 = quantile(data, 0.25); 
      var q3 = quantile(data, 0.75);
      var iqr = (q3-q1) * 1.5; //
      return data.filter(function(d) { return (d[0]>=(Math.max(q1-iqr,0)) && d[0]<=(q3+iqr)) });
   }
    
   function quantile(arr, p) {
      var length = arr.reduce(function(previousValue, currentValue, index, array){
         return previousValue + currentValue[1];
      }, 0) - 1;
      var H = length * p + 1, 
      h = Math.floor(H);

      var hValue, hMinus1Value, currValue = 0;
      for (var i=0; i < arr.length; i++) {
         currValue += arr[i][1];
         if (hMinus1Value == undefined && currValue >= (h-1))
            hMinus1Value = arr[i][0];
         if (hValue == undefined && currValue >= h) {
            hValue = arr[i][0];
            break;
         }
      } 
      var v = +hMinus1Value, e = H - h;
      return e ? v + e * (hValue - v) : v;
   } 
    
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
  
  chart.brush = function(_) {
    if (!arguments.length) return brush;
    brush = _;
    return chart; 
  };

  return chart;
}