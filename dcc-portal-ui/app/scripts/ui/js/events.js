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

/**
 * select-on-click: Select content on click event
 * disable-events: Disable pointer events reduce opacity to give it a disabled look and feel
 * autofocus: Focus element
 */
angular.module('icgc.ui.events', [])
.directive('selectOnClick', function() {
  return {
    restrict: 'A',
    link: function(scope, element) {
      element.on('click', function() {
        element.select();
      });
    }
  };
})
.directive('disableEvents', function() {
  return {
    restrict: 'A',
    replace: true,
    scope: {
      disableEvents: '='
    },
    link: function(scope, element) {

      function toggleEvents(predicate) {
        if (predicate === true) {
          element.css('pointer-events', 'none');
          element.css('opacity', 0.65);
        } else {
          element.css('pointer-events', '');
          element.css('opacity', '1');
        }
      }

      scope.$watch('disableEvents', function(n) {
        if (angular.isDefined(n)) {
          toggleEvents(n);
        }
      });
    }
  };
})
.directive('autofocus', function ($timeout) {
  return {
    restrict: 'A',
    link: function($scope, $element) {
      $timeout(function() {
         $element[0].focus();
      }, 90);
    }
  };
})
.directive('autoselect', function($timeout) {
  return {
    restrict: 'A',
    link: function($scope, $element) {
      $timeout(function() {
         $element[0].select();
      }, 90);
    }
  };

});



