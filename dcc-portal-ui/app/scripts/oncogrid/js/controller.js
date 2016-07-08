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

    module.controller('OncogridController', function($scope, SetService, $timeout) {

        $scope.clusterData = function () {
            $scope.grid.cluster();
        };

        // Export the subset(s), materialize the set along the way
        $scope.exportSet = function (id) {
            SetService.exportSet(id);
        };

        $scope.reloadGrid = function () {
            $scope.grid.destroy();
            $scope.cleanActives();
            $scope.grid = new OncoGrid(_.cloneDeep($scope.params));
            $scope.grid.render();
            $timeout(function() {
                $scope.grid.removeDonors(function(d){return d.score === 0;});
                $scope.grid.removeGenes(function(d){return d.score === 0;});
            }, 400);
        };

        $scope.legend = function () {
            $('#legend-button').toggleClass('active');
            $('#og-legend').toggle();
        };

        $scope.heatMap = function () {
            $('#heat-button').toggleClass('active');
            $('#og-variant-legend').toggle();
            $('#og-heatmap-legend').toggle();

            $scope.grid.toggleHeatmap();
        };

        $scope.gridLines = function () {
            $('#grid-button').toggleClass('active');
            $scope.grid.toggleGridLines();
        };

        $scope.crosshair = function () {
            $('#crosshair-button').toggleClass('active');
            var gridDriv = $('#oncogrid-div');
            gridDriv.toggleClass('og-pointer-mode'); gridDriv.toggleClass('og-crosshair-mode');
            $scope.grid.toggleCrosshair();
        };

        function exitFullScreen() {
            if (document.exitFullscreen) {
                document.exitFullscreen();
            } else if (document.mozCancelFullScreen) {
                document.mozCancelFullScreen();
            } else if (document.webkitExitFullscreen) {
                document.webkitExitFullscreen();
            }
        }

        function enterFullScreen() {
            var element = document.getElementById('oncogrid-container');
            if (element.requestFullscreen) {
                element.requestFullscreen();
            } else if (element.mozRequestFullScreen) {
                element.mozRequestFullScreen();
            } else if (element.webkitRequestFullScreen) {
                element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
            }
        }

        $scope.requestFullScreen = function () {

            if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
                enterFullScreen();
            } else {
                exitFullScreen();
            }

        };

    });
})(jQuery);