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

(function () {
  'use strict';


  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Define global namespace for the icgc app to be used by third parties as well as in the console.
  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  if (typeof window.$icgc === 'undefined') {
    (function(devConfig) {
      
      // used to configure dev profile for the API class
      var _defaultDevConfiguration = devConfig || {},
          _defaultDevHost = _defaultDevConfiguration.HOST || 'localhost',
          _defaultDevPort = _defaultDevConfiguration.API_PORT || '8080',
          _developerLocalMode = _defaultDevConfiguration.DEVELOPER_LOCAL_MODE === true ? true : false;

      // API Base class
      function API(version, localUIDevRun, host, port, context) {
        var _version = version || 1,
            _host = host || (localUIDevRun === true ? _defaultDevHost : null),
            _port =  port || (localUIDevRun === true ? _defaultDevPort : null),
            _context = context || '/api',
            _isDebugEnabled = false;


        this.getVersion = function() {
          return _version;
        };


        this.getBasePathURL = function() {
          return  (_host ? '//' + _host : '') +
              (_port ? ':' + _port : '')  +
              (_context + '/v' + _version);
        };

        this.setDebugEnabled = function(isDebugEnabled) {
          _isDebugEnabled = isDebugEnabled;
          return this;
        };


        this.isDebugEnabled = function() {
          return _isDebugEnabled;
        };


        return this;
      }



      // ICGC Constructor method which provides an access point for other JS logic to query rather than soley
      // relying off of angular. This will be useful for setting debug state on the fly as well as offering
      // an interface layer between the angular/non-angular worlds.
      function ICGCApp() {

        var _isDebugEnabled = false,
            _currentAPIVersion = 1,
            _APIInterface = null,
            _isLocalUIRun = false,
            _angularInjector = null;

        ///////////////////////////////////////////////////////
        // Private methods
        ///////////////////////////////////////////////////////
        // Default initializer
        function __initDefaultAPI() {
          _APIInterface = new API(_currentAPIVersion, _isLocalUIRun);
        }

        function __getAngularInjector() {
          if (_angularInjector) {
            return _angularInjector;
          }

          _angularInjector = angular.element(document.body).injector();

          return _angularInjector;
        }

        function __getAngularProvider(dependencyName) {

          if (__getAngularInjector) {
            __getAngularInjector();
          }

         var provider;

          try {
             provider = _angularInjector.get(dependencyName);
          }
          catch (e) {
            console.error('Cannot find dependency with name: ' + dependencyName);
            provider = null;
          }

          return provider;
        }

        function __setAPIDebugEnabled(isAPIDebugEnabled) {
          return _APIInterface.setDebugEnabled(isAPIDebugEnabled);
        }

        function __getQualifiedHost() {
          var url = '';

          if ( _isLocalUIRun === true ) {
            url = window.location.protocol + '//' + _defaultDevHost + ':' + _defaultDevPort;
          }

          return url;
        }

        ///////////////////////////////////////////////////////
        // Public API
        ////////////////////////////////////////////////////////
        this.getAPI = function() {

          if (_APIInterface === null) {
            __initDefaultAPI();
          }

          return _APIInterface;
        };

        this.setAPI = function(version) {
          _currentAPIVersion = version;
          // TODO: extend with potential to specify host and port (for now default it)
          _APIInterface = new API(_currentAPIVersion, _isLocalUIRun);
          return this;
        };

        // Turn on all debug across the app
        this.setDebugEnabled = function(isEnabled) {
          _isDebugEnabled = isEnabled;
          __setAPIDebugEnabled(isEnabled); // turn on the API debug as well
          return this;
        };

        this.setLocalUIRun = function(isLocalUIRun) {
          _isLocalUIRun = isLocalUIRun;
          return this;
        };

        // This method should be used for checking before performing a console.*
        // useful if debuging in a different environment that is not development
        this.isDebugEnabled = function() {
          return _isDebugEnabled;
        };

        // turn on the API debug only
        this.setAPIDebugEnabled = __setAPIDebugEnabled;

        this.getAngularInjector = __getAngularInjector;

        this.getAngularProvider = __getAngularProvider;

        this.getQualifiedHost = __getQualifiedHost;


      return this;
    }


     // Singleton global variable used to control the application settings across non-angular
     // libraries as well the the JS console
     window.$icgcApp = new ICGCApp().setLocalUIRun(_developerLocalMode);

    })(window.$ICGC_DEV_CONFIG || null);

  }
  //////////////////////////////////////////////////////////////////////////

  var toJson = angular.toJson;

  var module = angular.module('icgc', [
    'ngSanitize',
    'ngAnimate',
    'ngCookies',

    // angular plugins
    'restangular',
    'ui.scrollfix',
    'ui.bootstrap.modal',
    'ui.bootstrap.position',
    'template/modal/backdrop.html',
    'template/modal/window.html',
    'ui.router',
    'infinite-scroll',
    'angular-lodash',
    'angularytics',
    'angular-loading-bar',
    'btford.markdown',
    'LocalStorageModule',
    'toaster',


    // 3rd party
    'highcharts',

    // modules
    'icgc.modules.genomeviewer',
    'proteinstructureviewer',

    // core
    // new
    'icgc.ui',
    'icgc.share',
    'icgc.facets',
    'icgc.projects',
    'icgc.donors',
    'icgc.genes',
    'icgc.compounds',
    'icgc.mutations',
    'icgc.advanced',
    'icgc.releases',
    'icgc.portalfeature',
    'icgc.keyword',
    'icgc.browser',
    'icgc.donorlist',
    'icgc.genelist',
    'icgc.genesets',
    'icgc.visualization',
    'icgc.enrichment',
    'icgc.sets',
    'icgc.analysis',
    'icgc.phenotype',
    'icgc.beacon',
    'icgc.downloader',
    'icgc.pathwayviewer',
    'icgc.repositories',
    'icgc.repository',
    'icgc.software',
    'icgc.pancancer',
    'icgc.auth',
    'icgc.tokens',
    'icgc.pathways',
    'icgc.historyManager',

    // old
    'app.ui',
    'app.common',
    'app.downloader'
  ]);

  // Fix needed for loading subviews without jumping back to the
  // top of the page:
  // https://github.com/angular-ui/ui-router/issues/110#issuecomment-18348811
  // modified for our needs
  module
    .value('$anchorScroll', angular.noop)
    // Offer a means of forcing a reload of the current state when necessary
    .config(function($provide, cfpLoadingBarProvider) {

      // Global Loading Bar overrides
      cfpLoadingBarProvider.latencyThreshold = 100; // wait in ms before the loading bar shows up

      $provide.decorator('cfpLoadingBar', ['$delegate', function($delegate) {

        var _isEnabledLoadingBar = true,
            _originalStartFn = $delegate.start;

        if (angular.isDefined($delegate.enabled)) {
          console.warn('cfpLoadingBar.enabled exists! Aborting the decoration of cfpLoadingBar...');
          return;
        }

        $delegate.enabled = function (isEnabled) {
          _isEnabledLoadingBar = isEnabled === false ? false : true;
        };

        $delegate.start = function() {
          if (! _isEnabledLoadingBar) {
            return;
          }

          return _originalStartFn.apply($delegate, arguments);
        };

        return $delegate;
      }]);

      // Let's decorate our $state object to inline it with this functionality
      $provide.decorator('$state', ['$delegate', '$stateParams', function ($delegate, $stateParams) {
        $delegate.forceReload = function () {
          return $delegate.go($delegate.current, $stateParams, {
            reload: true,
            inherit: false,
            notify: true
          });
        };
        return $delegate;
      }]);



      $provide.decorator('Restangular', ['$delegate', '$q',  function($delegate, $q) {

        var _cancellableRequests = [];

        function _deletePromiseAbortCache(deferredKey) {

          var indexAt = _cancellableRequests.indexOf(deferredKey);

          if (indexAt < 0) {
            return;
          }

          _cancellableRequests.splice(indexAt, 1);

          //console.log('Removing deferred from abort cache: ', deferredKey);


           /*if (_cancellableRequests.length === 0) {
               console.info('Request abort cache is empty!');
           }*/
        }

        // Create a wrapped request function that will allow us to create http requests that
        // can timeout when the abort promise is resolved.
        function _createWrappedRequestFunction(restangularObject, requestFunction) {

          if (! angular.isDefined(requestFunction) || ! angular.isFunction(requestFunction)) {
            console.warn('Restangular REST function not defined cannot wrap!');
            return false;
          }

          // Function to wrap the call in which removes the abort promise from the queue on resolve/reject
          return function() {

            var deferred = $q.defer(),
              abortDeferred = $q.defer();

            // Save the deferred object so we may cancel it all later
            _cancellableRequests.push(abortDeferred);

            // Add an auxiliary method to cancel an individual request if one exists
            restangularObject.cancelRequest = function() {
                abortDeferred.resolve();
                _deletePromiseAbortCache(abortDeferred);
            };


            restangularObject.withHttpConfig({timeout: abortDeferred.promise});

            var requestPromise = requestFunction.apply(restangularObject, Array.prototype.slice.call(arguments));

            requestPromise.then(
              function(data) {
                //console.log('Success:', restangularObject, data);
                _deletePromiseAbortCache(abortDeferred);
                deferred.resolve(data);
              },
              function(error) {
                //console.log('Failure:', restangularObject);
                _deletePromiseAbortCache(abortDeferred);
                deferred.reject(error);
              }
            );

            return deferred.promise;
          };

        }

        function _createCancelableRequest(restangularCollectionFunction, args) {
          var callingArgs =  Array.prototype.slice.call(args),
            /*jshint validthis:true */
            _this = this;



          var restangularObject = restangularCollectionFunction.apply(_this, callingArgs);

          // Wrap the request items
          restangularObject.get = _createWrappedRequestFunction(
            restangularObject, restangularObject.get
          );

          restangularObject.getList = _createWrappedRequestFunction(
            restangularObject, restangularObject.getList
          );
          restangularObject.post = _createWrappedRequestFunction(
            restangularObject, restangularObject.post
          );

          _wrapRestangular(restangularObject);


          return restangularObject;
        }


        function _wrapRequest(fn) {

          return function() {
            return _createCancelableRequest.call(this, fn, arguments);
          };

        }


        function _wrapRequestFunctions(restangularObj) {

          if (! angular.isDefined(restangularObj.one)) {
            return;
          }

          restangularObj.one = _.bind(_wrapRequest(restangularObj.one), restangularObj);
          restangularObj.all = _.bind(_wrapRequest(restangularObj.all), restangularObj);
        }


        function _wrapRestangular(restangularObj) {

          _wrapRequestFunctions(restangularObj);


          if (! angular.isDefined(restangularObj.withHttpConfig)) {
            return;
          }

          var withHttpConfigFn = restangularObj.withHttpConfig;

          // Wrap the config
          restangularObj.withHttpConfig = function() {
            var withHttpConfigRestangularObject = withHttpConfigFn.apply(this, Array.prototype.slice.call(arguments));

            _wrapRequestFunctions(withHttpConfigRestangularObject);

            return withHttpConfigRestangularObject;
          };

        }

        function _init() {
          _wrapRestangular($delegate);
        }

        _init();

        ///////////////

        $delegate.abortAllHTTPRequests = function() {
          var requestUrls = _.keys(_cancellableRequests);
          var abortRequestLength = requestUrls.length;

          for (var i = 0; i < abortRequestLength; i++) {
            var requestURL = requestUrls[i];
            console.log('Cancelling HTTP Request: ', requestURL);
            _cancellableRequests[requestURL].resolve();
          }

          // Reset the deferred abort list
          _cancellableRequests.length = 0;
        };

        return $delegate;
      }]);

    })
    .run(function($state, $location, $window, $timeout, $rootScope, cfpLoadingBar, HistoryManager) {

      HistoryManager.addToIgnoreScrollResetWhiteList(['analysis','advanced', 'compound']);

      // Add UI Router Debug if there is a fatal state change error
      $rootScope.$on('$stateChangeError', function () { 
        console.error('State Change Error Occurred. Error occurred with arguments: ', arguments);
      });

      function _initProgressBarRunOnce() {
        var _shouldDisableLoadingBar = true,
            _timeoutHandle = null,
            _debounceDelayMS = 200;

        var deregisterLoadingFn = $rootScope.$on('cfpLoadingBar:loading', function () {
          //console.log('Progress Started!');
          _shouldDisableLoadingBar = false;
        });

        var deregisterCompletedFn = $rootScope.$on('cfpLoadingBar:completed', function () {
          // Disable the loading bar after the debounced run first run
          _shouldDisableLoadingBar = true;
          //console.log('Progress Completed!');

          if (_timeoutHandle) {
            clearTimeout(_timeoutHandle);
          }

          _timeoutHandle = setTimeout(function () {
            if (_shouldDisableLoadingBar) {
              //console.log('Progress Disabled!');
              cfpLoadingBar.enabled(false);
              deregisterLoadingFn();
              deregisterCompletedFn();
            }
          }, _debounceDelayMS);
        });
      }

      _initProgressBarRunOnce();
    });



  /**
   * This is the base URL for API requests. We can change this to use API from a different server, this is useful
   * when only testing the user interface, or to debug production UI issues.
   */

  module.constant('API', {
    BASE_URL: $icgcApp.getAPI().getBasePathURL()
  });



  module.config(function ($locationProvider, $stateProvider, $urlRouterProvider, $compileProvider,
                          AngularyticsProvider, $httpProvider, RestangularProvider,
                          markdownConverterProvider, localStorageServiceProvider, API,
    copyPasteProvider) {
                            
    // Let copyPasteProvider know where the flash app for copying and pasting is
    var copyPastePath = window.$ICGC_DEV_CONFIG ? null : 'bower_components/zeroclipboard/dist/ZeroClipboard.swf';
    copyPasteProvider.zeroClipboardPath(copyPastePath);
    
    
    // Disables debugging information
    $compileProvider.debugInfoEnabled(false);

    // Combine calls - needs more testing
    $httpProvider.useApplyAsync(true);

    // Use in production or when UI hosted by API
    RestangularProvider.setBaseUrl(API.BASE_URL);

    // Function that returns a interceptor function
    function _getInterceptorDebugFunction(requestType){

      return function(data, operation, model) {
        // perform this check dynamically the computation time is neglible so this shouldn't impede on perfomance
        if ($icgcApp.getAPI().isDebugEnabled()) {
          console.log(requestType + ' Method: ', operation.toUpperCase(), '\nModel: ', model, '\nData: ', data);
        }
        return data;
      };

    }

    RestangularProvider.setRequestInterceptor(_getInterceptorDebugFunction('Request'));
    RestangularProvider.setResponseInterceptor(_getInterceptorDebugFunction('Reponse'));



    RestangularProvider.setDefaultHttpFields({cache: true});

    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

    $locationProvider.html5Mode(true);

    AngularyticsProvider.setEventHandlers(['Google']);


    $stateProvider.state(
      'team', {
        url: '/team',
        templateUrl: '/scripts/static/views/team.html',
        controller: ['Page', function (Page) {
          Page.setTitle('The Team');
          Page.setPage('entity');
        }]
      });

    // All else redirect to home
    $urlRouterProvider.otherwise(function($injector, $location) {

      $injector.invoke(['Notify', 'Page', function(Notify, Page) {
        Page.setPage('error');
        Notify.setMessage('Cannot find: ' + $location.url());
        Notify.showErrors();
      }]);
    });

    markdownConverterProvider.config({
      extensions: ['table']
    });

    localStorageServiceProvider.setPrefix('icgc');
  });

  module.run(function ($http, $state, $timeout, $interval, $rootScope, $modalStack,
    Restangular, Angularytics, Compatibility, Notify, Page) {

    var ignoreNotFound = [
      '/analysis/',
      '/list',
      '/ui/reactome'
    ];

    Restangular.setErrorInterceptor(function (response) {

      if (response.status !== 401) {
        console.error ('Response Error: ', toJson (response));
      }

      if (response.status >= 500) {
        Notify.setMessage ('' + response.data.message || response.statusText);
        Notify.showErrors();
      } else if (response.status === 404) {

        // Ignore 404's from specific end-points, they are handled locally
        // FIXME: Is there a better way to handle this within restangular framework?
        var ignore = false;
        ignoreNotFound.forEach(function(endpoint) {
          if (response.config && response.config.url.indexOf(endpoint) >= 0) {
            ignore = true;
          }
        });
        if (ignore === true) {
          return true;
        }

        if (response.data.message) {
          Page.setPage('error');
          Notify.setMessage(response.data.message);
          Notify.showErrors();
        }
      } else if (response.status === 400) {
        if (response.data.message) {
          Notify.setMessage('' + response.data.message);
        }
        Notify.showErrors();
      }
    });

    Angularytics.init();
    // Browser compatibility tests
    Compatibility.run();


    // Close any modal dialogs on location chagne
    $rootScope.$on('$locationChangeSuccess', function (newVal, oldVal) {

      if (oldVal !== newVal && $modalStack.getTop()) {
        $modalStack.dismiss($modalStack.getTop().key);
      }

    });

  });


  /**
   * This holds the constant values for special fields that do not have a 1-to-1 correspondence with our
   * underlying data structure.
   *  ENTITY is mapped to id but references a set of unique identifiers.
   *  GENE_SET_ROOTS encapsulates the top level gene set information for various types of gene sets.
   */
  module.constant('Extensions', {

    // Donor, mutation or gene set ids
    ENTITY: 'entitySetId',
    
    ENTITY_PREFIX: 'ES:',

    // Order matters, this is in most important to least important (For enrichment analysis)
    GENE_SET_ROOTS: [
      {type: 'pathway', id: null, name: 'Reactome Pathways', universe: 'REACTOME_PATHWAYS'},
      {type: 'go_term', id: 'GO:0003674', name: 'GO Molecular Function', universe: 'GO_MOLECULAR_FUNCTION'},
      {type: 'go_term', id: 'GO:0008150', name: 'GO Biological Process', universe: 'GO_BIOLOGICAL_PROCESS'},
      {type: 'go_term', id: 'GO:0005575', name: 'GO Cellular Component', universe: 'GO_CELLULAR_COMPONENT'},
      {type: 'curated_set', id: 'GS1', name: 'Cancer Gene Census', universe: null}
    ]
  });


  module.controller('AppCtrl', function ($scope, Page) {
    var _ctrl = this;
    _ctrl.appLoaded = true;
    _ctrl.Page = Page;

    // for document level clicks
    _ctrl.handleApplicationClick = function () {
      $scope.$broadcast('application:click');
    };
  });
})();
