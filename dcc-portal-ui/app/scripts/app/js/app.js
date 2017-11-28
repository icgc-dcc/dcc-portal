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
      function API(version, localUIDevRun, host, port, context, protocol) {
        var _version = version || 1,
            _protocol = protocol || window.location.protocol,
            _host = host || (localUIDevRun === true ? _defaultDevHost : null) || window.location.hostname,
            _port =  port || (localUIDevRun === true ? _defaultDevPort : null) || window.location.port,
            _context = context || '/api',
            _isDebugEnabled = false;


        this.getVersion = function() {
          return _version;
        };


        this.getBasePathURL = function() {
          return _protocol + '//' + _host + (_port ? ':' + _port : '') + (_context + '/v' + _version);
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

    'ui.bootstrap.popover',
    'ui.bootstrap.tpls',
    'ui.scrollpoint',
    'ui.bootstrap.modal',
    'ui.bootstrap.position',
    'ui.bootstrap.pagination',
    'template/modal/backdrop.html',
    'template/modal/window.html',
    'template/pagination/pagination.html',
    'ui.router',
    'infinite-scroll',
    'angularytics',
    'angular-loading-bar',
    'hc.marked',
    'LocalStorageModule',
    'toaster',
    'dndLists',
    'gettext',
    'xeditable',
    'angular-bind-html-compile',


    // 3rd party
    'highcharts',

    // modules
    'icgc.modules.genomeviewer',
    'proteinstructureviewer',

    // core
    // new
    'icgc.ui',
    'icgc.share',
    'icgc.pql',
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
    'icgc.entitySetUpload',
    'icgc.genesets',
    'icgc.visualization',
    'icgc.enrichment',
    'icgc.sets',
    'icgc.analysis',
    'icgc.phenotype',
    'icgc.oncogrid',
    'icgc.beacon',
    'icgc.downloader',
    'icgc.pathwayviewer',
    'icgc.repositories',
    'icgc.repository',
    'icgc.pancancer',
    'icgc.auth',
    'icgc.tokens',
    'icgc.pathways',
    'icgc.historyManager',
    'icgc.survival',
    'icgc.404',
    'icgc.301',

    // old
    'app.ui',
    'app.common',
    'app.downloader'
  ]);

  const REDIRECTS = [
    {
      'from': '/icgc-in-the-cloud/guide',
      'to': 'http://docs.icgc.org/cloud/guide'
    }
  ];

  const getRedirectObject = (currentUrl) => {
    const redirect = _.find(REDIRECTS, (redirect) => _.startsWith(currentUrl, redirect.from.replace(/\/$/, "")));
    let state = '404', page = currentUrl;

    if(redirect){
      state = '301';
      page = redirect.to;
    }

    return {state, page};
  }
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

      require('./wrapRestangular')($provide);

    })
    .run(function($state, $location, $window, $timeout, $rootScope, cfpLoadingBar, HistoryManager, gettextCatalog, Settings) {
      
      // Setting the initial language to English CA.
      gettextCatalog.setCurrentLanguage('en_CA');

      HistoryManager.addToIgnoreScrollResetWhiteList(['analysis','advanced', 'compound', 'dataRepositories', 'donor', 'beacon', 'project', 'gene']);
      
      $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
        if(error.status === 404) {
          const redirect = getRedirectObject($location.url());
          $state.go(redirect.state, {page: redirect.page, name: toState.name, id: toParams.id, url: toState.url}, {location: false});
        } else {
          console.error(error.message);
          console.error(error.stack);
        }
      });

      $rootScope._ = require('lodash');

      $rootScope.track = require('../../common/js/track');

      const trackTimeouts = {};
      $rootScope.delayedTrack = (eventCategory, properties, delay) =>
        trackTimeouts[`${eventCategory}->${JSON.stringify(_.pick(properties, ['action', 'label']))}`] = setTimeout(() =>
          track(eventCategory, properties), delay);
      $rootScope.clearDelayedTrack = (eventCategory, properties) =>
        clearTimeout(trackTimeouts[`${eventCategory}->${JSON.stringify(_.pick(properties, ['action', 'label']))}`]);

      $rootScope.$on('$stateNotFound', function() {
        const redirect = getRedirectObject($location.url());
        $state.go(redirect.state, {page: redirect.page}, {location: false});
      });

      Settings.get().then(ICGC_SETTINGS => { $rootScope.ICGC_SETTINGS = ICGC_SETTINGS });

      function _initProgressBarRunOnce() {
        var _shouldDisableLoadingBar = true,
            _timeoutHandle = null,
            _debounceDelayMS = 200;

        var deregisterLoadingFn = $rootScope.$on('cfpLoadingBar:loading', function () {
          _shouldDisableLoadingBar = false;
        });

        var deregisterCompletedFn = $rootScope.$on('cfpLoadingBar:completed', function () {
          // Disable the loading bar after the debounced run first run
          _shouldDisableLoadingBar = true;

          if (_timeoutHandle) {
            clearTimeout(_timeoutHandle);
          }

          _timeoutHandle = setTimeout(function () {
            if (_shouldDisableLoadingBar) {
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
    BASE_URL: $icgcApp.getAPI().getBasePathURL(),
  });

  module.config(function ($locationProvider, $stateProvider, $urlRouterProvider, $compileProvider,
                          AngularyticsProvider, $httpProvider, RestangularProvider,
                          markedProvider, localStorageServiceProvider, API) {

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
        return data;
      };

    }

    RestangularProvider.setRequestInterceptor(_getInterceptorDebugFunction('Request'));
    RestangularProvider.setResponseInterceptor(_getInterceptorDebugFunction('Reponse'));

    RestangularProvider.addFullRequestInterceptor(function (element, operation, route, url, headers, params, httpConfig) {
      if (params && params.filters && JSON.stringify(params.filters).match('ES:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')) {
        return {
          httpConfig: {cache: false}
        };
      }
    });

    RestangularProvider.setDefaultHttpFields({cache: true});

    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

    $locationProvider.html5Mode(true);

    AngularyticsProvider.setEventHandlers(['GoogleUniversal']);


    $stateProvider.state(
      'team', {
        url: '/team',
        templateUrl: '/scripts/static/views/team.html',
        controller: function (Page, gettextCatalog) {
          Page.setTitle(gettextCatalog.getString('The Team'));
          Page.setPage('entity');
        }
      });

    // If invalid route is requested
    $urlRouterProvider.otherwise(function ($injector, $location){
      const redirect = getRedirectObject($location.url());
      $injector.invoke(['$state', function($state) {
        $state.go(redirect.state, {page: redirect.page}, {location: false});
      }]);
    });

    markedProvider.setOptions({ gfm: true });

    localStorageServiceProvider.setPrefix('icgc');
  });

  module.run(function ($http, $state, $timeout, $interval, $rootScope, $modalStack,
    Restangular, Angularytics, Compatibility, Notify) {

    Restangular.setErrorInterceptor(function (response) {

      if (response.status !== 401 && response.status !== -1) {
        console.error('Response Error: ', toJson (response));
      }
      
      if (response.status === 500) {
        Notify.setParams(response);
        Notify.showErrors();
      } else if (response.status === 404) {
        console.error(response.data.message);
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
      {type: 'pathway', id: null, name: gettext('Reactome Pathways'), universe: 'REACTOME_PATHWAYS'},
      {type: 'go_term', id: 'GO:0003674', name: gettext('GO Molecular Function'), universe: 'GO_MOLECULAR_FUNCTION'},
      {type: 'go_term', id: 'GO:0008150', name: gettext('GO Biological Process'), universe: 'GO_BIOLOGICAL_PROCESS'},
      {type: 'go_term', id: 'GO:0005575', name: gettext('GO Cellular Component'), universe: 'GO_CELLULAR_COMPONENT'},
      {type: 'curated_set', id: 'GS1', name: gettext('Cancer Gene Census'), universe: null}
    ]
  });

  module.controller('AppCtrl', function ($scope, Page, Settings) {
    var _ctrl = this;
    _ctrl.appLoaded = true;
    _ctrl.Page = Page;
    _ctrl.authEnabled = false;
    _ctrl.mirror = {};

    // for document level clicks
    _ctrl.handleApplicationClick = function () {
      $scope.$broadcast('application:click');
    };

    $scope.$on('$locationChangeStart', function () {
      $scope.$emit('tooltip::hide');
    });

    Settings.get().then(function(setting){
       _ctrl.authEnabled = setting.authEnabled;
       _ctrl.mirror = setting.mirror;
    });

  });

  function gettext(string){
    return string;
  }