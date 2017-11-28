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
require('./share.scss');

(function () {
  'use strict';

  var module = angular.module('icgc.share', []);

  module.directive('shareButton', function () {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      templateUrl: '/scripts/share/views/share.html',
      controller: 'shareCtrl as shareCtrl',
      link: function ($scope) {
        $scope.entityType = 'page';
      }
    };
  });

  module.directive('shareIcon', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: '/scripts/share/views/share.icon.html',
      controller: 'shareCtrl as shareCtrl',
      scope: {
        shareParams: '=',
        customPopupDisclaimer: '@'
      },
      link: function ($scope) {
        $scope.entityType = 'item';
      }
    };
  });

  module.service('Share', function (Restangular, $location) {
    var _service = this;

    _service.getShortUrl = function (params, shouldUseParamsOnlyForRequest) {

      var port = window.location.port ? ':' +  window.location.port : '',
          defaults = {
            url: window.location.protocol + '//' + window.location.hostname + 
            port + window.location.pathname
          },
        requestParams = (shouldUseParamsOnlyForRequest === true ? params : $location.search()),
        queryStr = '',
        urlShortnerParams = defaults;

      _.assign(urlShortnerParams, params);

      // For some reason JSHint is not correctly picking up that we are checking if the object
      // has it's own property when using the || - bug with JSHint so ignore the below...
      for (var requestKey in requestParams) { // jshint ignore:line

        if (!requestParams.hasOwnProperty(requestKey) ||
          (shouldUseParamsOnlyForRequest === true && requestKey === 'url')) {
          continue;
        }


        if (queryStr !== '') {
          queryStr += '&';
        }
        // FIXME: The url shortner decodes the GET request params for some reason - 
        // I will file a bug with them but in the meantime
        // this double encoding will do...fail...
        queryStr += requestKey + '=' + encodeURIComponent(encodeURIComponent(requestParams[requestKey]));

        // The webservice does not take any other parameters so only keep the 'url'
        // property - no point making a request for the rest since we are encoding
        // it with the url anyways.
        delete urlShortnerParams[requestKey];
      }


      if (queryStr.length > 0) {
        urlShortnerParams.url += '?' + queryStr;
      }

      return Restangular.one('short', '').get(urlShortnerParams);
    };
  });

  module.controller('SharePopupController', function ($scope, $modalInstance, shortUrl, customPopupDisclaimer) {
    $scope.shortUrl = shortUrl;
    $scope.customPopupDisclaimer = customPopupDisclaimer;
    $scope.cancel = function () {
      $modalInstance.dismiss('cancel');
    };
  });

  module.controller('shareCtrl', function ($scope, $element, $location, Share) {
    var _ctrl = this;
    this.oldUrl = '';
    this.changedUrl = true;
    _ctrl.toggle = function (opt) {
      _ctrl.active = opt;
    };
    this.popoverIsOpen = false;
    this.requestShortUrl = (shareParams, shouldUseParamsOnlyForRequest) => Share.getShortUrl(shareParams, shouldUseParamsOnlyForRequest);
    this.setShortUrl = (shareParams, shouldUseParamsOnlyForRequest) => this.requestShortUrl(shareParams, shouldUseParamsOnlyForRequest).then(value => {this.changedUrl = false; this.shortUrl = value.shortUrl;});

    this.handleClickShareButton = (shareParams, shouldUseParamsOnlyForRequest) => {
      this.Url = $location.url();
      if(this.oldUrl !== this.Url){
        this.changedUrl = true;
      }
      else{
        this.changedUrl = false;
      }
      this.oldUrl = this.Url;
      this.setShortUrl(shareParams, shouldUseParamsOnlyForRequest);
      $scope.$watch(() => (this.shortUrl), (newValue) => {
      if(newValue){
         setTimeout(() => {
          var input = $element.find('.short-url-input');      
          input.focus();
          input.select();
        });
      }
    });
      this.popoverIsOpen = true;
    };

    const bodyClickListener = function (e) {
      jQuery($element).each(function () {
        if ((!jQuery(this).is(e.target) && jQuery(this).has(e.target).length === 0 && jQuery('.popover').has(e.target).length === 0)) {
          _ctrl.popoverIsOpen = false;
        }
      });
    };

    jQuery('body').on('click', bodyClickListener);
    $scope.$on('$destroy', () => {
      jQuery('body').off('click', bodyClickListener);
    });
  });
})();
