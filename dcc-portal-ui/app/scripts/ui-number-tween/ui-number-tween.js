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

var ngmodule = angular.module('icgc.ui.numberTween', []);

ngmodule.directive('numberTween', function () {
  return {
    restrict: 'E',
    replace: true,
    template: '<span></span>',
    scope: {
      value: '<',
      filter: '<',
      onTweenStart: '&',
      onTweenEnd: '&',
    },
    link: function (scope, $element) {
      var getValue = function (value) {
        return scope.filter ? scope.filter(value) : value;
      };
      var onTweenStart = scope.onTweenStart();
      var onTweenEnd = scope.onTweenEnd();

      $element.text(getValue(scope.value));

      scope.$watch('value', function (newValue, oldValue) {
        if (newValue === oldValue) {
          return;
        }

        $element
          .stop()
          .animate({ val: oldValue }, 0)
          .animate({ val: newValue }, {
            step: function (now) {
              this.innerText = getValue(Math.round(now));
            },
            start: onTweenStart,
            always: onTweenEnd,
          });
      });
    }
  };
});