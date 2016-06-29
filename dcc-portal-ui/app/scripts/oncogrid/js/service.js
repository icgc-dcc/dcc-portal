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

  module.service('OncogridService', function (Donors, Genes, Occurrences, Consequence, $q, $filter) {

    var _srv = this;

    /**
     * Data Retrieval for OncoGrid
     */

    _srv.getDonors = function (donorSet) {
      return Donors.getAll({
        filters: _srv.donorFilter(donorSet),
        size: 100
      });
    };

    _srv.getGenes = function (geneSet) {
      return Genes.getAll({
        filters: _srv.geneFilter(geneSet),
        size: 100
      });
    };

    _srv.getCuratedSet = function (geneSet) {
      return Genes.getAll({
        filters: _srv.curatedFilter(geneSet),
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
          'age': (d.ageAtDiagnosis === undefined ? 0 : d.ageAtDiagnosis),
          'sex': (d.gender === undefined ? 'unknown' : d.gender),
          'vitalStatus': (d.vitalStatus === undefined ? false : (d.vitalStatus === 'alive' ? true : false)),
          'pcawg': _.has(d, 'studies') && d.studies.indexOf('PCAWG') >= 0,
          'cnsmExists': d.cnsmExists,
          'stsmExists': d.stsmExists,
          'sgvExists': d.sgvExists,
          'methArrayExists': d.methArrayExists
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
      var donorIds = _.map(donors, function (g) { return g.id; });
      var geneIds = _.map(genes, function (d) { return d.id; });

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

    /**
     * Private Helpers
     */
    function expandObs(o) {
      var expanded = [];
      var precedence = Consequence.precedence();

      _(o.genes).forEach(function (g) {
        var ret = _.clone(o);
        ret.geneId = g.geneId;

        ret.consequence = $filter('orderBy')(g.consequence, function (t) {
          var index = precedence.indexOf(t.consequenceType);
          if (index === -1) {
            return precedence.length + 1;
          }
          return index;
        })[0];

        expanded.push(ret);
      }).value();

      return expanded;
    }

  });

})();