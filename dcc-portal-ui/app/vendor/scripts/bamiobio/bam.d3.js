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
function bamD3(container, heightPct, color) {
   var margin = {top: 5, right: 30, bottom: 20, left: 30},
          width = $(container).width()*0.98 - margin.left - margin.right,
          height = $(container).height()*heightPct - margin.top - margin.bottom;
   var formatCount = d3.format(",.0f");
   
   var duration = 1000;
   
   var x = d3.scale.linear()
       .range([0, width]);
   var svg = d3.select(container).append("svg")
      .attr("width", '98%')
      .attr("height", parseInt(heightPct*100) + '%')
      .attr('viewBox',"0 0 " + parseInt(width+margin.left+margin.right) + " " + parseInt(height+margin.top+margin.bottom))
      .attr("preserveAspectRatio", "none")
      .append("g")
         .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
   

   function my(values, options) {
      var minMax = d3.extent(values, function(elem) {return elem.position} );
      var avgDepth = d3.mean(values, function(elem) { return elem.length } );
      var numBins = 20;
      if (options != undefined) {
         minMax = options.minMax || minMax;
         numBins = options.numBins || numBins;
      }
      x.domain( minMax );
      if(x.domain()[0] == x.domain()[1])
         x.domain([ x.domain()[0] - 10, x.domain()[1] + 10 ]);
      
      var data = d3.layout.histogram()
          .bins(x.ticks(numBins))
          .value(function(d){return d.position})
          (values);
      
      data.forEach(function(d) {
         d.y = d3.mean(d, function(elem) {return parseInt(elem.length*100)/100});
      })
          
      var y = d3.scale.linear()
          .domain([d3.min(data, function(d) { return d.y -2; }), d3.max(data, function(d) { return d.y+2; })])          
          .range([height, 0])
          
      var lineFunction = d3.svg.line()
         .x(function(d) { return x(d.x) + x(x.domain()[0] + data[0].dx)/2; })
         .y(function(d) { return y(d.y) })
         .interpolate("linear");
                  
                   
      var xAxis = d3.svg.axis()
         .scale(x)
         .tickFormat(function (d) {
            if ((d / 1000000) >= 1)
              d = d / 1000000 + "M";
            else if ((d / 1000) >= 1)
              d = d / 1000 + "K";
            
            return d;            
         })
         .orient("bottom");
         
      var yAxis = d3.svg.axis()
         .scale(y)
         .tickFormat(function(d) { 
            if (parseInt(d) == d)
               return (d + "X");
         })
         .orient("left");
      
      // handle new data
      var dot = svg.selectAll(".dot")
          .data(data);
          
       if (svg.select("path").empty()) {
          svg.append("path")
             .attr("d", lineFunction(data))
             .attr("stroke", color)
             .attr("stroke-width", 2)
             .attr("fill", "none");
       } else {
          svg.select("path").transition()
             .duration(duration)
             .attr("d", lineFunction(data))
       }
       
       if (svg.select(".avgdepth").empty()) {
          svg.append("line")
             .attr('class', 'avgdepth')
             .attr("stroke-dasharray", "5,5")
             .attr('x1', 0)
             .attr('y1', y(0))
             .attr('x2', x(x.domain()[1]))
             .attr('y2', y(0))
             .attr("stroke", '#2687BE')
             .attr("stroke-width", 2)
             .attr("fill", "none");
         
         svg.append("text")
            .attr("id", "refText")
            .attr("dy", ".75em")
            .attr("y", 4)
            .attr("x", 4)
            .style("fill", 'rgb(80,80,80)')
            .style("font-size", "11px")
            .attr("text-anchor", "left")
            .text("Sampled Avg");
            
       } else {
          svg.select(".avgdepth").transition()
             .duration(duration)
             .attr('x1', 0)
             .attr('y1', y(avgDepth))
             .attr('x2', x(x.domain()[1]) )
             .attr('y2', y(avgDepth))
         
         svg.select("#refText").transition()
            .duration(duration)
            .attr("y", y(avgDepth) + 4)
      }
       
       
      
      var dotEnter = dot.enter().append("g")
         .attr("class", "dot")
         .attr("transform", function(d) { return "translate(" + x(d.x) + ",0)"; });

               
      dotEnter.append("circle")
         .attr("cx", x(x.domain()[0] + data[0].dx)/2)
         .attr("cy", y(0))
         .attr("r", 5)
         .attr("fill", 'white')
         .attr("stroke", "white")
         .attr("stroke-width", "2px");
         
      dot.select("circle").transition()
         .duration(duration)
         .attr("cy", function(d) { return y(d.y); })
         .attr("r", function(d) { if (d.y == 0) {return 0;} else { return 5; } })
         .attr("fill", function(d) { if (d.y == 0) {return "white";} else { return color; } });            
               
      dot.exit().remove(); 
      
      
      if (svg.select(".x.axis").empty()) {
         svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .style("font-size", "9px")
            .call(xAxis);
      } else {
         svg.select(".x.axis").transition()
            .duration(duration)
            .call(xAxis);
      }
      
      if (svg.select(".y.axis").empty()) {
         svg.append("g")
            .attr("class", "y axis")
            .style("font-size", "9px")
            .call(yAxis);
      } else {
         svg.select(".y.axis").transition()
            .duration(duration)
            .call(yAxis);
      }
      
      
   }

   my.width = function(value) {
      if (!arguments.length) return width;
      width = value;
      return my;
   };
   return my;
   
   my.height = function(value) {
      if (!arguments.length) return height;
      height = value;
      return my;
   };
   return my;
}

