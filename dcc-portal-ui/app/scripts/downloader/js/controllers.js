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

angular.module('app.downloader.controllers', ['app.downloader.services']);

// Controls the download jobs for dynamic downloads
// Note: We try to preemptitively determine status as a way improve UX and reduce polls
angular.module('app.downloader.controllers').controller('DownloaderController',
  function ($window, $filter, $timeout, $scope, Page, DownloaderService,
    DataType, API, Restangular, RestangularNoCache, Settings, ids) {

    var cancelTimeout;
    var dataTypeOrder = DataType.precedence();

    Page.setTitle('Downloader');
    Page.setPage('downloader');

    //$scope.jobs = [];
    $scope.isDownloadLink = false;
    $scope.currentTime = (new Date()).getTime();
    $scope.refreshCounter = 0;

    if (ids.length === 0) {
      // Remove local history function for now
      // $scope.ids = DownloaderService.getCurrentJobIds();
      $scope.ids = [];
    } else {
      $scope.ids = ids;
      $scope.isDownloadLink = true;
    }


    // Clean up the filters
    function cleanFilter(filter) {
      ['donor', 'gene', 'mutation', 'project'].forEach(function(name) {
        if (filter.hasOwnProperty(name)) {
          if (_.isEmpty(filter[name])) {
            delete filter[name];
          }
        }
      });
      return filter;
    }

    function isError(s) {
      return ['NOT_FOUND', 'FAILED'].indexOf(s) >= 0;
    }

    function isRunning(s) {
      return ['RUNNING', 'FINISHING', 'PREP'].indexOf(s) >= 0;
    }

    /*
    function isDownloadable(s) {
      return ['RUNNING', 'FINISHING', 'SUCCEEDED'].indexOf(s) > 0;
    }
    */


    // Convert download progrees (0 to 1) to a progress-bar style
    $scope.convertProgress = function( progress ) {
      if (!progress || progress <= 0) {
        return { width: Math.min(0.1, $scope.refreshCounter*0.01) * 100 + '%' };
      } else {
        return { width: (10 + progress * 90) + '%' };
      }
    };

    // Download entire job as one large archive
    $scope.downloadAll = function(id) {
      var current = _.findWhere($scope.jobs, {'downloadId':id});

      // Double check status before sending download request
      DownloaderService.getJobStatus(id).then(function(jobs) {
        jobs = Restangular.stripRestangular(jobs);

        if (['SUCCEEDED', 'FINISHING'].indexOf(current.status) >= 0 &&
          ['SUCCEEDED', 'RUNNING'].indexOf(jobs[0].status) >= 0) {
          console.log('Starting Download', id);

          $window.location.assign(API.BASE_URL + '/download/' + id);
        } else {
          current.status = jobs[0].status;
        }
      });
    };

    // Download specific file type within a job
    $scope.downloadType = function(id, fileType) {
      var current = _.findWhere($scope.jobs, {'downloadId':id});


      // Double check status before sending download request
      DownloaderService.getJobStatus(id).then(function(jobs) {
        jobs = Restangular.stripRestangular(jobs);
        current.status = jobs[0].status;
        if (['SUCCEEDED', 'RUNNING'].indexOf(current.status) >= 0) {
          console.log('Starting Download', id);
          $window.location.assign(API.BASE_URL + '/download/' + id + '/' + fileType);
        }
      });

    };

    // Stop running job
    $scope.cancelDownload = function(id) {
      console.log('Cancelling Download', id);
      DownloaderService.cancelJob(id).then(function(job) {
        job = Restangular.stripRestangular(job);
        var current = _.findWhere($scope.jobs, {'downloadId':id});
        current.status = job.status;
      });
    };


    // Initialize jobs and jobs' metadata
    function init() {
      if ($scope.ids.length === 0) {
        $scope.jobs = [];
        return;
      }

      DownloaderService.getJobStatus($scope.ids).then(function(jobs) {
        jobs = Restangular.stripRestangular(jobs);
        $scope.jobs = jobs;
        $scope.showInfo = false;
        $scope.hasEmail = false;

        DownloaderService.getJobMetaData($scope.ids).then(function(metas) {
          metas = Restangular.stripRestangular(metas);

          // Init static things
          $scope.jobs.forEach(function(job) {
            var jobInfo = metas[job.downloadId];

            job.overallProgress = 0;

            // Set meta data
            if (! isError( job.status )) {
              job.startTime = jobInfo.startTime;
              job.ttl = parseInt(jobInfo.ttl, 10); // Time to live
              job.et  = parseInt(jobInfo.et, 10);  // end time

              if (job.status === 'SUCCEEDED') {
                job.archiveSize = jobInfo.fileSize;  // Total archive size
                job.overallProgress = 1;
              }

              // Filter is the expanded query, uiFilter is what user sees
              job.filter = cleanFilter(JSON.parse(jobInfo.filter));
              job.uiQueryFilter = cleanFilter(JSON.parse(decodeURIComponent(jobInfo.uiQueryStr)));

              if (_.isEmpty(job.filter)) {
                delete job.filter;
                delete job.uiQueryFilter;
              } else {
                job.filterStr = JSON.stringify(job.uiQueryFilter);
              }
              job.isExpanded = true;
              job.hasEmail = jobInfo.hasEmail === 'true';
            }

            // Sort each job's data type
            job.progress = _.sortBy(job.progress, function(p) {
              var index = dataTypeOrder.indexOf(p.dataType);
              if (index === -1) {
                return dataTypeOrder.length + 1;
              }
              return index;
            });
          });


          // Sort jobs by start time
          $scope.jobs = _.sortBy($scope.jobs, function(job) {
            return -job.startTime;
          });


          // FIXME: a compromise solution until we have user management, figure out
          // if job is done and if email was entered
          if ($scope.jobs.length > 0) {
            if (['SUCCEEDED', 'EXPIRED', 'FAILED', 'KILLED', 'NOT_FOUND'].indexOf($scope.jobs[0].status) === -1) {
              $scope.showInfo = true;
              $scope.hasEmail = $scope.jobs[0].hasEmail;
            }
          }

        });

        // Start refresh loop
        refresh();

      }, function(err) {
        console.log('Error', err);
        if (err.status === 403) {
          $scope.jobs = [];
          $scope.error = 'Your data download contains controlled data. ' +
            'You must log in first before you can acess the archive.';
        }

        //$scope.isControlled = true;
      });
    }


    // TODO: Consider switching to use $interval that is available in newer version of angularJS
    // it has more understandable syntax
    function refresh() {
      var activeJobs;
      // Only refresh transitional states, final states should never change
      activeJobs = _.filter($scope.jobs, function(job) {
        return isRunning(job.status);
        //return job.status === 'RUNNING';
      });

      if (activeJobs.length === 0) {
        return;
      }

      $scope.refreshCounter ++;
      $scope.currentTime = (new Date()).getTime();

      DownloaderService.getJobStatus(_.pluck(activeJobs, 'downloadId')).then(function(updates) {
        updates = Restangular.stripRestangular(updates);

        $scope.jobs.forEach(function(job) {
          var update = _.findWhere(updates, {'downloadId': job.downloadId});
          if (! update) {
            return;
          }
          job.status = update.status;
          if (! isError(job.status)) {
            var n = 0, d = 0, numCompleted = 0;
            job.progress = update.progress;
            job.progress.forEach(function(file) {
              n += parseInt(file.numerator, 10);
              d += parseInt(file.denominator, 10);
            });
            if (d > 0) {
              job.overallProgress = n/d;
            }

            // Sort each job's data type
            job.progress = _.sortBy(job.progress, function(p) {
              var index = dataTypeOrder.indexOf(p.dataType);
              if (index === -1) {
                return dataTypeOrder.length + 1;
              }
              return index;
            });


            // if status is success, fetch the end run meta data
            if (job.status === 'SUCCEEDED') {
              DownloaderService.getJobMetaData(job.downloadId).then(function(infos) {
                infos = Restangular.stripRestangular(infos);
                var info;
                if (!infos) {
                  return;
                }
                info = infos[job.downloadId];
                job.ttl = parseInt(info.ttl, 10); // Time to live
                job.et  = parseInt(info.et, 10);  // end time
                job.archiveSize = info.fileSize;  // Total archive size
              });
            } else {
              // Try to determine status ASAP
              numCompleted = _.filter(job.progress, function(type) {
                return type.completed === 'true';
              });
              if (numCompleted.length === job.progress.length) {
                job.status = 'FINISHING';
              }
            }
          } else {
            job.progress = [];
            job.overallProgress = 0;
          }

        });
      });

      cancelTimeout = $timeout(refresh, 5000);
    }


    // Start
    Settings.get().then(function(settings) {
      $scope.downloadEnabled = settings.downloadEnabled || false;
      if ($scope.downloadEnabled === true) {
        init();
      }
    });

    // Clean up events
    $scope.$on('$destroy', function() {
      $timeout.cancel(cancelTimeout);
    });

  });
