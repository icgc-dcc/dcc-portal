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

  var defaultConfig = {
      margin: {top:10,left:30,right:20,bottom:20},
      height: 500,
      width: 1000,
      colours: ['red','blue','green'],
      yaxis: {
        label:'Count of Things',
        ticks: 5
      },
      xaxis: {
        label:'Release',
        ticksValueRange: [0,10],
        secondaryLabel:{}
      },
      onClick: {},
      tooltipShowFunc: {},
      tooltipHideFunc: {},
      graphTitle: ['Aggregated Data','Indivudal Data'],
      offset: 'zero'
    };

  window.dcc = window.dcc || {};

  var StackedAreaChart = function(data, config){
    //clear everything before we start
    d3.select(this.element).selectAll('*').remove();

    this.data = data;
    this.config = config || defaultConfig;
    this.selectedView = 'area';
    this.margin = config.margin;

    //Adjust for margin to account for tick padding
    this.margin.left = this.margin.left +20;

    this.width = config.width - this.margin.left - this.margin.right;
    this.height = config.height - this.margin.top - this.margin.bottom;

    this.x = d3.scale.linear()
    .range([0, this.width]).domain(config.xaxis.ticksValueRange);
    this.xReverser = d3.scale.linear()
    .domain([0, this.width]).range(config.xaxis.ticksValueRange);

    this.y = d3.scale.linear()
    .range([this.height, 0]);

    this.colour = d3.scale.ordinal()
    .domain(d3.keys(config.colours))
    .range(d3.values(config.colours));
  };

  StackedAreaChart.prototype.render = function(element){
    var data = this.data;
    var config = this.config;
    var margin = this.margin, width = this.width, height = this.height, colour = this.colour;

    // FIXME: Need to move append so it doesn't pollute DOM
    d3.select(element).selectAll('*').remove();

    var xAxis = d3.svg.axis()
      .scale(this.x)
      .orient('bottom')
      .innerTickSize(-height)
      .ticks(config.xaxis.ticksValueRange[1]-config.xaxis.ticksValueRange[0])
      .tickPadding(10);

    var xAxisLabels = d3.svg.axis()
      .scale(this.x)
      .orient('bottom')
      .innerTickSize(0)
      .outerTickSize(0)
      .ticks(config.xaxis.ticksValueRange[1]-config.xaxis.ticksValueRange[0])
      .tickFormat(function(d){
        return config.xaxis.secondaryLabel(d);
      })
      .tickPadding(10);

    var yAxis = d3.svg.axis()
      .scale(this.y)
      .orient('left')
      .innerTickSize(-width)
      .ticks(config.yaxis.ticks)
      .tickFormat(d3.format('.2s'))
      .tickPadding(10);

    var graphTitle = d3.select(element).append('div')
      .style('margin-bottom','30px')
      .attr('class','graph_title')
      .attr('y',0)
      .attr('x',width/2)
      .attr('dy', '1em')
      .text(config.graphTitles[0]);

    var stack = d3.layout.stack()
      .offset(config.offset)
      .values(function(d) { return d.values })
      .x(function(d) { return d.index })
      .y(function(d) { return d.value });

    var nest = d3.nest()
      .key(function(d) { return d.group });

    var x = this.x, y=this.y;
    var area = d3.svg.area()
      .interpolate('linear')
      .x(function(d) { return x(d.index) })
      .y0(function(d) { return y(d.y0) })
      .y1(function(d) { return y(d.y0 + d.y) });

    var input = ['Area','Line'];

    var form = d3.select(element).append('form');

    var svg = d3.select(element).append('svg')
      .attr('viewBox','0 0 '+(width + margin.left + margin.right)+
            ' '+(height + margin.top + margin.bottom))
      .attr('preserveAspectRatio','xMidYMid')
      .attr('id','stackedareasvg')
      .append('g')
      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    var layers = stack(nest.entries(data));

    this.y.domain([0, d3.max(data, function(d) { return d.y+d.y0 })]);

    var line = d3.svg.line()
      .x(function(d) { return x(d.index) })
      .y(function(d) { return y(d.value) });

    var project = svg.selectAll('.layer-project')
      .data(layers)
      .enter().append('g')
      .attr('class', 'layer-project');

    var hintLine = svg.selectAll('.stackedareahint')
      .data([width*2]).enter()
      .append('line')
      .style('pointer-events','none')
      .style('fill','none')
      .attr({
        'class':'stackedareahint',
        'x1' : function(d){ return d},
        'x2' : function(d){ return d},
        'y1' : 0,
        'y2' : height,
        'shape-rendering' : 'crispEdges',
        'stroke' : 'grey',
        'stroke-width' : '1px',
        'pointer-events':'none'
      });

    var gridBlockWidth = width/(config.xaxis.ticksValueRange[1]-config.xaxis.ticksValueRange[0]);
    var hintHighlighter = svg.selectAll('rect').data([0]).enter()
     .append('rect')
     .style('opacity','0.33')
     .attr('class','stackedareahinthighlight')
     .style('fill','lightgrey')
     .attr({
        'x' : width*2,
        'y' : height,
        'width' : gridBlockWidth,
        'height' :3*margin.bottom/4,
        'stroke' : 'grey',
        'stroke-width' : '1px',
        'z-index':-10
      });
    var xReverser = this.xReverser;
    project.append('path')
      .attr('d', function(d) { return area(d.values) })
      .attr('stroke', '#FFF')
      .attr('stroke-width', '0.5px')
      .style('fill', function(d) { return colour(d.values[0].colourKey) })
      .style('sharp-rengering','crispEdges')
      .on('mousemove', function(d) {
            var coords = d3.mouse(this);
            var indexOffset = config.xaxis.ticksValueRange[0];
            var index = Math.round(xReverser(coords[0])) - indexOffset;
            var actualIndex = index + indexOffset;

            // Edge Case: where you are at the right most edge of the x-axis
            // get the last value count for the project. Rather than assuming
            // it exists prior to the release as the above formula does.
            if (actualIndex === config.xaxis.ticksValueRange[1]) {
              index = d.values.length - 1;
            }

            config.tooltipShowFunc(this,d.key,d.values[index].value, actualIndex);
            hintLine.transition().duration(80).attr('x1',x(actualIndex)).attr('x2',x(actualIndex));
            hintHighlighter.transition().duration(80).attr('x',x(actualIndex)-gridBlockWidth/2);
          })
      .on('mouseout', function() {
            config.tooltipHideFunc();
            hintLine.transition().duration(400).attr('x1',width*2).attr('x2',width*2);
            project.selectAll('path').transition().duration(100).style('opacity','1');
            hintHighlighter.transition().duration(400).attr('x',width*2);
          })
      .on('click',function(d){
            config.onClick(d.key);
          })
      .on('mouseover', function(data){
            project.selectAll('path')
                .transition().duration(100).style('opacity',function(d){return d.key === data.key?'1':'0.1'});
          });

    svg.append('g')
      .attr('class', 'stackedarea x axis')
      .attr('transform', 'translate(0,' + height + ')')
      .call(xAxis);

    svg.append('g')
      .attr('class', 'stackedarea x axis labels')
      .attr('transform', 'translate(0,' + (height+15)+ ')')
      .call(xAxisLabels)
      .style('font-size','10')
      .style('fill','grey');

    svg.append('g')
      .attr('class', 'stackedarea y axis')
      .call(yAxis);

    svg.select('.stackedarea.y.axis')
      .style('font-size','12')
      .style('fill','grey')
      .append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left)
      .attr('x',-height / 2 + margin.top)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .text(config.yaxis.label);

    svg.select('.stackedarea.x.axis')
      .style('font-size','12')
      .style('fill','grey')
      .append('text')
      .attr('y',3*margin.bottom/4)
      .attr('x',width/2)
      .attr('dy', '1em')
      .style('text-anchor', 'middle')
      .text(config.xaxis.label);

    var change = function (view){
      if(view === 'Line'){
        graphTitle.text(config.graphTitles[1]);
        y.domain([0, d3.max(data, function(d) { return d.value })]);
        svg.select('.stackedarea.y.axis').transition().duration(500).call(yAxis);
        project.selectAll('path').transition().duration(500)
          .attr('d', function(d){return line(d.values)})
          .style('fill','none')
          .attr('stroke', function(d) {return colour(d.values[0].colourKey) })
          .attr('class','line')
          .attr('stroke-width','3px');

      }else if(view ==='Area'){
        graphTitle.text(config.graphTitles[0]);
        y.domain([0, d3.max(data, function(d) { return d.y+d.y0 })]);
        svg.select('.stackedarea.y.axis').transition().duration(500).call(yAxis);
        project.selectAll('path').transition().duration(500)
          .attr('d', function(d) { return area(d.values) }).transition()
          .style('fill', function(d) {return colour(d.values[0].colourKey) })
          .attr('stroke', '#FFF')
          .attr('class','')
          .attr('stroke-width','0.5px');
      }
    };

    form.selectAll('label')
      .data(input).enter()
      .append('label')
      .text(function(d) {return d})
      .style('margin-left','15px')
      .insert('input')
      .style('margin','5px')
      .attr({
        type: 'radio',
        class: 'shape',
        name: 'mode',
        value: function(d, i) {return i}
      })
      .on('change',function(e){
          change(e);
        })
      .property('checked', function(d, i) {return i===0});
  };

  StackedAreaChart.prototype.destroy = function(){
    this.data = null;
    d3.select(this.element).selectAll('*').remove();
  };

  dcc.StackedAreaChart = StackedAreaChart;

})();
