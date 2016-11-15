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


angular.module('icgc.ui.lists', []).directive('hideLinkList', function () {
  return {
    restrict: 'A',
    transclude: true,
    template: '<div class="t_sh">' +
              '<span data-ng-if="!hasItems"><i class="icon-spin"></i></span>' +
              '<span data-ng-if="!show&&hasItems">--</span>' +
              '<div data-ng-if="show" class="text-right">' +
              '<a data-ng-href="{{ link }}">{{ more }}</a> ' +
              '<span data-ng-if="!expanded">' +
              '<span data-ng-click="toggle()" class="t_tools__tool"><i class="icon-caret-left"></i></span>' +
              '</span>' +
              '<span data-ng-if="expanded" data-ng-click="toggle()" class="t_tools__tool">' +
              '<i class="icon-caret-down"></i></span>' +
              '</div>' +
              '<div data-ng-transclude></div>' +
              '</div>',
    link: function (scope, elem, attrs) {
      var previous, next, limit;

      scope.hasItems = false;
      // How many items to show in collapsed list
      limit = attrs.limit ? parseInt(attrs.limit, 10) : 0;

      function swap() {
        previous = [next, next = previous][0];
      }

      function list(value) {
        scope.hasItems = true;
        scope.list = value;
        // How many items are hidden
        scope.more = scope.list.length - limit;
        // If there is more than 1 item in the collapsed list show toggle
        scope.show = scope.more > 0;

        previous = scope.list;
        next = scope.show ? scope.list.slice(0, limit) : scope.list;

        // If list updates while expanded
        if (scope.expanded) {
          swap();
        }

        scope.list = next;
      }

      scope.toggle = function () {
        scope.expanded = !scope.expanded;
        swap();
        scope.list = next;
      };

      // Need to use observe instead of scope so list still
      // has access to parent scope events
      attrs.$observe('hideLinkList', function (value) {
        if (value) {
          list(JSON.parse(value));
        }
      });

      attrs.$observe('link', function (value) {
        if (value) {
          scope.link = value;
        }
      });
    }
  };
});

angular.module('icgc.ui.lists').directive('hideList', function () {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      items: '=',
      templateName: '@',
      displayLimit: '@',
      highlightFilter: '='
    },
    template: '<ul class="t_sh">' +
              '<li data-ng-if="items.length == 0">--</li>' +
              '<li><ul>' +
              '<li data-ng-repeat="item in items | limitTo:(expanded? items.length : limit)" ' +
              'data-ng-include src="templateName"></li>' +
              '</ul></li>' +
              '<li data-ng-if="items.length > limit" class="t_sh__toggle">' +
              '<a ng-click="toggle()" href="" class="t_tools__tool">' +
              '<span ng-if="!expanded"><i class="icon-caret-down"></i> {{ (items.length - limit) }} more</span>' +
              '<span ng-if="expanded"><i class="icon-caret-up"></i> less</span>' +
              '</a>' +
              '</li></ul>',
    link: function (scope) {
      scope.limit = scope.displayLimit || 5;
      scope.expanded = false;
      scope.toggle = function () {
        scope.expanded = !scope.expanded;
      };
    }
  };
});


angular.module('icgc.ui.lists').directive('hideSumList', function (Projects) {
  return {
    restrict: 'A',
    transclude: true,
    template: '<div class="t_sh">' +
              '<span data-ng-if="!hasItems"><i class="icon-spin icon-spinner"></i></span>' +
              '<span data-ng-if="!show&&list.length === 0">--</span>' +
              '<div data-ng-if="show" class="text-right">' +
              '<a href="{{ link }}">{{sum | number}}</a>' +
              ' / ' +
              '<a href=\'/search?filters={"donor":{"availableDataTypes":{"is":["ssm"]}}}\'>{{sumTotal | number}}</a>' +
              ' <em>({{sum/sumTotal*100|number:2}}%)</em>' +
              '<span data-ng-click="toggle()" class="t_tools__tool">' +
              '<i data-ng-class="expanded ? \'icon-caret-down\' : \'icon-caret-left\'"></i>' +
              '</span>' +
              '</div>' +
              '<div data-ng-transclude></div>' +
              '</div>',
    link: function (scope, elem, attrs) {
      var previous, next, limit;

      scope.hasItems = false;
      // How many items to show in collapsed list
      limit = attrs.limit ? parseInt(attrs.limit, 10) : 0;

      function swap() {
        previous = [next, next = previous][0];
      }

      function list(value) {
        scope.hasItems = true;
        scope.list = value;
        // How many items are hidden
        scope.more = scope.list.length - limit;
        // If there is more than 1 item in the collapsed list show toggle
        scope.show = scope.more > 0;

        previous = scope.list;
        next = scope.show ? scope.list.slice(0, limit) : scope.list;

        // If list updates while expanded
        if (scope.expanded) {
          swap();
        }

        scope.list = next;

        scope.sum = _.reduce(_.pluck(value, 'count'), function (s, n) {
          return s + n;
        });

        // Calculate portal wide ssm tested donors
        Projects.getList().then(function(projects) {
          var total = 0;
          projects.hits.forEach(function(project) {
            total += project.ssmTestedDonorCount;
          });
          scope.sumTotal = total;
        });
      }

      scope.toggle = function () {
        scope.expanded = !scope.expanded;
        swap();
        scope.list = next;
      };

      // Need to use observe instead of scope so list still
      // has access to parent scope events
      attrs.$observe('hideSumList', function (value) {
        if (value) {
          list(angular.fromJson(value));
        }
      });

      attrs.$observe('sum', function (value) {
        if (value) {
          scope.sum = angular.fromJson(value);
        }
      });

      attrs.$observe('link', function (value) {
        if (value) {
          scope.link = value;
        }
      });
    }
  };
});


