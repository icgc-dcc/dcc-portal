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

'use strict';

import {ensureArray, ensureString, partiallyContainsIgnoringCase} from '../../common/js/ensure-input';

angular.module('icgc.ui.suggest', ['ngSanitize', 'icgc.common.text.utils']);

angular.module('icgc.ui.suggest').controller('suggestController',
  function ($scope, debounce, Keyword, Abridger, SetService) {
  var pageSize = 5, inactive = -1;

  $scope.active = inactive;
  $scope.entitySetSearch = false;

  $scope.clearActive = function () {
    if ($scope.results) {
      for (var i = 0; i < $scope.results.hits.length; ++i) {
        $scope.results.hits[i].active = false;
      }
      $scope.active = inactive;
    }
  };

  $scope.setActive = function (active) {
    $scope.clearActive();
    $scope.active = active;
    $scope.results.hits[active].active = true;
  };

  $scope.cycle = function (val) {
    $scope.showResults();

    var active = $scope.active + val;

    if (active >= $scope.results.hits.length) {
      active = 0;
    } else if (active < 0) {
      active = $scope.results.hits.length - 1;
    }

    $scope.setActive(active);
  };

  $scope.showResults = function () {
    var results = $scope.results ? !!$scope.results.hits.length : false;
    return !!($scope.focus && $scope.query.length >= 2 && results);
  };

  $scope.onChangeFn = function () {
    if ($scope.query.length >= 2) {
      var saved = $scope.query;
      $scope.focus = true;
      $scope.isBusy = true;

      Keyword.getList({q: $scope.query, type: $scope.type ? $scope.type : '', size: pageSize})
        .then(function (response) {
          if ($scope.type === 'file' && _.isEmpty(response.hits)) {
            SetService.getMetaData($scope.query).then(function(setResponse) {
              $scope.entitySetSearch = true;
              $scope.isBusy = false;
              $scope.active = inactive;
              $scope.activeQuery = saved;
              $scope.results = {hits: setResponse};
            });
          } else {
            $scope.entitySetSearch = false;
            $scope.isBusy = false;
            $scope.active = inactive;
            $scope.activeQuery = saved;
            $scope.results = response;
          }
        });
    }
  };

  $scope.badgeStyleClass = function (type) {
    var definedType = _.contains (['pathway', 'go_term', 'curated_set'], type) ? 'geneset' : type;
    return 't_badge t_badge__' + definedType;
  };

  var maxAbrigementLength = 80;
  var abridger = Abridger.of (maxAbrigementLength);

  function abridge (array) {
    var target = $scope.query;

    return abridger.abridge (array, target);
  }

  var maxConcat = 3;

  function concatMatches (array, target) {
    var matches = _(ensureArray (array))
      .filter (function (element) {
        return partiallyContainsIgnoringCase (ensureString (element), target);
      })
      .take (maxConcat);

    return matches.join (', ');
  }

  function reducedToMatchString (result, value) {
    if (_.isEmpty (result)) {
      result = concatMatches (value, $scope.query);
    }
    return result;
  }

  $scope.findFirstMatch = function (compound) {
    var singleValueFields = ['id', 'drugClass', 'inchikey'];
    var match = _(singleValueFields)
      .map (function (field) {
        return [_.get (compound, field, '')];
      })
      .reduce (reducedToMatchString, '')
      .trim();

    if (! _.isEmpty (match)) {
      return match;
    }

    var shortStringArrayFields = ['synonyms', 'atcCodes', 'atcLevel5Codes', 'trialConditionNames',
      'externalReferencesDrugbank', 'externalReferencesChembl'];
    match = _(shortStringArrayFields)
      .map (function (field) {
        return _.get (compound, field, []);
      })
      .reduce (reducedToMatchString, '')
      .trim();

    if (! _.isEmpty (match)) {
      return match;
    }

    var longStringArrayFields = ['atcCodeDescriptions', 'trialDescriptions'];
    match = _(longStringArrayFields)
      .map (function (field) {
        return _.get (compound, field, []);
      })
      .reduce (function (result, value) {
        if (_.isEmpty (result)) {
          result = abridge (value);
        }
        return result;
      }, '')
      .trim();

    return match;
  };

  $scope.onChange = debounce($scope.onChangeFn, 200, false);
});

angular.module('icgc.ui.suggest').directive('suggest', function ($compile, $document, $location, RouteInfoService,
  Extensions) {
  var dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;
  var compoundUrl = RouteInfoService.get ('drugCompound').href;

  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    templateUrl: 'suggestInput',
    controller: 'suggestController',
    link: function (scope, element, attrs) {
      var suggest;
      suggest = attrs.suggest;

      if (suggest === 'tags') {
        element.after($compile('<tags-popup></tags-popup>')(scope));
      } else {
        element.after($compile('<suggest-popup></suggest-popup>')(scope));
      }

      function addId() {
        var item;

        if (scope.active > -1) {
          item = scope.results.hits[scope.active];
        } else {
          item = scope.results.hits[0];
        }

        if (scope.entitySetSearch) {
          item.id = Extensions.ENTITY_PREFIX + item.id;
        }
        scope.addTerm(item);
        scope.query = '';
      }

      function goTo() {
        var item, selected;

        if (!scope.query || scope.query.length < 2) {
          return;
        }

        function url(item) {
          var resourceType = item.type;

          if (_.contains (['curated_set', 'go_term', 'pathway'], resourceType)) {
            resourceType = 'geneset';
          } else if ('file' === resourceType) {
            return dataRepoFileUrl + item.id;
          } else if ('compound' === resourceType) {
            return compoundUrl + item.id;
          }

          return '/' + resourceType + 's/' + item.id;
        }

        // Go the the 'active' hit
        if (scope.active > -1) {
          selected = scope.results.hits[scope.active];
          $location.path(url(selected)).search({});
        } else {
          // If there is only one hit just go to the page
          if (scope.results && scope.results.hits.length === 1) {
            item = scope.results.hits[0];
            $location.path(url(item)).search({});
            // Otherwise make a search
          } else {
            $location.path('/q').search({q: scope.query});
          }
        }
      }

      scope.keypress = function (e) {
        if (e.which === 13 || e.keyCode === 13) {
          enter(e);
        } else if (e.which === 38 || e.keyCode === 38) {
          scope.cycle(-1);
        } else if (e.which === 40 || e.keyCode === 40) {
          scope.cycle(1);
        }
      };

      function enter(e) {
        if (scope.results === undefined) {
          return;
        }
        else { 
          if (suggest === 'tags') {
            addId();
          } else {
            goTo();
          }
        }

        scope.focus = false;
        scope.clearActive();
      }

      function blur() {
        scope.focus = false;
      }

      scope.$on('application:click', blur);
    }
  };
});

angular.module('icgc.ui.suggest').directive('suggestPopup', function (LocationService, RouteInfoService) {
  var dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;
  var compoundUrl = RouteInfoService.get ('drugCompound').href;

  return {
    restrict: 'E',
    replace: true,
    templateUrl: '/scripts/ui/views/suggest_popup.html',
    link: function (scope) {
      scope.mouseover = function (i) {
        scope.setActive(i);
      };

      scope.mouseout = function () {
        scope.clearActive();
      };


      scope.click = function (item) {
        var url = '';

        if (item) {
          var resourceType = item.type;

          switch(resourceType.toLowerCase()) {
            case 'curated_set':
            case 'go_term':
            case 'pathway':
              url = '/genesets/';
              break;
            case 'file':
              url = dataRepoFileUrl;
              break;
            case 'compound':
              url = compoundUrl;
              break;
            default:
              url = '/' + resourceType + 's/';
              break;
          }

          LocationService.goToPath(url + item.id);

        }
        else {
          LocationService.goToPath('/q', {q: scope.query});
        }
      };
    }
  };
});

angular.module('icgc.ui.suggest').directive('tagsPopup', function (Extensions) {
  return {
    restrict: 'E',
    replace: true,
    templateUrl: '/scripts/ui/views/tags_popup.html',
    link: function (scope) {
      scope.mouseover = function (i) {
        scope.setActive(i);
      };

      scope.mouseout = function () {
        scope.clearActive();
      };

      scope.click = function (item) {
        scope.query = '';
        if (scope.entitySetSearch) {
          item.id = Extensions.ENTITY_PREFIX + item.id;
        }
        scope.addTerm(item);
      };
    }
  };
});

angular.module('icgc.ui.suggest').run(function ($templateCache) {
  $templateCache.put('suggestInput',
    '<input ' +
    'type="search" ' +
    'ng-model="query" ' +
    'ng-change="onChange()" ' +
    'autocomplete="off"' +
    'ng-keydown="keypress($event)"' +
    'ng-transclude>');
});
