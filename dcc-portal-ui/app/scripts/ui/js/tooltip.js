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

(function() {
  'use strict';

  var module = angular.module('icgc.ui.tooltip', []);

  /**
   * Centralized tooltip directive. There should be only one per application
   * This act as the tooltip "server" that waits for tooltip events
   */
  module.directive('tooltipControl', function ($position, $rootScope, $sce, $window) {
    return {
      restrict: 'E',
      replace: true,
      scope: {
      },
      templateUrl: 'template/tooltip.html',
      link: function (scope, element) {
        scope.placement = 'right';
        scope.html = '???';

        function calculateAbsoluteCoordinates(placement, target, targetPosition) {
          var position = targetPosition || $position.offset(target);
          var arrowOffset = 10;

          var tooltip = {
            width: element.prop('offsetWidth'),
            height: element.prop('offsetHeight')
          };

          // FIXME:
          // Need to make this work better for SVG, maybe use d3-tip plugin for calc
          // This is to avoid NaN
          position.width = position.width || 0;
          position.height = position.height || 0;

          switch(placement) {
          case 'right':
            return {
              top: position.top + position.height / 2 - tooltip.height / 2,
              left: position.left + position.width + arrowOffset
            };
          case 'left':
            return {
              top: position.top + position.height / 2 - tooltip.height / 2,
              left: position.left - tooltip.width - arrowOffset
            };
          case 'top':
            return {
              top: position.top - tooltip.height - arrowOffset,
              left: position.left > tooltip.width / 4 ? (position.left + position.width / 2 - tooltip.width / 2) : 0
            };
          case 'bottom':
            return {
              top: position.top + tooltip.height - arrowOffset,
              left: position.left > tooltip.width / 4 ? (position.left + position.width / 2 - tooltip.width / 2) : 0
            };
          default:
            return {
              top: position.top + arrowOffset,
              left: position.left + position.width / 2
            };
          }
        }

        $rootScope.$on('tooltip::show', function(evt, params) {
          scope.$apply(function() {
            if (params.text) {
              scope.html = $sce.trustAsHtml(params.text);
            }
            if (params.placement) {
              scope.placement = params.placement;
            }
          });

          element.css('visibility', 'visible');

          if(params.sticky){
            element.addClass('sticky');

            if(!$window.onmousemove){
              $window.onmousemove = function(e){
                if(element.hasClass('sticky')){
                  var position = calculateAbsoluteCoordinates(scope.placement, params.element, {
                    left: e.pageX,
                    top: e.pageY - (scope.placement === 'top' ? 8 : 0),
                    width: 10,
                    height: -6
                  });
                  element.css('top', position.top);
                  element.css('left', position.left);
                }
              };
            }
          }else{
            var position = calculateAbsoluteCoordinates(params.placement, params.element, params.elementPosition);
            element.css('top', position.top);
            element.css('left', position.left);
            element.removeClass('sticky');
          }
        });
        $rootScope.$on('tooltip::hide', function() {
          element.css('visibility', 'hidden');
          element.css('top', -999);
          element.css('left', -999);
        });
      }
    };
  });


  /**
   * Light weight directive for request tooltips
   */
  module.directive('tooltip', function($timeout) {
    return {
      restrict: 'A',
      replace: false,
      scope: {
      },
      link: function(scope, element, attrs) {
        var tooltipPromise;

        element.bind('mouseenter', function() {
          var placement = attrs.tooltipPlacement || 'top';

          if (! attrs.tooltip) {
            return;
          }

          // If placement = overflow, check if there is actually overflow
          if (attrs.tooltipPlacement === 'overflow') {
            if (element.context.scrollWidth <= element.context.clientWidth) {
              return;
            } else {
              placement = 'top';
            }
          }

          tooltipPromise = $timeout(function() {
            scope.$emit('tooltip::show', {
              element: element,
              text: attrs.tooltip || '???',
              placement: placement || 'top'
            });
          }, 500);
        });

        element.bind('mouseleave', function() {
          $timeout.cancel(tooltipPromise);
          scope.$emit('tooltip::hide');
        });

        element.bind('click', function() {
          $timeout.cancel(tooltipPromise);
          scope.$emit('tooltip::hide');
        });

        scope.$on('$destroy', function() {
          element.off();
          element.unbind();
        });
      }
    };
  });
})();
