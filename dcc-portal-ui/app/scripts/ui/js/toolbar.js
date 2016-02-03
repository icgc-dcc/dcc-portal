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

angular.module('icgc.ui.toolbar', []).directive('toolbar', function ($filter, $timeout, Page) {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      dl: '@'
    },
    templateUrl: '/scripts/ui/views/toolbar.html',
    link: function (scope) {

      scope.downloadHTMLTable = function(id, type) {
        Page.startExport();

        // There is probably a nicer way to do this..
        // Basically we need to trigger an $apply cycle to generate the hidden table to download,
        // but without clobbering ourselves by calling apply inside apply.
        $timeout(function() {
          scope.downloadHTMLTable2(id, type);
          Page.stopExport();
        }, 1, true);

      };

      scope.downloadHTMLTable2 = function (id, type) {
        var tableData, delimiter, filename;

        // i.e. mutation_2012_04_02_20_56_33.tsv
        filename = id + '_' + $filter('date')(new Date(), 'yyyy_MM_dd_hh_mm_ss') + '.' + type;

        if (type && type === 'tsv') {
          delimiter = '\t';
        } else {
          delimiter = ',';
        }

        tableData = jQuery('#' + id).table2CSV({
          delivery: 'value',
          separator: delimiter
        });


        if (window.Blob && window.File) {
          saveAs(new Blob([tableData], {type: 'text/plain;charset=ascii'}), filename);
        } else {
          // Fallback (IE and other browsers that lack support), create a form and
          // submit a post request to bounce the download content against the server
          jQuery('<form method="POST" id="htmlDownload" action="/api/echo" style="display:none">' +
                 '<input type="hidden" name="fileName" value="' + filename + '"/>' +
                 '<input type="hidden" name="text" value="' + tableData + '"/>' +
                 '<input type="submit" value="Submit"/>' +
                 '</form>').appendTo('body');
          jQuery('#htmlDownload').submit();
          jQuery('#htmlDownload').remove();
        }
      };
    }
  };
});
