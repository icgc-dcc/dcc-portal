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

'use strict';

angular.module('icgc.pathwayviewer.directives.services', [])
  .service('pathwayModelService', function() {

    var $ = jQuery;

    /*
     * Model for pathway
     */
    function PathwayModel() {

      this.nodes = [];
      this.reactions = [];
      this.links = [];


      PathwayModel.prototype.parse = function (xml) {
        var parsedXml =  $($.parseXML(xml));

        var checkandReturn = function(elements) {
          if (typeof elements !== 'undefined' && typeof elements[0] !== 'undefined') {
            var text = elements[0].textContent;
            return text.split(',');
          }else {
            return [];
          }
        };

        var process = parsedXml.find('Process')[0].attributes;

        // Is this a disease pathway?
        var isDisease = (process.isDisease && process.isDisease.nodeValue === 'true');
        var diseaseComponents = checkandReturn(parsedXml.find('diseaseComponents'));

        // Is this a normal draw diagram? (Disease Diagrams are sometimes resued in a non disease context)
        var forNormalDraw = (process.forNormalDraw && process.forNormalDraw.nodeValue === 'true');
        var normalComponents = checkandReturn(parsedXml.find('normalComponents'));

        // Parse all the nodes first
        var xmlNodes = parsedXml.find('Nodes')[0].children;
        var nodes = this.nodes;

        // Check if there will be an overlay
        var overlaidList = checkandReturn(parsedXml.find('overlaidComponents'));
        var overlaid = (overlaidList.length > 0) ? true : false;

        // Find if there are any crossed out
        var crossedList = checkandReturn(parsedXml.find('crossedComponents'));

        // Find if there are any loss of function nodes
        var lofList = checkandReturn( parsedXml.find('lofNodes'));

        $(xmlNodes).each(function(){
          var attrs = this.attributes;

          var bounds = attrs.bounds.nodeValue.split(' ');
          var textPosition = attrs.textPosition ?
            attrs.textPosition.nodeValue.split(' ') :
            attrs.bounds.nodeValue.split(' ');
          var lofIndex = lofList.indexOf(attrs.id.nodeValue);
          var overlaidIndex = overlaidList.indexOf(attrs.id.nodeValue);
          var type = this.tagName.substring(this.tagName.lastIndexOf('.') + 1);

          // We need to be careful and check if forNormalDraw has been set.
          // If so ONLY draw compartments and normal nodes
          if ( (forNormalDraw && normalComponents.indexOf(attrs.id.nodeValue) >= 0 ) ||
              (!forNormalDraw || type === 'RenderableCompartment') ) {
            nodes.push({
              position: {
                x: +bounds[0],
                y: +bounds[1]
              },
              size: {
                width: +bounds[2],
                height: +bounds[3]
              },
              type: type,
              id: attrs.id.nodeValue,
              crossed: (crossedList.indexOf(attrs.id.nodeValue) >= 0 ),
              lof: (lofIndex >= 0 ),
              grayed: (isDisease &&
                       (overlaid && overlaidIndex < 0) &&
                       diseaseComponents.indexOf(attrs.id.nodeValue) < 0),
              // Do not set as overlaid if it is already a loss of function node as lof ensures opaque background
              overlaid: (isDisease && (overlaid && overlaidIndex >= 0) && lofIndex < 0 ),
              // Empty compartments will not have any schemaClass. We use this info to not render them
              hasClass: attrs.schemaClass ? true : false,
              reactomeId: attrs.reactomeId ?
                attrs.reactomeId.nodeValue : 'missing',
              text: {
                content: this.textContent.trim(),
                position: {
                  x: +textPosition[0],
                  y: +textPosition[1]
                }
              }
            });
          }
        });

        var edges = parsedXml.find('Edges')[0].children;

        var getPointsArray = function(pointString){
          var points = [];
          pointString.split(',').forEach(function (p) {
            var point = p.trim().split(' ');
            points.push({
              x:point[0],
              y:point[1]
            });
          });
          return points;
        };

        // Parse all the reactions
        var reactions =  this.reactions;
        $(edges).each(function(){
          var base = getPointsArray(this.attributes.points.nodeValue);
          var nodes=[];
          var description = $(this).children().find('properties').context;
          var nodeColour = _.get(this.attributes, 'lineColor.nodeValue', false);

          var schemaClass = this.attributes.schemaClass;
          var failedReaction = false;
          if ((typeof schemaClass !== 'undefined' && schemaClass.nodeValue === 'FailedReaction') ||
              (nodeColour === '255 0 0' || nodeColour === '255 51 51')) {
            failedReaction = true;
          }

          var grayed = false;
          if (isDisease && nodeColour === '255 51 51') {
            grayed = true;
          }

          $(this).find('input,output,catalyst,activator,inhibitor').each(function(){
            nodes.push({
              type: this.localName.substring(0,1).toUpperCase()+this.localName.substring(1),
              base: this.getAttribute('points') ?
                getPointsArray(this.getAttribute('points')) : [],
              id: this.id,
            });
          });

          // We need to be careful and check if forNormalDraw has been set. If so ONLY draw normal nodes
          if ((forNormalDraw && normalComponents.indexOf(this.attributes.id.nodeValue) >= 0) || !forNormalDraw) {
            reactions.push({
              base: base,
              nodes: nodes,
              failedReaction: failedReaction,
              grayed: grayed,
              reactomeId: this.attributes.reactomeId ? this.attributes.reactomeId.nodeValue : 'missing',
              id: this.attributes.id.nodeValue,
              type: this.attributes.reactionType ? this.attributes.reactionType.nodeValue : 'missing',
              description: description ? description.textContent.trim() : 'no details',
              class: this.localName.substring(this.localName.lastIndexOf('.') + 1),
              center: getPointsArray(this.attributes.position.nodeValue)[0]
            });
          }
        });
      };

      PathwayModel.prototype.getNodeById = function (id) {
        return _.find(this.nodes, {id: id});
      };

      PathwayModel.prototype.getNodesByReactomeId = function (reactomeId) {
        return _.where(this.nodes, {reactomeId: reactomeId});
      };

      PathwayModel.prototype.getNodes = function () {
        return this.nodes;
      };

      PathwayModel.prototype.getReactions = function () {
        return this.reactions;
      };

      PathwayModel.prototype.getNodeIdsInReaction = function (reactomeId){
        var nodes = [];
        var model = this;
        this.reactions.forEach(function (reaction) {
          if(reaction.reactomeId === reactomeId){
            reaction.nodes.forEach(function (elem) {
              nodes.push(model.getNodeById(elem.id).reactomeId);
            });
          }
        });
        return nodes;
      };

      PathwayModel.prototype.getNodesInReaction = function (reaction){
        return _.map(reaction.nodes, function(node){ return this.getNodeById(node.id)}, this);
      };

    }


    this.getPathwayModel = function() {
      return PathwayModel;
    };

  })
  .service('PathwayRendererService', function() {

    ////////////////////////////////////////////////////////
    function Renderer(svg, config) {
      var defaultConfig =
      {
        onClick:{},
        urlPath: '',
        strokeColor: '#696969',
        mutationHighlightColor: '#9b315b',
        drugHighlightColor: 'navy',
        overlapColor : '#000000',
        subPathwayColor: 'navy'
      };


      this.svg = svg;
      this.config = config || defaultConfig;
      defineDefs(svg,config);


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
        var isCircular = function(type){return _.contains(['Catalyst','Catalyst-legend'],type)};
        var shifted = function(type){return _.contains(['Catalyst','Activator'],type)};
        var isLinear = function(type){return _.contains(['Inhibitor','Inhibitor-legend'],type)};

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


        var blurFilter = defs.append('svg:filter').attr({
          id: 'glow'
        });


        blurFilter.append('feGaussianBlur').attr({
          stdDeviation: '2',
          result: 'coloredBlur'
        });

        var blurFilterMerge = blurFilter.append('feMerge');

        blurFilterMerge
          .append('feMergeNode')
          .attr({'in': 'coloredBlur'});

        blurFilterMerge
          .append('feMergeNode')
          .attr({'in': 'SourceGraphic'});

        ////
        var blurFilter2 = defs.append('svg:filter').attr({
          id: 'blur',
          x: '-30',
          y: '-30',
          height: '65',
          width: '65'
        });

        blurFilter2.append('feGaussianBlur').attr({
          stdDeviation: '1.5',
          result: 'newBlur',
          in: 'SourceGraphic'
        });

        ////

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
       * Constants used to specify the highlight treatment of a gene.
       * */
      //Renderer.prototype.HIGH_LIGHT_TYPE = {MUTATION: 1, GENE_OVERLAP: 2};

      /*
       * Renders the background compartments along with its specially position text
       */
      Renderer.prototype.renderCompartments = function (compartments) {
        this.svg.selectAll('.RenderableCompartment').data(compartments).enter().append('rect').attr({
          'class': function (d) {
            return d.type + ' compartment'+d.reactomeId;
          },
          'x': function (d) {return d.position.x},
          'y': function (d) {return d.position.y},
          'width': function (d) {return d.size.width},
          'height': function (d) {return d.size.height},
          rx: 3,
          ry: 3
        });

        this.svg.selectAll('.RenderableCompartmentText').data(compartments).enter().append('foreignObject').attr({
          'class':function(d){return d.type+'Text RenderableCompartmentText'},
          'x':function(d){return d.text.position.x},
          'y':function(d){return d.text.position.y},
          'width':function(d){return d.size.width},
          'height':function(d){return d.size.height},
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
        var octs = _.filter(nodes,function(n){return n.type === 'RenderableComplex'});
        var rects = _.filter(nodes,function(n){return n.type !== 'RenderableComplex'});
        var crossed = _.filter(nodes, function(n){return n.crossed === true});

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
            'class': function(d){return 'pathway-node RenderableOct RenderableComplex entity'+d.id},
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
          'class': function (d) {return 'pathway-node RenderableRect ' + d.type + ' entity'+d.id},
          'filter': function (d) {
            if (d.grayed) {
              return (typeof config.urlPath==='undefined') ? '' : 'url(\''+config.urlPath+'#grayscale\')';
            } else {
              return '';
            }
          },
          'x': function (d) {return d.position.x},
          'y': function (d) {return d.position.y},
          'width': function (d) {return d.size.width},
          'height': function (d) {return d.size.height},
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
          'pointer-events':function(d){return d.type==='RenderableGene'?'none':''}
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
          'points': function(d) {return getCrossMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height)}
        });

        svg.selectAll('.crossed').data(crossed).enter().append('polyline').attr({
          'class': 'CrossedNode',
          'fill': 'none',
          'stroke': 'red',
          'stroke-width': '2',
          'points': function(d) {
            return getReverseCrossMap(+d.position.x, +d.position.y, +d.size.width, +d.size.height);
          }
        });

        // Add a foreignObject to contain all text so that warpping is done for us
        svg.selectAll('.RenderableText').data(nodes).enter().append('foreignObject').attr({
          'class':function(d){return d.type+'Text RenderableText'},
          'x':function(d){return d.position.x},
          'y':function(d){return d.position.y},
          'width':function(d){return d.size.width},
          'height':function(d){return d.size.height},
          'pointer-events':'none',
          'fill':'none'
        }).append('xhtml:body')
          .attr('class','node-text-body RenderableNodeText')
          .html(function(d){
            if (d.lof) {
              var lofClass = 'lof-'+ d.type;
              return '<table class="RenderableNodeTextCell ' + lofClass +'">' +
                     '<tr><td style="max-width:'+d.size.width+'px;" class="RenderableNodeTextCell lof-cell" ' +
                     ' valign="middle">' + d.text.content+'</td></tr></table>';
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
          'x1':function(d){return (+d.position.x)+(+d.size.width) - 0.5},
          'y1':function(d){return (+d.position.y) +1},
          'x2':function(d){return (+d.position.x)+(+d.size.width)  + 5.5},
          'y2':function(d){return (+d.position.y) + 1},
        }).attr('stroke','black')
          .attr('marker-end','url("' + config.urlPath + '#GeneArrow")');

      };

      /*
       * Renders all connecting edges and their arrow heads where appropriate
       */
      Renderer.prototype.renderEdges = function (edges) {
        var svg = this.svg, config = this.config;

        // In the odd case that there are layers of the same node/reaction, order things so that the
        // edges with markers (arrow heads, etc.) are on top.
        edges = _.sortBy(edges,function(n){return n.marked?1:0});

        var isStartMarker = function(type){return _.contains(['FlowLine','RenderableInteraction'],type)};
        var isLink = function(type) { return _.contains(['EntitySetAndMemberLink', 'EntitySetAndEntitySetLink'],type)};

        svg.selectAll('line').data(edges).enter().append('line').attr({
          'class':function(d){
            var classes = 'RenderableStroke reaction'+d.id+' '+d.type;
            if (d.failedReaction) {
              classes += ' ' + 'failed-reaction';
            }
            return classes;
          },
          'filter': function (d) {
            if (d.grayed || d.overlapping) {
              return (typeof config.urlPath==='undefined') ? '' : 'url(\''+config.urlPath+'#grayscale\')';
            } else {
              return '';
            }
          },
          'x1':function(d){return d.x1},
          'y1':function(d){return d.y1},
          'x2':function(d){return d.x2},
          'y2':function(d){return d.y2},
          'stroke': config.strokeColor
        }).attr({
          'marker-start':function(d){
            return d.marked && isStartMarker(d.marker) && !isLink(d.type)?
            'url("' + config.urlPath + '#' + d.marker + '")' : '';
          },
          'marker-end':function(d){
            return d.marked && !isStartMarker(d.marker) && !isLink(d.type)?
            'url("' + config.urlPath + '#' + d.marker + '")' : '';
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
            'x1':function(d){return (+d.x)-30},
            'y1':function(d){return d.y},
            'x2':function(d){return (+d.x)+30},
            'y2':function(d){return d.y},
            'stroke':config.strokeColor
          });
        }

        svg.selectAll('.RenderableReactionLabel').data(labels).enter().append('rect')
          .attr({
            'class':function(d){return 'RenderableReactionLabel reaction'+d.id},
            'x':function(d){return +d.x - (size/2)},
            'y':function(d){return +d.y - (size/2)},
            'rx':function(d){return _.contains(circular,d.reactionType)?(size/2):''},
            'ry':function(d){return _.contains(circular,d.reactionType)?(size/2):''},
            'width':size,
            'height':size,
            'stroke':config.strokeColor
          }).style('fill',function(d){return _.contains(filled,d.reactionType)?config.strokeColor:'white'});

        svg.selectAll('.ReactionLabelText').data(labels).enter().append('text')
          .attr({
            'class':'ReactionLabelText',
            'x':function(d){return +d.x - (size/4)},
            'y':function(d){return +d.y + (size/4)},
            'font-weight':'bold',
            'font-size':'8px',
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
      Renderer.prototype.highlightEntity = function (mutationHighlights, drugHighlights, overlaps, model) {
        var svg = this.svg;
        var config = this.config;

        // Remove old highlights if there are any
        svg.selectAll('.banner-text').remove();
        svg.selectAll('.value-banner').remove();
        svg.selectAll('.pathway-node').style('stroke','').style('stroke-width','');

        _drawAnnotations({
          type: 'mutation',
          nodeValues: _getNodeValues(mutationHighlights), 
          location: 'right', 
          color: config.mutationHighlightColor
        });
        _drawAnnotations({
          type: 'drug', 
          nodeValues: _getNodeValues(drugHighlights),
          location: 'left', 
          color: config.drugHighlightColor
        });
        
        var link = (typeof config.urlPath === 'undefined') ? '' : 'url(\'' + config.urlPath + '#blur\')';
        _.keys(overlaps).forEach(function (id) {

          var nodes = model.getNodesByReactomeId(id);

          nodes.forEach(function (node) {
            var svgNode = svg.selectAll('.entity'+node.id);

            svgNode
              .style({
                'stroke': config.overlapColor,
                'stroke-width': '5px'
              })
              .attr('filter', link ?  link : '')
              .attr('stroke-linejoin', 'round')
              .attr('stroke-linecap', 'round');
          });
        });
        
        function _getNodeValues(highlights) {
          var nodeValues = {};
          var highlighted = [];
          // Compute final highlight text value first
          highlights.forEach(function (highlight) {
            var nodes = model.getNodesByReactomeId(highlight.id);

            if (nodes.length === 0) {
              return;
            }
            
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
          
          return nodeValues;
        }

        function _drawAnnotations(annotations) {
          var nodeValues = annotations.nodeValues;
          var location = annotations.location;
          var color = annotations.color;
          
          // Add SVG elements to nodes with highlight values
          for (var nodeId in nodeValues) {
            if (!(nodeValues.hasOwnProperty(nodeId))) {
              continue;
            }

            var svgNode = svg.selectAll('.entity'+model.getNodeById(nodeId).id);
            svgNode.style('stroke-width','3px');

            var renderedValue = nodeValues[nodeId];
            
            // Draw rectangular container for annotation
            svg.append('rect').attr({
              class:'value-banner value-banner'+nodeId,
              x: _coordinates(nodeId, location),
              y: _coordinates(nodeId, 'top'),
              width:(renderedValue.toString().length*5)+10,
              height:15,
              rx: 7,
              ry: 7,
            }).style({
              fill:color
            });

            // Draw annotation value inside container
            svg.append('text').attr({
              'class':'banner-text banner-text'+nodeId,
              'x': _coordinates(nodeId, location, true),
              'y': _coordinates(nodeId, 'top', true),
              'pointer-events':'none',
              'font-size':'9px',
              'font-weight':'bold',
              'fill':'white'
            }).text(renderedValue);
          }
          
          function _coordinates(nodeId, location, forText) {
            var node = model.getNodeById(nodeId);
            var renderedValue = nodeValues[nodeId];
            
            var coordinateFunctionMap = {
              left : function(forText) {
                var leftX = (+node.position.x) - ((renderedValue.toString().length * 5) + 10) + 10;
                if (forText) {
                  leftX += 5;
                }
                
                return leftX;
              },
              right : function(forText) {
                var rightX = (+node.position.x) + (+node.size.width) - 10;
                if (forText) {
                  rightX += 5;
                }
                
                return rightX;
              },
              top : function(forText) {
                var topY = (+node.position.y) - 7;
                if (forText) {
                  topY += 11;
                }
                
                return topY;
              }
            };
            
            return coordinateFunctionMap[location](forText);
          }
        }
      };

      Renderer.prototype.outlineSubPathway = function (svg, reactomeId) {
        svg.selectAll('.reaction'+reactomeId)
          .attr('stroke',this.config.subPathwayColor)
          .classed('pathway-sub-reaction-line',true);
      };

    }

    this.getPathwayRenderer = function() {
      return Renderer;
    };


  })
  .service('PathwayerViewerRenderUtilsService', function(){

    ////////////////////////////////////////////////////////

    function RendererUtils() {


      /*
       * Create an array of reaction labels for every reaction based on its type
       */
      RendererUtils.prototype.generateReactionLabels = function (reactions) {
        var labels = [];

        reactions.forEach(function (reaction) {
          var hasBase = _.some(reaction.nodes,function(node){
            return (node.base && node.base.length > 0);
          });

          if(hasBase){
            labels.push({
              x:reaction.center.x,
              y:reaction.center.y,
              reactionType:reaction.type,
              description:reaction.description,
              id:reaction.reactomeId
            });
          }
        });
        return labels;
      };

      /*
       * Goes through the model's reactions and creates a large arrays of all lines
       *  based on the human-curated list of points.
       */
      RendererUtils.prototype.generateLines = function (model) {
        var lines = [];
        var reactions = model.getReactions();

        // Make sure arrow heads aren't added to special dashed lines
        var isArrowHeadLine = function(type){
          return !_.contains(['entitysetandmemberlink','entitysetandentitysetlink','missing'],type);
        };

        // Adds a line to the lines array gives an array of points and description of the line
        var generateLine = function (points, color, type, id, lineType, failed) {
          for (var j = 0; j < points.length - 1; j++) {
            lines.push({
              x1: points[j].x,
              y1: points[j].y,
              x2: points[j+1].x,
              y2: points[j+1].y,
              marked: j === points.length-2 && isArrowHeadLine(lineType) && type!=='missing',
              marker: type,
              color: color, // For debugging, every line type has a color
              id:id,
              type: lineType,
              failedReaction: failed
            });
          }
        };

        // Gets the center of node with its position and size
        var getNodeCenter = function(nodeId){
          var node = model.getNodeById(nodeId);
          // Genes are special because they are not boxes.. just an arrow
          if(node.type === 'RenderableGene'){
            return {x: (+node.position.x) + (+node.size.width) + 5,y:node.position.y};
          }
          return { x: ((+node.position.x) + (+node.size.width/2)),
            y: ((+node.position.y) + (+node.size.height/2))};
        };

        // Gets the first input node in a reaction (used when the reaction
        //  has no human-curated node lines)
        var getFirstInputNode =  function(nodes){
          return _.find(nodes, {type:'Input'});
        };

        // Generate a line based on the type of reaction & node using human-curated points
        var getNodeLines = function (reaction, node, reactionId, reactionClass, failed) {
          var count = {inputs:0,outputs:0};
          if(!node.base || node.base.length === 0){
            return 'missing';
          }
          var base =  node.base.slice();

          // eslint-disable-next-line default-case
          switch (node.type) {
            case 'Input':
              base.push(reaction.base[0]);
              base[0] = getNodeCenter(node.id);
              generateLine(base, 'red', 'Input', reactionId, reactionClass, failed);
              count.inputs = count.inputs + 1;
              break;
            case 'Output':
              base.push(reaction.center);
              base.reverse(); // Make sure output points at the output
              generateLine(base, 'green', 'Output', reactionId,reactionClass, failed);
              count.outputs = count.outputs + 1;
              break;
            case 'Activator':
              base.push(reaction.center);
              base[0] = getNodeCenter(node.id);
              generateLine(base, 'blue', 'Activator', reactionId, reactionClass, failed);
              break;
            case 'Catalyst':
              base.push(reaction.center);
              base[0] = getNodeCenter(node.id);
              generateLine(base, 'purple', 'Catalyst', reactionId, reactionClass, failed);
              break;
            case 'Inhibitor':
              base.push(reaction.center);
              base[0] = getNodeCenter(node.id);
              generateLine(base, 'orange', 'Inhibitor', reactionId, reactionClass, failed);
              break;
          }

          return node.type;
        };

        reactions.forEach(function (reaction) {
          var id = reaction.reactomeId;
          var addedTypes = [];

          reaction.nodes.forEach(function (node) {
            addedTypes.push(getNodeLines(reaction, node, id, reaction.class, reaction.failedReaction));
          });

          var hasInputs = _.contains(addedTypes,'Input');
          var hasOutputs =  _.contains(addedTypes,'Output');

          // If it doesn't have human-curated input lines, "snap" line to first input node, if it has one
          if(!hasInputs && getFirstInputNode(reaction.nodes)){
            reaction.base[0] = getNodeCenter(getFirstInputNode(reaction.nodes).id);
          }
          var baseLine = reaction.base.slice();
          if(hasOutputs){
            baseLine.pop(); // It's duplicated
          }

          // This creates a base reaction line
          generateLine(baseLine,
            hasOutputs ?'black':'navy',
            hasOutputs ?reaction.type:'Output', id, reaction.class, reaction.failedReaction);
        });

        return lines;
      };

      /*
       * Create a grid of all nodes for legend
       */
      RendererUtils.prototype.getLegendNodes =  function(marginLeft,marginTop, svg){
        var nodes = [];
        var mutatedNodeText = 'Mutated Gene(s)';
        var druggableNodeText = 'Targetable Gene(s)';
        var overlappedNodeText = 'Overlapping Gene(s)';
        var failedText = 'Failed Output';
        var lofText = 'LossOfFunction';
        var x = marginLeft, y= marginTop;
        var types = [
          'Complex','Protein',
          'EntitySet','Chemical',
          'Compartment','ProcessNode',
          failedText,lofText,
          mutatedNodeText, null, 
          druggableNodeText, null,
          overlappedNodeText
        ];

        for(var i=0;i<types.length;i++){
          x = i%2 === 0 ? marginLeft : marginLeft+130;
          y = Math.floor(i/2)*40 + marginTop + 5*Math.floor(i/2);
          var type = types[i];

          if (type === null) {
            continue;
          }

          var node = {
            position:{x:x,y:y},
            size:{width:110,height:30},
            type: _getNodeType(type),
            id: _getIDForType(type),
            crossed:type===failedText?true:false,
            lof:type===lofText?true:false,
            grayed: false,
            reactomeId: _getIDForType(type),
            text:{content:type,position:{x:x,y:y}}
          };

          nodes.push(node);

          if (type === mutatedNodeText) {
            addMutatedNodeComment();
          }

          if (type === druggableNodeText) {
            addDruggableNodeComment();
          }
        }

        return nodes;

        function _getIDForType(type) {
          var _id = 'fake';

          switch(type) {
            case mutatedNodeText:
              _id = 'mutated';
              break;
            case druggableNodeText:
              _id = 'druggable';
              break;
            case overlappedNodeText:
              _id = 'overlapping';
              break;
            default:
              break;
          }

          return _id;
        }

        function _getNodeType(type) {
          var newType = 'Renderable'+type;

          switch(type) {
            case 'ProcessNode':
              newType = type;
              break;
            case failedText:
              newType ='RenderableFailed';
              break;
            case overlappedNodeText:
              newType = 'RenderableOverlappedEntitySet';
              break;
            case lofText:
              newType ='RenderableEntitySet';
              break;
            default:
              break;
          }

          return newType;
        }

        function addMutatedNodeComment() {
            // Add extra comment for mutated gene node to show what the value in the corner means
            svg.append('foreignObject').attr({
              x: marginLeft+100,
              y: y - 15,
              width:100,
              height:35,
              'fill':'none'
            }).append('xhtml:body')
              .attr('class','RenderableNodeText')
              .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
                    '<--- # ICGC Mutations'+'</td></tr></table>');
        }

        function addDruggableNodeComment() {
            // Add extra comment for druggable gene node to show what the value in the corner means
            svg.append('foreignObject').attr({
              x: 5,
              y: y - 22,
              width:275,
              height:35,
              'fill':'none'
            }).append('xhtml:body')
              .attr('class','RenderableNodeText')
              .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
                    '<------------------------------ # Targeting Compounds'+'</td></tr></table>');
        }
      };

      /*
       * Create a list of reaction lines for legend
       */
      RendererUtils.prototype.getLegendLines = function (marginLeft,marginTop,svg) {
        var lines = [];
        var y=marginTop;
        var markers = ['Output','Catalyst','Activator','Inhibitor','Link','Disease-Associated'];
        markers.forEach(function (elem) {
          lines.push({
            x1: marginLeft,
            y1:y,
            x2: marginLeft+80,
            y2: y,
            marked: true,
            marker: elem+'-legend',
            color: 'black',
            id: (function(elem) {
              if (elem==='Disease-Associated') {
                return '-failed-example';
              } else {
                return 'fake';
              }
            })(elem),
            type: elem==='Link'?'entitysetandmemberlink':'fake'
          });
          svg.append('foreignObject').attr({
            x: marginLeft+80,
            y:y-15,
            width:105,
            height:30,
            'fill':'none'
          }).append('xhtml:body')
            .attr('class','RenderableNodeText')
            .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
                  elem+'</td></tr></table>');

          y+=25;
        });

        return lines;
      };

      /*
       * Create a list of reaction lines for legend
       */
      RendererUtils.prototype.getLegendLabels = function (marginLeft,marginTop,svg) {
        var labels = [];
        var y=marginTop;
        var reactions = ['Association','Dissociation','Transition','Omitted Process','Uncertain'];
        reactions.forEach(function (elem) {
          labels.push({
            x: marginLeft+40,
            y:y,
            reactionType: elem
          });
          svg.append('foreignObject').attr({
            x: marginLeft+80,
            y:y-15,
            width:110,
            height:30,
            'fill':'none'
          }).append('xhtml:body')
            .attr('class','RenderableNodeText')
            .html('<table class="RenderableNodeTextCell"><tr><td valign="middle">'+
                  (elem==='Association'?'Association/Binding':elem)+'</td></tr></table>');

          y+=25;
        });

        return labels;
      };

    }

    this.getRenderUtils = function() {
      return RendererUtils;
    };
  })
  .service('PathwayDataService', function ($q, Restangular, GeneSetHierarchy, GeneSetService, SetService,
                                           CompoundsService, GeneSetVerificationService, LocationService) {
    var _pathwayDataService = this;
    _pathwayDataService.getGeneSet = function (geneSetId) {
      return Restangular.one('genesets').one(geneSetId).get();
    };
    _pathwayDataService.getUIParentPathways = function (geneSet) {
      return GeneSetHierarchy.uiPathwayHierarchy(geneSet.hierarchy, geneSet);
    };
    _pathwayDataService.getParentPathwayId = function (geneSet) {
      return _pathwayDataService.getUIParentPathways(geneSet)[0].geneSetId;
    };
    _pathwayDataService.getPathwayId = function (geneSet) {
      return _pathwayDataService.getUIParentPathways(geneSet)[0].diagramId;
    };
    _pathwayDataService.getPathwayXML = function (geneSet) {
      return GeneSetService.getPathwayXML(_pathwayDataService.getPathwayId(geneSet));
    };
    _pathwayDataService.getPathwayZoom = function (geneSet) {
      if (_pathwayDataService.getParentPathwayId(geneSet) === _pathwayDataService.getPathwayId(geneSet)) {
        return $q.resolve;
      }
 
      return GeneSetService.getPathwayZoom(_pathwayDataService.getParentPathwayId(geneSet));
    };
    _pathwayDataService.getPathwayProteinMap = function (geneSet) {
      var geneSetFilter = {};
      geneSetFilter[geneSet.queryType] = {is: [geneSet.id]};
      var mergedGeneSetFilter = LocationService.mergeIntoFilters({gene:geneSetFilter});
      var mutationImpact = mergedGeneSetFilter.mutation && mergedGeneSetFilter.mutation.functionalImpact ?
        mergedGeneSetFilter.mutation.functionalImpact.is : [];

      return GeneSetService.getPathwayProteinMap(_pathwayDataService.getParentPathwayId(geneSet), mutationImpact);
    };
    _pathwayDataService.getEntitySetId = function (geneSet) {      
      return SetService.getTransientSet('GENE', {
        'filters': {gene:{pathwayId:{is:[_pathwayDataService.getParentPathwayId(geneSet)]}}},
        'sortBy': 'id',
        'sortOrder': 'ASCENDING',
        'name': _pathwayDataService.getParentPathwayId(geneSet),
        'size': geneSet.geneCount,
        'transient': true 
      }).then(function (entitySetData) {
        return entitySetData.id;
      });
    };
    _pathwayDataService.getDrugs = function (entitySetId) {
      return CompoundsService.getCompoundsFromEntitySetId(entitySetId);
    };
    _pathwayDataService.getMutationHighlights = function (pathwayProteinMap) {
      return _(pathwayProteinMap.plain())
        .map(function (pathwayProtein, uniprotId) {
          return _.assign({}, pathwayProtein, {uniprotId: uniprotId});
        })
        .compact()
        .filter(function (pathwayProtein) {
          return pathwayProtein.dbIds;
        })
        .map(function (pathwayProtein) {
          return {
            uniprotId: pathwayProtein.uniprotId,
            dbIds: pathwayProtein.dbIds.split(','),
            value: pathwayProtein.value
          };
        })
        .value();
    };
    _pathwayDataService.getDrugHighlights = function (drugs, pathwayProteinMap) {
      var drugMap = getDrugMap(drugs);

      return _(pathwayProteinMap.plain())
        .map(function (pathwayProtein, uniprotId) {
          return _.assign({}, pathwayProtein, {uniprotId: uniprotId});
        })
        .compact()
        .filter(function (pathwayProtein) {
          return pathwayProtein.dbIds && drugMap[pathwayProtein.uniprotId];
        })
        .map(function (pathwayProtein) {
          return {
            uniprotId: pathwayProtein.uniprotId,
            dbIds: pathwayProtein.dbIds.split(','),
            drugs: drugMap[pathwayProtein.uniprotId]
          };
        })
        .value();

      function getDrugMap(drugs) {
        return _(drugs)
          .map(function (drug) {
            return drug.genes.map (function (gene) {
              return {
                uniprot: gene.uniprot,
                zincId: drug.zincId,
                name: drug.name
              };
            });
          })
          .flatten()
          .groupBy('uniprot')
          .value();
      }
    };
    _pathwayDataService.getGeneListData = function (geneSetOverlapFilters) {
      if (!geneSetOverlapFilters) {
        return $q.resolve;
      }

      return Restangular.one('genes').get({filters: geneSetOverlapFilters});
    };
    _pathwayDataService.getGeneOverlapExistsHash = function (geneListData) {
      return _(_.get(geneListData, 'hits', {}))
        .map('externalDbIds.uniprotkb_swissprot')
        .flatten()
        .compact()
        .zipObject()
        .mapValues(function () {
          return true;
        })
        .value();
    };
    _pathwayDataService.getGeneAnnotatedHighlights = function (highlightData) {
      var uniprotIds = _.map(highlightData.highlights, 'uniprotId');
      return GeneSetVerificationService.verify(uniprotIds.join(',')).then(function (data) {
        var geneKey = 'external_db_ids.uniprotkb_swissprot';
        if (!data.validGenes[geneKey]) {
          return [];
        }
        
        return _(highlightData.highlights)
          .map(function (highlight) {
            var uniprotObj = data.validGenes[geneKey][highlight.uniprotId];
            if (!uniprotObj) {
              return highlight;
            }
            
            return _.assign({}, highlight, {
              geneSymbol: uniprotObj[0].symbol,
              geneId: uniprotObj[0].id,
              advQuery: highlightData.includeAdvQuery ?
                LocationService.mergeIntoFilters({gene: {id: {is: [uniprotObj[0].id]}}}) :
                {}
            });
          })
          .value();
      });
    };
    _pathwayDataService.getGeneOverlapExistsHashUsingDbIds = function (geneOverlapExistsHash, annotatedHighlights) {
      var geneCount = 0;
      var geneOverlapExistsHashUsingDbIds = Object.assign({}, geneOverlapExistsHash);

      if (geneOverlapExistsHash && annotatedHighlights) {
        _.forEach(annotatedHighlights, function (annotatedHighlight) {
          if (angular.isDefined(geneOverlapExistsHashUsingDbIds[annotatedHighlight.uniprotId])) {
            geneCount++;

            _.forEach(annotatedHighlight.dbIds, function (dbID) {
              // Swap in Reactome keys but maintain the id we use this to determine overlaps in O(1)
              // later... The dbID is used as a reference to the reactome SVG nodes...
              geneOverlapExistsHashUsingDbIds[dbID] = {
                id: annotatedHighlight.uniprotId,
                geneId: annotatedHighlight.geneId,
                geneSymbol: annotatedHighlight.geneSymbol
              };
            });
            delete geneOverlapExistsHashUsingDbIds[annotatedHighlight.uniprotId];
          }
        });
      }
      return geneOverlapExistsHashUsingDbIds;
    };

    _pathwayDataService.getPathwayData = function (geneSetId, geneSetOverlapFilters) {
      var pathwayData = {};

      return _pathwayDataService.getGeneSet(geneSetId)
      .then(function (geneSet) {
        pathwayData.geneSet = geneSet;

        return $q.all({
          pathwayProteinMap: _pathwayDataService.getPathwayProteinMap(geneSet),
          entitySetId: _pathwayDataService.getEntitySetId(geneSet),
          pathwayXML: _pathwayDataService.getPathwayXML(geneSet),
          zoomSet: _pathwayDataService.getPathwayZoom(geneSet),
          uiParentPathways: _pathwayDataService.getUIParentPathways(geneSet)
        });
      })
      .then(function (results) {
        pathwayData.xml = results.pathwayXML;
        pathwayData.zooms = results.zoomSet;
        pathwayData.uiParentPathways = results.uiParentPathways;

        return $q.all({
          geneListData: _pathwayDataService.getGeneListData(geneSetOverlapFilters),
          pathwayProteinMap: results.pathwayProteinMap,
          drugs: _pathwayDataService.getDrugs(results.entitySetId)
        });
      })
      .then(function (results) {
        return $q.all({
          geneOverlapExistsHash: _pathwayDataService.getGeneOverlapExistsHash(results.geneListData),
          annotatedMutationHighlights: _pathwayDataService.getGeneAnnotatedHighlights({
            highlights: _pathwayDataService.getMutationHighlights(results.pathwayProteinMap),
            includeAdvQuery: true
          }),
          annotatedDrugHighlights: _pathwayDataService.getGeneAnnotatedHighlights({
            highlights: _pathwayDataService.getDrugHighlights(results.drugs, results.pathwayProteinMap),
            includeAdvQuery: false
          })
        });
      })
      .then(function (results) {
        pathwayData.overlaps = _pathwayDataService.getGeneOverlapExistsHashUsingDbIds(
          results.geneOverlapExistsHash,
          _.union(results.annotatedMutationHighlights, results.annotatedDrugHighlights)
        );

        pathwayData.mutationHighlights = results.annotatedMutationHighlights;
        pathwayData.drugHighlights = results.annotatedDrugHighlights;

        pathwayData.geneSet.showPathway = true;

        return pathwayData;
      });
    };
  });