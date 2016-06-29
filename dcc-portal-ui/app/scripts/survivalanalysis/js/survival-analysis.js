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

  angular.module('icgc.survival', [
    'icgc.survival.directives',
    'icgc.survival.services',
    'icgc.survival.controllers'
  ]);

  angular.module('icgc.survival.directives', ['icgc.survival.services', 'icgc.survival.controllers'])
    .directive('survivalResult', function() {
      return {
        restrict: 'E',
        scope: {
          // TODO 1.5: this should be a one-way `<` binding
          item: '='
        },
        templateUrl: '/scripts/survivalanalysis/views/survival-analysis.html',
        controller: 'survivalPlotController as plot',
        link: function($scope) {
          console.log($scope.item);
        }
      };
    });

  angular.module('icgc.survival.controllers', ['icgc.survival.services'])
    .controller('survivalPlotController', [function () {

    }]);


  angular.module('icgc.survival.services', ['icgc.donors.models'])
    .service('survivalPlotService', [function () {

    }]);

})();