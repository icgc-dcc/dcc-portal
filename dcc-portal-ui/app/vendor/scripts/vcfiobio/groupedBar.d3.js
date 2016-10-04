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

var groupedBarD3 = function module() {

  var margin = { left: 30, right: 30, top: 10, bottom: 30 };

  var width = 600 - margin.left - margin.right;
  var height = 220 - margin.top - margin.bottom;
  var widthPercent = "95%";
  var heightPercent = "95%";

  var categoryPadding = .4;

  // default colors
  var colorScale = d3.scale.category20();

  var showXAxis = true;
  var showXTicks = true;
  var xAxisLabel = null;

  var showYAxis = true;
  var yAxisTickLabel = null;
  var yAxisLabel = null;

  var showTooltip = true;
  var showBarLabel = true;
  var categories = null;

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

  /*
  * The default function for getting the category from the data.
  * Category is the field that the bars are grouped by.
  */
  function category(d) {
    return d.category;
  }

  /*
  * The default function for getting the name from the data.
  * Name is the field that represents the individual bar.
  */
  function name(d) {
    return d.name;
  }

  /*
  * The default function for getting the value from the data.
  * Value is the field that represents the height of the
  * individual bar.
  */
  function value(d) {
    return d.value;
  }


  /*
  * The default function for filling the individual
  * bar.  We use the category to map 
  * to a particular color in a color scale.
  */
  function fill(d, i) {
    return colorScale(name(d));
  }

  /*
  * The default function for the category label.  We
  * use the name (associated with the value).
  */
  function barLabel(d, i) {
    return d.name;
  }


  /*
  *  The default function for creating a scale for the x-axis.
  */
  function scale(x) {
    return range[((index.get(x) || (ranger.t === "range" ? index.set(x, domain.push(x)) : NaN)) - 1) % range.length];
  }



  /*
  *  The main function to render a grouped bar chart.
  *  It takes one argument, the d3 selected parent(s),
  *  primed with the data.  For each parent (typically
  *  just one), the function will create an SVG object,
  *  a grouped bar chart.  This function should be callled
  *  each time the data changes. Subsequent calls after
  *  the first call will remove and recreate the axis as well 
  *  as the individual bars.
  */
  function exports(selection) {


    selection.each(function(data) {

      // Make sure we have categories
      if (!categories) {
        console.log("ERROR - cannot create groupedBarD3 because categories were not provided.")
        return;
      }


      // Add a property to the JSON object called "values" that contains an array of objects 
      // example value: [{name: cat1, value: 50}, {name: cat2, value: 33}]
      // Exclude the value that has 0 as this is the base that is the category. (e.g. 
      // exclude A value for A: )
      data.forEach(function(d) {
        d.values = categories.map(function(catName, i) {
          var valueObj = { category: category(d), name: catName, value: +d.values[i] };
          if (isNaN(valueObj.value)) {
            valueObj.value = 0;
          }
          return valueObj;
        });
      });



      var x0 = d3.scale.ordinal()
        .rangeRoundBands([0, width], categoryPadding);

      var x1 = d3.scale.ordinal();

      var y = d3.scale.linear()
        .range([height, 0]);

      if (showXAxis) {
        var xAxis = d3.svg.axis()
          .scale(x0)
          .orient("bottom");
        if (!showXTicks) {
          xAxis.tickSize(0);
        }
      }

      if (showYAxis) {
        var yAxis = d3.svg.axis()
          .scale(y)
          .orient("left");
        //.tickFormat(d3.format(".2s"));
      }


      x0.domain(data.map(function(d) { return category(d); }));
      x1.domain(categories).rangeRoundBands([0, x0.rangeBand()]);
      y.domain([0, d3.max(data, function(d) { return d3.max(d.values, function(d1) { return value(d1); }); })]);


      var svg = d3.select(this)
        .selectAll("svg")
        .data([data]);

      svg.enter()
        .append("svg")
        .attr("width", widthPercent)
        .attr("height", heightPercent)
        .attr('viewBox', "0 0 " + parseInt(width + margin.left + margin.right) + " " + parseInt(height + margin.top + margin.bottom))
        .attr("preserveAspectRatio", "xMidYMid meet");

      var defs = svg.selectAll("defs").data([data]).enter()
        .append("defs");

      var svgGroup = svg.selectAll("g.group").data([data]).enter()
        .append("g")
        .attr("class", "group")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

      if (showXAxis) {
        svgGroup = svg.selectAll("g.group");
        svgGroup.selectAll("g.x").remove();
        svgGroup.selectAll("g.x").data([data]).enter()
          .append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(0," + height + ")")
          .call(xAxis);

        // Add the text label for the x axis
        if (xAxisLabel) {
          svgGroup = svg.selectAll("g.group");
          svgGroup.selectAll("text.x.axis.label").remove();
          svgGroup.append("text")
            .attr("class", "x axis label")
            .attr("transform", "translate(" + (width / 2) + " ," + (height + margin.bottom) + ")")
            .style("text-anchor", "middle")
            .text(xAxisLabel);
        }

      }

      if (showYAxis) {
        svgGroup.selectAll("g.y").remove();
        svgGroup.selectAll("g.y").data([data]).enter()
          .append("g")
          .attr("class", "y axis")
          .call(yAxis);
        if (yAxisTickLabel) {
          svgGroup = svg.selectAll("g.group");
          svgGroup.selectAll("text.y.axis.label").remove();
          svgGroup.append("text")
            .attr("class", "y axis label")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .attr("font-size", "10px")
            .text(yAxisLabel);

        }

        // Add the text label for the Y axis
        if (yAxisLabel) {
          svgGroup.selectAll("g.y axis label").remove();
          svgGroup.append("text")
            .attr("class", "y axis label")
            .attr("transform", "rotate(-90)")
            .attr("y", 0 - margin.left)
            .attr("x", 0 - (height / 2))
            .attr("dy", "1em")
            .style("text-anchor", "middle")
            .text(yAxisLabel);

        }
      }

      svgGroup = svg.selectAll("g.group")
      svgGroup.selectAll("g.category").remove();
      var barGroup = svgGroup.selectAll(".category")
        .data(data)
        .enter().append("g")
        .attr("class", "category")
        .attr("transform", function(d) { return "translate(" + x0(category(d)) + ",0)"; });

      var bars = barGroup.selectAll("rect")
        .data(function(d) { return d.values; });


      bars.enter()
        .append("rect")
        .attr("width", x1.rangeBand())
        .attr("x", function(d) { return x1(name(d)); })
        .attr("y", function(d) { return y(value(d)); })
        .attr("height", function(d) { return height - y(value(d)); })
        .style("fill", fill)
        .on("mouseover", function(d) {
          if (showTooltip) {
            div.transition()
              .duration(200)
              .style("opacity", .9);
            div.html(d3.round(value(d)))
              .style("left", (d3.event.pageX) + "px")
              .style("text-align", 'left')
              .style("top", (d3.event.pageY - 24) + "px");
          }
        })
        .on("mouseout", function(d) {
          if (showTooltip) {
            div.transition()
              .duration(500)
              .style("opacity", 0);
          }
        });

      bars.exit().remove();

      if (showBarLabel) {
        barGroup.selectAll("text")
          .data(function(d) { return d.values; })
          .enter()
          .append("text").text(barLabel)
          .attr("x", function(d) { return x1(name(d)) + (x1.rangeBand() / 2); })
          .attr("y", function(d) {
            return y(value(d)) - 5;
          })
          .attr("text-anchor", "middle")
      }



    });  // end for loop on selection
  }



  /*
  *
  *  All functions in this section allow the grouped bar chart widget to be
  *  customized, using the "chained" approach, a "shorthand" style for
  *  calling functions, one after another.
  *
  */
  exports.showTooltip = function(_) {
    if (!arguments.length) return showTooltip;
    showTooltip = _;
    return exports;
  };

  exports.tooltipSelector = function(_) {
    if (!arguments.length) return tooltipSelector;
    tooltipSelector = _;
    return exports;
  };

  exports.margin = function(_) {
    if (!arguments.length) return margin;
    margin = _;
    return exports;
  };

  exports.width = function(_) {
    if (!arguments.length) return width;
    width = _;
    return exports;
  };

  exports.height = function(_) {
    if (!arguments.length) return height;
    height = _;
    return exports;
  };

  exports.widthPercent = function(_) {
    if (!arguments.length) return widthPercent;
    widthPercent = _;
    return exports;
  };

  exports.heightPercent = function(_) {
    if (!arguments.length) return heightPercent;
    heightPercent = _;
    return exports;
  };


  exports.category = function(_) {
    if (!arguments.length) return category;
    category = _;
    return exports;
  }

  exports.nameFunction = function(_) {
    if (!arguments.length) return name;
    name = _;
    return exports;
  }

  exports.values = function(_) {
    if (!arguments.length) return value;
    value = _;
    return exports;
  }

  exports.categories = function(_) {
    if (!arguments.length) return categories;
    categories = _;
    return exports;
  }

  exports.fill = function(_) {
    if (!arguments.length) return fill;
    fill = _;
    return exports;
  }

  exports.colorList = function(_) {
    if (!arguments.length) return colorList;
    colorList = _;
    colorScale = d3.scale.ordinal().range(colorList);
    return exports;
  }

  exports.colorScale = function(_) {
    if (!arguments.length) return colorScale;
    colorScale = _;
    return exports;
  }

  exports.showBarLabel = function(_) {
    if (!arguments.length) return showBarLabel;
    showBarLabel = _;
    return exports;
  };

  exports.barLabel = function(_) {
    if (!arguments.length) return barLabel;
    barLabel = _;
    return exports;
  }

  exports.showXAxis = function(_) {
    if (!arguments.length) return showXAxis;
    showXAxis = _;
    return exports;
  }

  exports.showYAxis = function(_) {
    if (!arguments.length) return showYAxis;
    showYAxis = _;
    return exports;
  }

  exports.showXTicks = function(_) {
    if (!arguments.length) return showXTicks;
    showXTicks = _;
    return exports;
  }

  exports.yAxisTickLabel = function(_) {
    if (!arguments.length) return yAxisTickLabel;
    yAxisTickLabel = _;
    return exports;
  }

  exports.categoryPadding = function(_) {
    if (!arguments.length) return categoryPadding;
    categoryPadding = _;
    return exports;
  }

  exports.xAxisLabel = function(_) {
    if (!arguments.length) return xAxisLabel;
    xAxisLabel = _;
    return exports;
  }

  exports.yAxisLabel = function(_) {
    if (!arguments.length) return yAxisLabel;
    yAxisLabel = _;
    return exports;
  }




  return exports;
}