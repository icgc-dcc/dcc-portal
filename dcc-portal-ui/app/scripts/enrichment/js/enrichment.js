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

  angular.module('icgc.enrichment', [
    'icgc.enrichment.controllers',
    'icgc.enrichment.directives',
    'icgc.enrichment.services'
  ]);
})();



(function () {
  'use strict';

  var module = angular.module('icgc.enrichment.controllers', []);
  module.controller('EnrichmentUploadController',
    function($scope, $modalInstance, $location, Restangular, LocationService, Extensions, geneLimit, filters) {

    var _launchingEnrichmentUpload = false;

    $scope.params = {};
    $scope.Extensions = Extensions;
    $scope.filters = filters;
    $scope.hasValidParams = true;

    // Params and their default values
    $scope.params.maxGeneSetCount = 50;
    $scope.params.fdr = 0.05;
    $scope.params.universe = 'REACTOME_PATHWAYS';
    $scope.params.maxGeneCount = Math.min(geneLimit || 10000, 10000);
    $scope.geneLimit = $scope.params.maxGeneCount; // Absolute max


    function hasValidFDR(val) {
      var v = parseFloat(val);

      if (isNaN(val) === true) {
        return false;
      }
      if (angular.isNumber(v) === false) {
        return false;
      }
      if (v >= 0.005 && v <= 0.5) {
        return true;
      }
      return false;
    }


    function hasValidGeneCount(val) {
      var v = parseInt(val, 10);
      if (isNaN(val) === true) {
        return false;
      }
      if (angular.isNumber(v) === false || v > $scope.geneLimit || v <= 0) {
        return false;
      }
      return true;
    }


    function buildEnrichmentRequest() {
      var data, geneSortParam;
      var defaultOrder = 'DESC';
      var defaultSort = 'affectedDonorCountFiltered';


      // Check if we should use a provided filter or the default (LocationService)
      if (angular.isDefined(filters)) {
        data = 'params=' + JSON.stringify($scope.params) + '&' +
          'filters=' + JSON.stringify(filters) + '&' ;
      } else {
        data = 'params=' + JSON.stringify($scope.params) + '&' +
          'filters=' + JSON.stringify(LocationService.filters()) + '&' ;
      }

      geneSortParam = LocationService.getJqlParam('genes');

      if (!_.isEmpty(geneSortParam)) {
        var sort, order;
        sort = geneSortParam.sort || defaultSort;
        order = geneSortParam.order === 'asc'? 'ASC' : 'DESC';
        data += 'sort=' + sort + '&order=' + order;
      } else {
        data += 'sort=' + defaultSort + '&order=' + defaultOrder;
      }
      return data;
    }

    $scope.isLaunchingEnrichmentUpload = function() {
      return _launchingEnrichmentUpload;
    };

    $scope.newGeneSetEnrichment = function() {

      if (_launchingEnrichmentUpload) {
        return;
      }

      _launchingEnrichmentUpload = true;

      var promise, data;
      data = buildEnrichmentRequest();
      promise = Restangular.one('analysis')
        .withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, 'enrichment');

      // Send and forget, we really just need to get the analysis id
      // to start the redirection
      promise.then(function(result) {
        var id = result.id;
        $modalInstance.dismiss('cancel');
        $location.path('/analysis/view/enrichment/' + id).search({});
      })
      .finally(function() {
        _launchingEnrichmentUpload = false;
      });
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };


    $scope.checkInput = function() {
      var params = $scope.params;
      if (hasValidGeneCount(params.maxGeneCount) === false ||
        hasValidFDR(params.fdr) === false ||
        angular.isDefined(params.universe) === false) {
        $scope.hasValidParams = false;
      } else {
        $scope.hasValidParams = true;
      }
    };


  });

})();




(function () {
  'use strict';

  angular.module('icgc.enrichment.directives', []);

  angular.module('icgc.enrichment.directives').directive('enrichmentResult',
    function (Extensions, Restangular, EnrichmentService, ExportService, TooltipText, Settings) {

    return {
      restrict: 'E',
      scope: {
        item: '=',
        callingContext: '@'
      },
      templateUrl: '/scripts/enrichment/views/enrichment.result.html',
      link: function($scope) {
        $scope.predicate = 'pvalue';
        $scope.reverse = false;

        $scope.TooltipText = TooltipText;

        $scope.encode = function(f) {
          return encodeURIComponent(JSON.stringify(f));
        };

        function refresh() {

          var enrichment = $scope.item;

          // No results yet, can't do anything
          if (! enrichment.results) {
            return;
          }

          // Get original sort and order
          $scope.sortParams = EnrichmentService.sortParams(enrichment);

          // Create links for overview
          $scope.overviewUniverseFilters = EnrichmentService.overviewUniverseFilters(enrichment);
          $scope.overviewInputFilters = EnrichmentService.overviewInputFilters(enrichment);
          $scope.overviewGeneOverlapFilters = EnrichmentService.overviewGeneOverlapFilters(enrichment);

          // Compute queries to go to advanced search page
          enrichment.results.forEach(function(row) {
            row.geneSetFilters = EnrichmentService.geneSetFilters(enrichment, row);
            row.geneSetOverlapFilters = EnrichmentService.geneSetOverlapFilters(enrichment, row);
          });

          // Check if the analysis is still valid with respect to current data
          Settings.get().then(function(settings) {
            if (! enrichment.version || settings.dataVersion !== enrichment.version) {
              $scope.isDeprecated = true;
            } else {
              $scope.isDeprecated = false;
            }

          });

        }

        $scope.exportEnrichment = function(id) {
          Restangular.one('analysis/enrichment', id).get({}, {'Accept': 'text/tsv'}).then(function(data) {
            var filename = id + '.tsv';
            ExportService.exportData(filename, data);
          });
        };

        $scope.$watch('item', function(n) {
          if (angular.isDefined(n) && !_.isEmpty(n)) {
            refresh();
          }
        });

      }
    };
  });

})();




(function () {
  'use strict';

  var module = angular.module('icgc.enrichment.services', []);

  /**
   * Encapsulates some of the cringe-worthy filter manipulations in one place
   */
  module.service('EnrichmentService', function(Extensions) {

    function ensureGeneExist(filters) {
      if (! filters.gene) {
        filters.gene = {};
      }
      return filters;
    }

    /**
     * Replace list with the input gene list limit
     * Input gene list takes precedence over gene identifiers (id, entityListId)
     * 1) Replace gene.entityListId
     * 2) Remove gene.id
     */
    function mergeInputGeneList(filters, geneListId) {
      delete filters.gene[Extensions.ENTITY];
      delete filters.gene.id;
      filters.gene.id = {
        is: [Extensions.ENTITY_PREFIX + geneListId]
      };
      return filters;
    }

    /**
     * Returns original gene sort information
     */
    this.sortParams = function(enrichment) {
      var sortResult = {};
      if (enrichment.query.sort && enrichment.query.order) {
        sortResult.sort = enrichment.query.sort;
        sortResult.order = enrichment.query.order.toLowerCase();
      }
      return sortResult;
    };


    /**
     * Returns a filter to specify the selected universe
     */
    this.overviewUniverseFilters = function(enrichment) {
      var filters = {};
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });

      filters = ensureGeneExist(filters);

      // Add universe type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = {
          is: [universe.id]
        };
      } else if (universe.type === 'pathway') {
        filters.gene.hasPathway = true;
      }
      return filters;
    };


    /**
     * Returns a filters to specify genes in the original request (original query intersect gene-limit)
     * with the genes in the selected universe
     *
     * Empty filters if there are overlapping pathway ids or goTerm ids
     */
    this.overviewGeneOverlapFilters = function(enrichment) {
      var filters = angular.copy(enrichment.query.filters);
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });

      filters = ensureGeneExist(filters);

      // Disallow goTerm ids overlapping
      if (filters.gene && filters.gene.goTermId && universe.type === 'go_term') {
        return null;
      }

      // Disallow pathway ids overlapping
      if (filters.gene && filters.gene.pathwayId && universe.type === 'pathway') {
        return null;
      }

      // Replace list with input limit
      filters = mergeInputGeneList(filters, enrichment.id);

      // Add universe type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = { 'is': [universe.id] };
      } else if (universe.type === 'pathway') {
        filters.gene.hasPathway = true;
      }

      return filters;
    };


    /**
     * Returns a filters to specify genes in the original request (original query intersect gene-limit)
     */
    this.overviewInputFilters = function(enrichment) {
      var filters = angular.copy(enrichment.query.filters);
      filters = ensureGeneExist(filters);
      filters = mergeInputGeneList(filters, enrichment.id);
      return filters;
    };


    /**
     * Returns a filters to specify genes in the given gene set
     */
    this.geneSetFilters = function(enrichment, row) {
      var filters = {};
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });
      filters = ensureGeneExist(filters);

      // Route based on universe type
      if (universe.type === 'go_term') {
        filters.gene.goTermId = {
          is: [row.geneSetId]
        };
      } else if (universe.type === 'pathway') {
        filters.gene.pathwayId = {
          is: [row.geneSetId]
        };
      }
      return filters;
    };


    /**
     * Returns a filters to specify genes in in original request (original query intersects gene-limit)
     * with the genes in the selected universe with a given gene set
     */
    this.geneSetOverlapFilters = function(enrichment, row) {
      var filters = angular.copy(enrichment.query.filters);
      var universe = _.find(Extensions.GENE_SET_ROOTS, function(go) {
        return go.universe === enrichment.params.universe;
      });

      filters = ensureGeneExist(filters);

      // Disallow goTerm ids overlapping
      if (filters.gene && filters.gene.goTermId && universe.type === 'go_term') {
        return null;
      }

      // Disallow pathway ids overlapping
      if (filters.gene && filters.gene.pathwayId && universe.type === 'pathway') {
        return null;
      }
      if (filters.gene && filters.gene.hasPathway && universe.type === 'pathway') {
        return null;
      }

      // Replace list with input limit
      filters = mergeInputGeneList(filters, enrichment.id);

      // Add universe type specific conditions
      if (universe.type === 'go_term') {
        filters.gene.goTermId = { 'is': [row.geneSetId] };
      } else if (universe.type === 'pathway') {
        filters.gene.pathwayId = { 'is': [row.geneSetId] };
      }
      return filters;
    };

  });

})();

