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
  
  var defaultConfig =
  {
    onClick:{},
    urlPath: '',
    strokeColor: '#696969',
    highlightColor: 'red',
    subPathwayColor: 'navy'
  };

  var Renderer = function(svg, config) {
    this.svg = svg;
    this.config = config || defaultConfig;
    defineDefs(svg,config);
  };

  function defineDefs(svg, config){
    var strokeColor =  config.strokeColor;
    var markers = ['Output','Activator','ProcessNode','RenderableInteraction','GeneArrow','Catalyst',
                  'Catalyst-legend','Activator-legend','Output-legend','Inhibitor','Inhibitor-legend'];
    var isBaseMarker = function(type){
      return _.contains(['Output','Activator','Catalyst','Inhibitor'],type); // Part of subpathway reactions
    };
    var filled = function(type){
      return _.contains(['Output','RenderableInteraction','Output-legend','GeneArrow'],type);
    };
    var isCircular = function(type){return _.contains(['Catalyst','Catalyst-legend'],type);};
    var shifted = function(type){return _.contains(['Catalyst','Activator'],type);};
    var isLinear = function(type){return _.contains(['Inhibitor','Inhibitor-legend'],type);};
    
    var circle = {
      'element':'circle',
      'attr':{
        'cx':10,
        'cy':0,
        'r':10,
        'stroke-width':'2px',
        'markerWidth':'8',
        'markerHeight':'8'
      },
      'viewBox':'0 -14 26 28',
      'refX':'20'
    };
    
    var arrow = {
      'element':'path',
      'attr':{
        d:'M0,-5L10,0L0,5L0,-5',
        'stroke-width':'1px',
        markerWidth:'8',
        markerHeight:'8'
      },
      refX: '10',
      viewBox:'0 -6 12 11'
    };
    
    var line = {
      'element':'path',
      'attr':{
        d:'M0,-6L0,6',
        'stroke-width':'2px',
        markerWidth:'8',
        markerHeight:'8'
      },
      refX: '0',
      viewBox:'0 -6 2 11'
    };
   
    var defs = svg.append('svg:defs');
    
    // Provides the grayscale filter in case of disease pathways		
    defs.append('svg:filter').attr({		
      id: 'grayscale'		
    }).append('feColorMatrix').attr({		
      type: 'matrix',		
      values: '0.5066 0.3333 0.3333 0 0 0.3333 0.5066 0.3333 0 0 0.3333 0.3333 0.5066 0 0 0 0 0 1 0'		
    });
    
    markers.forEach(function (elem) {
      var def;
      if(isCircular(elem)){
        def = circle;
      }else if(isLinear(elem)){
        def = line;
      }else{
        def = arrow;
      }
      
      var color = strokeColor;
      
      // Special arrow for genes (see react_11118 for an example)
      if(elem === 'GeneArrow'){
        def.attr.markerWidth = 5;
        def.attr.markerHeight = 5;
        color = 'black';
      }
      
      defs.append('svg:marker')
        .attr({
          'id': elem,
          'viewBox': def.viewBox,
          'refX': (+def.refX)*(shifted(elem)?1.5:1),
          'markerHeight':def.attr.markerHeight,
          'markerWidth':def.attr.markerWidth,
          'orient':'auto'
        }).append(def.element)
        .attr(def.attr)
        .attr('stroke',color)
        .style('fill',filled(elem)?color:'white');
      
      if(isBaseMarker(elem)){
        color = config.subPathwayColor;
        
        defs.append('svg:marker')
          .attr({
            'id': elem+'-subpathway',
            'viewBox': def.viewBox,
            'refX': (+def.refX)*(shifted(elem)?1.5:1),
            'markerHeight':def.attr.markerHeight,
            'markerWidth':def.attr.markerWidth,
            'orient':'auto'
          }).append(def.element)
          .attr(def.attr)
          .attr('stroke',color)
          .style('fill',filled(elem)?color:'white');
      }
    });
  }

  /*
  * Renders the background compartments along with its specially position text
  */
  Renderer.prototype.renderCompartments = function (compartments) {
    this.svg.selectAll('.RenderableCompartment').data(compartments).enter().append('rect').attr({
      'class': function (d) {
        return d.type + ' compartment'+d.reactomeId;
      },
      'x': function (d) {return d.position.x;},
      'y': function (d) {return d.position.y;},
      'width': function (d) {return d.size.width;},
      'height': function (d) {return d.size.height;},
      rx: 3,
      ry: 3
    });

    this.svg.selectAll('.RenderableCompartmentText').data(compartments).enter().append('foreignObject').attr({
        'class':function(d){return d.type+'Text RenderableCompartmentText';},
        'x':function(d){return d.text.position.x;},
        // A little bit of top padding so compartment text does not intersect with nodes. 
        'y':function(d){return d.text.position.y+10;},
        'width':function(d){return d.size.width;},
        'height':function(d){return d.size.height;},
        'pointer-events':'none',
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','RenderableCompartmentText')
      .html(function(d){
        return '<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
          d.text.content+'</td></tr></table>';
      });
  };

  /*
  * Render all the nodes and their text
  */
  Renderer.prototype.renderNodes = function (nodes) {
    var svg = this.svg, config = this.config;
    // Split into normal rectangles and octagons based on node type
    var octs = _.filter(nodes,function(n){return n.type === 'RenderableComplex';});
    var rects = _.filter(nodes,function(n){return n.type !== 'RenderableComplex';});
    var crossed = _.filter(nodes, function(n){return n.crossed === true;});

    var pointMapToString = function(map) {
      var val = '';
      map.forEach(function (elem) {
        val= val+elem.x+','+elem.y+' ';
      });
      return val;
    };

    // Create a point map for the octagons
    var getPointsMap = function(x,y,w,h,a){
      var points = [{x:x+a,   y:y},
                    {x:x+w-a, y:y},
                    {x:x+w,   y:y+a},
                    {x:x+w,   y:y+h-a},
                    {x:x+w-a, y:y+h},
                    {x:x+a,   y:y+h},
                    {x:x,     y:y+h-a},
                    {x:x,     y:y+a}];
      return pointMapToString(points);
    };
    
    var getCrossMap = function(x,y,w,h){
      var points = [{x:x, y:y},
                    {x:x+w, y:y+h}];
      return pointMapToString(points);
    };
  
    var getReverseCrossMap = function(x,y,w,h){
      var points = [{x:x, y:y+h},
                    {x:x+w, y:y}];
      return pointMapToString(points);
    };
    
    // Render all complexes as octagons
    svg.selectAll('.RenderableOct').data(octs).enter().append('polygon')
      .attr({
        'class': function(d){return 'pathway-node RenderableOct RenderableComplex entity'+d.id;},
        'filter': function (d) {
          if (d.grayed) {
            return (typeof config.urlPath==='undefined') ? '' : 'url(\''+config.urlPath+'#grayscale\')';
          } else {
            return '';
          }
        },
        'points': function (d) {
          return getPointsMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height, 4);
        },
        'stroke': 'Red',
        'stroke-width': 1
      }).on('mouseover', function (d) {
        d.oldColor = d3.rgb(d3.select(this).style('fill'));
        d3.select(this).style('fill', d.oldColor.brighter(0.25));
      }).on('mouseout', function (d) {
        d3.select(this).style('fill', d.oldColor);
      }).on('click',config.onClick);

    // Render all other normal rectangular nodes after octagons
    svg.selectAll('.RenderableRect').data(rects).enter().append('rect').attr({
      'class': function (d) {return 'pathway-node RenderableRect ' + d.type + ' entity'+d.id;},
      'filter': function (d) {
        if (d.grayed) {
          return (typeof config.urlPath==='undefined') ? '' : 'url(\''+config.urlPath+'#grayscale\')';
        } else {
          return '';
        }
      },
      'x': function (d) {return d.position.x;},
      'y': function (d) {return d.position.y;},
      'width': function (d) {return d.size.width;},
      'height': function (d) {return d.size.height;},
      'rx': function (d) {
        switch (d.type) {
        case 'RenderableGene':
        case 'RenderableEntitySet':
        case 'RenderableEntity':
          return 0;
        case 'RenderableChemical':
          return d.size.width / 2;
        case 'RenderableFailed':
          return d.size.width / 2;
        default:
          return 3;
        }
      },
      'ry': function (d) {
        switch (d.type) {
        case 'RenderableGene':
        case 'RenderableEntitySet':
        case 'RenderableEntity':
          return 0;
        case 'RenderableChemical':
          return d.size.width / 2;
        case 'RenderableFailed':
          return d.size.width / 2;
        default:
          return 3;
        }
      },
      'stroke-dasharray': function (d) { //Gene has border on bottom and right side
        if (d.type === 'RenderableGene'){
          return 0 + ' ' + ((+d.size.width) + 1) + ' ' + ((+d.size.height) + (+d.size.width)) + ' 0';
        }else{
          return '';
        }
      },
      'pointer-events':function(d){return d.type==='RenderableGene'?'none':'';}
    }).on('mouseover', function (d) {
      d.oldColor = d3.rgb(d3.select(this).style('fill'));
      d3.select(this).style('fill', d.oldColor.brighter(0.25));
    }).on('mouseout', function (d) {
      d3.select(this).style('fill', d.oldColor);
    }).on('click',config.onClick);
    
    svg.selectAll('.crossed').data(crossed).enter().append('polyline').attr({
      'class': 'CrossedNode',
      'fill': 'none',
      'stroke': 'red',
      'stroke-width': '2',
      'points': function(d) {return getCrossMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height);}
    });
    
    svg.selectAll('.crossed').data(crossed).enter().append('polyline').attr({
      'class': 'CrossedNode',
      'fill': 'none',
      'stroke': 'red',
      'stroke-width': '2',
      'points': function(d) {return getReverseCrossMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height);}
    });
    
    // Add a foreignObject to contain all text so that warpping is done for us
    svg.selectAll('.RenderableText').data(nodes).enter().append('foreignObject').attr({
        'class':function(d){return d.type+'Text RenderableText';},
        'x':function(d){return d.position.x;},
        'y':function(d){return d.position.y;},
        'width':function(d){return d.size.width;},
        'height':function(d){return d.size.height;},
        'pointer-events':'none',
        'fill':'none'
      }).append('xhtml:body')
      .attr('class','node-text-body RenderableNodeText')
      .html(function(d){
        if (d.lof) {
          var lofClass = 'lof-'+ d.type;
          return '<table class="RenderableNodeTextCell ' + lofClass +'"><tr>' + 
            '<td style="max-width:'+d.size.width+'px;" class="RenderableNodeTextCell lof-cell" valign="middle">'+
            d.text.content+'</td></tr></table>';
        } else if (d.overlaid && !d.crossed) {
          return '<table class="RenderableNodeTextCell">' +
                 '<tr><td style="max-width:'+d.size.width+'px;" valign="middle">' +
          '<span class="span__'+ d.type +'">'+
            d.text.content+'</span></td></tr></table>';
        } else {
          return '<table class="RenderableNodeTextCell">' +
                 '<tr><td style="max-width:'+d.size.width+'px;" valign="middle">'+
            d.text.content+'</td></tr></table>';
        }
      });
    
    // if it's a gene, we have to add a sepcial array in the top right corner
    var genes =  _.where(nodes,{type : 'RenderableGene'});

    svg.selectAll('.RenderableGeneArrow').data(genes).enter().append('line').attr({
      'class':'RenderableGeneArrow',
      'x1':function(d){return (+d.position.x)+(+d.size.width) - 0.5;},
      'y1':function(d){return (+d.position.y) +1;},
      'x2':function(d){return (+d.position.x)+(+d.size.width)  + 5.5;},
      'y2':function(d){return (+d.position.y) + 1;},
    }).attr('stroke','black')
      .attr('marker-end','url("'+config.urlPath + '#GeneArrow")');
    
  };

  /*
  * Renders all connecting edges and their arrow heads where appropriate 
  */
  Renderer.prototype.renderEdges = function (edges) {
    var svg = this.svg, config = this.config;

    // In the odd case that there are layers of the same node/reaction, order things so that the
    // edges with markers (arrow heads, etc.) are on top.
    edges = _.sortBy(edges,function(n){return n.marked?1:0;});

    var isStartMarker = function(type){return _.contains(['FlowLine','RenderableInteraction'],type);};
    var isLink = function(type) { return _.contains(['EntitySetAndMemberLink', 'EntitySetAndEntitySetLink'],type);};

    svg.selectAll('line').data(edges).enter().append('line').attr({
      'class':function(d){
          var classes = 'RenderableStroke reaction'+d.id+' '+d.type;
          if (d.failedReaction) {
            classes += ' ' + 'failed-reaction';
          }
          return classes;
        },
      'filter': function (d) {
        if (d.grayed) {
          return (typeof config.urlPath==='undefined') ? '' : 'url(\''+config.urlPath+'#grayscale\')';
        } else {
          return '';
        }
      },
      'x1':function(d){return d.x1;},
      'y1':function(d){return d.y1;},
      'x2':function(d){return d.x2;},
      'y2':function(d){return d.y2;},
      'stroke': config.strokeColor
    }).attr({
      'marker-start':function(d){
        return d.marked && isStartMarker(d.marker) && !isLink(d.type)?
          'url("'+config.urlPath+'#'+d.marker+'")' : '';
      },
      'marker-end':function(d){
        return d.marked && !isStartMarker(d.marker) && !isLink(d.type)?
          'url("'+config.urlPath+'#'+d.marker+'")' : '';
      }
    });
  };

  /*
  * Render a label in the middle of the line to indicate the type
  */
  Renderer.prototype.renderReactionLabels = function (labels, legend) {
    var size = 7, svg = this.svg, config = this.config;
    var circular = ['Association','Dissociation','Binding'];
    var filled = ['Association','Binding'];

    // Add lines behind labels for legend to make it looks more realistic
    if(legend){
      svg.selectAll('.pathway-legend-line').data(labels).enter().append('line').attr({
        'class':'pathway-legend-line',
        'x1':function(d){return (+d.x)-30;},
        'y1':function(d){return d.y;},
        'x2':function(d){return (+d.x)+30;},
        'y2':function(d){return d.y;},
        'stroke':config.strokeColor
      });
    }
    
    svg.selectAll('.RenderableReactionLabel').data(labels).enter().append('rect')
    .attr({
      'class':function(d){return 'RenderableReactionLabel reaction'+d.id;},
      'x':function(d){return +d.x - (size/2);},
      'y':function(d){return +d.y - (size/2);},
      'rx':function(d){return _.contains(circular,d.reactionType)?(size/2):'';},
      'ry':function(d){return _.contains(circular,d.reactionType)?(size/2):'';},
      'width':size,
      'height':size,
      'stroke':config.strokeColor
    }).style('fill',function(d){return _.contains(filled,d.reactionType)?config.strokeColor:'white';})
      .on('mouseover',function(d){
        console.log(d.description);
      });

    svg.selectAll('.ReactionLabelText').data(labels).enter().append('text')
    .attr({
      'class':'ReactionLabelText',
      'x':function(d){return +d.x - (size/4);},
      'y':function(d){return +d.y + (size/4);},
      'font-weight':'bold',
      'font-size':'5px',
      'fill':config.strokeColor
    }).text(function(d){
      if(d.reactionType === 'Omitted Process'){
        return '\\\\';
      }else if(d.reactionType === 'Uncertain'){
        return '?';
      }else{
        return '';
      }
    });
  };

  /*
  * Highlights the given list of nodes with a red border and puts
  *   the 'value' of the node in a badge in the top right corner
  * 
  * Takes an array of Highlight and the model 
  * Highlight: { id, value }
  *
  */
  Renderer.prototype.highlightEntity = function (highlights, model) {
    var svg = this.svg, config = this.config;
    var highlighted = [];
    var nodeValues = {};
    
    // Remove old highlights if there are any
    svg.selectAll('.banner-text').remove();
    svg.selectAll('.value-banner').remove();
    svg.selectAll('.pathway-node').style('stroke','').style('stroke-width','');
    
    // Compute final mutation highlight text value first
    highlights.forEach(function (highlight) {
      var nodes = model.getNodesByReactomeId(highlight.id);
      nodes.forEach(function (node) {
        var renderedValue = highlight.value;
        
        if (highlighted.indexOf(node.id) >= 0){
          nodeValues[node.id] =  '*';  
        } else {
          nodeValues[node.id] =  renderedValue; 
          highlighted.push(node.id);
        }
      });
    });
    
    // Add SVG elements to nodes with highlight values
    for (var nodeId in nodeValues) {
      if (nodeValues.hasOwnProperty(nodeId)) {
       
        var node = model.getNodeById(nodeId);
        var renderedValue = nodeValues[nodeId];
        var svgNode = svg.selectAll('.entity'+node.id);
        svgNode.style('stroke',config.highlightColor);
        svgNode.style('stroke-width','3px');
        
        svg.append('rect')
          .attr({
            class:'value-banner value-banner'+nodeId,
            x: (+node.position.x)+(+node.size.width) - 10,
            y: (+node.position.y)- 7,
            width:(renderedValue.toString().length*5)+10,
            height:15,
            rx: 7,
            ry: 7,
          }).style({
            fill:config.highlightColor
          });

        svg.append('text').attr({
          'class':'banner-text banner-text'+nodeId,
          'x':(+node.position.x)+(+node.size.width) - 5,
          'y':(+node.position.y)+4,
          'pointer-events':'none',
          'font-size':'9px',
          'font-weight':'bold',
          'fill':'white'
        }).text(renderedValue);
      }
    }
  };
  
  Renderer.prototype.outlineSubPathway = function (svg, reactomeId) {
    svg.selectAll('.reaction'+reactomeId)
      .attr('stroke',this.config.subPathwayColor)
      .classed('pathway-sub-reaction-line',true);
  };

  dcc.Renderer = Renderer;

})();
