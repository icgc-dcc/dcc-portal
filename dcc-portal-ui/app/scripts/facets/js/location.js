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

angular.module('icgc.facets.location', []);

angular.module('icgc.facets.location')
  .controller('locationFacetCtrl', function ($scope, Facets, FilterService, Chromosome) {

  var submitted;

  $scope.regex = /^(chr)?(x|y|mt|[0-9]+)(:\d+(\-\d+)?)?$/i;

  submitted = false;

  function setup() {
    $scope.actives = Facets.getActiveLocations({
      type: $scope.type,
      facet: $scope.facetName
    });
    
    var params = {type: $scope.type,facet: $scope.facetName};
    $scope.isNot = Facets.isNot(params);
    $scope.activeClass = Facets.isNot(params) ? 
      't_facets__facet__not' : '';
  }

  function checkLocation() {
    if (submitted) {
      var input = $scope.location;

      if ($scope.regex.exec(input)) {
        $scope.state = 'valid';
      } else {
        $scope.state = 'invalid';
      }

      var chr = input.split(':')[0].replace(/chr/i, '');
      var range = input.split(':')[1];

      if (angular.isDefined(range) && $scope.state === 'valid') {
        range = range.split('-');
        if (range.length === 2) {
          $scope.state = Chromosome.validate(chr, range[0], range[1])? 'valid' : 'invalid';
        } else {
          $scope.state = Chromosome.validate(chr, range[0])? 'valid' : 'invalid';
        }
      }
      if (input === '') {
        $scope.state = 'unsubmitted';
      }

    } else {
      $scope.state = 'unsubmitted';
    }
  }

  $scope.removeTerm = function (term) {
    Facets.removeTerm({
      type: $scope.type,
      facet: $scope.facetName,
      term: term
    });
  };
  
  $scope.notFacet = function() {
      Facets.notFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
    };
    
    $scope.isFacet = function() {
      Facets.isFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
    };

  $scope.submit = function () {
    submitted = true;
    checkLocation();
    if ($scope.state === 'valid') {
      Facets.addTerm({
        type: $scope.type,
        facet: $scope.facetName,
        term: $scope.location
      });
      $scope.location = '';
      submitted = false;
    }
  };

  $scope.keydown = function (e) {
    if (e.keyCode === 13) {
      $scope.submit();
    }
  };

  $scope.removeFacet = function () {
    submitted = false;
    $scope.location = '';

    Facets.removeFacet({
      type: $scope.type,
      facet: $scope.facetName
    });
  };

  $scope.$watch('location', function (n, o) {
    if (n === o) {
      return;
    }
    checkLocation();
  });

  // Needed if term removed from outside scope
  $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, setup);

  setup();
});

angular.module('icgc.facets.location').directive('locationFacet', function () {
  return {
    restrict: 'A',
    scope: {
      facetName: '@',
      label: '@',
      type: '@',
      example: '@',
      placeholder: '@'
    },
    templateUrl: '/scripts/facets/views/location.html',
    controller: 'locationFacetCtrl'
  };
});
