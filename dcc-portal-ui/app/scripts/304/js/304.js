/*
 * Copyright 2017(c) The Ontario Institute for Cancer Research. All rights reserved.
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

angular.module('icgc.304', ['icgc.304.controllers', 'ui.router'])
  .config(function($stateProvider){
    $stateProvider.state('304', {
      url: '/304?page',
      templateUrl: '/scripts/404/views/404.html',
      controller: '304Controller as ctrlr'
    });
  });

(function(){
  angular.module('icgc.304.controllers', [])
    .controller('304Controller', function($stateParams, Page, $timeout, $window){
      const _ctrl = this;
      _ctrl.isRedirect = true;
      _ctrl.pathToGoTo = $stateParams.page;
      _ctrl.timeToRedirect = 5;

      Page.setTitle('304 - Redirecting');
      Page.setPage('error');

      const processRedirect = () => {
         $timeout(() => {
           _ctrl.timeToRedirect--;
           if(_ctrl.timeToRedirect == 0){
             $window.location.href = _ctrl.pathToGoTo;
           } else {
             processRedirect();
           }
         },1000);
       };

       processRedirect();
    });
})();