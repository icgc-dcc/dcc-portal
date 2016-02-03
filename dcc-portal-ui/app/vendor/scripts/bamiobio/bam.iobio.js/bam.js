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
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// bam.js: indexed binary alignments
//
'use strict';

var BAM_MAGIC = 21840194;
var BAI_MAGIC = 21578050;

function BamFile() {
}

function Vob(b, o) {
    this.block = b;
    this.offset = o;
}

Vob.prototype.toString = function() {
    return '' + this.block + ':' + this.offset;
}

function Chunk(minv, maxv) {
    this.minv = minv; this.maxv = maxv;
}

function makeBam(data, bai, callback) {
    var bam = new BamFile();
    bam.data = data;
    bam.bai = bai;

    bam.data.slice(0, 65536).fetch(function(r) {
        if (!r) {
            return console.log('Couldn\'t access BAM');
        }

        var unc = unbgzf(r);
        var uncba = new Uint8Array(unc);

        var magic = readInt(uncba, 0);
        var headLen = readInt(uncba, 4);
        var header = '';
        for (var i = 0; i < headLen; ++i) {
            header += String.fromCharCode(uncba[i + 8]);
        }
        bam.header = header;
        var nRef = readInt(uncba, headLen + 8);
        var p = headLen + 12;

        bam.chrToIndex = {};
        bam.indexToChr = [];
        for (var i = 0; i < nRef; ++i) {
            var lName = readInt(uncba, p);
            var name = '';
            for (var j = 0; j < lName-1; ++j) {
                name += String.fromCharCode(uncba[p + 4 + j]);
            }
            var lRef = readInt(uncba, p + lName + 4);
            // dlog(name + ': ' + lRef);
            bam.chrToIndex[name] = i;
            if (name.indexOf('chr') == 0) {
                bam.chrToIndex[name.substring(3)] = i;
            } else {
                bam.chrToIndex['chr' + name] = i;
            }
            bam.indexToChr.push(name);

            p = p + 8 + lName;
        }

        if (bam.indices) {
            return callback(bam);
        }
    });

    bam.bai.fetch(function(header) {   // Do we really need to fetch the whole thing? :-(
        if (!header) {
            return dlog("Couldn't access BAI");
        }

        var uncba = new Uint8Array(header);
        var baiMagic = readInt(uncba, 0);
        if (baiMagic != BAI_MAGIC) {
            return dlog('Not a BAI file');
        }

        var nref = readInt(uncba, 4);

        bam.indices = [];

        var p = 8;
        for (var ref = 0; ref < nref; ++ref) {
            var blockStart = p;
            var nbin = readInt(uncba, p); p += 4;
            for (var b = 0; b < nbin; ++b) {
                var bin = readInt(uncba, p);
                var nchnk = readInt(uncba, p+4);
                p += 8 + (nchnk * 16);
            }
            var nintv = readInt(uncba, p); p += 4;
            p += (nintv * 8);
            if (nbin > 0) {
                bam.indices[ref] = new Uint8Array(header, blockStart, p - blockStart);
            }                     
        }
        if (bam.chrToIndex) {
            return callback(bam);
        }
    });
}



BamFile.prototype.blocksForRange = function(refId, min, max) {
    var index = this.indices[refId];
    if (!index) {
        return [];
    }

    var intBinsL = reg2bins(min, max);
    var intBins = [];
    for (var i = 0; i < intBinsL.length; ++i) {
        intBins[intBinsL[i]] = true;
    }
    var leafChunks = [], otherChunks = [];

    var nbin = readInt(index, 0);
    var p = 4;
    for (var b = 0; b < nbin; ++b) {
        var bin = readInt(index, p);
        var nchnk = readInt(index, p+4);
        p += 8;
        if (intBins[bin]) {
            for (var c = 0; c < nchnk; ++c) {
                var cs = readVob(index, p);
                var ce = readVob(index, p + 8);
                (bin < 4681 ? otherChunks : leafChunks).push(new Chunk(cs, ce));
                p += 16;
            }
        } else {
            p +=  (nchnk * 16);
        }
    }

    var nintv = readInt(index, p);
    var lowest = null;
    var minLin = Math.min(min>>14, nintv - 1), maxLin = Math.min(max>>14, nintv - 1);
    for (var i = minLin; i <= maxLin; ++i) {
        var lb =  readVob(index, p + 4 + (i * 8));
        if (!lb) {
            continue;
        }
        if (!lowest || lb.block < lowest.block || lb.offset < lowest.offset) {
            lowest = lb;
        }
    }
    // dlog('Lowest LB = ' + lowest);
    
    var prunedOtherChunks = [];
    if (lowest != null) {
        for (var i = 0; i < otherChunks.length; ++i) {
            var chnk = otherChunks[i];
            if (chnk.maxv.block >= lowest.block && chnk.maxv.offset >= lowest.offset) {
                prunedOtherChunks.push(chnk);
            }
        }
    }
    // dlog('prunedOtherChunks = ' + miniJSONify(prunedOtherChunks));
    otherChunks = prunedOtherChunks;

    var intChunks = [];
    for (var i = 0; i < otherChunks.length; ++i) {
        intChunks.push(otherChunks[i]);
    }
    for (var i = 0; i < leafChunks.length; ++i) {
        intChunks.push(leafChunks[i]);
    }

    intChunks.sort(function(c0, c1) {
        var dif = c0.minv.block - c1.minv.block;
        if (dif != 0) {
            return dif;
        } else {
            return c0.minv.offset - c1.minv.offset;
        }
    });
    var mergedChunks = [];
    if (intChunks.length > 0) {
        var cur = intChunks[0];
        for (var i = 1; i < intChunks.length; ++i) {
            var nc = intChunks[i];
            if (nc.minv.block == cur.maxv.block /* && nc.minv.offset == cur.maxv.offset */) { // no point splitting mid-block
                cur = new Chunk(cur.minv, nc.maxv);
            } else {
                mergedChunks.push(cur);
                cur = nc;
            }
        }
        mergedChunks.push(cur);
    }
//    dlog('mergedChunks = ' + miniJSONify(mergedChunks));

    return mergedChunks;
}

BamFile.prototype.fetch = function(chr, min, max, callback, options) {
    var thisB = this;

    var chrId = this.chrToIndex[chr];
    var chunks;
    if (chrId === undefined) {
        chunks = [];
    } else {
        chunks = this.blocksForRange(chrId, min, max);
        if (!chunks) {
            callback(null, 'Error in index fetch');
        }
    }
    
    var records = [];
    var index = 0;
    var data;
    var binData = "";

    function tramp() {
        if (index >= chunks.length) {
            if (options && options.raw) {
               return callback(binData);
            } else if (options && options.format) {
               var converted = ""
               records.forEach(function(r) { converted += r.convertTo(options.format); });
               return callback(converted);
            } else {
               return callback(records);
            }
        } else if (!data) {
            // dlog('fetching ' + index);
            var c = chunks[index];
            var fetchMin = c.minv.block;
            var fetchMax = c.maxv.block + (1<<16); // *sigh*
            thisB.data.slice(fetchMin, fetchMax - fetchMin).fetch(function(r) {
                if (options && options.raw)
                   data = r;
                else
                   data = unbgzf(r, c.maxv.block - c.minv.block + 1);
                return tramp();
            }, options);
        } else {
            if (options && options.raw) {
               binData += data;
            } else {
               var ba = new Uint8Array(data);
               thisB.readBamRecords(ba, chunks[index].minv.offset, records, min, max, chrId);
            }
            data = null;
            ++index;
            return tramp();
        }
    }
    tramp();
}

var SEQRET_DECODER = ['=', 'A', 'C', 'x', 'G', 'x', 'x', 'x', 'T', 'x', 'x', 'x', 'x', 'x', 'x', 'N'];
var CIGAR_DECODER = ['M', 'I', 'D', 'N', 'S', 'H', 'P', '=', 'X', '?', '?', '?', '?', '?', '?', '?'];

function BamRecord() {
   
}

BamRecord.prototype.convertTo = function(format) {
 var record = "";
 var keys = [ "readName", "flag", "segment", "pos", "mq", "cigar", "rnext", "pnext", "tlen", "seq", "quals", "tags"   ]
 
 if (format == "sam") {
    for (var i=0; i < keys.length; i++)
        record += this[keys[i]] + '\t'
      // if (keys[i] == 'rnext')
      //   record += this.indexToChr[ keys['rnext']-1 ] + '\t';
      // else
      //   record += this[keys[i]] + '\t'
    record = record.slice(0, record.length-1).trim() + "\n";    
 }  
 
 return record;
}

BamFile.prototype.readBamRecords = function(ba, offset, sink, min, max, chrId) {;
    while (true) {
        var blockSize = readInt(ba, offset);
        var blockEnd = offset + blockSize + 4;
        if (blockEnd >= ba.length) {                     
            return sink;
        }

        var record = new BamRecord();

        var refID = readInt(ba, offset + 4);
        var pos = readInt(ba, offset + 8);
        
        var bmn = readInt(ba, offset + 12);
        var bin = (bmn & 0xffff0000) >> 16;
        var mq = (bmn & 0xff00) >> 8;
        var nl = bmn & 0xff;

        var flag_nc = readInt(ba, offset + 16);
        var flag = (flag_nc & 0xffff0000) >> 16;
        var nc = flag_nc & 0xffff;
        
        record.flag = flag;
    
        var lseq = readInt(ba, offset + 20);
        
        var nextRef  = readInt(ba, offset + 24);
        var nextPos = readInt(ba, offset + 28);
        
        record.rnext = this.indexToChr[ nextRef ];
        record.pnext = nextPos;
        
        var tlen = readInt(ba, offset + 32);
        record.tlen = tlen;
    
        var readName = '';
        for (var j = 0; j < nl-1; ++j) {
            readName += String.fromCharCode(ba[offset + 36 + j]);
        }
    
        var p = offset + 36 + nl;

        var cigar = '';
        for (var c = 0; c < nc; ++c) {
            var cigop = readInt(ba, p);
            cigar = cigar + (cigop>>4) + CIGAR_DECODER[cigop & 0xf];
            p += 4;
        }
        record.cigar = cigar;
    
        var seq = '';
        var seqBytes = (lseq + 1 ) >> 1;
        for (var j = 0; j < seqBytes; ++j) {
            var sb = ba[p + j];
            seq += SEQRET_DECODER[(sb & 0xf0) >> 4];
            seq += SEQRET_DECODER[(sb & 0x0f)];
        }
        // acount for odd sequences and remove last character
        // this is needed b\c each character is a half byte and we are taking a byte at a time
        if (lseq % 2 == 1) seq = seq.slice(0, seq.length-1);
        p += seqBytes;
        record.seq = seq;

        var qseq = '';
        for (var j = 0; j < lseq; ++j) {
            qseq += String.fromCharCode(ba[p + j] + 33);
        }
        p += lseq;
        record.quals = qseq;
        
        record.pos = pos;
        record.mq = mq;
        record.readName = readName;
        record.segment = this.indexToChr[refID];
        record.tags = "";

        while (p < blockEnd) {
            var tag = String.fromCharCode(ba[p]) + String.fromCharCode(ba[p + 1]);
            var type = String.fromCharCode(ba[p + 2]);
            var value;

            if (type == 'A') {
                value = String.fromCharCode(ba[p + 3]);
                p += 4;
            } else if (type == 'i' || type == 'I') {
                value = readInt(ba, p + 3);                
                p += 7;
            } else if (type == 'c' || type == 'C') {
                value = ba[p + 3];
                type = "i";
                p += 4;
            } else if (type == 's' || type == 'S') {
                value = readShort(ba, p + 3);
                type = "i";
                p += 5;
            } else if (type == 'f') {
                throw 'FIXME need floats';
            } else if (type == 'Z') {
                p += 3;
                value = '';
                for (;;) {
                    var cc = ba[p++];
                    if (cc == 0) {
                        break;
                    } else {
                        value += String.fromCharCode(cc);
                    }
                }
            } else {
                throw 'Unknown type '+ type;
            }
            record[tag] = value;
            record.tags += tag + ":" + type + ":" + value + "\t";
        }
        record.tags = record.tags.trim();

        if (!min || record.pos <= max && record.pos + lseq >= min) {
            if (chrId === undefined || refID == chrId) {
                sink.push(record);
            }
        }
        offset = blockEnd;
    }

}

function readInt(ba, offset) {
    return (ba[offset + 3] << 24) | (ba[offset + 2] << 16) | (ba[offset + 1] << 8) | (ba[offset]);
}

function readShort(ba, offset) {
    return (ba[offset + 1] << 8) | (ba[offset]);
}

function readVob(ba, offset) {
    var block = ((ba[offset+6] & 0xff) * 0x100000000) + ((ba[offset+5] & 0xff) * 0x1000000) + ((ba[offset+4] & 0xff) * 0x10000) + ((ba[offset+3] & 0xff) * 0x100) + ((ba[offset+2] & 0xff));
    var bint = (ba[offset+1] << 8) | (ba[offset]);
    return new Vob(block, bint);
}

//
// Binning (transliterated from SAM1.3 spec)
//

/* calculate bin given an alignment covering [beg,end) (zero-based, half-close-half-open) */
function reg2bin(beg, end)
{
    --end;
    if (beg>>14 == end>>14) return ((1<<15)-1)/7 + (beg>>14);
    if (beg>>17 == end>>17) return ((1<<12)-1)/7 + (beg>>17);
    if (beg>>20 == end>>20) return ((1<<9)-1)/7 + (beg>>20);
    if (beg>>23 == end>>23) return ((1<<6)-1)/7 + (beg>>23);
    if (beg>>26 == end>>26) return ((1<<3)-1)/7 + (beg>>26);
    return 0;
}

/* calculate the list of bins that may overlap with region [beg,end) (zero-based) */
var MAX_BIN = (((1<<18)-1)/7);
function reg2bins(beg, end) 
{
    var i = 0, k, list = [];
    --end;
    list.push(0);
    for (k = 1 + (beg>>26); k <= 1 + (end>>26); ++k) list.push(k);
    for (k = 9 + (beg>>23); k <= 9 + (end>>23); ++k) list.push(k);
    for (k = 73 + (beg>>20); k <= 73 + (end>>20); ++k) list.push(k);
    for (k = 585 + (beg>>17); k <= 585 + (end>>17); ++k) list.push(k);
    for (k = 4681 + (beg>>14); k <= 4681 + (end>>14); ++k) list.push(k);
    return list;
}