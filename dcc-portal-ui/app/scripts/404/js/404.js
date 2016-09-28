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

angular.module('icgc.404', ['icgc.404.controllers', 'ui.router'])
  .config(function($stateProvider){
    $stateProvider.state('404', {
      url: '/404?page&id&url',
      templateUrl: '/scripts/404/views/404.html',
      controller: '404Controller as ctrlr'
    });
  });

(function(){
  angular.module('icgc.404.controllers', [])
    .controller('404Controller', function($stateParams, Page){
      var _ctrl = this;
      _ctrl.info = '';

      Page.setTitle('404');
      Page.setPage('error');

      if($stateParams.page && $stateParams.id && $stateParams.url){
        _ctrl.info = {page: $stateParams.page, id: $stateParams.id, url: $stateParams.url};
      }

      _ctrl.page = $stateParams.page;
      
      _ctrl.emailSubject = _ctrl.info ? 
        'ICGC DCC /' + _ctrl.info.page  + '/' + _ctrl.info.id +' Page Not Found' : 
        'ICGC DCC Page Not Found' ;
    });
})();