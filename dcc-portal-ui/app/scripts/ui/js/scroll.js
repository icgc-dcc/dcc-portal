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

angular.module('icgc.ui.scroll', [
  'icgc.ui.scroll.scrollto',
  'icgc.ui.scroll.scrollSpy',
  'icgc.ui.scroll.resetScroll'
]);


angular.module('icgc.ui.scroll.resetScroll', []).directive('resetScroll', function($location, $timeout) {
  return {
    restrict: 'A',
    scope: {
      resetScroll: '@'
    },
    link: function (scope, elem) {
      var _scrollOffset = scope.resetScroll, 
          _top = 0;
      
      // Do not scroll if we are trying to anchor within another page.    
      if ($location.hash()) {
        return;
      }
      
      if (! isNaN(_scrollOffset)) {
        _top = Math.round(elem.offset().top + parseInt(_scrollOffset, 10));
      }
      else if (angular.isString(_scrollOffset)) {        
        
        switch (_scrollOffset.toLowerCase()) {  
          case 'top':
           _top = 0;
          break;
          
          default:
            _top = 0;
          break;
        }
        
      }
      
      $timeout(function() { 
        jQuery('body,html').scrollTop(_top); 
        }, 100);
    }
  };
});

angular.module('icgc.ui.scroll.scrollto', [])
.directive('scrollto', function () {
  return function (scope, elm, attrs) {
    elm.bind('click', function (e) {
      e.preventDefault();
      var offset = Number(attrs.offset) || 40;
      var speed = Number(attrs.speed) || 800;

       if (attrs.scrollto) {
          console.warn('The scrollto attribute is deprecated, please use href instead');
        }

      attrs.scrollto = attrs.scrollto || attrs.href;    

      scrollToSelector(attrs.scrollto, { offset: offset, speed: speed });
    });
  };
});


angular.module('icgc.ui.scroll.scrollSpy', []).directive('scrollSpy', function ($window) {
  return {
    restrict: 'A',
    controller: function ($scope) {
      $scope.spies = [];
      this.addSpy = function (spyObj) {
        return $scope.spies.push(spyObj);
      };
    },
    link: function (scope, elem) {
      var spyElems, w;
      spyElems = [];
      w = jQuery($window);

      function scrl() {
        var highlightSpy, pos, spy, _i, _len, _ref;
        highlightSpy = null;
        _ref = scope.spies;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          spy = _ref[_i];
          spy.out();
          pos = spyElems[spy.id].offset().top;
          if (pos - $window.scrollY <= 65) {
            spy.pos = pos;
            if (!highlightSpy) {
              highlightSpy = spy;
            }
            if (highlightSpy.pos < spy.pos) {
              highlightSpy = spy;
            }
          }
        }
        return highlightSpy ? highlightSpy['in']() : scope.spies[0]['in']();
      }

      scope.$watch('spies', function (spies) {
        var spy, _i, _len, _results;
        _results = [];
        for (_i = 0, _len = spies.length; _i < _len; _i++) {
          spy = spies[_i];
          if (!spyElems[spy.id]) {
            _results.push(spyElems[spy.id] = elem.find('#' + spy.id));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      });

      w.on('scroll', scrl);
      scope.$on('$destroy', function () {
        w.off('scroll', scrl);
      });
    }
  };
})
.directive('spy', function () {
  return {
    restrict: 'A',
    require: '^scrollSpy',
    link: function (scope, elem, attrs, affix) {
      return affix.addSpy({
        id: attrs.spy,
        'in': function () {
          return elem.addClass('current');
        },
        out: function () {
          return elem.removeClass('current');
        }
      });
    }
  };
});

function scrollToSelector (selector, options) {
  invariant(selector, 'Missing required argument `selector`');
  options = _.defaults({
    offset: 40,
    speed: 800
  }, options);

  var top = jQuery(selector).offset().top - options.offset;
  jQuery('body,html').animate({ scrollTop: top }, options.speed);
}


