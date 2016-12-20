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

angular.module('icgc.modules.genomeviewer.header', []);

angular.module('icgc.modules.genomeviewer.header').controller('GenomeViewerHeaderController', function ($scope) {
  $scope.options = $scope.options || {};
  var defaults = {
    location: true,
    autofit: true,
    panels: true,
    zoom: 50
  };

  $scope.options = angular.extend(defaults, $scope.options);
  $scope.zoom = {
    min: 0,
    max: 100,
    current: $scope.options.zoom,
    active: false,
    dec: function () {
      this.current = parseInt(this.current, 10) - 1;
    },
    inc: function () {
      this.current = parseInt(this.current, 10) + 1;
    }
  };

  $scope.panels = {
    karyotype: false,
    chromosome: false,
    overviewTrackList: true
  };

  $scope.reset = function (e) {
    //$scope.zoom.current = $scope.options.zoom;
    $scope.$emit('gv:reset', e);
  };

  $scope.autoFit = function (e) {
    //$scope.zoom.current = $scope.options.zoom;
    $scope.$emit('gv:autofit', e);
  };

  $scope.fullScreen = function() {
    if (!document.fullscreenElement && !document.mozFullScreenElement && !document.webkitFullscreenElement) {
      var element = document.getElementById('genomic');
      
      if (element.requestFullscreen) {
        element.requestFullscreen();
      } else if (element.mozRequestFullScreen) {
        element.mozRequestFullScreen();
      } else if (element.webkitRequestFullScreen) {
        element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.mozCancelFullScreen) {
        document.mozCancelFullScreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      }
    }
  };

  $scope.togglePanel = function (panel) {
    $scope.panels[panel] = !$scope.panels[panel];
    $scope.$emit('gv:toggle:panel', {panel: panel, active: $scope.panels[panel]});
  };

  $scope.zoomOut = function () {
    if ($scope.zoom.current > $scope.zoom.min) {
      $scope.zoom.dec();
    }
  };
  $scope.zoomIn = function () {
    if ($scope.zoom.current < $scope.zoom.max) {
      $scope.zoom.inc();
    }
  };
  $scope.moveStart = function () {
  };
  $scope.moveEnd = function () {
    $scope.zoom.current = $scope.zoom.current - 0 + 0;
    $scope.$apply();
  };

  $scope.setRegion = function (params) {
    // Check for required parameters
    [ 'chromosome', 'start', 'end'].forEach(function (rp) {
      if (!params.hasOwnProperty(rp)) {
        throw new Error('Missing required parameter: ' + rp);
      }
    });

    $scope.$emit('gv:set:region', params);
  };

  $scope.parseLocation = function () {
    var regex, chromosome, start, end, parse;

    regex = /^(chr)?([xy0-9]+)(:(\d+)(\-(\d+))?)?$/i;
    parse = regex.exec($scope.location);
    chromosome = parse[2];
    start = parse[4];
    end = parse[6];

    $scope.setRegion({chromosome: chromosome, start: start, end: end});
  };
});

angular.module('icgc.modules.genomeviewer.header').directive('gvHeader', function () {
  return {
    restrict: 'A',
    templateUrl: '/scripts/genomemaps/views/header.html',
    scope: {
      options: '=?'
    },
    controller: 'GenomeViewerHeaderController',
    link: function () {}
  };
});
