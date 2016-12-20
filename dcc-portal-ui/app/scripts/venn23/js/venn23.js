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

  window.dcc = window.dcc || {};

  /********************************************************************************
  *
  * Venn diagram for 2 or 3 sets.
  *
  * This version uses SVG clipping areas to achieve the effect of indivisual 'exploded'
  * pieces. The position of the circles are place into SVG-defs and clipped against each
  * other.
  *
  * Use arc diagrams for 4 or more sets, it scales much better and should be much easier
  * to interact with. Also note arc diagrams do not suffer from degenerated cases of using
  * circular geometry.
  *
  * Venn23 rendering consumes a data array of arrays, where each subarray is used to
  * denote specific ownership groups. For exaple:
  *
  *   [ {id:A, count:10}, {id:B, count:10}]
  *
  * indicates that there are 10 elments that exists in BOTH A and B, and
  *
  *   [ {id:B, count:20}]
  *
  * indicates that there are 20 elements that exists only in B
  *
  * So for set A, B the expected data structure will look like
  *
  * [
  *    [ {id:A, count: X}],
  *    [ {id:B, count: Y}],
  *    [ {id:A, count: Z}, {id:B, count:Z} ]
  * ]
  *
  ********************************************************************************/
  var Venn23 = function(data, config) {
    var defaultConfig = {
      width: 500,
      height: 500,
      margin: 5,
      paddingTop: 10,
      paddingBottom: 10,
      paddingLeft: 10,
      paddingRight: 10,
      outlineColour: '#999',
      outlineWidth: 1.5,

      selectColour: '#A4DEF4',
      hoverColour: '#daf2fb',

      urlPath: '',
      mapFunc: function(data) {
        return data;
      },
      clickFunc: function() {
      },
      mouseoverFunc: function() {
      },
      mouseoutFunc: function() {
      },

      // Set label
      setLabelFunc: function(d) {
        return d;
      },

      // Value label
      valueLabelFunc: function(d) {
        return d;
      }

    };


    config = config || {};
    Object.keys(defaultConfig).forEach(function (key) {
      if (! config.hasOwnProperty(key)) {
        config[key] = defaultConfig[key];
      }
    });

    config.visWidth  = config.width - 2.0 * config.margin;
    config.visHeight = config.height - 2.0 * config.margin;
    config.chartWidth  = config.visWidth - (config.paddingLeft + config.paddingRight);
    config.chartHeight = config.visHeight - (config.paddingTop + config.paddingBottom);

    this.data = data;
    this.config = config;
    this.translate = function(x, y) {
      return 'translate(' + x + ',' + y + ')';
    };

    this.getValueBySetIds = function(ids) {
      var val = 0;
      this.data.forEach(function(group) {
        var groupIds = _.pluck(group, 'id');
        if (_.difference(groupIds, ids).length === 0 && _.difference(ids, groupIds).length === 0) {
          val = group[0].count;
        }
      });
      return val;
    };

    this.max = 0;
    for (var i=0; i < this.data.length; i++) {
      if (this.data[i][0].count > this.max) {
        this.max = this.data[i][0].count;
      }
    }

    // Scale function
    this.colours = ['rgb(242,242,242)'];
    this.ramp = d3.scale.linear().domain([0, this.max]).range([0, this.colours.length-1]);
    this.getColourBySetIds = function() {
      return this.colours[0];
    };

    this.svg = undefined;
    this.vis = undefined;
    this.chart = undefined;
    this.getId = _.memoize((id) => _.uniqueId(id));
  };


  Venn23.prototype.render2 = function() {
    var _this = this;
    var config = _this.config;

    // Just to position the set nicely
    var cy = 0.3 * config.chartHeight;
    var cx = 0.5 * config.chartWidth;

    var svg = _this.svg;
    var defs = svg.append('svg:defs');
    var radius = 100;
    var factor = 0.60;
    var uniqueIds = this.getDistinctIds();

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle1-set2'))
      .append('svg:circle')
      .attr('cx', cx - radius * factor)
      .attr('cy', cy)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle2-set2'))
      .append('svg:circle')
      .attr('cx', cx + radius * factor)
      .attr('cy', cy)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle1_out-set2'))
      .append('svg:circle')
      .attr('cx', cx - radius * factor)
      .attr('cy', cy)
      .attr('r', radius+ config.outlineWidth);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle2_out-set2'))
      .append('svg:circle')
      .attr('cx', cx + radius * factor)
      .attr('cy', cy)
      .attr('r', radius+ config.outlineWidth);


    // 1 intersection
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out-set2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out-set2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);

    svg.append('svg:rect')
      .datum({selected: false, data:[uniqueIds[0]]})
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1-set2')})`)
      .attr('class', 'inner')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2-set2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });


    // 2 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out-set2')})`)
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out-set2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1-set2')})`)
      .append('svg:rect')
      .datum({selected: false, data:[uniqueIds[0], uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2-set2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });


    // Label - name
    svg.append('text')
      .attr('x', cx - 2.8*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'end')
      .style('fill', '#333333')
      .append('tspan')
      .html(config.setLabelFunc(uniqueIds[0]))
      .style('alignment-baseline', 'central');

    svg.append('text')
      .attr('x', cx + 2.8*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'start')
      .style('fill', '#333333')
      .append('tspan')
      .html(config.setLabelFunc(uniqueIds[1]))
      .style('alignment-baseline', 'central');


    // Label - value
    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx -  1.1*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'end')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[0]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + 1.1*radius * factor)
      .attr('y', cy)
      .attr('text-anchor', 'start')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[1]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx)
      .attr('y', cy)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1]])));



  };

  Venn23.prototype.render3 = function() {
    var _this = this;
    var config = _this.config;

    // Just to position the set nicely
    var cy = 0.4 * config.chartHeight;
    var cx = 0.5 * config.chartWidth;
    var svg = _this.svg;
    var defs = svg.append('svg:defs');
    var radius = 100;
    var factor = 0.75;
    var uniqueIds = this.getDistinctIds();

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle1'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 300/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 300/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle2'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 60/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 60/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle3'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 180/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 180/180) * radius * factor)
      .attr('r', radius);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle1_out'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 300/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 300/180) * radius * factor)
      .attr('r', radius+ config.outlineWidth);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle2_out'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 60/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 60/180) * radius * factor)
      .attr('r', radius+ config.outlineWidth);

    defs.append('svg:clipPath')
      .attr('id', this.getId('circle3_out'))
      .append('svg:circle')
      .attr('cx', cx + Math.sin(Math.PI * 180/180) * radius * factor)
      .attr('cy', cy - Math.cos(Math.PI * 180/180) * radius * factor)
      .attr('r', radius+ config.outlineWidth);


    // 1 intersection
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);

    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0]]})
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1')})`)
      .attr('class', 'inner')
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });
    svg.append('svg:rect')
      .datum({selected:false, data:[uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });

    // 2 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out')})`)
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1')})`)
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0], uniqueIds[1]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });

    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out')})`)
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2')})`)
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[1], uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });

    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3_out')})`)
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3')})`)
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[2], uniqueIds[0]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });


    // 3 intersections
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3_out')})`)
      .append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2_out')})`)
      .append('svg:rect')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1_out')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', config.outlineColour);
    svg.append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle3')})`)
      .append('svg:g')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle2')})`)
      .append('svg:rect')
      .datum({selected:false, data:[uniqueIds[0], uniqueIds[1], uniqueIds[2]]})
      .attr('class', 'inner')
      .attr('clip-path', 'url(' + config.urlPath + `#${this.getId('circle1')})`)
      .attr('width', config.chartWidth)
      .attr('height', config.chartHeight)
      .style('fill', function() {
        return _this.getColourBySetIds();
      });

    // Label - name
    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 300/180) * 2.5*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 300/180) * 2.5*radius * factor)
      .attr('text-anchor', 'end')
      .style('fill', '#333333')
      .append('tspan')
      .html(config.setLabelFunc(uniqueIds[0]))
      .style('alignment-baseline', 'central');

    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 60/180) * 2.5*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 60/180) * 2.5*radius * factor)
      .attr('text-anchor', 'start')
      .style('fill', '#333333')
      .append('tspan')
      .html(config.setLabelFunc(uniqueIds[1]))
      .style('alignment-baseline', 'central');

    svg.append('text')
      .attr('x', cx + Math.sin(Math.PI * 180/180) * 2.6*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 180/180) * 2.6*radius * factor)
      .attr('text-anchor', 'middle')
      .style('fill', '#333333')
      .append('tspan')
      .html(config.setLabelFunc(uniqueIds[2]))
      .style('alignment-baseline', 'central');


    // Label - value
    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 300/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 300/180) * 1.1*radius * factor)
      .attr('text-anchor', 'end')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[0]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 60/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 60/180) * 1.1*radius * factor)
      .attr('text-anchor', 'start')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[1]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 180/180) * 1.1*radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 180/180) * 1.1*radius * factor)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[2]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 360/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 360/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 120/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 120/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[1], uniqueIds[2]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx + Math.sin(Math.PI * 240/180) * 0.85 * radius * factor)
      .attr('y', cy - Math.cos(Math.PI * 240/180) * 0.85 * radius * factor)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[2], uniqueIds[0]])));

    svg.append('text')
      .classed('venn-count', true)
      .attr('x', cx)
      .attr('y', cy)
      .attr('text-anchor', 'middle')
      .text(config.valueLabelFunc(_this.getValueBySetIds([uniqueIds[0], uniqueIds[1], uniqueIds[2]])));
  };


  Venn23.prototype.getDistinctIds = function() {
    var _this = this;
    var result = [];
    _this.data.forEach(function(subset) {
      subset.forEach(function(item) {
        if (result.indexOf(item.id) === -1) {
          result.push(item.id);
        }
      });
    });
    return result;
  };


  Venn23.prototype.render = function(element) {
    var _this = this;
    var config = _this.config;

    var uniqueIds = this.getDistinctIds();

    _this.svg = d3.select(element).append('svg')
      .attr('viewBox', '0 0 ' + config.width + ' ' + config.height)
      .attr('preserveAspectRatio', 'xMidYMid');

    _this.vis = _this.svg.append('g').attr('transform', _this.translate(config.margin, config.margin));
    _this.chart = _this.vis.append('g').attr('transform', _this.translate(config.paddingLeft, config.paddingTop));

    if (uniqueIds.length === 2) {
      this.render2();
    } else if (uniqueIds.length === 3) {
      this.render3();
    }

    // Add interactions
    _this.svg.selectAll('.inner')
      .on('mouseover', function(d) {
        d3.select(this).style('fill', config.hoverColour);
        config.mouseoverFunc(d);
      })
      .on('mouseout', function(d) {
        d3.select(this).style('fill', d.selected? config.selectColour : _this.getColourBySetIds());
        config.mouseoutFunc(d);
      })
      .on('click', function(d) {
        config.clickFunc(d);
      });


    // Global setting
    _this.svg.selectAll('text').style('pointer-events', 'none');
    _this.svg.selectAll('.inner').style('cursor', 'pointer');
    _this.svg.selectAll('.inner').each(function(d) {
      d.count = _this.getValueBySetIds(d.data);
    });

  };


  Venn23.prototype.toggle = function(ids, forcedState){
    var _this = this;

    d3.selectAll('.inner')
      .filter(function(d) {
        return _.difference(d.data, ids).length === 0 && _.difference(ids, d.data).length === 0;
      })
      .each(function(d) {
        if (typeof forcedState === 'undefined') {
          d.selected = !d.selected;
        } else {
          d.selected = forcedState;
        }
        _this.toggleHighlight(d.data);
      });
  };


  Venn23.prototype.toggleHighlight = function(ids, hovering) {
    var _this = this;
    var config = _this.config;

    d3.selectAll('.inner')
      .filter(function(d) {
        return _.difference(d.data, ids).length === 0 && _.difference(ids, d.data).length === 0;
      })
      .style('fill', function(d) {
        if (typeof hovering === 'undefined') {
          if (d.selected === true) {
            return config.selectColour;
          } else {
            return _this.getColourBySetIds();
          }
        } else {
          if (hovering === true) {
            return config.hoverColour;
          } else {
            return d.selected? config.selectColour : _this.getColourBySetIds();
          }
        }

      });
  };


  Venn23.prototype.update = function() {
    // TODO
  };

  dcc.Venn23 = Venn23;

})();
