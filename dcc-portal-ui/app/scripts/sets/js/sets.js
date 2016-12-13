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

  angular.module('icgc.sets', [
    'icgc.sets.directives',
    'icgc.sets.controllers',
    'icgc.sets.services'
  ]);
})();


(function() {
  'use strict';

  var module = angular.module('icgc.sets.controllers', []);

  module.controller('SetUploadController',
    function($scope, $rootScope, $modalInstance, $timeout, LocationService, SetService, Settings, 
      setType, setLimit, setUnion, selectedIds, FiltersUtil, FilterService, $filter, 
      CompoundsService, GeneSymbols, SetNameService) {

    $scope.setLimit = setLimit;
    $scope.isValid = false;
    
    // Input data parameters
    $scope.params = {};
    $scope.params.setName = '';
    $scope.params.setDescription = '';
    $scope.params.setType = setType;
    $scope.params.setLimit  = setLimit;
    $scope.params.setSize = 0;
    $scope.params.setUnion = setUnion;
    $scope.params.selectedIds = selectedIds;
    $scope.params.isLoading = true;


    /**
     * Save either a new set from search filters, or a derived set from existing sets
     */
    $scope.submitNewSet = function() {
      var params = {}, sortParam;

      params.type = $scope.params.setType;
      params.name = $scope.params.setName;
      params.size = $scope.params.setSize;

      if (angular.isDefined($scope.params.setLimit)) {
        params.filters = LocationService.filters();

        sortParam = LocationService.getJqlParam($scope.setType + 's');

        if (angular.isDefined(sortParam)) {
          params.sortBy = sortParam.sort;
          if (sortParam.order === 'asc') {
            params.sortOrder = 'ASCENDING';
          } else {
            params.sortOrder = 'DESCENDING';
          }
        }
      }
      if (angular.isDefined($scope.params.setUnion)) {
        params.union = $scope.params.setUnion;
      }

      if (angular.isDefined($scope.params.setLimit)) {
        SetService.addSet(setType, params).then((set) => {
          $rootScope.$broadcast(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, set);
        });
      } else {
        SetService.addDerivedSet(setType, params).then((set) => {
          $rootScope.$broadcast(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, set);
        });
      }

      // Reset
      $modalInstance.dismiss('cancel');
    };

    $scope.submitNewExternalSet = function() {
      var params = {};

      params.type = $scope.params.setType;
      params.name = $scope.params.setName;
      params.size = $scope.params.setSize;
      params.sortBy = 'fileName';

      if (angular.isDefined($scope.params.setLimit)) {
        params.filters = LocationService.filters();

        if (! _.isEmpty ( $scope.params.selectedIds)) {
          // Only used for files
          _.set (params.filters, 'file.id.is',  $scope.params.selectedIds);
        }
      }

      if (angular.isDefined($scope.params.setUnion)) {
        params.union = $scope.params.setUnion;
      }

      if (angular.isDefined($scope.params.setLimit)) {
        SetService.addExternalSet(setType, params).then((set) => {
          $rootScope.$broadcast(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, set);
        });
      }

      // Reset
      $modalInstance.dismiss('cancel');
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };


    /**
     * Validate size, name
     */
    $scope.validateInput = function() {
      if (_.isEmpty($scope.params.setName) === true) {
        $scope.isValid = false;
        return;
      }

      if ($scope.params.setLimit) {
        if (isNaN($scope.params.setSize) === true) {
          $scope.isValid = false;
          return;
        }

        if ($scope.params.setSize <= 0 || $scope.params.setSize > $scope.params.setSizeLimit) {
          $scope.isValid = false;
          return;
        }
      }
      $scope.isValid = true;
    };

    function updateSetName(){
      return SetNameService.getSetFilters()
        .then(filters => SetNameService.getSetName(filters, $scope.params.setType))
        .then(setName => {
          $scope.params.setName = setName;
        });
    }

    // Start. Get limit restrictions from the server side
    Settings.get().then(function(settings) {
      $scope.params.setSize = Math.min($scope.setLimit || 0, settings.maxNumberOfHits);
      $scope.params.setSizeLimit = $scope.params.setSize;
      updateSetName();
      $timeout(function () {
        $scope.params.isLoading = false;
        $scope.validateInput();
      }, 500);
      $scope.uiFilters = LocationService.filters();
    });

  });

})();


(function () {
  'use strict';

  var module = angular.module('icgc.sets.directives', []);

  /**
   * Display s subset from set operations using set notation
   */
  module.directive('setNotation', function(SetOperationService) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        item: '=',
        setList: '='
      },
      template: '<div>' +
        '(<span data-ng-repeat="setId in item.intersection">' +
        ' <span data-ng-bind-html="getName(setId)"></span> ' +
        '<span data-ng-if="!$last" class="set-symbol">&cap;</span>' +
        '</span>)' +
        '<span data-ng-if="item.exclusions.length > 0" class="set-symbol"> &minus; </span>' +
        '<span data-ng-if="item.exclusions.length > 0">(</span>' +
        '<span data-ng-repeat="setId in item.exclusions">' +
        ' <span data-ng-bind-html="getName(setId)"></span> ' +
        '<span data-ng-if="!$last" class="set-symbol">&cup;</span>' +
        '</span>' +
        '<span data-ng-if="item.exclusions.length > 0">)</span>' +
        '</div>',
      link: function($scope) {
        $scope.getName = function(setId) {
          return SetOperationService.getSetShortHand(setId, $scope.setList);
        };
      }
    };
  });

  module.directive('setOperation', function($location, $timeout, $filter, $modal, Page, LocationService,
    Settings, SetService, SetOperationService, Extensions, RouteInfoService) {
    var dataRepoRouteInfo = RouteInfoService.get ('dataRepositories');
    var dataRepoUrl = dataRepoRouteInfo.href;

    return {
      restrict: 'E',
      scope: {
        item: '='
      },
      templateUrl: '/scripts/sets/views/sets.result.html',
      link: function($scope, $element) {
        var vennDiagram;

        $scope.selectedTotalCount = 0;
        $scope.current = [];
        $scope.selected = [];

        $scope.dialog = {
        };

        function toggleSelection(intersection, count) {
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(intersection, subset);
          });

          if (existIdex === -1) {
            $scope.selected.push(intersection);
            $scope.selectedTotalCount += count;
          } else {
            _.remove($scope.selected, function(subset) {
              return SetOperationService.isEqual(intersection, subset);
            });
            if (SetOperationService.isEqual(intersection, $scope.current) === true) {
              $scope.current = [];
            }
            $scope.selectedTotalCount -= count;
          }
          vennDiagram.toggle(intersection);
        }

        function wait(id, numTries, callback) {
          if (numTries <= 0) {
            Page.stopWork();
            return;
          }

          SetService.getMetaData([id]).then(function(data) {
            if (data[0].state === 'FINISHED') {
              Page.stopWork();
              callback();
            } else {
              $timeout( function() {
                wait(id, --numTries, callback);
              }, 800);
            }
          });
        }


        // Compute the union of single item, of currently selected
        function computeUnion(item) {
          var union = [];
          if (angular.isDefined(item) ) {
            union.push({
              intersection: item.intersection,
              exclusions: item.exclusions
            });
          } else {
            $scope.selected.forEach(function(selectedIntersection) {
              for (var j=0; j < $scope.data.length; j++) {
                if (SetOperationService.isEqual($scope.data[j].intersection, selectedIntersection)) {
                  var newUnion = _.pick($scope.data[j], ['exclusions', 'intersection']);
                  union.push(newUnion);
                  break;
                }
              }
            });
          }
          return union;
        }

        $scope.dataRepoTitle = dataRepoRouteInfo.title;

        $scope.saveDerivedSet = function() {

          $modal.open({
            templateUrl: '/scripts/sets/views/sets.upload.html',
            controller: 'SetUploadController',
            resolve: {
              setType: function() {
                return $scope.item.type.toLowerCase();
              },
              setLimit: function() {
                return undefined;
              },
              setUnion: function() {
                return $scope.dialog.setUnion;
              },
              selectedIds: function() {
                return $scope.selected;
              }
            }
          });
        };

        $scope.downloadDerivedSet = function(item) {
          Page.startWork();
          var params, type, name;
          type = $scope.item.type.toLowerCase();
          name = 'Input ' + type + ' set';

          params = {
            union: computeUnion(item),
            type: $scope.item.type.toLowerCase(),
            name: name
          };
          return SetService.materializeSync(type, params).then(function(data) {
            Page.stopWork();
            $modal.open({
              templateUrl: '/scripts/downloader/views/request.html',
              controller: 'DownloadRequestController',
              resolve: {
                filters: function() { return {donor:{id:{is:[Extensions.ENTITY_PREFIX + data.id]}}} }
              }
            });
          });
        };

        $scope.viewInExternal = function (item) {
          Page.startWork();
          var params, type, name;
          type = $scope.item.type.toLowerCase();
          name = 'Input ' + type + ' set';

          params = {
            union: computeUnion(item),
            type: $scope.item.type.toLowerCase(),
            name: name
          };

          var filterTemplate = function(id) {
            if (type === 'donor') {
              return {file: {donorId: {is: [Extensions.ENTITY_PREFIX + id]}}};
            } else if (type === 'file') {
              return {file: {id: {is: [Extensions.ENTITY_PREFIX + id]}}};
            }
          };

          SetService.materializeSync(type, params).then(function(data) {
            Page.stopWork();
            if (! data.id) {
              throw new Error('The set id was not found!', data);
            } else {
              var newFilter = JSON.stringify(filterTemplate(data.id));
              $location.path (dataRepoUrl).search ('filters', newFilter);
            }
          });
        };

        // Export the subset(s), materialize the set along the way
        $scope.export = function(item) {
          var params, name, type = $scope.item.type.toLowerCase();
          name = 'Input ' + type + ' set';

          params = {
            union: computeUnion(item),
            type: $scope.item.type.toLowerCase(),
            name: name
          };
          SetService.materialize(type, params).then(function(data) {
            function exportSet() {
              SetService.exportSet(data.id);
            }
            wait(data.id, 10, exportSet);
          });
        };


        // Redirect to advanced search to show the subset(s), materialize the set along the way
        $scope.redirect = function(item) {
          var params, type, name, path = '/search';

          type = $scope.item.type.toLowerCase();
          name = 'Input ' + type + ' set';

          // Determine which tab we should land on
          if (['gene', 'mutation'].indexOf(type) >= 0) {
            path += '/' + type.charAt(0);
          }

          params = {
            union: computeUnion(item),
            type: $scope.item.type.toLowerCase(),
            name: name,
            isTransient: true
          };

          Page.startWork();
          SetService.materialize(type, params).then(function(data) {
            function redirect2Advanced() {
              var filters = {};
              filters[type] = {};
              filters[type].id = { is: [Extensions.ENTITY_PREFIX+data.id] };

              $location.path(path).search({filters: angular.toJson(filters)});
            }
            wait(data.id, 10, redirect2Advanced);
          });
        };


        $scope.calculateUnion = function(item) {
          $scope.dialog.setUnion = computeUnion(item);
          $scope.dialog.setType = $scope.item.type.toLowerCase();
        };

        $scope.selectAll = function() {
          $scope.selected = [];
          $scope.selectedTotalCount = 0;
          $scope.data.forEach(function(set) {
            $scope.selected.push(set.intersection);
            vennDiagram.toggle(set.intersection, true);
            $scope.selectedTotalCount += set.count;
          });
        };

        $scope.selectNone = function() {
          $scope.data.forEach(function(set) {
            vennDiagram.toggle(set.intersection, false);
          });
          $scope.selected = [];
          $scope.selectedTotalCount = 0;
        };

        $scope.isHighlighted = function(ids) {
          return SetOperationService.isEqual(ids, $scope.current);
        };

        $scope.isSelected = function(ids) {
          var existIdex = _.findIndex($scope.selected, function(subset) {
            return SetOperationService.isEqual(ids, subset);
          });
          return existIdex >= 0;
        };

        $scope.tableMouseEnter = function(ids) {
          vennDiagram.toggleHighlight(ids, true);
          $scope.current = ids;
        };

        $scope.tableMouseOut = function(ids) {
          vennDiagram.toggleHighlight(ids, false);
          $scope.current = [];
        };

        $scope.getSetShortHand = SetOperationService.getSetShortHand;
        $scope.toggleSelection = toggleSelection;

        var config = {
          // Because SVG urls are based on <base> tag, we need absolute path
          urlPath: $location.url(),

          mouseoverFunc: function(d) {
            $scope.$apply(function() {
              $scope.current = d.data;
            });
          },

          mouseoutFunc: function() {
            $scope.$apply(function() {
              $scope.current = [];
            });
          },
          clickFunc: function(d) {
            $scope.$apply(function() {
              toggleSelection(d.data, d.count);
              $scope.current = [];
            });
          },

          valueLabelFunc: function(val) {
            return $filter('number')(val);
          },
          setLabelFunc: function(id) {
            return $scope.setList.indexOf(id) + 1;
          }
        };


        function initVennDiagram() {
          $scope.setType = $scope.item.type.toLowerCase();
          $scope.current = [];
          $scope.selected = [];
          $scope.selectedTotalCount = 0;

          // Normalize and sort for tabluar display
          $scope.data = SetOperationService.sortData($scope.item.result);
          $scope.vennData = SetOperationService.transform($scope.data);
          $scope.setList = SetOperationService.extractUniqueSets($scope.data);

          SetService.getMetaData($scope.setList).then(function(results) {
            $scope.setMap = {};
            results.forEach(function(set) {
              set.advLink = SetService.createAdvLink(set);
              $scope.setMap[set.id] = set;
            });

             // Because SVG urls are based on <base> tag, we need absolute path
            config.urlPath = $location.url();
            config.setLabelFunc = id => SetOperationService.getSetShortHandSVG(id, _.map(results, 'id'));

            vennDiagram = new dcc.Venn23($scope.vennData, config);
            vennDiagram.render( $element.find('.canvas')[0]);
          });
        }

        $scope.$watch('item', function(n) {
          $scope.isDeprecated = false;
          if (n && n.result) {
            Settings.get().then(function(settings) {
              $scope.downloadsEnabled = settings.downloadEnabled || false;
              // The maximum allowed items from union operation
              $scope.unionMaxLimit = settings.maxNumberOfHits * settings.maxMultiplier;

              // Check if the analysis is still valid with respect to current data
              if (! n.version || settings.dataVersion !== n.version) {
                $scope.isDeprecated = true;
              } else {
                $scope.isDeprecated = false;
              }

              initVennDiagram();
            });
          }
        });

      }
    };
  });

})();
