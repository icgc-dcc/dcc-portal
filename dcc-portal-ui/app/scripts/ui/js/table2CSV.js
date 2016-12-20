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

/** 
 * Adapated to work with ICGC table double header format 
 */
jQuery.fn.table2CSV = function(opts) {
  var options = jQuery.extend({
        separator: ',',
        header: [],
        delivery: 'popup' // popup, value
      },
    opts);

  var csvData = [];
  var el = this;

  //header
  var numCols = options.header.length;
  var tmpRow = []; // construct header avalible array

  if (numCols > 0) {
    for (var i = 0; i < numCols; i++) {
      tmpRow[tmpRow.length] = formatData(options.header[i]);
    }
  } else {

    // portal-ui: support 2-level table header, dependent on the use of subhead class
    var subQueue = [];
    var idx = -1;
    jQuery(el).find('tr').each(function() {
      // Second level processing
      if (jQuery(this).hasClass('subhead')) {
        jQuery(this).find('th').each(function() {
          if (subQueue.length > 0) {
            idx = subQueue.splice(0, 1);
            tmpRow[idx] += ' ' + formatData(jQuery(this).html());
          }
        });
      } else {
        // Top level processing
        jQuery(this).find('th').each(function() {
          if (jQuery(this).attr('colspan')) {
            var cols = jQuery(this).attr('colspan');
            for (var i=0; i < cols; i++) {
              subQueue.push( tmpRow.length );
              tmpRow[tmpRow.length] = formatData(jQuery(this).html());
            }
          } else {
            tmpRow[tmpRow.length] = formatData(jQuery(this).html());
          }
        });
      }
    });
  }

  row2CSV(tmpRow);

  // actual data
  jQuery(el).find('tr').each(function() {
    var tmpRow = [];
    jQuery(this).find('td').each(function() {
      tmpRow[tmpRow.length] = formatData(jQuery(this).html());
    });
    row2CSV(tmpRow);
  });

  var mydata = '';

  if (options.delivery ==='popup') {
    mydata = csvData.join('\n');
    return popup(mydata);
  }
  else {
    mydata = csvData.join('\n');
    return mydata;
  }

  function row2CSV(tmpRow) {
    var tmp = tmpRow.join(''); // to remove any blank rows
    if (tmpRow.length > 0 && tmp !== '') {
      csvData[csvData.length] = tmpRow.join(options.separator);
    }
  }
  function formatData(input) {
    var regexp, output;

    // replace " with â€œ
    regexp = new RegExp('"', 'g');
    output = input.replace(regexp, 'â€œ');


    //HTML
    regexp = new RegExp('<[^<]+>', 'g');
    output = output.replace(regexp, '');

    // portal-ui: additional formatting
    // - Remove non-breaking space
    // - Decode symbols (i.e. <, >) 
    // - Trim leading/trailing spaces
    // - Trim internal spaces
    output = output.replace(/&nbsp;/g, '');
    output = output.replace('&lt;', '<');
    output = output.replace('&gt;', '>');
    output = output.replace(/^\s+|\s+$/g, '');
    output = output.replace(/\n/g, '');
    output = output.replace(/\s+/g, ' ');

    if (output === '') {
      return output;
    }

    // Do not wrap in quotes if format is tsv
    if (options.separator === '\t') {
      return output;
    }

    return '"' + output + '"';
  }
  function popup(data) {
    var generator = window.open('', 'csv', 'height=400,width=600');
    generator.document.write('<html><head><title>CSV</title>');
    generator.document.write('</head><body >');
    generator.document.write('<textArea cols=70 rows=15 wrap="off" >');
    generator.document.write(data);
    generator.document.write('</textArea>');
    generator.document.write('</body></html>');
    generator.document.close();
    return true;
  }
};
