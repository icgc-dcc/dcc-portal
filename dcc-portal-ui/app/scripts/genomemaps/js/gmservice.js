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

angular.module('icgc.modules.genomeviewer.service', []);

angular.module('icgc.modules.genomeviewer.service').service('GMService', function ($filter, Consequence) {

  // FIXME: might want to move this else where to centralize all configs
  var config = {
    cellBaseHost: '//www.ebi.ac.uk/cellbase'
  };

  this.getConfiguration = function() {
    return config;
  };

  this.isValidChromosome = function(chromosome) {
    // A quick check decide whether to show GV or not
    if (! isNaN(parseInt(chromosome, 10))) {
      return true;
    } else {
      if (['X', 'Y', 'MT'].indexOf(chromosome) >= 0) {
        return true;
      }
    }
    return false;
  };

  // Format a list of consequences for diplsaying in the tooltip
  // The expected input is of the form: [transcript_id, consequence_type, gene, aa_mutation],
  // this pivot/collapse the struture into a more compact format of consequence_type: [ gene [aa_mutation] ]
  this.tooltipConsequences = function (consequences) {
    var consequenceType, geneSymbol, aaChange, consequenceMap = {}, consequenceList = [],
      consequencesTxt = '', geneList, i;

    // Group into hierarchy, then sort by importance
    for (i = 0; i < consequences.length; i++) {
      consequenceType = consequences[i][1];
      geneSymbol = consequences[i][2];
      aaChange = consequences[i][3];

      if (!consequenceMap.hasOwnProperty(consequenceType)) {
        consequenceMap[consequenceType] = {};
      }

      if (!geneSymbol || geneSymbol === '') {
        continue;
      }

      if (!consequenceMap[consequenceType].hasOwnProperty(geneSymbol)) {
        consequenceMap[consequenceType][geneSymbol] = [];
      }

      if (aaChange && aaChange !== '' && consequenceMap[consequenceType][geneSymbol].indexOf(aaChange) === -1) {
          consequenceMap[consequenceType][geneSymbol].push(aaChange);
      }
    }

    // Sort by consequenceType
    for (var k in consequenceMap) {
      if (consequenceMap.hasOwnProperty(k)) {
        consequenceList.push({
          consequence: k,
          data: consequenceMap[k]
        });
      }
    }
    consequenceList = $filter('orderBy')(consequenceList, function (t) {
      var index = Consequence.precedence().indexOf(t.consequence);
      if (index === -1) {
        return Consequence.precedence().length + 1;
      }
      return index;
    });

    // Dump into html format
    for (i = 0; i < consequenceList.length; i++) {
      consequencesTxt += Consequence.translate(consequenceList[i].consequence);
      geneList = [];
      for (geneSymbol in consequenceList[i].data) {
        if (consequenceList[i].data.hasOwnProperty(geneSymbol)) {
          geneList.push(
            '<em>' + geneSymbol + '</em>' + '&nbsp;' + consequenceList[i].data[geneSymbol].join(', ')
          );
        }
      }

      if (geneList && geneList.length > 0) {
        consequencesTxt += ':&nbsp;';
      }
      consequencesTxt += geneList.join('&nbsp;-&nbsp;');
      consequencesTxt += '<br>';
    }
    return consequencesTxt;
  };

});

