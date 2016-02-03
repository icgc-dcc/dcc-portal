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

(function($) {

  'use strict';

  window.dcc = window.dcc || {};

  var PathwayModel = function () {
    this.nodes = [];
    this.reactions = [];
    this.links = [];
  };

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

      // We need to be careful and check if forNormalDraw has been set. If so ONLY draw compartments and normal nodes
      if (  (forNormalDraw && normalComponents.indexOf(attrs.id.nodeValue) >= 0 ) ||
            (! forNormalDraw) ||
            (type === 'RenderableCompartment') ) {
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
          grayed: (isDisease && (overlaid && overlaidIndex < 0) && diseaseComponents.indexOf(attrs.id.nodeValue) < 0),
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
      
      var schemaClass = this.attributes.schemaClass;
      var failedReaction = false;
      var lineColour = _.get(this.attributes, 'lineColor.nodeValue', false);

      if ( (typeof schemaClass !== 'undefined' && schemaClass.nodeValue === 'FailedReaction') ||
              (lineColour === '255 0 0' || lineColour === '255 51 51') ) {
        failedReaction = true;
      }
      
      var grayed = false;
      if (isDisease && lineColour === '255 51 51') {
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
    return _.map(reaction.nodes, function(node){ return this.getNodeById(node.id);}, this);
  };

  dcc.PathwayModel = PathwayModel;

})(jQuery);
