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

angular.module('app.downloader.model', []);

angular.module('app.downloader.model').factory('Downloader', function ($http, RestangularNoCache) {
  return {

    requestDownloadJob: function (filters, info, email, downloadUrl, uiQueryStr) {
      filters = JSON.stringify(filters);
      info = JSON.stringify(info);

      return RestangularNoCache.one('download/submit').get({
        filters: filters,
        info: info,
        email: email,
        downloadUrl: downloadUrl,
        uiQueryStr: encodeURIComponent(uiQueryStr)
      });
    },

    getSizes: function(filters) {
      return RestangularNoCache.one('download', 'size').get({
        filters: filters
      });
    },

    cancelJob: function (ids) {
      return RestangularNoCache.one('download', ids).one('cancel').get({});
    },

    getJobMetaData: function (ids) {
      if (angular.isArray(ids)) {
        return RestangularNoCache.one('download', ids.join(',')).one('info').get({});
      }
      return RestangularNoCache.one('download', ids).one('info').get({});
    },

    getJobStatus: function (ids) {
      if (angular.isArray(ids)) {
        return RestangularNoCache.one('download', ids.join(',')).one('status').get({});
      }
      return RestangularNoCache.one('download', ids).one('status').get({});
    }
  };
});
