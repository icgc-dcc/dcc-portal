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

  var module = angular.module('icgc.common.notify', []);

  module.factory('Notify', function ($location, Page) {
    var visible = false,
      removable = true,
      error = false,
      message = '',
      theme = '';

    function isVisible() {
      return !!visible;
    }

    function show() {
      visible = true;
    }

    function showErrors() {
      // Portal convention
      Page.setError(true);
      error = true;
      visible = true;
    }

    function hide() {
      visible = false;
      if (error === true) {
        error = false;
        Page.setError(false);
        Page.stopAllWork();
      }
    }

    function redirectHome() {
      visible = false;
      if (error === true) {
        error = false;
        Page.setError(false);
        Page.stopAllWork();
        $location.path('/').search({});
      }
    }

    function setMessage(m) {
      if (!angular.isDefined(m)) {
        throw new Error('Notify requires a message');
      }
      if (!angular.isString(m)) {
        throw new Error('msg must be a string');
      }
      message = m;
    }

    function getMessage() {
      return message;
    }

    function isRemovable() {
      return !!removable;
    }

    function setRemovable(r) {
      if (angular.isDefined(r) && typeof r !== 'boolean') {
        throw new Error('r must be a boolean');
      }
      removable = r;
    }

    function setTheme(t) {
      theme = t;
    }

    function getTheme() {
      return theme;
    }

    return {
      isVisible: isVisible,
      show: show,
      showErrors: showErrors,
      hide: hide,
      redirectHome: redirectHome,
      setMessage: setMessage,
      getMessage: getMessage,
      setRemovable: setRemovable,
      isRemovable: isRemovable,
      setTheme: setTheme,
      getTheme: getTheme
    };
  });

  module.controller('NotifyCtrl', function ($scope, Page, Notify) {
    $scope.notify = Notify;
    $scope.$on('$locationChangeSuccess', function() {
      if (Notify.isVisible() && Page.page() !== 'error') {
        Notify.hide();
      }
    });
  });

  module.directive('notify', function () {
    return {
      restrict: 'E',
      replace: true,
      controller: 'NotifyCtrl',
      scope: true,
      template: '<div><div data-ng-if="notify.isVisible()" class="t_notify_popup {{notify.getTheme()}}">' +
                '<div class="t_notify_body pull-left" data-ng-bind-html="notify.getMessage()"></div>' +
                '<div class="pull-right">' +
                '<a class="t_notify_link" data-ng-href="" data-ng-click="notify.redirectHome()">'+
                '<i data-ng-if="notify.isRemovable()" class="icon-home"></i>Home</a>' +
                '<span>&nbsp;&nbsp;</span>' +
                '<a class="t_notify_link" data-ng-href="" data-ng-click="notify.hide()">' +
                '<i data-ng-if="notify.isRemovable()" class="icon-cancel"></i>Close</a>' +
                '</div>' +
                '</div>' +
                '</div>'
    };
  });
})();
