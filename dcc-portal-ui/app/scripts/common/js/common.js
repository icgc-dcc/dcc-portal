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

require('../components');

(function () {
  'use strict';


  var module = angular.module('app.common', [
    'app.common.components',
    'app.common.services',
    'app.common.header',
    'app.common.footer',
    'icgc.common.version',
    'icgc.common.notify',
    'icgc.common.location',
    'icgc.common.filters',
    'icgc.common.display',
    'icgc.common.external',
    'icgc.common.text.utils',

    // UI
    'icgc.common.codetable',
    'icgc.common.translator',

    // Biological modules
    'icgc.common.chromosome',
    'icgc.common.consequence',
    'icgc.common.datatype',
    'icgc.common.pcawg',

    // Query langauge
    // Note: currently unused in the application
    // 'icgc.common.pql.translation',
    // 'icgc.common.pql.queryobject',
    // 'icgc.common.pql.utils'
  ]);



  // Translate project code into disease code. ie. BRCA-US => BRCA
  module.filter('diseaseCode', function() {
    return function(item) {
      return item.split('-')[0];
    };
  });



  module.filter('sum', function () {
    return function (items, param) {
      var ret = null;
      if (angular.isArray(items)) {
        ret = _.reduce(_.pluck(items, param), function (sum, num) {
          return sum + num;
        });
      }
      return ret;
    };
  });

  module.filter('_', function () {
    return function () {
      var input = arguments[0];
      var method = arguments[1];
      invariant(_.isString(method), 'The first argument must be a string that specifies a lodash method name');
      var args = Array.prototype.slice.call(arguments, 2);
      return _[method].apply(null, [input].concat(args));
    };
  });

  module.filter('startsWith', function () {
    return function (string, start) {
      var ret = null;
      if (angular.isString(string)) {
        ret = string.indexOf(start) === 0 ? string : null;
      }
      return ret;
    };
  });

  module.filter('numberPT', function ($filter) {
    return function (number) {
      if (angular.isNumber(number)) {
        return $filter('number')(number);
      } else {
        return number;
      }
    };
  });


  // a filter used to provide a tooltip (descriptive name) for a gene-set
  module.filter ( 'geneSetNameLookup', function (GeneSetNameLookupService) {
    return function (id) {
      return GeneSetNameLookupService.get ( id );
    };
  });


  module.filter('highlight', function () {
    return function (text, search, hide) {
      text = text || '';
      hide = hide || false;
      if (search) {
        text = angular.isArray(text) ? text.join(', ') : text.toString();
        // Shrink extra spaces, restrict to alpha-numeric chars and a few other special chars
        search = search.toString().replace(/\s+/g, ' ').replace(/[^a-zA-Z0-9:,\s\-_\.]/g, '').split(' ');
        for (var i = 0; i < search.length; ++i) {
          text = text.replace(new RegExp(search[i], 'gi'), '^$&$');
        }

        // if match return text
        if (text.indexOf('^') !== -1) {
          return text.replace(/\^/g, '<span class="match">').replace(/\$/g, '</span>');
        } else { // no match
          if (hide) {
            return '';
          } // hide
        }
      }

      // return base text if no match and not hiding
      return text;
    };
  });

  module.factory('debounce', function ($timeout, $q) {
    return function (func, wait, immediate) {
      var timeout, deferred;
      deferred = $q.defer();

      return function () {
        var context, later, callNow, args;

        context = this;
        args = arguments;
        later = function () {
          timeout = null;
          if (!immediate) {
            deferred.resolve(func.apply(context, args));
            deferred = $q.defer();
          }
        };
        callNow = immediate && !timeout;
        if (timeout) {
          $timeout.cancel(timeout);
        }
        timeout = $timeout(later, wait);
        if (callNow) {
          deferred.resolve(func.apply(context, args));
          deferred = $q.defer();
        }
        return deferred.promise;
      };
    };
  });

  function LoadState(params) {
    params = params || {};
    var selfLoadState = { isLoading: !!params.isLoading };
    var contributingLoadStates = _.compact([selfLoadState].concat(params.contributingLoadStates));
    var isPromise = function (input) {
      return _.isFunction(input.then);
    };

    var loadState = {
      get isLoading() {
        return _.some(contributingLoadStates, 'isLoading');
      },
      set isLoading(val) {
        selfLoadState.isLoading = val;
      },
      addContributingLoadState: function (loadState) {
        if (_.contains(contributingLoadStates, loadState)) {
          console.warn('load state is already in contributing loadStates, this shouldnt happen');
          return;
        }
        contributingLoadStates = contributingLoadStates.concat(loadState);
      },
      removeContributingLoadState: function (loadState) {
        contributingLoadStates = _.without(contributingLoadStates, loadState);
      },
      loadWhile: function (work) {
        invariant(isPromise(work) || _.isArray(work) && _.every(work, isPromise),
          'loadWhileAsync requires a promise or an array of promises');
        var promise = _.isArray(work) ? Promise.all(work) : work;
        loadState.startLoad();
        return promise
          .catch(loadState.endLoad)
          .then(loadState.endLoad);
      },
      startLoad: function () {
        selfLoadState.isLoading = true;
      },
      endLoad: function () {
        selfLoadState.isLoading = false;
      },
      register: function (scope) {
        invariant(_.isFunction(scope.registerLoadState), 'Required function $scope.registerLoadState is invalid');
        invariant(_.isFunction(scope.deregisterLoadState), 'Required function $scope.deregisterLoadState is invalid');
        scope.registerLoadState(loadState);
        scope.$on('$destroy', function () {
          scope.deregisterLoadState(loadState);
        });
      }
    };

    if (params.scope) {
      loadState.register(params.scope);
    }

    return loadState;
  }

  module.constant('LoadState', LoadState);

  module.filter('unique', function () {
    return function (items) {
      var i, set, item;

      set = [];
      if (items && items.length) {
        for (i = 0; i < items.length; ++i) {
          item = items[i];
          if (set.indexOf(item) === -1) {
            set.push(item);
          }
        }
      }
      return set;
    };
  });

// Convert a non-array item into an array
  module.filter('makeArray', function () {
    return function (items) {
      if (angular.isArray(items)) {
        return items;
      }
      return [items];
    };
  });


  module.filter('bytes', function () {
    return function (input) {
      var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB'],
        postTxt = 0,
        bytes = input,
        precision = 2;

      if (bytes <= 1024) {
        precision = 0;
      }

      while (bytes >= 1024) {
        postTxt++;
        bytes = bytes / 1024;
      }

      return Number(bytes).toFixed(precision) + ' ' + sizes[postTxt];
    };
  });

  module.filter('projectCode', function () {
    return function (input) {
      return input.toString().replace(/[\[\"][\"\]]/g, '');
    };
  });

  module.filter('withoutFirst', function () {
    return function (input) {
      return input.slice(1,input.length).toString();
    };
  });

  // TODO: Ideally this should be done during the ETL phase.
  module.filter ('formatCompoundClass', function () {
    var fda = 'fda';

    return function (compoundClass) {
      return (fda === compoundClass) ? compoundClass.toUpperCase() : _.capitalize (compoundClass);
    };
  });

  module.service('ApiService', function (FilterService) {
    
    var getAll = function(resource, params) {
      var defaults = {
        size: 100,
        from: 1,
        filters: FilterService.filters()
      };

      var newParams = _.extend({}, defaults, params);

      var acc = [];

      function page(params) {
        return resource.get('', params).then(function (data) {
          acc = acc.concat(data.hits);
          var pagination = data.pagination;
          if (pagination.page < pagination.pages) {
            var newParams = _.extend({}, params, {
              from: (pagination.page + 1 - 1) * 100 + 1
            });
            return page(newParams);
          } else {
            return acc;
          }
        });
      }
      
      return page(newParams);
    };
    
    return {
      getAll: getAll
    };
  });

  module.service('FullScreenService', function($rootScope) {

    var exitFullScreen = function() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.mozCancelFullScreen) {
            document.mozCancelFullScreen();
        } else if (document.webkitExitFullscreen) {
            document.webkitExitFullscreen();
        }
    };

    var enterFullScreen = function(element) {
        if (element.requestFullscreen) {
            element.requestFullscreen();
        } else if (element.mozRequestFullScreen) {
            element.mozRequestFullScreen();
        } else if (element.webkitRequestFullScreen) {
            element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
        }
    };

    var getFullScreenElement = function () {
      return document.fullScreenElement || document.mozFullScreenElement || document.webkitFullscreenElement;
    };

    function onFullScreenChange() {
      // Digest cycle won't get auto-triggered when fullscreen mode exits natively
      if(!$rootScope.$$phase) {
        $rootScope.$apply();
      }
    }

    document.addEventListener('fullscreenchange', onFullScreenChange, false);
    document.addEventListener('webkitfullscreenchange', onFullScreenChange, false);
    document.addEventListener('mozfullscreenchange', onFullScreenChange, false);

    return {
      exitFullScreen: exitFullScreen,
      enterFullScreen: enterFullScreen,
      get fullScreenElement () {
        return getFullScreenElement();
      }
    };
  });

  module.filter('subDelimiters', function($interpolate){
    return function(string, context){
       string = string.replace(/\[\[/g, '{{').replace(/\]\]/g, '}}');
      var interpolateFn = $interpolate(string);
      return interpolateFn(context);
    };
  });

  // This is a workaroud required for Internationalization of 'Experimental&nbsp;Strategy'
  module.filter('replace', function(){
    return function(string, pattern, replacement){
      return string.replace(new RegExp(pattern, 'g'), replacement);
    };
  });

  module.directive('ngLazyShow', require('./lazy-show'));
  module.filter('pluralize', () => (...args) => require('pluralize')(...args));

})();