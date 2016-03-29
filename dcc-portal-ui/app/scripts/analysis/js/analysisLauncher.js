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

  var module = angular.module('icgc.analysis.controllers');


  /**
   * Manages the launch of an analysis, one of
   * - gene set enrichment
   * - set operations
   * - phenotype analysis
   */
  module.controller('NewAnalysisController',
    function($scope, $modal, $location, $timeout, Page, AnalysisService, Restangular, SetService, Extensions) {

    var _this = this,
        _isLaunchingAnalysis = false;

    _this.analysisType = null; // One of "enrichment", "set", "phenotype" or "coverage"
    _this.filteredList = [];
    _this.filteredSetType = '';
    _this.selectedIds = [];

    _this.allSets = SetService.getAll();

    // Pass-thru
    _this.analysisName = AnalysisService.analysisName;
    _this.analysisDescription = AnalysisService.analysisDescription;
    _this.analysisDemoDescription = AnalysisService.analysisDemoDescription;

    _this.toggle = function(setId) {
      if (_this.selectedIds.indexOf(setId) >= 0) {
        _.remove(_this.selectedIds, function(id) {
          return id === setId;
        });
      } else {
        _this.selectedIds.push(setId);
      }

      // Apply filer to disable irrelevant results
      if (_this.selectedIds.length === 0) {
        _this.filteredSetType = '';
      }
      _this.applyFilter(_this.analysisType);
    };

    _this.isInFilter = function(set) {
      return _.some(_this.filteredList, function(s) {
        return s.id === set.id;
      });
    };

    _this.applyFilter = function(type) {

      if (type === 'enrichment') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'gene' && set.count <= 10000;
        });
      } else if (type === 'set') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          if (_this.filteredSetType !== '') {
            return set.type === _this.filteredSetType;
          }
          return true;
        });
      } else if (type === 'phenotype') {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'donor';
        });
      } else {
        _this.filteredList = _.filter(SetService.getAll(), function(set) {
          return set.type === 'donor';
        });
      }
    };


    _this.isLaunchingAnalysis = function() {
      return _isLaunchingAnalysis;
    };


    /* Phenotype comparison only takes in donor set ids */
    _this.launchPhenotype = function(setIds) {

      if (_isLaunchingAnalysis) {
        return;
      }

      _isLaunchingAnalysis = true;

      var payload = setIds;
      var promise = Restangular.one('analysis').post('phenotype', payload, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        if (data.id) {
          $location.path('analysis/view/phenotype/' + data.id);
        }
      })
      .finally(function() {
        _isLaunchingAnalysis = false;
      });
    };


    _this.launchSet = function(type, setIds) {

      if (_isLaunchingAnalysis) {
        return;
      }

      _isLaunchingAnalysis = true;

      var payload = {
        lists: setIds,
        type: type.toUpperCase()
      };
      var promise = Restangular.one('analysis').post('union', payload, {}, {'Content-Type': 'application/json'});
      promise.then(function(data) {
        if (!data.id) {
          console.log('cannot create set operation');
        }
        $location.path('analysis/view/set/' + data.id);
      })
      .finally(function() {
        _isLaunchingAnalysis = false;
      });
    };


    // Wait for sets to materialize
    function wait(ids, numTries, callback) {
      if (numTries <= 0) {
        return;
      }
      SetService.getMetaData(ids).then(function(data) {
        var finished = _.filter(data, function(d) {
          return d.state === 'FINISHED';
        });


        if (finished.length === ids.length) {
          callback(data);
        } else {
          $timeout(function() {
            wait(ids, --numTries, callback);
          }, 1500);
        }
      });
    }

    _this.demoPhenotype = function() {
      var p1, p2, p3, type = 'donor';
      p1 = {
        filters: {
          donor:{ primarySite: { is: ['Brain'] } }
        },
        isTransient: true,
        type: type,
        name: 'Brain Cancer'
      };

      p2 = {
        filters: {
          donor:{ primarySite: { is: ['Breast'] } }
        },
        isTransient: true,
        type: type,
        name: 'Breast Cancer'
      };

      p3 = {
        filters: {
          donor:{ primarySite: { is: ['Colorectal'] } }
        },
        isTransient: true,
        type: type,
        name: 'Colorectal Cancer'
      };

      var demoSetIds = [];
      Page.startWork();
      SetService.addSet(type, p1).then(function(r1) {
        demoSetIds.push(r1.id);
        SetService.addSet(type, p2).then(function(r2) {
          demoSetIds.push(r2.id);
          SetService.addSet(type, p3).then(function(r3) {
            demoSetIds.push(r3.id);

            function proxyLaunch() {
              Page.stopWork();
              _this.launchPhenotype(demoSetIds);
            }
            wait(demoSetIds, 7, proxyLaunch);
          });
        });
      });

    };

    _this.demoSetOperation = function() {
      var p1, p2, p3, type = 'mutation';
      p1 = {
        filters: {
          donor:{
            primarySite: {is: ['Brain']},
            projectId: {is: ['GBM-US']}
          },
          mutation: {
            functionalImpact: {is: ['High']}
          }
        },
        type: type,
        isTransient: true,
        name: 'GBM-US High Impact Mutation'
      };
      p2 = {
        filters: {
          donor:{
            primarySite: {is: ['Brain']},
            projectId: {is: ['LGG-US']}
          },
          mutation: {
            functionalImpact: {is: ['High']}
          }
        },
        type: type,
        isTransient: true,
        name: 'LGG-US High Impact Mutation'
      };
      p3 = {
        filters: {
          donor:{
            primarySite: {is: ['Brain']},
            projectId: {is: ['PBCA-DE']}
          },
          mutation: {
            functionalImpact: {is: ['High']}
          }
        },
        type: type,
        isTransient: true,
        name: 'PBCA-DE High Impact Mutation'
      };

      var demoSetIds = [];
      Page.startWork();
      SetService.addSet(type, p1).then(function(r1) {
        demoSetIds.push(r1.id);
        SetService.addSet(type, p2).then(function(r2) {
          demoSetIds.push(r2.id);
          SetService.addSet(type, p3).then(function(r3) {
            demoSetIds.push(r3.id);

            function proxyLaunch() {
              Page.stopWork();
              _this.launchSet('mutation', demoSetIds);
            }
            wait(demoSetIds, 7, proxyLaunch);
          });
        });
      });

    };

    _this.demoEnrichment = function() {
      var filters, type, params;
      filters = {
        gene: {
          symbol: {
            is: ['TP53','NRG1','FIP1L1','CAMTA1','FHIT','PIK3CA','ALK','RUNX1','NTRK3',
              'PDE4DIP','LPP','KRAS','PTPRK','BRAF','ZNF521','FOXP1','STAG2','GPHN','EBF1',
              'CUX1','RANBP17','PRDM16','NF1','SETBP1','CDH11','MAML2','GOPC','ERC1','PTEN',
              'ATRX','APC','PBX1','EGFR','ARID1A','GAS7','NCOA2','CACNA1D','ERG','SRGAP3',
              'AKAP9','CTNNB1','EIF3E','EXT1','LHFP','RSPO2','PTPRB','NOTCH2','KMT2D','PTPRC','GPC3']
          }
        }
      };
      type = 'gene';
      params = {
        filters: filters,
        type: type,
        isTransient: true,
        name: 'Demo'
      };

      // Create a temporary set, then launch the enrichment analysis
      Page.startWork();
      SetService.addSet(type, params).then(function(result) {
        function proxyLaunch(sets) {
          Page.stopWork();
          launchEnrichment(sets[0]);
        }
        wait([result.id], 5, proxyLaunch);
      });
    };

    _this.launchEnrichment = function(setId) {
      var set = _.filter(_this.filteredList, function(set) {
        return set.id === setId;
      })[0];
      launchEnrichment(set);
    };

    function launchEnrichment(set) {
      var filters = {
        gene: {}
      };
      filters.gene.id = { is: [Extensions.ENTITY_PREFIX + set.id] };

      $modal.open({
        templateUrl: '/scripts/enrichment/views/enrichment.upload.html',
        controller: 'EnrichmentUploadController',
        resolve: {
          geneLimit: function() {
            return set.count;
          },
          filters: function() {
            return filters;
          }
        }
      });
    }

    $scope.$on('$locationChangeSuccess', function() {
      _this.filteredList = [];
      _this.selectedIds = [];
      _this.analysisType = null;
      _this.filteredSetType = '';
    });


    $scope.$watch(function() {
      return _this.analysisType;
    }, function(n) {
      if (!n) {
        return;
      }
      _this.selectedIds = [];
      _this.applyFilter(n);
    });
  });
})();


