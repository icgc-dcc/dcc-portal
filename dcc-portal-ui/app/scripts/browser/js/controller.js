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

  var module = angular.module('icgc.browser.controllers', []);

  module.controller('BrowserController',
    function ($scope, $state, Page, Genes, Mutations, GMService, Restangular, LocationService, gettextCatalog) {
      Page.setTitle(gettextCatalog.getString('Genome Viewer'));
      Page.setPage('browser');

      var pageSize = 20, done = false;

      $scope.isValidChromosome = true;
      $scope.filters = LocationService.filters();
      $scope.filtersStr = encodeURIComponent(JSON.stringify($scope.filters));


      $scope.setRegion = function (params) {
        // Check for required parameters
        [ 'chromosome', 'start', 'end'].forEach(function (rp) {
          if (!params.hasOwnProperty(rp)) {
            throw new Error('Missing required parameter: ' + rp);
          }
        });

        if (!GMService.isValidChromosome(params.chromosome)) {
          $scope.isValidChromosome = false;
          return;
        }

        $scope.isValidChromosome = true;
        $scope.$broadcast('gv:set:region', params);
      };

      function clearGenes() {
        if ($scope.genes.hasOwnProperty('data') && $scope.genes.data.hasOwnProperty('hits')) {
          $scope.genes.data.hits.forEach(function (g) {
            delete g.active;
          });
        }
      }

      function clearMutations() {
        if ($scope.mutations.hasOwnProperty('data') && $scope.mutations.data.hasOwnProperty('hits')) {
          $scope.mutations.data.hits.forEach(function (m) {
            delete m.active;
          });
        }
      }

      $scope.setGeneActive = function (id, region) {
        var gene = _.find($scope.genes.data.hits, function (g) {
          return g.id === id;
        });
        if (gene.active) {
          return;
        }
        clearMutations();
        clearGenes();
        gene.active = true;
        var offset = (region.end - region.start) * 0.05;
        region.start -= offset;
        region.end += offset;
        $scope.setRegion(region);
      };

      $scope.setMutationActive = function (id, region) {
        var mutation = _.find($scope.mutations.data.hits, function (m) {
          return m.id === id;
        });
        if (mutation.active) {
          return;
        }
        clearMutations();
        clearGenes();
        mutation.active = true;
        $scope.setRegion(region);
      };

      $scope.genes = {
        from: 1,
        size: 20,
        isBusy: false,
        isFinished: false
      };
      $scope.mutations = {
        from: 1,
        size: 20,
        isBusy: false,
        isFinished: false
      };

      function genes(settings) {
        settings = settings || {};
        $scope.genes.isBusy = true;

        Genes.getList({
          from: $scope.genes.from,
          size: $scope.genes.size
        })
          .then(function (response) {
            $scope.genes.isFinished = response.pagination.total - $scope.genes.from < pageSize;
            $scope.genes.isBusy = false;

            if (settings.scroll) {
              $scope.genes.data.pagination = response.pagination ? response.pagination : {};
              for (var i = 0; i < response.hits.length; i++) {
                $scope.genes.data.hits.push(response.hits[i]);
              }
            } else {
              $scope.genes.data = response;
            }
          });
      }

      $scope.nextGenes = function () {
        if ($scope.genes.isBusy || $scope.genes.isFinished) {
          return;
        }

        $scope.genes.from += pageSize;
        genes({scroll: true});
      };

      function handleMutationResponse(response, settings) {
        $scope.mutations.isFinished = response.pagination.total - $scope.mutations.from < pageSize;
        $scope.mutations.isBusy = false;

        if (settings.scroll) {
          $scope.mutations.data.pagination = response.pagination ? response.pagination : {};
          for (var i = 0; i < response.hits.length; i++) {
            $scope.mutations.data.hits.push(response.hits[i]);
          }
        } else {
          $scope.mutations.data = response;
        }
      }

      function mutations(settings) {
        settings = settings || {};
        $scope.mutations.isBusy = true;

        if (LocationService.search().context === 'donor') {
          var donorId = LocationService.filters().donor.id.is[0];
          var filters = LocationService.filters();
          delete filters.donor.id;

          Restangular.one('ui').one('search').one('donor-mutations').get({
            filters: filters,
            donorId: donorId,
            include: 'consequences',
            from: $scope.mutations.from,
            size: $scope.mutations.size
          }).then(function(response) {
            handleMutationResponse(response, settings);
          });
        } else {
          Mutations.getList({
            from: $scope.mutations.from,
            size: $scope.mutations.size
          }).then(function(response) {
            handleMutationResponse(response, settings);
          });
        }
      }

      $scope.nextMutations = function () {
        if ($scope.mutations.isBusy || $scope.mutations.isFinished) {
          return;
        }

        $scope.mutations.from += pageSize;
        mutations({scroll: true});
      };

      genes();
      mutations();

      $scope.activateTab = function (tab) {
        $scope.tab = tab;
      };

      $scope.activateTab($state.current.data.tab);

      $scope.$watch('genes', function (genes) {
        if (!done &&
            genes.hasOwnProperty('data') &&
            genes.data.hasOwnProperty('hits') &&
            genes.data.hits.length &&
            $scope.tab === 'genes') {
          done = true;
          var gene = genes.data.hits[0];
          $scope.setGeneActive(gene.id, {chromosome: gene.chromosome, start: gene.start, end: gene.end});
        }
      }, true);
      $scope.$watch('mutations', function (mutations) {
        if (!done &&
            mutations.hasOwnProperty('data') &&
            mutations.data.hasOwnProperty('hits') &&
            mutations.data.hits.length &&
            $scope.tab === 'mutations') {
          done = true;
          var mutation = mutations.data.hits[0];
          $scope.setMutationActive(mutation.id,
            {
              chromosome: mutation.chromosome,
              start: mutation.start,
              end: mutation.start
            });
        }
      }, true);
    });
})();
