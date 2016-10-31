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

(function ($, OncoGrid) {
    'use strict';

    var module = angular.module('icgc.oncogrid.controllers', []);

    module.controller('OncogridController', function($scope, LocationService, OncogridService, SetService,
        $timeout, $modal, Extensions, Settings, FullScreenService) {
        var _this = this;

        Settings.get().then(function(settings) {
            _this.downloadEnabled = settings.downloadEnabled || false;
        });

        _this.getGrid = function() {
            return _this.grid;
        };

        _this.initGrid = function(params) {
            _this.grid = new OncoGrid(params);
            _this.grid.render();
        };

        _this.downloadDonorData = function(id) {
            $modal.open({
                templateUrl: '/scripts/downloader/views/request.html',
                controller: 'DownloadRequestController',
                resolve: {
                filters: function() { return {donor:{id:{is:[Extensions.ENTITY_PREFIX + id]}}}; }
                }
            });
        };

        _this.clusterData = function () {
            _this.grid.cluster();
        };

        // Export the subset(s), materialize the set along the way
        $scope.exportSet = function (id) {
            SetService.exportSet(id);
        };

        _this.reloadGrid = function () {
            _this.grid.destroy();
            $scope.cleanActives();
            $('#grid-button').addClass('active');
            _this.initGrid(_.cloneDeep($scope.params));
            if ($scope.crosshairMode) {
                _this.crosshair();
            }
            if (document.fullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement) {
                setTimeout(function () {
                    _this.grid.resize(screen.width - 400, screen.height - 400, true);
                }, 0);
            }
        };

        _this.heatMap = function () {
            $('#heat-button').toggleClass('active');
            $('#og-variant-legend').toggle();
            $('#og-heatmap-legend').toggle();

            _this.grid.toggleHeatmap();
        };

        _this.gridLines = function () {
            $('#grid-button').toggleClass('active');
            _this.grid.toggleGridLines();
        };

        _this.crosshair = function () {
            $scope.crosshairMode = $scope.crosshairMode ? false : true;
            $('#og-crosshair-message').toggle();
            $('#crosshair-button').toggleClass('active');
            var gridDriv = $('#oncogrid-div');
            gridDriv.toggleClass('og-pointer-mode'); gridDriv.toggleClass('og-crosshair-mode');
            _this.grid.toggleCrosshair();
        };

        function exitFullScreen() {
            FullScreenService.exitFullScreen();
        }

        function enterFullScreen() {
            var element = document.getElementById('oncogrid-container');
            FullScreenService.enterFullScreen(element);
        }

        _this.requestFullScreen = function () {

            if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
                enterFullScreen();
            } else {
                exitFullScreen();
            }

        };

    });

    module.controller('OncoGridUploadController', function(Restangular, SetNameService, donorsLimit, genesLimit, filters, 
        LocationService, $scope, $modalInstance, $location, $q, SetService, $timeout){

        $scope.donorsLimit = donorsLimit;
        $scope.genesLimit = genesLimit;
        $scope.filters = filters;
        $scope.isLaunchingOncoGrid = false;

        $scope.maxDonorsLimit = 3000;
        $scope.maxGenesLimit = 100;

        $scope.params = {};
        $scope.hasValidParams= false;

        $scope.params.donorsCount = Math.min($scope.donorsLimit || 3000, $scope.maxDonorsLimit);
        $scope.params.genesCount = Math.min($scope.genesLimit || 100, $scope.maxGenesLimit);
        $scope.params.setName = '';

        $scope.hasValidDonorCount = function(value){
            var val = parseInt(value,10);
            if (isNaN(val)) {
                return false;
            }
            if (!angular.isNumber(val) || val > $scope.maxDonorsLimit || val <= 0 || val > $scope.donorsLimit) {
                return false;
            }
            return true;
        }

        $scope.hasValidGeneCount = function(value){
            var val = parseInt(value,10);
            if (isNaN(val)) {
                return false;
            }
            if (!angular.isNumber(val) || val > $scope.maxGenesLimit || val <= 0 || val > $scope.genesLimit) {
                return false;
            }
            return true;
        }

        $scope.checkInput = function() {
            var params = $scope.params;
            if ($scope.hasValidDonorCount(params.donorsCount) && 
                $scope.hasValidGeneCount(params.genesCount)) {
                $scope.hasValidParams = true;
            } else {
                $scope.hasValidParams = false;
            }
        };

        $scope.getSetName = function(filters){
            return SetNameService.getSetFilters()
                .then(function (filters) {
                    return SetNameService.getSetName(filters);
                })
                .then(function (setName) {
                    $scope.params.setName = setName;
                });
        }

        $scope.getDonorsParams = function(){

            var donorSetParams = {
                filters: $scope.filters || {},
                size: $scope.params.donorsCount,
                type: 'donor',
                isTransient: true,
                name: `Top ${$scope.params.donorsCount} Donors: ${_.includes($scope.params.setName, 'All') ? '' : $scope.params.setName}`
            };

            return donorSetParams;
        }

        $scope.getGenesParams = function(){

             var geneSetParams = {
                filters: $scope.filters || {},
                size: $scope.params.genesCount,
                type: 'gene',
                isTransient: true,
                name: `Top ${$scope.params.genesCount} Genes: ${_.includes($scope.params.setName, 'All') ? '' : $scope.params.setName}`
            };

            return geneSetParams;
        }

        // Wait for sets to materialize
        function wait(ids, numTries, callback) {
            if (numTries <= 0) {
                return;
            }
            SetService.getMetaData(ids).then(function(data) {
                var finished = _.filter(data, function(d) {
                return d.state === 'FINISHED';
                });


                if (finished.length === ids.length) {
                callback(data);
                } else {
                $timeout(function() {
                    wait(ids, --numTries, callback);
                }, 1500);
                }
            });
        }

        $scope.launchOncogridAnalysis = function (setIds) {
            console.log('Launching OncoGrid with: ', setIds);
            
            var payload = {
                donorSet: setIds.donor,
                geneSet: setIds.gene
            };
            
            return Restangular
                .one('analysis')
                .post('oncogrid', payload, {}, { 'Content-Type': 'application/json' })
                .then(function (data) {
                    if (!data.id) {
                        throw new Error('Received invalid response from analysis creation');
                    }
                    LocationService.goToPath('analysis/view/oncogrid/' + data.id);
                }).finally(function(){
                    $scope.isLaunchingOncoGrid = false;
                });
        };

        $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
        };

        $scope.newOncoGridAnalysis = function(){
            $scope.isLaunchingOncoGrid = true;
            $q.all({
                r1: SetService.addSet('donor', $scope.getDonorsParams()),
                r2: SetService.addSet('gene', $scope.getGenesParams())
            }).then(function (responses) {
                var r1 = responses.r1;
                var r2 = responses.r2;

                function proxyLaunch() {
                    $scope.launchOncogridAnalysis({donor: r1.id, gene: r2.id});
                }
                wait([r1.id, r2.id], 7, proxyLaunch);
            });
        }

        $scope.checkInput();
        $scope.getSetName($scope.filters);
    });

})(jQuery, OncoGrid);