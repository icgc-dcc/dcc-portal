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

(function ($) {
    'use strict';

    var module = angular.module('icgc.oncogrid.controllers', []);

    module.controller('OncogridController', function($scope, LocationService, OncogridService, SetService,
        $timeout, $modal, Extensions, Settings, FullScreenService) {
        var _this = this;

        Settings.get().then(function(settings) {
            _this.downloadEnabled = settings.downloadEnabled || false;
        });

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
            $scope.grid.cluster();
        };

        // Export the subset(s), materialize the set along the way
        $scope.exportSet = function (id) {
            SetService.exportSet(id);
        };

        _this.reloadGrid = function () {
            $scope.grid.destroy();
            $scope.cleanActives();
            $('#grid-button').addClass('active');
            $scope.grid = new OncoGrid(_.cloneDeep($scope.params));
            $scope.grid.render();
            $scope.grid.removeDonors(function(d){return d.score === 0;});
            $scope.grid.removeGenes(function(d){return d.score === 0;});
                
            if ($scope.crosshairMode) {
                _this.crosshair();
            }
        };

        _this.heatMap = function () {
            $('#heat-button').toggleClass('active');
            $('#og-variant-legend').toggle();
            $('#og-heatmap-legend').toggle();

            $scope.grid.toggleHeatmap();
        };

        _this.gridLines = function () {
            $('#grid-button').toggleClass('active');
            $scope.grid.toggleGridLines();
        };

        _this.crosshair = function () {
            $scope.crosshairMode = $scope.crosshairMode ? false : true;
            $('#og-crosshair-message').toggle();
            $('#crosshair-button').toggleClass('active');
            var gridDriv = $('#oncogrid-div');
            gridDriv.toggleClass('og-pointer-mode'); gridDriv.toggleClass('og-crosshair-mode');
            $scope.grid.toggleCrosshair();
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
})(jQuery);