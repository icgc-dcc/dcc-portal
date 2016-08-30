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

angular.module('icgc.common.version', []);

angular.module('icgc.common.version').factory('VersionService', function ($rootScope, $timeout, gettextCatalog) {
  function browserCheck() {
    if (window.attachEvent && !window.addEventListener) {
      $rootScope.$broadcast('notify', '<i class="icon-attention"></i>' + 
        gettextCatalog.getString('Your Browser is not supported, some features may be broken or missing.') + 
        ' <strong><a target="_blank" style="color: #fff;text-decoration: underline;" href="http://browser-update.org/en/update.html">' +
        gettextCatalog.getString('Learn how to update your browser') + '</a></strong>',
        true);
    }
  }

  $timeout(browserCheck, 0);

  return {
    outOfDate: function () {
      $rootScope.$broadcast('notify', '<i class="icon-attention"></i>' + 
        gettextCatalog.getString('A new version of the application has been released. Please refresh your browser before continuing.'));
    }
  };
});
