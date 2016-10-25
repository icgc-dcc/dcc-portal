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

  var module = angular.module('icgc.facets.terms', ['icgc.facets.helpers']);

  module.controller('termsCtrl', 
    function ($scope, $filter, Facets, HighchartsService, ProjectCache, ValueTranslator, LocationService) {
 
      $scope.resetPaginationOnChange = _.isUndefined($scope.resetPaginationOnChange) ? true : $scope.resetPaginationOnChange;  

    // Translation on UI is slow, do in here
    function addTranslations (terms, facetName, missingText) {
      var t = ValueTranslator;

      terms.forEach (function (term) {
        var termName = term.term;
        term.label = t.translate (termName, facetName);

        if (termName === '_missing' && missingText) {
          term.label = missingText;
        }

        if (_.contains (['projectId', 'projectCode'], facetName)) {
          ProjectCache.getData().then (function (cache) {
            term.tooltip = cache[termName];
          });
        } else {
          term.tooltip = t.tooltip (termName, facetName);
        }
      });
    }

    function splitTerms() {
      var facetName = $scope.facetName;
      var terms = $scope.facet.terms;
      var missingText = $scope.missingText;

      var actives = Facets.getActiveTerms ({
        type: $scope.type,
        facet: facetName,
        terms: terms
      });

      var params = {type: $scope.type,facet: $scope.facetName};
      $scope.isNot = Facets.isNot(params);
      $scope.activeClass = Facets.isNot(params) ? 
        't_facets__facet__not' : '';

      $scope.actives = actives;
      addTranslations (actives, facetName, missingText);

      var inactives = Facets.getInactiveTerms ({
        actives: actives,
        terms: terms
      });

      $scope.inactives = inactives;
      addTranslations (inactives, facetName, missingText);
    }

    function refresh() {
      if ($scope.facet) {
        splitTerms();
      }
      
      var params = {type: $scope.type,facet: $scope.facetName};
      $scope.isNot = Facets.isNot(params);
      $scope.activeClass = Facets.isNot(params) ? 
        't_facets__facet__not' : '';
      $scope.displayLimit = $scope.expanded === true? $scope.inactives.length : 5;
    }

    function onChange() {
      if ($scope.resetPaginationOnChange) {
        LocationService.goToFirstPage($scope.type + 's');
      }
    }

    $scope.displayLimit = 5;

    $scope.addTerm = function (term) {
      Facets.addTerm({
        type: $scope.type,
        facet: $scope.facetName,
        term: term
      });
      onChange();
    };

    $scope.removeTerm = function (term) {
      Facets.removeTerm({
        type: $scope.type,
        facet: $scope.facetName,
        term: term
      });
      onChange();
    };

    $scope.removeFacet = function () {
      Facets.removeFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
      onChange();
    };
    
    $scope.notFacet = function() {
      Facets.notFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
      onChange();
    };

    $scope.isFacet = function() {
      Facets.isFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
      onChange();
    };
    
    $scope.bar = function (count) {
      return {width: (count / ($scope.facet.total + $scope.facet.missing) * 100) + '%'};
    };

    $scope.iconClass = function (data) {
      var f = $scope.iconGetter();
      return _.isFunction (f) ? f (data) : '';
    };

    $scope.toggle = function() {
      $scope.expanded = !$scope.expanded;
      if (!$scope.collapsed) {
        $scope.displayLimit = $scope.expanded === true? $scope.inactives.length : 5;
      }
    };

    $scope.sites = HighchartsService.primarySiteColours;

    refresh();
    $scope.$watch('facet', refresh);
  });

  module.directive('terms', function () {
    return {
      restrict: 'E',
      scope: {
        // Routing
        type: '@',
        facetName: '@',

        // Label
        label: '@',
        hideCount: '=',
        hideText: '@',
        missingText: '@',

        facet: '=',
        defined: '@',
        collapsed: '@',

        iconGetter: '&iconGetter',
        showWhenEmpty: '<',

        resetPaginationOnChange: '<'
      },
      transclude: true,
      templateUrl: '/scripts/facets/views/terms.html',
      controller: 'termsCtrl'
    };
  });

  module.directive('activeTerm', function () {
    return {
      restrict: 'A',
      //require: '^terms',
      link: function (scope, element) {

        scope.mouseOver = function () {
          element.find('i').removeClass('icon-ok').addClass('icon-cancel');
          element.find('.t_facets__facet__terms__active__term__label__text span').css({textDecoration: 'line-through'});
        };

        scope.mouseLeave = function () {
          element.find('i').removeClass('icon-cancel').addClass('icon-ok');
          element.find('.t_facets__facet__terms__active__term__label__text span').css({textDecoration: 'none'});
        };
      }
    };
  });
})();
