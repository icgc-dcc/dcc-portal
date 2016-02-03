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

(function() {
  'use strict';

  angular.module('icgc.phenotype', [
    'icgc.phenotype.directives',
    'icgc.phenotype.services'
  ]);

})();


(function() {
  'use strict';

  var module = angular.module('icgc.phenotype.directives', ['icgc.phenotype.services']);

  module.directive('phenotypeResult', function(SetService, PhenotypeService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/phenotype/views/phenotype.result.html',
      link: function($scope) {

        // From D3's cat20 scale
        $scope.seriesColours = ['#6baed6', '#fd8d3c', '#74c476'];

        function normalize() {
          // Normalize results: Sort by id, then sort by terms
          $scope.item.results.forEach(function(subAnalysis) {

            if (subAnalysis.name === 'ageAtDiagnosisGroup') {

              // Age group need to be sorted as numerics
              subAnalysis.data.forEach(function(d) {
                d.terms = _.sortBy(d.terms, function(term) {
                  return +term.term.split('-')[0];
                });
              });
            } else {
              subAnalysis.data.forEach(function(d) {
                d.terms = _.sortBy(d.terms, function(term) {
                  return term.term;
                });
              });
            }

            subAnalysis.data = _.sortBy(subAnalysis.data, function(d) {
              return d.id;
            });
          });
        }


        function buildAnalyses() {

          // Globals
          $scope.setIds = _.pluck($scope.item.results[0].data, 'id');
          $scope.setFilters = $scope.setIds.map(function(id) {
            return PhenotypeService.entityFilters(id);
          });


          SetService.getMetaData($scope.setIds).then(function(results) {
            $scope.setMap = {};
            results.forEach(function(set) {
              set.advLink = SetService.createAdvLink(set);
              $scope.setMap[set.id] = set;
            });

            // Fetch analyses
            var gender = _.find($scope.item.results, function(subAnalysis) {
              return subAnalysis.name === 'gender';
            });
            var vital = _.find($scope.item.results, function(subAnalysis) {
              return subAnalysis.name === 'vitalStatus';
            });
            var age = _.find($scope.item.results, function(subAnalysis) {
              return subAnalysis.name === 'ageAtDiagnosisGroup';
            });

            $scope.gender = PhenotypeService.buildAnalysis(gender, $scope.setMap);
            $scope.vital = PhenotypeService.buildAnalysis(vital, $scope.setMap);
            $scope.age = PhenotypeService.buildAnalysis(age, $scope.setMap);
            $scope.meanAge = age.data.map(function(d) { return d.summary.mean; });

          });

        }

        $scope.$watch('item', function(n) {
          if (n) {
            normalize();
            buildAnalyses();
          }
        });

      }
    };
  });
})();



(function() {
  'use strict';

  var module = angular.module('icgc.phenotype.services', ['icgc.donors.models']);

  module.service('PhenotypeService', function(Extensions, ValueTranslator) {

    function getTermCount(analysis, term, donorSetId) {
      var data, termObj;
      data = _.find(analysis.data, function(set) {
        return donorSetId === set.id;
      });

      // Special case
      if (term === '_missing') {
        return data.summary.missing || 0;
      }
      termObj = _.find(data.terms, function(t) {
        return t.term === term;
      });
      if (termObj) {
        return termObj.count;
      }
      return 0;
    }

    function getSummary(analysis, donorSetId) {
      var data;
      data = _.find(analysis.data, function(set) {
        return donorSetId === set.id;
      });
      return data.summary;
    }

    this.entityFilters = function(id) {
      var filters = {
        donor:{}
      };
      filters.donor.id = {
        is: [Extensions.ENTITY_PREFIX+id]
      };
      return filters;
    };


    /**
     * Returns UI representation
     */
    this.buildAnalysis = function(analysis, setMap) {
      var uiTable = [];
      var uiSeries = [];
      var terms = _.pluck(analysis.data[0].terms, 'term');
      var setIds = _.pluck(analysis.data, 'id');

      // Create 'no data' term
      terms.push('_missing');

      // Build table row
      terms.forEach(function(term) {
        var row = {};
        row.term = term;

        setIds.forEach(function(donorSetId) {
          var count = getTermCount(analysis, term, donorSetId);
          var summary = getSummary(analysis, donorSetId);
          var advQuery = {};
          advQuery[Extensions.ENTITY] = {
            is: [donorSetId]
          };
          advQuery[analysis.name] = {
            is: [term]
          };

          row[donorSetId] = {};
          row[donorSetId].count = count;
          row[donorSetId].total = (summary.total + summary.missing);
          row[donorSetId].percentage = count/(summary.total + summary.missing);
          row[donorSetId].advQuery = {
            donor: advQuery
          };
        });
        uiTable.push(row);
      });

      // Build graph series
      setIds.forEach(function(setId) {
        uiSeries.push({
          name: setMap[setId].name || setId,

          data: uiTable.map(function(row) {
            return {
              y: isNaN(row[setId].percentage) ? 0 : row[setId].percentage,
              count: row[setId].count
            };
          })
        });
      });

      // Build final result
      return {
        uiTable: uiTable,
        uiGraph: {
          categories: terms.map(function(term) { return ValueTranslator.translate(term); }),
          series: uiSeries
        }
      };
    };

  });


})();
