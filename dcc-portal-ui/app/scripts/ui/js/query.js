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

  /**
   * Display the JSON filter in more user friendly format
   */
  angular.module('icgc.ui.query', []).directive('queryDisplay',
    function(FiltersUtil, LocationService, SetService, Extensions, GeneSymbols) {

    return {
      restrict: 'E',
      replace: true,
      scope: {
        filters: '='
      },
      templateUrl: 'scripts/ui/views/query.html',
      link: function(scope) {

        function resolveGeneIds (filters) {
          var activeGeneIds;
          if (_.has(filters, 'gene.id.not')) {
              activeGeneIds = _(_.get (filters, 'gene.id.not', []))
              .filter ({controlFacet: 'id', controlType: 'gene'})
              .map ('term')
              .value();
          } else if (_.has(filters, 'gene.id.is')) {
            activeGeneIds = _(_.get (filters, 'gene.id.is', []))
              .filter ({controlFacet: 'id', controlType: 'gene'})
              .map ('term')
              .value();
          }

          if (_.isEmpty (activeGeneIds)) {
            scope.ensemblIdGeneSymbolMap = {};
            return;
          }

          GeneSymbols.resolve (activeGeneIds).then (function (ensemblIdGeneSymbolMap) {
            scope.ensemblIdGeneSymbolMap = ensemblIdGeneSymbolMap.plain();
          });
        }

        scope.resolveGeneSymbols = function (type, term) {
          if ('gene' !== type) {
            return term;
          }

          return _.get (scope.ensemblIdGeneSymbolMap, term, term);
        };

        function refresh() {
          // Make a copy and clean up any non-meaningful fields
          var uiFilters = angular.copy(scope.filters);

          ['gene', 'donor', 'mutation'].forEach(function(type) {
            if (uiFilters.hasOwnProperty(type) && _.isEmpty(uiFilters[type])) {
              delete uiFilters[type];
            }
          });

          if (_.isEmpty(uiFilters)) {
            scope.uiFilters = null;
          } else {
            var ids = LocationService.extractSetIds(uiFilters);

            if (ids.length > 0) {
              SetService.getMetaData(ids).then(function(results) {
                scope.uiFilters = FiltersUtil.buildUIFilters (uiFilters,
                  SetService.lookupTable (results.plain()));
                resolveGeneIds (scope.uiFilters);
              });
            } else {
              scope.uiFilters = FiltersUtil.buildUIFilters(uiFilters, {});
              resolveGeneIds (scope.uiFilters);
            }
          }
        }

        scope.$watch('filters', function(n) {
          if (n) {
            refresh();
          }
        });

        scope.Extensions = Extensions;

      }
    };

  });
})();
