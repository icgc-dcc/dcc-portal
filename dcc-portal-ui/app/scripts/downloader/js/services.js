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

angular.module('app.downloader.services', ['app.downloader.model']);


// DownloaderSerices handles two tasks
// 1) Request/query download against the server
// 2) Local download job cache management
angular.module('app.downloader.services').service('DownloaderService', function(Downloader, $window) {
  this.requestDownloadJob = function(filters, info, email, downloadUrl, uiQueryStr) {
    return Downloader.requestDownloadJob(filters, info, email, downloadUrl, uiQueryStr);
  };
  this.cancelJob = function(ids) {
    return Downloader.cancelJob(ids);
  };
  this.getJobMetaData = function(ids) {
    return Downloader.getJobMetaData(ids);
  };
  this.getJobStatus = function(ids) {
    return Downloader.getJobStatus(ids);
  };


  this.getSizes = function (filters) {
    return Downloader.getSizes(filters);
  };

  // Dealing with cache
  this.getCurrentJobIds = function() {
    var c = $window.localStorage.getItem('dcc.icgc.downloader');
    if (! c) {
      return [];
    } else {
      return JSON.parse(c);
    }
  };

  this.setCurrentJobIds = function(data) {
    console.log('Saving...', data);
    $window.localStorage.setItem('dcc.icgc.downloader', JSON.stringify(data));
  };

});
