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

  angular.module('icgc.genelist', [
    'icgc.genelist.controllers',
    'icgc.genelist.services'
  ]);

})();


(function() {
  'use strict';

  var module = angular.module('icgc.genelist.services', []);

  module.service('GeneSetVerificationService', function(Restangular, LocationService, Extensions, Facets) {

    /* Verify text input */
    this.verify = function(text) {
      var data = 'geneIds=' + encodeURI(text);
      return Restangular.one('genesets').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, undefined, {'validationOnly':true});
    };


    /* Create new gene set based on text input - assumes input is already correct */
    this.create = function(text) {
      var data = 'geneIds=' + encodeURI(text);

      return Restangular.one('genesets').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data);
    };

    /* Echo back the text content of file */
    this.fileContent = function(filepath) {
      var data = new FormData();
      data.append('filepath', filepath);
      return Restangular.one('ui').one('search').withHttpConfig({transformRequest: angular.identity})
        .customPOST(data, 'file', {}, {'Content-Type': undefined});
    };


    this.geneSetIdFilters = function(geneSetId) {
      var filters = LocationService.filters(),
          entitySpecifier = 'id',
          newGeneSetCollection = [Extensions.ENTITY_PREFIX + geneSetId],
          logicalISorNOT = 'is';


      if (! filters.hasOwnProperty('gene')) {
        filters.gene = {};
      }

      if (! filters.gene.hasOwnProperty(entitySpecifier)) {
        filters.gene[entitySpecifier] = {};
      }

      var params = {type: 'gene', facet: 'id'};

      if (Facets.isNot(params)) {
        logicalISorNOT = 'not';
      }

      filters.gene[entitySpecifier][logicalISorNOT] = newGeneSetCollection;

      return filters;
    };


    /**
     * Generate UI display table for matched genes, and compute summary statistics.
     * This will pivot the valid genes (organized by search type) and group it by
     * gene symbols.
     *
     * Input:
     *   {
     *      _gene_id: { k1: g1, k2: g2 ....},
     *      symbol: { k3: g3, k4: g4 ...},
     *      external_db_ids.uniprotkb_swissprot: { k5: g5, k6: g6 }
     *   }
     *
     * Output:
     *   {
     *      symbol1: { _gene_id: [...], symbol: [...], external_db_ids.uniprotkb_swissprot: [...] },
     *      symbol2: { _gene_id: [...], symbol: [...], external_db_ids.uniprotkb_swissprot: [...] },
     *      symbol3: { _gene_id: [...], symbol: [...], external_db_ids.uniprotkb_swissprot: [...] }
     *      ...
     *   }
     *
     */
    this.formatResult = function(verifyResult) {
      var uiResult = {}, uniqueEnsemblMap = {}, totalInputCount = 0;
      var validIds = [], hasType = {};

      angular.forEach(verifyResult.validGenes, function(type, typeName) {
        angular.forEach(type, function(geneList, inputToken) {

          // Sanity check
          if (!geneList || geneList.length === 0) {
            return;
          }

          geneList.forEach(function(gene) {
            var symbol = gene.symbol, row;

            // Initialize row structure
            if (! uiResult.hasOwnProperty(symbol)) {
              uiResult[symbol] = {};
            }
            row = uiResult[symbol];

            // Aggregate input ids that match to the same symbol
            if (! row.hasOwnProperty(typeName)) {
              row[typeName] = [];
            }
            if (row[typeName].indexOf(inputToken) === -1) {
              row[typeName].push(inputToken);

              // Mark it for visibility test on the view
              hasType[typeName] = 1;
            }

            // Aggregate matched ensembl ids that match to the same symbol
            if (! row.hasOwnProperty('matchedId')) {
              row.matchedId = [];
            }
            if (row.matchedId.indexOf(gene.id) === -1) {
              row.matchedId.push(gene.id);
              validIds.push(gene.id);
            }
            uniqueEnsemblMap[gene.id] = 1;

          });
          totalInputCount ++;
        });
      });


      return {
        uiResult: uiResult,
        totalInput: totalInputCount,
        totalMatch: Object.keys(uniqueEnsemblMap).length,
        totalColumns: Object.keys(hasType).length,
        hasType: hasType,
        validIds: validIds,
        invalidIds: verifyResult.invalidGenes,
        warnings: verifyResult.warnings || []
      };
    };

  });
})();


(function () {
  'use strict';

  var module = angular.module('icgc.genelist.controllers', []);

  module.controller('GeneListController', function($scope, $rootScope, $timeout, $location,
    $modalInstance, GeneSetVerificationService, LocationService, SetService, Page) {

    var verifyPromise = null;
    var delay = 1000;

    // Fields for searching by custom gene identifiers
    $scope.params = {};
    $scope.params.rawText = '';
    $scope.params.state = '';
    $scope.params.myFile = null;
    $scope.params.fileName = '';
    $scope.params.inputMethod = 'id';

    // Fields needed for saving into custom gene set
    $scope.params.setName = '';


    $scope.params.savedSets = SetService.getAllGeneSets();
    $scope.params.selectedSavedSet = -1;

    // Output
    $scope.out = {};

    // Determine display params based on current page
    $scope.analysisMode = Page.page() === 'analysis'? true: false;


    function verify() {
      $scope.params.state = 'verifying';
      GeneSetVerificationService.verify($scope.params.rawText).then(function(result) {
        $scope.params.state = 'verified';
        $scope.out = GeneSetVerificationService.formatResult(result);
      });
    }

    function verifyFile() {
      // Update UI
      $scope.params.state = 'uploading';
      $scope.params.fileName = $scope.params.myFile.name;

      // The $timeout is just to give sufficent time in order to convey system state
      $timeout(function() {
        GeneSetVerificationService.fileContent($scope.params.myFile).then(function(result) {
          $scope.params.rawText = result.data;
          verify();
        });
      }, 1000);
    }

    function createNewGeneList() {
      GeneSetVerificationService.create($scope.params.rawText).then(function(result) {
        var search = LocationService.search();
        search.filters = angular.toJson(GeneSetVerificationService.geneSetIdFilters(result.geneListId));

        // Upload gene list redirects to gene tab, regardless of where we came from
        $location.path('/search/g').search(search);
      });
    }


    $scope.submitList = function() {

      if ($scope.analysisMode === true) {
        var setParams = {};
        setParams.type = 'gene';
        setParams.name = $scope.params.setName;
        setParams.size = $scope.out.validIds.length;
        setParams.filters = {
          gene: {
            id: {
              is: $scope.out.validIds
            }
          }
        };
        SetService.addSet(setParams.type, setParams).then((set) => {
          $rootScope.$broadcast(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, set);
          $modalInstance.close();
        });
        return;
      }

      if ($scope.params.selectedSavedSet >= 0) {
        var id = $scope.params.savedSets[$scope.params.selectedSavedSet].id;
        var search = LocationService.search();
        search.filters = angular.toJson(GeneSetVerificationService.geneSetIdFilters(id));
        $location.path('/search/g').search(search);

      } else {
        createNewGeneList();
      }
      $modalInstance.dismiss('cancel');
    };

    // This may be a bit brittle, angularJS as of 1.2x does not seem to have any native/clean
    // way of modeling [input type=file]. So to get file information, it is proxied through a
    // directive that gets the file value (myFile) from input $element
    //
    // Possible issues with illegal invocation
    //  - https://github.com/danialfarid/ng-file-upload/issues/776#issuecomment-106929172
    $scope.$watch('params.myFile', function(newValue) {
      if (! newValue) {
        return;
      }
      verifyFile();
    });


    $scope.updateGenelist = function() {
      // If content was from file, clear out the filename
      $scope.params.fileName = null;
      if ($scope.params.myFile) {
        $scope.params.myFile = null;
      }

      $timeout.cancel(verifyPromise);
      verifyPromise = $timeout(verify, delay, true);
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.resetListInput = function() {
      $scope.params.selectedSavedSet = -1;
    };

    $scope.resetCustomInput = function() {
      $scope.params.state = '';
      $scope.params.fileName = null;
      $scope.params.rawText = '';
      $scope.out = {};
      if ($scope.params.myFile) {
        $scope.params.myFile = null;
      }
    };

    $scope.$on('$destroy', function() {
      if (verifyPromise) {
        $timeout.cancel(verifyPromise);
      }
      $scope.params = null;
      $scope.out = null;
    });

  });
})();

