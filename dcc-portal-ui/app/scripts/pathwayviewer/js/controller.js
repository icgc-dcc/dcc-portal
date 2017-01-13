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

angular.module('icgc.pathwayviewer.directives.controller', ['icgc.pathwayviewer.directives.services'])
  .controller('PathwayViewerCtrl', function(PathwayerViewerRenderUtilsService, PathwayRendererService,
                                            pathwayModelService) {

    ////////////////////////////////////////////////////////
    /**
     * Reactome Pathway controller (Main Logic)
     * **/
    function ReactomePathway(config) {

      var _defaultConfig = {
        width: 500,
        height: 500,
        onClick: {},
        urlPath: '',
        strokeColor: 'black',
        mutationHighlightColor: '#9b315b',
        drugHighlightColor: 'navy',
        overlapColor: '#000000',
        subPathwayColor: 'blue',
        initScaleFactor: 0.90
      };

      this.config = config || _defaultConfig;
      this.rendererUtils = new RendererUtils();
      this.model = null;

      /*
       * Takes in an xml of the pathway diagram and a list of reactions to zoom in
       * on and highlight. The color of the reactions is set with config.subPathwayColor
       *
       */
      ReactomePathway.prototype.render = function (xml, zoomedOnElements) {
        var config = this.config;
        var model = new PathwayModel();
        var nodesInPathway = [];
        this.model = model;
        model.parse(xml);

        var getBoundingBox = function(nodes,box){
          nodes.forEach(function (node) {
            box.height = Math.max(node.position.y + node.size.height, box.height);
            box.width = Math.max(node.position.x + node.size.width, box.width);
            box.minHeight = Math.min(node.position.y, box.minHeight);
            box.minWidth = Math.min(node.position.x, box.minWidth);
          });
          return box;
        };

        var pathwayBox = getBoundingBox(model.getNodes(),{height:0,width:0,minHeight:10000,minWidth:10000});

        // Find out the size of the actual contents of the pathway so we can center it
        model.getNodes().forEach(function (node) {
          pathwayBox.height = Math.max(node.position.y + node.size.height, pathwayBox.height);
          pathwayBox.width = Math.max(node.position.x + node.size.width, pathwayBox.width);
          pathwayBox.minHeight = Math.min(node.position.y, pathwayBox.minHeight);
          pathwayBox.minWidth = Math.min(node.position.x, pathwayBox.minWidth);
        });

        // Calculate scale factor s, based on container size and size of contents
        var scaleFactor = Math.min(config.height / (pathwayBox.height - pathwayBox.minHeight),
          config.width / (pathwayBox.width - pathwayBox.minWidth));

        // Set the zoom extents based on scale factor
        var zoom = d3.behavior.zoom().scaleExtent([scaleFactor*0.9, scaleFactor*17]);

        var svg = d3.select(config.container).append('svg')
          .attr('class', 'pathwaysvg pathway-no-scroll')
          .attr('viewBox', '0 0 ' + config.width + ' ' + config.height)
          .attr('preserveAspectRatio', 'xMidYMid')
          .append('g')
          .call(zoom)
          .on('dblclick.zoom', null) // Make double click reset instead of zoom a level
          .append('g');

        zoom.on('zoom', function () {
          svg.attr('transform', 'translate(' + d3.event.translate + ')scale(' + d3.event.scale + ')');
        });

        // Set initial positioning and zoom out a little
        scaleFactor = scaleFactor * config.initScaleFactor;
        var offsetX = (config.width - (pathwayBox.width - pathwayBox.minWidth) * scaleFactor) / 2;
        var offsetY = (config.height - (pathwayBox.height - pathwayBox.minHeight) * scaleFactor) / 2;

        zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
          -pathwayBox.minHeight * scaleFactor + offsetY]);
        svg.attr('transform', 'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
            -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
                              'scale(' + scaleFactor + ')');

        // So that the whole thing can be dragged around
        svg.append('rect').attr({
          'class': 'svg-invisible-backdrop',
          'x': 0,
          'y': 0,
          'width': pathwayBox.width,
          'height': pathwayBox.height,
        }).style('opacity', 0);

        // Reset view on double click
        d3.select('.pathwaysvg').on('dblclick', function () {
          zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
            -pathwayBox.minHeight * scaleFactor + offsetY]);
          svg.transition().attr('transform',
            'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
              -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
            'scale(' + scaleFactor + ')');
        });

        // Render everything
        this.renderer = new Renderer(svg, {
          onClick: function (d) {
            d.isPartOfPathway = (nodesInPathway.length<=0 || nodesInPathway.indexOf(d.reactomeId) >= 0);
            config.onClick(d);
          },
          urlPath: config.urlPath,
          strokeColor: config.strokeColor,
          mutationHighlightColor: config.mutationHighlightColor,
          drugHighlightColor: config.drugHighlightColor,
          overlapColor: config.overlapColor,
          subPathwayColor: config.subPathwayColor
        });

        this.renderer.renderCompartments(_.where(model.getNodes(),{type:'RenderableCompartment', hasClass:true}));
        this.renderer.renderEdges(this.rendererUtils.generateLines(model));
        this.renderer.renderNodes(_.filter(model.getNodes(),
          function(n){return n.type!=='RenderableCompartment'}));
        this.renderer.renderReactionLabels(this.rendererUtils.generateReactionLabels(model.getReactions()));

        // Zoom in on the elements of interest if there are any
        if(zoomedOnElements[0] && zoomedOnElements[0].length !== 0){

          var subPathwayReactions = _.filter(model.getReactions(),
            function(n){return _.contains(zoomedOnElements,n.reactomeId)});
          var renderer = this.renderer;

          pathwayBox = {height:0,width:0,minHeight:10000,minWidth:10000};

          // Go through all reactions: add their nodes and zoom in on them
          subPathwayReactions.forEach(function (reaction) {
            // Add nodes in this pathway to list
            nodesInPathway = nodesInPathway.concat(model.getNodeIdsInReaction(reaction.reactomeId));
            // Outline in pink
            renderer.outlineSubPathway(svg,reaction.reactomeId);
            // Get box
            pathwayBox = getBoundingBox(model.getNodesInReaction(reaction),pathwayBox);
          });

          // Add some buffer to the zoomed in area
          pathwayBox.width += 50;
          pathwayBox.minWidth -= 50;

          // Recalcualte the scale factor and offset and the zoom and transition
          scaleFactor = Math.min(config.height / (pathwayBox.height - pathwayBox.minHeight),
            config.width / (pathwayBox.width - pathwayBox.minWidth));

          scaleFactor = scaleFactor * config.initScaleFactor;
          offsetX = (config.width - (pathwayBox.width - pathwayBox.minWidth) * scaleFactor) / 2;
          offsetY = (config.height - (pathwayBox.height - pathwayBox.minHeight) * scaleFactor) / 2;
          zoom.scale(scaleFactor).translate([-pathwayBox.minWidth * scaleFactor + offsetX,
            -pathwayBox.minHeight * scaleFactor + offsetY]);
          svg.transition().attr('transform', 'translate(' + [-pathwayBox.minWidth * scaleFactor + offsetX,
              -pathwayBox.minHeight * scaleFactor + offsetY] + ')'+
                                             'scale(' + scaleFactor + ')');
        }

        this.nodesInPathway = nodesInPathway;
      };

      /**
       * Renders a legend svg in pathway-legend div given a width and height
       * Assumes the existance of a div with the class 'pathway-legend-svg'.
       *
       * On the other hand, if it already rendered it, it will simply set the opacity
       * of this div to 1.
       */
      ReactomePathway.prototype.renderLegend = function (w,h) {
        d3.select('.pathway-legend-svg').remove();
        var config =  this.config;
        var rendererUtils = this.rendererUtils;

        var legendSvg = d3.select('.pathway-legend-content').append('svg')
          .attr('class','pathway-legend-svg')
          .attr('viewBox', '0 0 ' +w+ ' ' + h)
          .attr('preserveAspectRatio', 'xMidYMid')
          .append('g');

        var legendRenderer = new Renderer(legendSvg, {
          onClick: function(){},
          urlPath: config.urlPath,
          strokeColor: config.strokeColor,
          mutationHighlightColor: config.mutationHighlightColor,
          drugHighlightColor: config.drugHighlightColor,
          overlapColor: config.overlapColor
        });

        var nodes = rendererUtils.getLegendNodes(20,0,legendSvg);

        legendRenderer.renderNodes(nodes);
        legendRenderer.renderEdges(rendererUtils.getLegendLines(40,Math.ceil(h*0.50),legendSvg));
        legendRenderer.renderReactionLabels(rendererUtils.getLegendLabels(35,Math.ceil(h*0.72),legendSvg),true);
        var model = new PathwayModel();
        model.nodes = _.takeRight(nodes,3);
        legendRenderer.highlightEntity(
            [{id:'mutated',value:99}],
            [{id:'druggable',value:99}],
            {'overlapping': 0} ,
            model
        );

        legendSvg.selectAll('.reaction-failed-example')
          .classed('failed-reaction',true);
      };

      /**
       *  Takes in raw highlight data of the form:
       *  [{dbIds:[123,124,125],value:10, uniprotId: X0000}...]
       *
       *  and transforms and renders with the form:
       *  [{id:123, value:10},{id:124, value:10},...]
       */
      ReactomePathway.prototype.highlight = function (rawMutationHighlights, rawDrugHighlights, overlaps) {
        var nodesInPathway = this.nodesInPathway;

        _.keys(overlaps).forEach(function (dbId) {

            // Only highlight overlaps it if it's part of the pathway we're zooming in on
            // And only hide parts of it we are zooming in on a pathway
            if((nodesInPathway.length !== 0 && ! _.contains(nodesInPathway,dbId))){
              delete overlaps[dbId];
            }
        });

        this.renderer.highlightEntity(
            _retrieveHighlights('mutation', rawMutationHighlights, this.nodesInPathway),
            _retrieveHighlights('drug', rawDrugHighlights, this.nodesInPathway),
            overlaps,
            this.model
        );

        function _retrieveHighlights(type, rawHighlights, nodesInPathway) {
          return _(rawHighlights)
            .map(function (rawHighlight) {
              var highlightValue = _getHighlightValue(rawHighlight, type);

              return rawHighlight.dbIds
                .filter(function (dbId) {
                  return nodesInPathway.length === 0 ||
                    (_.contains(nodesInPathway, dbId) && highlightValue >= 0);
                })
                .map(function (dbId) {
                  return {
                    id: dbId,
                    value: highlightValue
                  };
                });
            })
            .flatten()
            .value();
        }

        function _getHighlightValue(rawHighlight, type) {
          var highlightValueFunctionMap = {
            'mutation' : function (rawHighlight) { return rawHighlight.value },
            'drug' : function (rawHighlight) { return rawHighlight.drugs.length }
          };

          return highlightValueFunctionMap[type](rawHighlight);
        }
      };
    }


    // Public API
    var _ctrl = this,
        PathwayModel = pathwayModelService.getPathwayModel(),
        Renderer = PathwayRendererService.getPathwayRenderer(),
        RendererUtils = PathwayerViewerRenderUtilsService.getRenderUtils();

        _ctrl.ReactomePathway = ReactomePathway;
        _ctrl.Renderer = Renderer;
        _ctrl.PathwayModel = PathwayModel;

  });