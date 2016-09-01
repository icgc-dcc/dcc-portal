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

  
  var module = angular.module('icgc.beacon', [
    'icgc.beacon.controllers'
  ]);
   
  module.config(function ($stateProvider) {
    $stateProvider.state('beacon', {
      url: '/ga4gh/beacon',
      templateUrl: 'scripts/beacon/views/beacon.html',
      controller: 'BeaconCtrl as BeaconCtrl'
    });
  });

})();


(function() {
  'use strict';

  var module = angular.module('icgc.beacon.controllers', []);

  var DATASET_ALL = 'All Projects';

  module.controller('BeaconCtrl', function($scope, LocationService, $location,$timeout, Page, Restangular, 
    Chromosome, gettextCatalog) {
      
    Page.setTitle(gettextCatalog.getString('Beacon'));
    Page.setPage('beacon');

    var lengths = Chromosome.get();

    $scope.hasInvalidParams = false;
    $scope.errorMessage = '';
    $scope.result = {
      exists:false,
      value:''
    };
    $scope.chromosomes = Object.keys(lengths);
    $scope.inProgress = false;
    $scope.lengths = lengths;
    $scope.invalidParams = {
      isPositon:false,
      isAllele:false,
      isReference:false
    };

    var saveParameters = function(){
      LocationService.setParam('proj',$scope.params.project.id);
      LocationService.setParam('chr',$scope.params.chr);
      LocationService.setParam('pos',$scope.params.position);
      LocationService.setParam('ref',$scope.params.reference);
      LocationService.setParam('ale',$scope.params.allele);
      LocationService.setParam('result',($scope.result.exists && !$scope.hasInvalidParams)?'true':'false');
    };

    var loadParameters = function(){
      var loadedProject = LocationService.getParam('proj');
      $scope.projects.every(function (p) {
        if(p.id === loadedProject){
          $scope.params.project = p;
          return false;
        }else{
          return true;
        }
      });

      $scope.params.chr = $scope.chromosomes.indexOf(LocationService.getParam('chr'))>-1?
        LocationService.getParam('chr'):'1';
      $scope.params.position = LocationService.getParam('pos')?
        LocationService.getParam('pos').replace( /[^0-9]+/g, '').replace(/^0+/,''):'';
      if(LocationService.getParam('ref')){
        $scope.params.reference = LocationService.getParam('ref');
      }
      $scope.params.allele = LocationService.getParam('ale')?
        LocationService.getParam('ale').replace( /[^ACTGactg>-]+/g, '').toUpperCase():'';
      $scope.result.exists = LocationService.getParam('result') === 'true'?true:false;
      $scope.checkParams();
    };

    var projectsPromise = Restangular.one('projects')
      .get({
        'field' : 'id',
        'sort':'id',
        'order':'asc',
        'size' : 100
      },{'Accept':'application/json'});

    $scope.checkParams = function(){

      $scope.hasInvalidParams = false;
      $scope.invalidParams = {
        isPositon:false,
        isAllele:false,
        isReference:false
      };
      $scope.errorMessage = '';

      // check that the position is less than length of chromosome
      if($scope.params.position && ($scope.params.position > lengths[$scope.params.chr])){
        $scope.invalidParams.isPosition = true;
        $scope.hasInvalidParams = true;
      }
      if($scope.params.reference && ($scope.params.reference !== 'GRCh37')){
        $scope.invalidParams.isReference = true;
        $scope.hasInvalidParams = true;
      }

      if($scope.params.allele && !(
         /^[ACTG]+$/.test($scope.params.allele) ||
         /^([ACTG])>\1[ACTG]+$/.test($scope.params.allele) ||
         /^->[ACTG]+$/.test($scope.params.allele) ||
         /^([ACTG])[ACTG]+>\\1$/.test($scope.params.allele) ||
         /^[ACTG]+>-$/.test($scope.params.allele))){
        $scope.invalidParams.isAllele = true;
        $scope.hasInvalidParams = true;
      }
      if(!($scope.params.reference && $scope.params.position && $scope.params.allele)){
        $scope.hasInvalidParams = true;
      }

      //Save state of things
      saveParameters();
    };

    $scope.submitQuery = function() {
      $scope.inProgress=true;
      var promise = Restangular.one('beacon', 'query')
      .get({
        'chromosome' : $scope.params.chr,
        'reference':$scope.params.reference,
        'position':$scope.params.position,
        'allele':$scope.params.allele ? $scope.params.allele : '',
        'dataset':$scope.params.project.id === DATASET_ALL ? '':$scope.params.project.id
      },{'Accept':'application/json'});


      promise.then(function(data){
        $scope.result.exists = true;
        $scope.result.value = data.response.exists;
        var url = data.getRequestedUrl();

        if(url.indexOf(location.protocol) !== 0){
          $scope.requestedUrl = location.protocol + '//' + location.host +
            url.substring(0,url.indexOf('?'))+'/query'+url.substring(url.indexOf('?'));
        }else{
          $scope.requestedUrl = url.substring(0,url.indexOf('?'))+'/query'+url.substring(url.indexOf('?'));
        }
        $timeout(function() {
          $scope.inProgress = false;
        }, 50);
        saveParameters();
      });

    };

    $scope.exampleQuery =  function(type){
      if(type === 'true'){
        $location.search({'proj':'All Projects', 'chr':'1','ref':'GRCh37', 'pos':'16918653','ale':'T',result:'true'});
      }else if(type === 'false'){
        $location.search({'proj':'PACA-CA', 'chr':'12','ref':'GRCh37', 'pos':'25398285','ale':'C',result:'true'});
      }else{
        $location.search({'proj':'All Projects', 'chr':'1','ref':'GRCh37', 'pos':'10000','ale':'G',result:'true'});
      }
      $scope.inProgress =true;
      $scope.hasInvalidParams = false;
      $scope.errorMessage = '';
      loadParameters();
      $scope.submitQuery();
    };

    $scope.resetQuery = function() {
      $scope.params = {
        project:$scope.projects[0],
        chr:'1',
        reference:'GRCh37',
      };
      $scope.hasInvalidParams = true;
      $scope.invalidParams = {
        isPositon:false,
        isAllele:false,
        isReference:false
      };
      $scope.errorMessage = '';
      $scope.result = {
        exists:false,
        value:''
      };
      $scope.requestedUrl = null;
      saveParameters();
    };

    projectsPromise.then(function(data){
      $scope.projects = data.hits;
      $scope.projects.unshift({id:'--------'});
      $scope.projects.unshift({id:DATASET_ALL});
      $scope.params = {
        project:$scope.projects[0],
        chr:'1',
        reference:'GRCh37',
      };

      loadParameters();
      if($scope.result.exists && !$scope.hasInvalidParams){
        $scope.submitQuery();
      }else{
        $scope.result.exists = false;
      }
    });
  });

  module.directive('alleleValidator', function() {
    return {
      require: '?ngModel',
      link: function(scope, element, attrs, ngModelCtrl) {
        if(!ngModelCtrl) {
          return;
        }

        ngModelCtrl.$parsers.push(function(val) {
          if (angular.isUndefined(val)) {
            val = '';
          }
          var clean = val.replace( /[^ACTGactg>-]+/g, '');
          clean = clean.toUpperCase();
          if (val !== clean) {
            ngModelCtrl.$setViewValue(clean);
            ngModelCtrl.$render();
          }
          return clean;
        });
      }
    };
  });

  module.directive('searchOnEnter', function() {
    return {
      require: '?ngModel',
      link: function(scope, element) {
        element.bind('keypress', function(event) {
          if(event.keyCode === 32) {
            event.preventDefault();
          }else if(event.keyCode === 13) {
            scope.checkParams();
            if(!scope.hasInvalidParams){
              scope.submitQuery();
            }
          }
        });
      }
    };
  });

  /** From github issue 638 (https://github.com/angular/angular.js/issues/638)
      on angularjs page. In short, it allows for certain options in ng-options to be disabled
      if they are true for a certain condition. See "options-disabled" in beacon.html for use.**/

  module.directive('optionsDisabled', function($parse) {
    var disableOptions = function(scope, attr, element, data, fnDisableIfTrue) {
      // refresh the disabled options in the select element.
      angular.forEach(element.find('option'), function(value, index){
        var elem = angular.element(value);
        if(elem.val()!==''){
          var locals = {};
          locals[attr] = data[index];
          elem.attr('disabled', fnDisableIfTrue(scope, locals));
        }
      });
    };
    return {
      priority: 0,
      require: 'ngModel',
      link: function(scope, iElement, iAttrs) {
        // parse expression and build array of disabled options
        var expElements = iAttrs.optionsDisabled.match(/^\s*(.+)\s+for\s+(.+)\s+in\s+(.+)?\s*/);
        var attrToWatch = expElements[3];
        var fnDisableIfTrue = $parse(expElements[1]);
        scope.$watch(attrToWatch, function(newValue) {
          if(newValue){
            disableOptions(scope, expElements[2], iElement, newValue, fnDisableIfTrue);
          }
        }, true);
      }
    };
  });
})();
