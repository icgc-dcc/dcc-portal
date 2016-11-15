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


  function HistoryManager() {

    var _self = this,
        _scrollTimeoutHandle = null,
        _whiteList = [],
        _$state = null,
        _$location = null,
        _$window = null,
        _previousWhiteListMatches = null;

    ///////
    function _wrapHistoryAPI(method) {

      var _originalHistoryMethod = _$window.window.history[method];

      _$window.window.history[method] = function() {

        var _h = _$location.hash();

        if (! _shouldSuppressScrollTop() && (! _h || ! _h.match(/^!([\w\-]+)$/i))) {
          jQuery(window).scrollTop(0);
        }

        return _originalHistoryMethod.apply(this, Array.prototype.slice.call(arguments));
      };

    }
    ////////

    function _shouldSuppressScrollTop() {
      var _stateName = _$state.current.name,
          whiteListStateMatches = _.filter(_whiteList, function(whiteListState) {
          return _stateName.indexOf(whiteListState) >= 0;
        });

      var isInWhiteList = angular.isArray(whiteListStateMatches) && whiteListStateMatches.length > 0;

      // Look for an intersection in previous matches to maintain whether or not we have transitioned to a new state
      // - if we have then do not suppress the scroll reset
      var wasStateInPreviousWhiteList = (_.intersection(whiteListStateMatches, _previousWhiteListMatches)).length > 0;

      _previousWhiteListMatches = whiteListStateMatches;

      return isInWhiteList && wasStateInPreviousWhiteList;
    }

    ////////
    function _scroll() {

      function _doInlineScroll(hash) {
        // Give angular some time to do digests then check for a
        // in page scroll

        var match = hash ? hash.match(/^!([\w\-]+)$/i) : false,
          to = 0,
          HEADER_HEIGHT = 49 + 10, // Height of header + some nice looking offset.
          el = null;

        if (match && match.length > 1) {
          hash = match[1];
          to = - HEADER_HEIGHT;
        }

        if (hash) {
          el = jQuery('#' + hash);
        }

        if (el && el.length > 0) {
          to += Math.round(parseFloat(el.offset().top));
          to = Math.max(0, to);
        }


        jQuery(window).scrollTop( to );
      }

      /////

      var _hash = _$location.hash();

      // Prevents browser window from jumping around while navigating analysis
      if ( _shouldSuppressScrollTop() && ! _hash) {
        return;
      }

      // Prevent the timeout from being fired multiple times if called before previous
      // timeout is complete. Make the last request the most valid.
      if (_scrollTimeoutHandle) {
        clearTimeout(_scrollTimeoutHandle);
      }

      _scrollTimeoutHandle = setTimeout(function () {
        _doInlineScroll(_hash);
        _scrollTimeoutHandle = null;
      }, 500);

    }
    ////////


    function _addToIgnoreScrollResetWhiteList(routes) {
      var routeList = [];

      if (! angular.isArray(routes)) {
        routeList.push(routes);
      }
      else {
        routeList = routes;
      }

      _.remove(routeList, function(route) {
        return typeof route !== 'string' || _.trim(route) === '';
      });

      // Ensure that the white list is unique
      if (routeList.length) {
        _whiteList = _.uniq(_whiteList.concat(routeList));
      }

      return _self;

    }

    ////////


    function _init($state, $location, $window, $rootScope) {
      _$state = $state;
      _$location = $location;
      _$window = $window;

      _.map(['pushState', 'replaceState'], _wrapHistoryAPI);

      $rootScope.$on('$viewContentLoaded', _scroll);
      $rootScope.$on('$stateChangeSuccess', _scroll);
    }

    function _isManaging() {
      return _$state !== null;
    }


    _self.addToIgnoreScrollResetWhiteList = _addToIgnoreScrollResetWhiteList;
    _self.startManaging = _init;
    _self.isManaging = _isManaging;

  }




  angular.module('icgc.historyManager', [])
    .provider('HistoryManager', function() {

      var _provider = this,
          _historyManager = new HistoryManager();

      _provider.addRoutesToWhiteList = function(routes) {
        _historyManager.addRoutesToWhiteList(routes);
      };

      _provider.$get = ['$state', '$location', '$window' , '$rootScope',
        function($state, $location, $window, $rootScope) {
          if (! _historyManager.isManaging()) {
            _historyManager.startManaging($state, $location, $window, $rootScope);
          }

          return _historyManager;
      }];
    });



})();