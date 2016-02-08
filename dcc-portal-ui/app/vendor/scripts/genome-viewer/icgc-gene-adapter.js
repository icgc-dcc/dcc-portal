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

function IcgcGeneAdapter(args) {

  _.extend(this, Backbone.Events);

  this.host = window.$icgcApp.getQualifiedHost() + '/api/browser';
  this.gzip = true;

  this.setSpecies = function(species) {
    this.species = species;
  };

  this.chunksDisplayed = {};

  this.params = {};
  if (args != null) {

    this.chromosomeLimitMap = args.chromosomeLimitMap || {};

    if (args.host != null) {
      this.host = args.host;
    }
    if (args.species != null) {
      this.species = args.species;
    }
    if (args.category != null) {
      this.category = args.category;
    }
    if (args.subCategory != null) {
      this.subCategory = args.subCategory;
    }
    if (args.resource != null) {
      this.resource = args.resource;
    }
    if (args.featureCache != null) {
      var argsFeatureCache = args.featureCache;
    }
    if (args.params != null) {
      this.params = args.params;
    }
    if (args.filters != null) {
      this.filters = args.filters;
    }
    if (args.options != null) {
      this.options = args.options;
    }
    if (args.featureConfig != null) {
      if (args.featureConfig.filters != null) {
        this.filtersConfig = args.featureConfig.filters;
      }
      if (args.featureConfig.options != null) {
        this.optionsConfig = args.featureConfig.options;
        for (var i = 0; i < this.optionsConfig.length; i++) {
          if (this.optionsConfig[i].checked == true) {
            this.options[this.optionsConfig[i].name] = true;
            this.params[this.optionsConfig[i].name] = true;
          }
        }
      }
    }
  }
  this.featureCache = new FileFeatureCache(argsFeatureCache);
}

IcgcGeneAdapter.prototype.clearData = function () {
  this.featureCache.clear();
};

IcgcGeneAdapter.prototype.setFilters = function (filters) {
  this.clearData();
  this.filters = filters;
  for (filter in filters) {
    var value = filters[filter].toString();
    delete this.params[filter];
    if (value != '') {
      this.params[filter] = value;
    }
  }
};
IcgcGeneAdapter.prototype.setOption = function (opt, value) {
  if (opt.fetch) {
    this.clearData();
  }
  this.options[opt.name] = value;
  for (option in this.options) {
    if (this.options[opt.name] != null) {
      this.params[opt.name] = this.options[opt.name];
    } else {
      delete this.params[opt.name];
    }
  }
};

IcgcGeneAdapter.prototype.getData = function (args) {
  var _this = this;
  var rnd = String.fromCharCode(65 + Math.round(Math.random() * 10));

  var params = {
    histogram: args.histogram,
    interval: args.interval,
    chromosome: args.chromosome,
    resource: args.resource,
    transcript: args.transcript
  }

  var start = (args.start < 1) ? 1 : args.start;
  var end = (args.end > 300000000) ? 300000000 : args.end;

  var dataType = 'data';
  if (params.histogram) {
    dataType = 'histogram' + params.interval;
  }
  if (params.transcript) {
    dataType = 'withTranscripts';
  }

  params['dataType'] = dataType;

  var firstChunk = this.featureCache._getChunk(start);
  var lastChunk = this.featureCache._getChunk(end);
  var chunks = [];
  var itemList = [];
  for (var i = firstChunk; i <= lastChunk; i++) {
    var key = args.chromosome + ':' + i;
    if (this.featureCache.cache[key] == null || this.featureCache.cache[key][dataType] == null) {
      chunks.push(i);
    } else {
      var item = this.featureCache.getFeatureChunk(key);
      itemList.push(item);
    }
  }

  var webServiceCallBack = function (data, segment) {
    var jsonResponse = data[0];

    if (params.histogram == true) {
      jsonResponse = data;

    }

    if (typeof jsonResponse !== 'undefined') {
      for (var i = 0; i < jsonResponse.length; i++) {
        var feature = jsonResponse[i];
        feature.transcripts = feature.transcripts || [];
        feature.name = feature.externalName;
        feature.id = feature.stableId;
        delete feature.externalName;
        delete feature.stableId;

        for (var j = 0; j < feature.transcripts.length; j++) {
          var transcript = feature.transcripts[j];
          transcript.exons = transcript.exonToTranscripts;
          transcript.name = transcript.externalName;
          transcript.id = transcript.stableId;
          transcript.geneId = feature.id;
          transcript.genomicCodingEnd = transcript.codingRegionEnd;
          transcript.genomicCodingStart = transcript.codingRegionStart;
          delete transcript.exonToTranscripts;
          delete transcript.externalName;
          delete transcript.stableId;
          delete transcript.codingRegionEnd;
          delete transcript.codingRegionStart;
          for (var k = 0; k < transcript.exons.length; k++) {
            var exon = transcript.exons[k];
            _.extend(exon, exon.exon);
            delete exon.exon;
            exon.id = exon.stableId;
            exon.geneId = feature.id;
            delete exon.stableId;
          }
        }
      }
    }

    /**/
//        if (params.histogram == true) {
//            _this.featureCache.putFeaturesByRegion(jsonResponse, segment, 'gene', dataType);
//        } else {
    var fc = _this.featureCache._getChunk(segment.start);
    var k = segment.chromosome + ':' + fc;

    if (_this.featureCache.cache[key] == null || _this.featureCache.cache[key][dataType] == null) {
      _this.featureCache.putFeaturesByRegion(jsonResponse || {}, segment, 'gene', dataType);
    }
//        }

    /**/
//        _this.featureCache.putFeaturesByRegion(jsonResponse, segment, 'gene', dataType);
    var items = _this.featureCache.getFeatureChunksByRegion(segment);
    if (items != null) {
      itemList = itemList.concat(items);
    }
    if (itemList.length > 0) {
      _this.trigger('data:ready', {items: itemList, params: params, cached: false, sender: _this});
    }
  };


  var queries = [];
  var updateStart = true;
  var updateEnd = true;
  if (chunks.length > 0) {
    //    console.log(chunks);

    for (var i = 0; i < chunks.length; i++) {

      if (updateStart) {
        var chunkStart = parseInt(chunks[i] * this.featureCache.chunkSize);
        updateStart = false;
      }
      if (updateEnd) {
        var chunkEnd = parseInt((chunks[i] * this.featureCache.chunkSize) + this.featureCache.chunkSize - 1);
        updateEnd = false;
      }

      if (chunks[i + 1] != null) {
        if (chunks[i] + 1 == chunks[i + 1]) {
          updateEnd = true;
        } else {
          var query = args.chromosome + ':' + chunkStart + '-' + Math.min((this.chromosomeLimitMap[args.chromosome] || 0), chunkEnd);
          queries.push(query);
          updateStart = true;
          updateEnd = true;
        }
      } else {
        var query = args.chromosome + ':' + chunkStart + '-' + Math.min((this.chromosomeLimitMap[args.chromosome] || 0), chunkEnd);
        queries.push(query);
        updateStart = true;
        updateEnd = true;
      }
    }
    //    console.log(querys);
    console.time(_this.resource + ' get and save ' + rnd);

    var queryCount = queries.length;
    for (var i = 0; i < queryCount; i++) {
      this._callWebService (queries [i], webServiceCallBack, params);
    }
  } else {
    if (itemList.length > 0) {
      _this.trigger('data:ready', {items: itemList, params: params, sender: this});
    }
  }
};

IcgcGeneAdapter.prototype._callWebService = function (segmentString, callback, params) {
//    https://dcc.icgc.org/api/browser/gene?segment=17:7450000-7699999&resource=gene&dataType=withTranscripts
  var callParams = {
    segment: segmentString,
    resource: this.resource,
    histogram: params.histogram,
    interval: params.interval,
    dataType: params.dataType
  };
  var url = this.host + '/' + this.resource + this._getQuery(callParams);
  $.ajax({
    type: 'GET',
    url: url,
    dataType: 'json',//still firefox 20 does not auto serialize JSON, You can force it to always do the parsing by adding dataType: 'json' to your call.
    async: true,
    success: function (data) {
      var region = new Region(segmentString);
      callback(data, region);
    },
    error: function(e) {
      var region = new Region(segmentString);
      callback({}, region);
    }
  });
};

IcgcGeneAdapter.prototype._getQuery = function (paramsWS) {
  var query = "";
  for (var key in paramsWS) {
    if (paramsWS[key] != null)
      query += key + '=' + paramsWS[key] + '&';
  }
  if (query != '')
    query = "?" + query.slice(0, -1);
  return query;
};
