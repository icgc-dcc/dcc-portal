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
// extending Thomas Down's original BAM js work
'use strict';

var Bam = Class.extend({

  init: function (bamUri, options) {
    this.bamUri = bamUri;
    this.options = options; // *** add options mapper ***
    // test if file or url

    this.sourceType = 'dcc';

    // set iobio servers
    this.iobio = {};
    var currentHost = 'iobio.icgc.org';
    this.iobio.samHeader = "wss://" + currentHost + "/samheader/";
    this.iobio.bamReadDepther = "wss://" + currentHost + "/bamreaddepther/";
    this.iobio.bamstatsAlive = "wss://" + currentHost + "/bamstatsalive/";
    
    this.sampleClient = undefined;
    
    return this;
  },

  fetch: function (name, start, end, callback, options) {
    var me = this;
    // handle bam has been created yet
    if (this.bam === undefined) {// **** TEST FOR BAD BAM ***
      this.promise(function () {
        me.fetch(name, start, end, callback, options);
      });
    } else {
      this.bam.fetch(name, start, end, callback, options);
    }
  },

  promise: function (callback) {
    this.promises.push(callback);
  },

  provide: function (bam) {
    this.bam = bam;
    while (this.promises.length !== 0) {
      this.promises.shift()();
    }
  },

  _makeid: function () {
    // make unique string id;
    var text = '';
    var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

    for (var i = 0; i < 5; i++) {
      text += possible.charAt(Math.floor(Math.random() * possible.length));
    }

    return text;
  },

  _generateExomeBed: function (id) {
    var bed = '';
    var readDepth = this.readDepth[id];
    var start, end;
    
    for (var i = 0; i < readDepth.length; i++) {
      if (readDepth[i].depth < 20) {
        if (start !== undefined) {
          bed += id + '\t' + start + '\t' + end + '\t.\t.\t.\t.\t.\t.\t.\t.\t.\n';
        }
        start = undefined;
      } else {
        if (start === undefined) {
          start = readDepth[i].pos;
        }
        end = readDepth[i].pos + 16384;
      }
    }
    // add final record if data stopped on non-zero
    if (start !== undefined) {
      bed += id + '\t' + start + '\t' + end + '\t.\t.\t.\t.\t.\t.\t.\t.\t.\n';
    }
    return bed;
  },

  _tmp: function (ref, regions, bed) {
    var me = this;
    var bedRegions = [];
    var a = this._bedToCoordinateArray(ref, bed);
    regions.forEach(function (reg) {
      var start = reg.start;
      var length = reg.end - reg.start;
      var ci = me._getClosestValueIndex(a, reg.start); // update lo start value
      var maxci = a.length;
      while (length > 0 && ci < maxci) {
        var newStart, newEnd;

        // determine start position
        if (a[ci].start <= start) {
          newStart = start;
        } else {
          newStart = a[ci].start;
        }

        // determine end position
        if (a[ci].end >= newStart + length) {
          newEnd = newStart + length;
        } else {
          newEnd = a[ci].end;
          ci += 1;
        }

        // update length left to sample
        length -= newEnd - newStart;
        // push new regions
        bedRegions.push({
          name: reg.name,
          'start': newStart,
          'end': newEnd
        });
      }
    });
    return bedRegions;
  },

  _mapToBedCoordinates: function (ref, regions, bed) {
    var a = this._bedToCoordinateArray(ref, bed);
    var a_i = 0;
    var bedRegions = [];
    if (a.length === 0) {
      console.log('Bed file doesn\'t have coordinates for reference: ' + regions[0].name + '. Sampling normally');
      return null;
    }
    regions.forEach(function (reg) {
      for (a_i; a_i < a.length; a_i++) {
        if (a[a_i].end > reg.end) {
          break;
        }

        if (a[a_i].start >= reg.start) {
          bedRegions.push({
            name: reg.name,
            start: a[a_i].start,
            end: a[a_i].end
          });
        }
      }
    });
    return bedRegions;
  },

  _bedToCoordinateArray: function (ref, bed) {
    var me = this;
    var a = [];
    bed.split('\n').forEach(function (line) {
      if (line[0] === '#' || line === '') {
        return;
      }

      var fields = line.split('\t');
      if (me._referenceMatchesBed(ref, fields[0])) {
        a.push({
          chr: ref,
          start: parseInt(fields[1]),
          end: parseInt(fields[2])
        });
      }
    });
    return a;
  },

  _referenceMatchesBed: function (ref, bedRef) {
    if (ref === bedRef) {
      return true;
    }
    // Try stripping chr from reference names and then comparing
    var ref1 = ref.replace(/^chr?/, '');
    var bedRef1 = bedRef.replace(/^chr?/, '');

    return (ref1 === bedRef1);
  },

  _getClosestValueIndex: function (a, x) {
    var lo = -1,
      hi = a.length;
    while (hi - lo > 1) {
      var mid = Math.round((lo + hi) / 2);
      if (a[mid].start <= x) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    if (lo === -1) {
      return 0;
    }
    if (a[lo].end > x) {
      return lo;
    } else {
      return hi;
    }
  },

  // *** bamtools functionality ***

  convert: function (format, name, start, end, callback, options) {
    // Converts between BAM and a number of other formats
    if (!format || !name || !start || !end) {
      return 'Error: must supply format, sequenceid, start nucleotide and end nucleotide';
    }

    if (format.toLowerCase() !== 'sam') {
      return 'Error: format + " + options.format + " is not supported';
    }
    var me = this;
    this.fetch(name, start, end, function (data, e) {
      if (options && options.noHeader) {
        callback(data, e);
      } else {
        me.getHeader(function (h) {
          callback(h.toStr + data, e);
        });
      }
    }, {
      'format': format
    });
  },

  count: function () {
    // Prints number of alignments in BAM file(s)
  },

  coverage: function () {
    // Prints coverage statistics from the input BAM file
  },

  filter: function () {
    // Filters BAM file(s) by user-specified criteria
  },

  //_lastUpdateTimer: null,

  estimateBaiReadDepth: function (callback) {
    var me = this,
    readDepth = {};
    me.readDepth = {};

    function cb() {
      if (me.header) {
        for (var id in readDepth) {
          if (readDepth.hasOwnProperty(id)) {
            var name = me.header.sq[parseInt(id)].name;
            if (me.readDepth[name] == undefined) {
              me.readDepth[name] = readDepth[id];
              callback(name, readDepth[id]);
            }
          }
        }
      }
    }

    me.getHeader(function () {
      if (Object.keys(me.readDepth).length > 0) {
        cb();
      }
    });

    if (Object.keys(me.readDepth).length > 0) {
      callback(me.readDepth);
    }

    else if (me.sourceType === 'dcc') {
      var client = new BinaryClient(me.iobio.bamReadDepther);
      var url = encodeURI(me.iobio.bamReadDepther + '?cmd=' + me.bamUri + ".bai")
      client.on('open', function (stream) {
        stream = client.createStream({
          event: 'run',
          params: {
            'url': url
          }
        });
        var currentSequence;
        stream.on('data', function (data) {
          data = data.split('\n');
          for (var i = 0; i < data.length; i++) {
            if (data[i][0] === '#') {
              if (Object.keys(readDepth).length > 0) {
                cb();
              }
              currentSequence = data[i].substr(1);
              readDepth[currentSequence] = [];
            } else {
              if (data[i] !== '') {
                var d = data[i].split('\t');
                readDepth[currentSequence].push({
                  pos: parseInt(d[0]),
                  depth: parseInt(d[1])
                });
              }
            }
          }
        });
        stream.on('end', function () {
          cb();
        });
      });
    }
  },

  getHeader: function (callback) {
    var me = this;
    if (me.header) {
      callback(me.header);
    } else {
      var client = BinaryClient(me.iobio.samHeader);
      var url = encodeURI(me.iobio.samHeader + '?cmd= ' + this.bamUri);
      client.on('open', function (stream) {
        stream = client.createStream({
          event: 'run',
          params: {
            'url': url
          }
        });
        var rawHeader = '';
        stream.on('data', function (data, options) {
          rawHeader += data;
        });
        stream.on('end', function () {
          me.setHeader(rawHeader);
          callback(me.header);
        });
      });
    }

    // need to make this work for URL bams
    // need to incorporate real promise framework throughout
  },

  setHeader: function (headerStr) {
    var header = {
      sq: [],
      toStr: headerStr
    };
    var lines = headerStr.split('\n');
    for (var i = 0; i < lines.length > 0; i++) {
      var fields = lines[i].split("\t");
      if (fields[0] === '@SQ') {
        var fHash = {};
        fields.forEach(function (field) {
          var values = field.split(':');
          fHash[values[0]] = values[1];
        });
        header.sq.push({
          name: fHash.SN,
          end: 1 + parseInt(fHash.LN)
        });
      }
    }
    this.header = header;
  },

  index: function () {
    // Generates index for BAM file
  },

  merge: function () {
    // Merge multiple BAM files into single file
  },

  random: function () {
    // Select random alignments from existing BAM file(s), intended more as a testing tool.
  },

  resolve: function () {
    // Resolves paired-end reads (marking the IsProperPair flag as needed)
  },

  revert: function () {
    // Removes duplicate marks and restores original base qualities
  },

  sort: function () {
    // Sorts the BAM file according to some criteria
  },

  split: function () {
    // Splits a BAM file on user-specified property, creating a new BAM output file for each value found
  },

  stats: function (name, start, end, callback) {
    // Removed
  },

  sampleStats: function (callback, options) {
    // Prints some basic statistics from sampled input BAM file(s)      
    options = $.extend({
      binSize: 20000, // defaults
      binNumber: 20,
      start: 1,
    }, options);
    var me = this;

    function goSampling(SQs) {
      var regions = [];
      var bedRegions;
      for (var j = 0; j < SQs.length; j++) {
        var sqStart = options.start;
        var length = SQs[j].end - sqStart;
        if (length < options.binSize * options.binNumber) {
          SQs[j].start = sqStart;
          regions.push(SQs[j]);
        } else {
          // create random reference coordinates
          var regions = [];
          for (var i = 0; i < options.binNumber; i++) {
            var s = sqStart + parseInt(Math.random() * length);
            regions.push({
              'name': SQs[j].name,
              'start': s,
              'end': s + options.binSize
            });
          }
          // sort by start value
          regions = regions.sort(function (a, b) {
            var x = a.start;
            var y = b.start;
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
          });

          // intelligently determine exome bed coordinates
          if (options.exomeSampling) {
            options.bed = me._generateExomeBed(options.sequenceNames[0]);
          }

          // map random region coordinates to bed coordinates
          if (options.bed !== undefined) {
            bedRegions = me._mapToBedCoordinates(SQs[0].name, regions, options.bed);
          }
        }
      }

      if (me.sampleClient !== undefined) {
        me.sampleClient.close(1000);
      }
      me.sampleClient = new BinaryClient(me.iobio.bamstatsAlive);
      var regStr = JSON.stringify((bedRegions || regions).map(function (d) {
        return {
          start: d.start,
          end: d.end,
          chr: d.name
        };
      }));
      
      var url = encodeURI( me.iobio.bamstatsAlive + '?cmd=\'' + regStr + '\' ' + me.bamUri);
      var buffer = '';
         me.sampleClient.on('open', function(stream){
            stream = me.sampleClient.createStream({event:'run', params : {'url':url}});
            stream.on('error', function(err) {
              console.log(err);
            });
        stream.on('data', function (datas) {
          datas.split(';').forEach(function (data) {
            if (data === undefined || data === '\n') {
              return;
            }
            var success = true;
            var obj;
            try {
              obj = JSON.parse(buffer + data);
            } catch (e) {
              success = false;
              buffer += data;
            }
            if (success) {
              buffer = '';
              callback(obj, options.sequenceNames[0]);
            }
          });
        });

        stream.on('end', function () {
          if (options.onEnd !== undefined) {
            options.onEnd();
          }
        });
      });
    }

    if (options.sequenceNames !== undefined && options.sequenceNames.length === 1 && options.end !== undefined) {
      goSampling([{
        name: options.sequenceNames[0],
        end: options.end
      }]);
    } else if (options.sequenceNames !== undefined && options.sequenceNames.length === 1) {
      this.getHeader(function (header) {
        var sq;
        $(header.sq).each(function (i, d) {
          if (d.name === options.sequenceNames[0]) {
            sq = d;
          }
        });
        goSampling([sq]);
      });
    } else {
      this.getHeader(function (header) {
        goSampling(header.sq);
      });
    }
  }

});