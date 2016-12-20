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

  angular.module('icgc.oncogrid', ['icgc.oncogrid.directives', 'icgc.oncogrid.services', 'icgc.oncogrid.controllers']);

})();

(function ($) {
  'use strict';

  var module = angular.module('icgc.oncogrid.directives', []);

  module.directive('oncogridAnalysis', function (Donors, Genes, Occurrences, Consequence,
    $q, $filter, OncogridService, SetService, $timeout, LocationService, gettextCatalog, localStorageService) {
    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      controller: 'OncogridController',
      controllerAs: 'OncoCtrl',
      templateUrl: '/scripts/oncogrid/views/oncogrid-analysis.html',
      link: function ($scope, $element) {
        var donorSearch = '/search?filters=';
        var geneSearch = '/search/g?filters=';
        var obsSearch = '/search/m/o?filters=';

        function createLinks() {
          $scope.geneSet = $scope.item.geneSet;
          $scope.donorSet = $scope.item.donorSet;

          $scope.donorFilter = OncogridService.donorFilter($scope.donorSet);
          $scope.donorLink = donorSearch + JSON.stringify($scope.donorFilter);

          $scope.geneFilter = OncogridService.geneFilter($scope.geneSet);
          $scope.geneLink = geneSearch + JSON.stringify($scope.geneFilter);

          var obsFilter = OncogridService.observationFilter($scope.donorSet, $scope.geneSet);
          $scope.obsLink = obsSearch + JSON.stringify(obsFilter);
          $scope.gvLink = '/browser/m?filters=' + JSON.stringify(obsFilter);

          $scope.donorShare = {
            url: LocationService.buildURLFromPath('search'),
            filters: JSON.stringify($scope.donorFilter)
          };

          $scope.geneShare = {
            url: LocationService.buildURLFromPath('search/g'),
            filters: JSON.stringify($scope.geneFilter)
          };
        }

        $scope.materializeSets = function () {
          var donorPromise = OncogridService.getDonors($scope.donorSet)
            .then(function (data) {
              $scope.donors = data;
            });

          var genePromise = OncogridService.getGenes($scope.geneSet)
            .then(function (data) {
              $scope.genes = data;
            });

          var geneCuratedSetPromise = OncogridService.getCuratedSet($scope.geneSet)
            .then(function (data) {
              $scope.curatedList = _.map(data, function (g) {
                return g.id;
              });
            });

          var occurrencePromise = OncogridService.getOccurences($scope.donorSet, $scope.geneSet)
            .then(function (data) {
              $scope.obsCount = data.length;
              $scope.occurrences = data;
            });

          return $q.all([donorPromise, genePromise, geneCuratedSetPromise, occurrencePromise]);
        };

        $scope.initOnco = function () {

          var donors = OncogridService.mapDonors($scope.donors);
          var genes = OncogridService.mapGenes($scope.genes, $scope.curatedList);
          var observations = OncogridService.mapOccurences($scope.occurrences, donors, genes);

          // Clean gene & donor data before using for oncogrid. 
          var donorObs = _.map(observations, 'donorId');
          var geneObs = _.map(observations, 'geneId');
          donors = _.filter(donors, function(d) { return donorObs.indexOf(d.id) >= 0});
          genes = _.filter(genes, function(g) { return geneObs.indexOf(g.id) >= 0});

          if (observations.length === 0) {
            $('#oncogrid-controls').toggle();
            $('#oncogrid-no-data').toggle();
            return; 
          }

          var sortInt = function (field) {
            return function (a, b) {
              return b[field] - a[field];
            };
          };

          var sortBool = function (field) {
            return function (a, b) {
              if (a[field] && !b[field]) {
                return -1;
              } else if (!a[field] && b[field]) {
                return 1;
              } else {
                return 0;
              }
            };
          };

          var sortByString = function (field) {
            return function (a, b) {
              if (a[field] > b[field]) {
                return 1;
              } else if (a[field] < b[field]) {
                return -1;
              } else {
                return 0;
              }
            };
          };

          var donorTracks = [
            { 'name': gettextCatalog.getString('Age at Diagnosis'), 
              'fieldName': 'age', 'type': 'int', 'sort': sortInt, 'group': 'Clinical'},
            { 'name': gettextCatalog.getString('Vital Status'), 
              'fieldName': 'vitalStatus', 'type': 'vital', 'sort': sortByString, 'group': 'Clinical' },
            { 'name': gettextCatalog.getString('Survival Time'), 
              'fieldName': 'survivalTime', 'type': 'survival', 'sort': sortInt, 'group': 'Clinical'},
            { 'name': gettextCatalog.getString('Sex'), 'fieldName': 'sex', 'type': 'sex', 'sort': sortByString, 'group': 'Clinical' },
            { 'name': 'CNSM', 'fieldName': 'cnsmExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'STSM', 'fieldName': 'stsmExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'SGV', 'fieldName': 'sgvExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'METH-A' , 
              'fieldName': 'methArrayExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'METH-S', 
              'fieldName': 'methSeqExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'EXP-A', 
              'fieldName': 'expArrayExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'EXP-S', 
              'fieldName': 'expSeqExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'PEXP', 'fieldName': 'pexpExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'miRNA-S', 
              'fieldName': 'mirnaSeqExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'JCN', 'fieldName': 'jcnExists', 'type': 'bool', 'sort': sortBool, 'group': 'Data Types' },
            { 'name': 'PCAWG', 'fieldName': 'pcawg', 'type': 'bool', 'sort': sortBool, 'group': 'Study' }
          ];

          var maxSurvival = _.max(_.map(donors, function(d) { return d.survivalTime } ));

          var donorOpacity = function (d) {
            if (d.type === 'int') {
              return d.value / 100;
            } else if (d.type === 'vital' || d.type === 'sex') {
              return 1;
            } else if (d.type === 'bool') {
              return d.value ? 1 : 0;
            } else if (d.type === 'survival') {
              return d.value / maxSurvival;
            } else {
              return 0;
            }
          };

          var geneTracks = [
            { 'name': gettextCatalog.getString('# Donors affected '),
               'fieldName': 'totalDonors', 'type': 'int', 'sort': sortInt, 'group': 'ICGC' },
            { 'name': gettextCatalog.getString('Curated Gene Census '),
               'fieldName': 'cgc', 'type': 'bool', 'sort': sortBool, 'group': 'Gene Sets'}
          ];

          var maxDonorsAffected = _.max(genes, function (g) { return g.totalDonors }).totalDonors;

          var geneOpacity = function (g) {
            if (g.type === 'int') {
              return g.value / maxDonorsAffected;
            } else if (g.type === 'bool') {
              return g.value ? 1 : 0;
            } else {
              return 1;
            }
          };

          var gridClick = function (o) {
            window.location = obsSearch +
              '{"mutation":{"id": {"is":["' + o.id + '"]}}}';
          };

          var donorClick = function (d) {
            window.location = /donors/ + d.id +
              '?filters={"mutation":{"functionalImpact":{"is":["High"]}}}';
          };

          var donorHistogramClick = function (d) {
            window.location = donorSearch +
              '{"donor":{"id":{"is": ["' + d.id + '"]}}, "mutation":{"functionalImpact":{"is":["High"]}},' + 
                '"gene":{"id":{"is":["ES:' + $scope.geneSet + '"]}}}';
          };

          var geneClick = function (g) {
            window.location = /genes/ + g.id +
              '?filters={"mutation":{"functionalImpact":{"is":["High"]}}}';
          };

          var geneHistogramClick = function (g) {
            window.location = geneSearch +
              '{"gene":{"id":{"is": ["' + g.id + '"]}}, "mutation":{"functionalImpact":{"is":["High"]}},'+ 
                '"donor":{"id":{"is":["ES:' + $scope.donorSet + '"]}}}';
          };

          var colorMap = {
            'missense_variant': '#ff9b6c',
            'frameshift_variant': '#57dba4',
            'stop_gained': '#af57db',
            'start_lost': '#ff2323',
            'stop_lost': '#d3ec00',
            'initiator_codon_variant': '#5abaff'
          };

          var trackLegends = {
            'ICGC': OncogridService.icgcLegend(maxDonorsAffected),
            'Gene Sets': OncogridService.geneSetLegend(),
            'Clinical': OncogridService.clinicalLegend(maxSurvival),
            'Data Types': OncogridService.dataTypeLegend(),
            'Study': OncogridService.studyLegend()
          };

          var templates = {
            mainGridCrosshair: `
              <div class="og-crosshair-tooltip">
                {{#donor}}
                <div>
                  <span><b>Donor</b>:&nbsp;</span>
                  <span>{{donor.id}}</span>
                </div>
                {{/donor}}
                {{#gene}}
                <div>
                  <span><b>Gene</b>:&nbsp;</span>
                  <span>{{gene.symbol}}</span>
                </div>
                {{/gene}}
                {{#obs}}
                <div>
                  <span><b>Mutations</b>:&nbsp;</span>
                  <span>{{obs}}</span>
                </div>
                {{/obs}}`
          };

          $scope.params = {
            donors: donors,
            genes: genes,
            observations: observations,
            element: $element.find('#oncogrid-div').get(0),
            height: 150,
            width: 680,
            colorMap: colorMap,
            gridClick: gridClick,
            heatMap: false,
            grid: true,
            minCellHeight: 8,
            trackHeight: 12,
            trackLegends: trackLegends,
            trackLegendLabel: '<i class="fa fa-question-circle legend-icon baseline"></i>',
            trackPadding: 25,
            templates,
            donorTracks: donorTracks,
            donorOpacityFunc: donorOpacity,
            donorClick: donorClick,
            donorHistogramClick: donorHistogramClick,
            geneTracks: geneTracks,
            geneOpacityFunc: geneOpacity,
            geneClick: geneClick,
            geneHistogramClick: geneHistogramClick,
            margin: { top: 30, right: 50, bottom: 200, left: 80 }
          };

          $scope.OncoCtrl.initGrid(_.cloneDeep($scope.params));
        };

        $scope.cleanActives = function () {
          $('#oncogrid-controls').show();
          $('#oncogrid-no-data').hide();

          $('#crosshair-button').removeClass('active');
          $('#heat-button').removeClass('active');
          $('#grid-button').removeClass('active');

          $('#og-crosshair-message').hide();
          var gridDiv = $element.find('#oncogrid-div');
          gridDiv.addClass('og-pointer-mode'); 
          gridDiv.removeClass('og-crosshair-mode');
        };

        function processItem() {
          if (!$scope.item) {
            return;
          }
            var getName = type => _(localStorageService.get('entity'))
                            .filter( e => e.id === $scope.item[type])
                            .map( e => e.name)
                            .value()[0];

            $scope.geneSetName = getName('geneSet');
            $scope.donorSetName = getName('donorSet');
            $element.find('#oncogrid-spinner').toggle(true);
            createLinks();
            $scope.materializeSets().then(function () {
              if ($scope.OncoCtrl.grid) {
                $scope.cleanActives();
                $scope.OncoCtrl.grid.destroy();
              }
              $element.find('#oncogrid-spinner').toggle(false);

              // Temporary fix:
              //http://stackoverflow.com/a/23444942
              $('.btn-onco').click(function () {
                // Removes focus of the button.
                $(this).blur();
              });
              $scope.repoLink = SetService.createRepoLink({ id: $scope.item.donorSet });
              $scope.crosshairMode = false;
              $scope.initOnco();
              $('#grid-button').addClass('active');
            });
        }

        $scope.$watch('item', processItem);
        processItem();

        $scope.fullScreenHandler = function () {
          if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
            setTimeout(function () {
                $scope.OncoCtrl.getGrid().resize(680, 150, false);
            }, 0);
          } else {
            // TODO: Maybe come up with a better way to deal with fullscreen spacing.
            setTimeout(function () { 
              $scope.OncoCtrl.getGrid().resize(screen.width - 400, screen.height - 400, true);
            }, 0);
          }
          var fButton = $('#og-fullscreen-button');
          fButton.toggleClass('icon-resize-full');
          fButton.toggleClass('icon-resize-small');
        };

        if (document.addEventListener) {
          document.addEventListener('webkitfullscreenchange', $scope.fullScreenHandler);
          document.addEventListener('mozfullscreenchange', $scope.fullScreenHandler);
          document.addEventListener('fullscreenchange', $scope.fullScreenHandler);
        }

        $scope.$on('$destroy', function () {
          if (typeof $scope.OncoCtrl.grid !== 'undefined') {
            $scope.OncoCtrl.grid.destroy();
          }

          document.removeEventListener('webkitfullscreenchange', $scope.fullScreenHandler);
          document.removeEventListener('mozfullscreenchange', $scope.fullScreenHandler);
          document.removeEventListener('fullscreenchange', $scope.fullScreenHandler);
        });

      }
    };
  });

})(jQuery);