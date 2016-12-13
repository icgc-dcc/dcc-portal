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

(function () {
  'use strict';

  var module = angular.module('icgc.oncogrid.services', []);

  module.service('OncogridService', function (Donors, Genes, Occurrences, Consequence, $q, $filter, gettextCatalog) {

    var _srv = this;

    /**
     * Data Retrieval for OncoGrid
     */

    _srv.getDonors = function (donorSet) {
      return Donors.getAll({
        filters: _srv.donorFilter(donorSet),
        from: 1,
        size: 100
      });
    };

    _srv.getGenes = function (geneSet) {
      return Genes.getAll({
        filters: _srv.geneFilter(geneSet),
        from: 1,
        size: 100
      });
    };

    _srv.getCuratedSet = function (geneSet) {
      return Genes.getAll({
        filters: _srv.curatedFilter(geneSet),
        from: 1,
        size: 100
      });
    };

    _srv.getOccurences = function (donorSet, geneSet) {
      return Occurrences.getAll({
        filters: _srv.observationFilter(donorSet, geneSet),
        from: 1,
        size: 100
      });
    };

    _srv.donorFilter = function (donorSet) {
      var filter = {
        donor: {
          id: {
            is: ['ES:' + donorSet]
          }
        }
      };

      return filter;
    };

    _srv.geneFilter = function (geneSet) {
      var filter = {
        gene: {
          id: {
            is: ['ES:' + geneSet]
          }
        }
      };

      return filter;
    };

    _srv.curatedFilter = function (geneSet) {
      var filter = {
        gene: {
          id: {
            is: ['ES:' + geneSet]
          },
          curatedSetId: {
            is: ['GS1']
          }
        }
      };

      return filter;
    };

    _srv.observationFilter = function (donorSet, geneSet) {
      var filter = {
        donor: {
          id: {
            is: ['ES:' + donorSet]
          }
        },
        gene: {
          id: {
            is: ['ES:' + geneSet]
          }
        },
        mutation: {
          functionalImpact: {
            is: ['High']
          }
        }
      };

      return filter;
    };

    /**
     * Data Mapping for OncoGrid 
     */

    _srv.mapDonors = function (donors) {
      return _.map(donors, function (d) {
        return {
          'id': d.id,
          'age': (d.ageAtDiagnosis === undefined ? -777 : d.ageAtDiagnosis),
          'sex': (d.gender === undefined ? 'unknown' : d.gender),
          'vitalStatus': (d.vitalStatus === undefined ? false : (d.vitalStatus === 'alive' ? true : false)),
          'survivalTime': (d.survivalTime === undefined ? -777 : d.survivalTime),
          'pcawg': _.has(d, 'studies') && d.studies.indexOf('PCAWG') >= 0,
          'cnsmExists': d.cnsmExists,
          'stsmExists': d.stsmExists,
          'sgvExists': d.sgvExists,
          'methArrayExists': d.methArrayExists,
          'methSeqExists': d.methSeqExists,
          'expArrayExists': d.expArrayExists,
          'expSeqExists': d.expSeqExists,
          'pexpExists': d.pexpExists,
          'mirnaSeqExists': d.mirnaSeqExists,
          'jcnExists': d.jcnExists
        };
      });
    };

    _srv.mapGenes = function (genes, curatedList) {
      return _.map(genes, function (g) {
        return {
          'id': g.id,
          'symbol': g.symbol,
          'totalDonors': g.affectedDonorCountTotal,
          'cgc': curatedList.indexOf(g.id) >= 0
        };
      });
    };

    _srv.mapOccurences = function(occurrences, donors, genes) {
      var donorIds = _.map(donors, function (g) { return g.id });
      var geneIds = _.map(genes, function (d) { return d.id });

      var geneIdToSymbol = {};
      _(genes).forEach(function(g) {
        geneIdToSymbol[g.id] = g.symbol;
      }).value();

      function validOnco(o) {
        return geneIds.indexOf(o.geneId) >= 0 && donorIds.indexOf(o.donorId) >= 0 && o.functionalImpact === 'High';
      }

      function toOnco(o) {
        return {
          id: o.mutationId,
          donorId: o.donorId,
          geneId: o.geneId,
          geneSymbol: geneIdToSymbol[o.geneId],
          consequence: o.consequence.consequenceType,
          functionalImpact: o.consequence.functionalImpact
        };
      }

      return _(occurrences)
            .map(expandObs)
            .flatten()
            .map(toOnco)
            .filter(validOnco)
            .value();
    };

    _srv.icgcLegend = function(max) {
      var value = '<b>' + gettextCatalog.getString('# of Donors Affected') + ':</b> </br>' + 
              '0 <div class="onco-track-legend onco-total-donor-legend" style="opacity:0.1"></div>' + 
              '<div class="onco-track-legend onco-total-donor-legend" style="opacity:0.4"></div>' + 
              '<div class="onco-track-legend onco-total-donor-legend" style="opacity:0.7"></div>' +
              '<div class="onco-track-legend onco-total-donor-legend" style="opacity:1"></div>' +
              max;

      return value;
    };

    _srv.geneSetLegend = function() {
      var value = '<b> ' + gettextCatalog.getString('Gene Sets') + ': </b> <br>' + 
              '<div class="onco-track-legend onco-cgc-legend"></div> '+
              gettextCatalog.getString('Gene belongs to Cancer Gene Census');
  
      return value;
    };

    _srv.clinicalLegend = function(maxSurvival) {
      var value =  '<b>' + gettextCatalog.getString('Clinical Data') + ':</b> <br>' +
      '<b>' + gettextCatalog.getString('Age at Diagnosis (years)') + ': </b> ' + 
        '0 <div class="onco-track-legend onco-age-legend" style="opacity:0.05"></div>' +
        '<div class="onco-track-legend onco-age-legend" style="opacity:0.4"></div>' + 
        '<div class="onco-track-legend onco-age-legend" style="opacity:0.7"></div>' + 
        '<div class="onco-track-legend onco-age-legend" style="opacity:1"></div> 100+ <br>' + 
      '<b>' + gettextCatalog.getString('Vital Status') + ':</b> ' +
        gettextCatalog.getString('Deceased') + ': <div class="onco-track-legend onco-deceased-legend"></div> ' + 
        gettextCatalog.getString('Alive') + ': <div class="onco-track-legend onco-alive-legend"></div><br>' + 
      '<b>' + gettextCatalog.getString('Survival Time (days)') + ':</b> ' +
        '0 <div class="onco-track-legend onco-survival-legend" style="opacity:0.05"></div>' +
        '<div class="onco-track-legend onco-survival-legend" style="opacity:0.4"></div>' + 
        '<div class="onco-track-legend onco-survival-legend" style="opacity:0.7"></div>' + 
        '<div class="onco-track-legend onco-survival-legend" style="opacity:1"></div>' + 
        maxSurvival + '<br>' + 
      '<b>' + gettextCatalog.getString('Sex') + ':</b> ' + gettextCatalog.getString('Male') + 
      ' <div class="onco-track-legend onco-male-legend"></div> ' + 
        gettextCatalog.getString('Female') + ' <div class="onco-track-legend onco-female-legend"></div><br>';

      return value;
    };

    _srv.dataTypeLegend = function() {
      var value = '<b>' + gettextCatalog.getString('Available Data Types') + ':</b><br>' +
        '<div class="onco-track-legend onco-cnsm-legend"></div> ' +
          gettextCatalog.getString('Copy Number Somatic Mutations (CNSM)') + ' <br>' +
        '<div class="onco-track-legend onco-stsm-legend"></div> ' +
          gettextCatalog.getString('Structural Somatic Mutations (StSM)') + ' <br>' +
        '<div class="onco-track-legend onco-sgv-legend"></div> ' +
          gettextCatalog.getString('Simple Germline Variants (SGV)') + ' <br>' +
        '<div class="onco-track-legend onco-metha-legend"></div> ' +
          gettextCatalog.getString('Array-based DNA Methylation (METH-A)') + ' <br>' +
        '<div class="onco-track-legend onco-meths-legend"></div> ' +
          gettextCatalog.getString('Sequence-based DNA Methylation (METH-S)') + ' <br>' +
        '<div class="onco-track-legend onco-expa-legend"></div> ' +
          gettextCatalog.getString('Array-based Gene Expression (EXP-A)') + ' <br>' +
        '<div class="onco-track-legend onco-exps-legend"></div> ' +
          gettextCatalog.getString('Sequence-based Gene Expression (EXP-S)') + ' <br>' +
        '<div class="onco-track-legend onco-pexp-legend"></div> ' +
          gettextCatalog.getString('Protein Expression (PEXP)') + ' <br>' +
        '<div class="onco-track-legend onco-mirna-legend"></div> ' +
          gettextCatalog.getString('Sequence-based miRNA Expression (miRNA)') + ' <br>' +
        '<div class="onco-track-legend onco-jcn-legend"></div> ' +
          gettextCatalog.getString('Exon Junctions (JCN)') + ' <br>';
                  
      return value;
    };

    _srv.studyLegend = function() {
      var value = '<b>Studies:</b> <br>' + 
        '<div class="onco-track-legend onco-pcawg-legend" style="opacity:1"></div>' +
        gettextCatalog.getString('Donor in PCAWG Study');

      return value;
    };

    /**
     * Private Helpers
     */
    function expandObs(o) {
      var expanded = [];
      var precedence = Consequence.precedence();

      _(o.genes).forEach(function (g) {
        var ret = _.clone(o);
        ret.geneId = g.geneId;

        var consequences = $filter('orderBy')(g.consequence, function (t) {
          var index = precedence.indexOf(t.consequenceType);
          if (index === -1) {
            return precedence.length + 1;
          }
          return index;
        });

        ret.consequence = _(consequences).filter(function (d) { return d.functionalImpact === 'High' } ).value()[0];
        if (ret.consequence === undefined) {
          ret.consequence = {functionalImpact: null, consequenceType: null};
        }

        expanded.push(ret);
      }).value();

      return expanded;
    }

  });

})();