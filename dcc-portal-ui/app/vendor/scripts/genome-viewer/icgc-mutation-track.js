/*
 * Copyright (c) 2012 Francisco Salavert (ICM-CIPF)
 * Copyright (c) 2012 Ruben Sanchez (ICM-CIPF)
 * Copyright (c) 2012 Ignacio Medina (ICM-CIPF)
 *
 * This file is part of JS Common Libs.
 *
 * JS Common Libs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * JS Common Libs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JS Common Libs. If not, see <http://www.gnu.org/licenses/>.
 */

/** DEPRECATED **/

'use strict';

IcgcMutationTrack.prototype = new Track({});

function IcgcMutationTrack(args) {
  Track.call(this, args);
  // Using Underscore 'extend' function to extend and add Backbone Events
  _.extend(this, Backbone.Events);

  //set default args

  //save default render reference;
  this.defaultRenderer = this.renderer;
  this.histogramRenderer = new HistogramRenderer();
  this.resource = this.dataAdapter.resource;

  this.chunksDisplayed = {};

  //set instantiation args, must be last
  _.extend(this, args);
}


IcgcMutationTrack.prototype.updateHeight = function () {
  //this._updateHeight();
  if (this.histogram) {
    this.contentDiv.style.height = this.histogramRenderer.histogramHeight + 5 + 'px';
    this.main.setAttribute('height', this.histogramRenderer.histogramHeight);
    return;
  }

  var renderedHeight = this.svgCanvasFeatures.getBoundingClientRect().height;
  this.main.setAttribute('height', renderedHeight);

  if (this.resizable) {
    if (this.autoHeight === false) {
      this.contentDiv.style.height = this.height + 10 + 'px';
    } else if (this.autoHeight === true) {
      var x = this.pixelPosition;
      var width = this.width;
      var lastContains = 0;
      for (var i in this.renderedArea) {
        if (this.renderedArea[i].contains({
            start: x,
            end: x + width
          })) {
          lastContains = i;
        }
      }
      var visibleHeight = parseInt(lastContains) + 30;
      this.contentDiv.style.height = visibleHeight + 10 + 'px';
      this.main.setAttribute('height', visibleHeight);
    }
  }
};

IcgcMutationTrack.prototype.setWidth = function(width) {
  this._setWidth(width);
  this.main.setAttribute("width", this.width);
};

IcgcMutationTrack.prototype.initializeDom = function(targetId) {
  this._initializeDom(targetId);

  this.main = SVG.addChild(this.contentDiv, 'svg', {
    'class': 'trackSvg',
    'x': 0,
    'y': 0,
    'width': this.width
  });
  this.svgCanvasFeatures = SVG.addChild(this.main, 'svg', {
    'class': 'features',
    'x': -this.pixelPosition,
    'width': this.svgCanvasWidth
  });
  this.updateHeight();
};

function getSvgCanvasOffset(icgcMutationTrack) {
  if (!window.GENOME_VIEWER_PIXELBASE_DOES_NOT_ACCOUNT_FOR_ZOOM) {
    console.warn(`Was genome-viewer updated?
      This method currently assumes pixelBase does not account for zoom (zoom is different from zoomMultiplier).
      If pixelBase now accounts for zoom, remove "/ Math.min(window.gv.zoom, 1)"
    `);
  }
  return (icgcMutationTrack.width * 3 / 2) / icgcMutationTrack.pixelBase / Math.min(window.gv.zoom, 1);
};

IcgcMutationTrack.prototype.render = function (targetId) {
  var _this = this;
  _this.initializeDom(targetId);

  _this.svgCanvasOffset = getSvgCanvasOffset(this);
  _this.svgCanvasLeftLimit = _this.region.start - _this.svgCanvasOffset * 2;
  _this.svgCanvasRightLimit = _this.region.start + _this.svgCanvasOffset * 2;

  _this.dataAdapter.on('data:ready', function (event) {
    var features;
    if (event.params.histogram === true) {
      _this.renderer = _this.histogramRenderer;
      features = _this._histogramRendererCheck(_this._getFeaturesByChunks(event));
    } else {
      _this.renderer = _this.defaultRenderer;
      features = _this._getFeaturesByChunks(event);
    }
    _this.renderer.render(features, {
      svgCanvasFeatures: _this.svgCanvasFeatures,
      renderedArea: _this.renderedArea,
      pixelBase: _this.pixelBase,
      position: _this.region.center(),
      regionSize: _this.region.length(),
      maxLabelRegionSize: _this.maxLabelRegionSize,
      width: _this.width,
      pixelPosition: _this.pixelPosition,
      resource: _this.resource,
      species: _this.species
    });

    _this.updateHeight();
    _this.setLoading(false);
  });

  this.modalDiv = jQuery('<div></div>')[0];
  jQuery('body').append(this.modalDiv);
};

IcgcMutationTrack.prototype.clean = function() {
  //    console.time("-----------------------------------------empty");
  while (this.svgCanvasFeatures.firstChild) {
    this.svgCanvasFeatures.removeChild(this.svgCanvasFeatures.firstChild);
  }
  //    console.timeEnd("-----------------------------------------empty");
  this._clean();
};

IcgcMutationTrack.prototype.draw = function () {
  this.svgCanvasOffset = getSvgCanvasOffset(this);
  this.svgCanvasLeftLimit = this.region.start - this.svgCanvasOffset * 2;
  this.svgCanvasRightLimit = this.region.start + this.svgCanvasOffset * 2;

  this.updateHistogramParams();
  this.clean();

  if (typeof this.visibleRegionSize === 'undefined' || this.region.length() < this.visibleRegionSize) {
    this.setLoading(true);
    this.dataAdapter.getData({
      chromosome: this.region.chromosome,
      start: this.region.start - this.svgCanvasOffset * 2,
      end: this.region.end + this.svgCanvasOffset * 2,
      histogram: this.histogram,
      interval: this.interval,
      functional_impact: this.functional_impact
    });
  } else {
  }
};

IcgcMutationTrack.prototype.move = function (disp) {
  var _this = this;
  _this.region.center();
  _this.svgCanvasOffset = getSvgCanvasOffset(this);
  var pixelDisplacement = disp * _this.pixelBase;
  this.pixelPosition -= pixelDisplacement;

  //parseFloat important
  var move = parseFloat(this.svgCanvasFeatures.getAttribute('x')) + pixelDisplacement;
  this.svgCanvasFeatures.setAttribute('x', move);

  var virtualStart = parseInt(this.region.start - this.svgCanvasOffset);
  var virtualEnd = parseInt(this.region.end + this.svgCanvasOffset);
  // check if track is visible in this zoom

  if (typeof this.visibleRegionSize === 'undefined' || this.region.length() < this.visibleRegionSize) {
    if (disp > 0 && virtualStart < this.svgCanvasLeftLimit) {
      this.dataAdapter.getData({
        chromosome: _this.region.chromosome,
        start: parseInt(this.svgCanvasLeftLimit - this.svgCanvasOffset),
        end: this.svgCanvasLeftLimit,
        histogram: this.histogram,
        interval: this.interval,
        functional_impact: this.functional_impact
      });
      this.svgCanvasLeftLimit = parseInt(this.svgCanvasLeftLimit - this.svgCanvasOffset);
    }

    if (disp < 0 && virtualEnd > this.svgCanvasRightLimit) {
      this.dataAdapter.getData({
        chromosome: _this.region.chromosome,
        start: this.svgCanvasRightLimit,
        end: parseInt(this.svgCanvasRightLimit + this.svgCanvasOffset, 10),
        histogram: this.histogram,
        interval: this.interval,
        functional_impact: this.functional_impact
      });
      this.svgCanvasRightLimit = parseInt(this.svgCanvasRightLimit + this.svgCanvasOffset, 10);
    }

  }

};


IcgcMutationTrack.prototype._histogramRendererCheck = function (items) {
  //modifies input!
  var arr = [];
  for (var i = 0; i < items.length; i++) {
    var chunk = items[i];
    arr.push({value: chunk});
  }
  return arr;
};

IcgcMutationTrack.prototype._getFeaturesByChunks = function (response) {
  //Returns an array avoiding already drawn features in this.chunksDisplayed
  var chunks = response.items;
  var dataType = response.params.dataType;
  var chromosome = response.params.chromosome;

  var feature, displayed, featureFirstChunk, featureLastChunk, features = [];
  for (var i = 0, leni = chunks.length; i < leni; i++) {

    if (this.chunksDisplayed[chunks[i].key + dataType] !== true) {//check if any chunk is already displayed and skip it

      for (var j = 0, lenj = chunks[i][dataType].length; j < lenj; j++) {
        feature = chunks[i][dataType][j];

        //check if any feature has been already displayed by another chunk
        displayed = false;
        featureFirstChunk = this.dataAdapter.featureCache._getChunk(feature.start);
        featureLastChunk = this.dataAdapter.featureCache._getChunk(feature.end);
        for (var f = featureFirstChunk; f <= featureLastChunk; f++) {
          var fkey = chromosome + ':' + f;
          if (this.chunksDisplayed[fkey + dataType] === true) {
            displayed = true;
            break;
          }
        }
        if (!displayed) {
          features.push(feature);
        }
      }
      this.chunksDisplayed[chunks[i].key + dataType] = true;
    }
  }
  return features;
};

IcgcMutationTrack.prototype._variantInfo = function (e) {
  var _this = this;
  var chromosome = e.feature.chromosome;
  var position = e.feature.start;
  var mutation = e.feature.mutation.replace(/>/gi, ':');
  var url = 'http://ws.bioinfo.cipf.es/cellbase/rest/v2/hsa/genomic/variant/' + chromosome + ':' +
    position + ':' + mutation + '/consequence_type?of=json';
  var mutationPhenUrl = 'http://ws.bioinfo.cipf.es/cellbase/rest/v2/hsa/genomic/variant/' + chromosome +
    ':' + position + ':' + mutation + '/mutation_phenotype?of=json';

  var consequenceResponse = {}, phenotypeResponse = {};
  var id = Utils.randomString();
  jQuery.ajax({
    url: url,
    dataType: 'json',
    async: false,
    success: function (data) {
      consequenceResponse = data;
    }
  });

  jQuery.ajax({
    url: mutationPhenUrl,
    dataType: 'json',
    async: false,
    success: function (data) {
      phenotypeResponse = data[0];
    }
  });

  var table = '<table class="table table-condensed">' +
    '<thead><tr></thead>' +
    '<td>Position</td>' +
    '<td>Mutation</td>' +
    '<td>Gene ID</td>' +
    '<td>Feature ID</td>' +
    '<td>Feature type</td>' +
    '<td>Feature position</td>' +
    '<td>Consequence type</td>' +
    '<td>Consequence type Obo</td>' +
    '<td>AA position</td>' +
    '<td>Aminoacid change</td>' +
    '<td>Codon change</td>' +
    '</tr></thead>';
  for (var c in consequenceResponse) {
    if (consequenceResponse.hasOwnProperty(c)) {
      var item = consequenceResponse[c];
      table += '<tr>' +
        '<td>' + item.chromosome + ':' + item.position + '</td>' +
        '<td>' + item.referenceAllele + '>' + item.alternativeAllele + '</td>' +
        '<td>' + item.geneId + '</td>' +
        '<td>' + item.featureId + '</td>' +
        '<td>' + item.featureType + '</td>' +
        '<td>' + item.featureStart + '-' + item.featureEnd + '</td>' +
        '<td>' + item.consequenceType + '</td>' +
        '<td>' + item.consequenceTypeObo + '</td>' +
        '<td>' + item.aaPosition + '</td>' +
        '<td>' + item.aminoacidChange + '</td>' +
        '<td>' + item.codonChange + '</td>' +
        '</tr>';
    }
  }
  table += '</table>';

  var table2 = '<table class="table table-condensed">' +
    '<thead><tr></thead>' +
    '<td>Position</td>' +
    '<td>Mutation CDS</td>' +
    '<td>Mutation Aa</td>' +
    '<td>Gene name</td>' +
//        '<td>Transcript</td>'+
    '<td>Primary site</td>' +
    '<td>Site subtype</td>' +
    '<td>Primary histology</td>' +
    '<td>Mutation description</td>' +
    '<td>Mutation zigosity</td>' +
    '<td>Description</td>' +
    '<td>Source</td>' +
    '</tr></thead>';

  for (var p in phenotypeResponse) {
    if (phenotypeResponse.hasOwnProperty(p)) {
      var item2 = phenotypeResponse[p];
      table2 += '<tr>' +
        '<td>' + item2.chromosome + ':' + item2.start + '-' + item2.end + '</td>' +
        '<td>' + item2.mutationCds + '</td>' +
        '<td>' + item2.mutationAa + '</td>' +
        '<td>' + item2.geneName + '</td>' +
//            '<td>'+item2.ensemblTranscript+'</td>'+
        '<td>' + item2.primarySite + '</td>' +
        '<td>' + item2.siteSubtype + '</td>' +
        '<td>' + item2.primaryHistology + '</td>' +
        '<td>' + item2.mutationDescription + '</td>' +
        '<td>' + item2.mutationZigosity + '</td>' +
        '<td>' + item2.primaryHistology + '</td>' +
        '<td>' + item2.description + '</td>' +
        '<td>' + item2.source + '</td>' +
        '</tr>';
    }
  }
  table2 += '</table>';

  var html =
    '<div id="mutationModal' + id + '" class="modal hide fade" tabindex="-1" role="dialog"' +
    ' aria-labelledby="myModalLabel" aria-hidden="true" style="width:1200px;margin-left:-600px;">' +
    '<div class="modal-header">' +
    '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>' +
    '<h3 id="myModalLabel">Variant - ' + e.feature.id + '</h3>' +
    '</div>' +
    '<div class="modal-body">' +
    '<ul class="nav nav-tabs" id="mutationTabs' + id + '">' +
    '<li class="active"><a href="#vareffect' + id + '">Variant effect</a></li>' +
    '<li><a href="#mutphenotype' + id + '">Mutaton phenotype</a></li>' +
    '</ul>' +
    '<div class="tab-content">' +
    '<div class="tab-pane active" id="vareffect' + id + '">' + table + '</div>' +
    '<div class="tab-pane" id="mutphenotype' + id + '">' + table2 + '</div>' +
    '</div>' +
    '</div>' +
    '<div class="modal-footer">' +
    '<button class="btn btn-primary" data-dismiss="modal">Close</button>' +
    '</div>' +
    '</div>' +
    '';

  jQuery(_this.modalDiv).html(html);

  jQuery('#mutationTabs' + id + ' a').click(function (e) {
    e.preventDefault();
    jQuery(this).tab('show');
  });

  jQuery('#mutationModal' + id).modal('show');
};
