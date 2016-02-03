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

function movingLineD3(container) {
   var margin = {top: 0, right: 15, bottom: 30, left: 15},
          width = $(container).width()*0.982 - margin.left - margin.right,
          height = $(container).height()*0.60 - margin.top - margin.bottom;

   var numBins = 20;
   
   var x = d3.scale.linear()
       .range([0, width]);
   var brush = d3.svg.brush()
      .x(x);
          
   var svg = d3.select(container).append("svg")
      .attr("width", '98%')
      .attr("height", '100%')
      .attr('viewBox',"0 0 " + parseInt(width+margin.left+margin.right) + " " + parseInt(height+margin.top+margin.bottom))
      .attr("preserveAspectRatio", "none")
      .append("g")
         .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
  
   function my(data, options) {
      
      
      
      var epsilonRate = 0.3;
      var epislon = parseInt( epsilonRate * (data[data.length-1].pos - data[0].pos) / width );
      var points = data.map(function(d) { return [d.pos,d.depth]; });
      data = properRDP(points, epislon);
      // data = points;
      
      x.domain(d3.extent(data, function(d){ return d[0]; }));            
      var y = d3.scale.linear().domain([0, d3.max(data, function(d){ return d[1]; })]).range([height,0]);
      
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
      
      //.style("visibility", "hidden");
      
       // add mouseover
       var formatter = d3.format(',');
       svg.on("mouseover", function() {  
         div.transition()        
                 .duration(200)      
                 .style("opacity", .9);      
         div.html(formatter(parseInt(x.invert(d3.event.pageX - $(this).position().left))))
                 .style("left", (d3.event.pageX) + "px") 
                 .style("text-align", 'left')    
                 .style("top", (d3.event.pageY - 24) + "px");    
             })                  
         .on("mousemove", function() {       
            div.html(formatter(parseInt(x.invert(d3.event.pageX - $(this).offset().left))))
               .style("left", (d3.event.pageX) + "px") 
               .style("top", (d3.event.pageY - 24) + "px");
          })               
         .on("mouseout", function() {       
             div.transition()        
                 .duration(500)      
                 .style("opacity", 0);   
       });   
         
      var line = d3.svg.line()
        .interpolate("linear")
        .x(function(d,i) { return parseInt(x(d[0])); })
        .y(function(d) { return parseInt(y(d[1])); })

   
      svg.select(".read-depth-path").remove();
         var path = svg.append("path")
           .attr('class', "read-depth-path")
           .attr("d", line(data))

         var totalLength = path.node().getTotalLength();

         path
           .attr("stroke-dasharray", totalLength + " " + totalLength)
           .attr("stroke-dashoffset", totalLength)
           .transition()
             .duration(2000)
             .ease("linear")
             .attr("stroke-dashoffset", 0);
         
      // update x axis
      if (svg.select(".x.axis").empty()) {
         svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);
      } else {
         svg.select(".x.axis").transition()
            .duration(200)
            .call(xAxis);
      }

      svg.append("g")
            .attr("class", "x brush")
            .call(brush)
          .selectAll("rect")
            .attr("y", -6)
            .attr("height", height + 6);
      
   }
   
   my.on = function(ev, listener) { 
      if (ev == "brush" || ev == "brushstart" || ev == "brushend")
         brush.on(ev, function() { listener(x,brush); } );
      return my;
   }
   
   my.brush = function(value) {
      if (!arguments.length) return brush;
      brush = value;
      return my;
   };
   
   return my;
}