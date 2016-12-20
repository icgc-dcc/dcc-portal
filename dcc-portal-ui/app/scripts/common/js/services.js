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

  var module = angular.module('app.common.services', []);

  module.factory('RestangularNoCache', function(Restangular) {
    return Restangular.withConfig(function(RestangularConfigurer) {
      RestangularConfigurer.setDefaultHttpFields({cache: false});
    });
  });


  module.factory('Page', function (gettextCatalog) {
    var title = gettextCatalog.getString('Loading...'),
      page = 'home',
      error = false,
      working = 0,
      exporting = false;

    return {
      title: function () {
        return title;
      },
      setTitle: function (t) {
        if (angular.isDefined(t)) {
          title = t;
        }
      },
      page: function () {
        return page;
      },
      setPage: function (p) {
        if (angular.isDefined(p)) {
          page = p;
        }
      },
      startWork: function (work) {
        invariant(!work || work.then, 'Optional argument "work" must be a promise.');
        working++;
        if (work) {
          work.finally(function () {
            working--;
          });
          return work;
        }
      },
      stopWork: function () {
        if (working > 0) {
          working--;
        }
      },
      working: function () {
        return working;
      },
      // For reseting state on error
      stopAllWork: function() {
        working = 0;
        angular.element(document.querySelector('article')).css('visibility', 'visible');
        angular.element(document.querySelector('aside')).css('visibility', 'visible');
      },
      setError: function(e) {
        error = e;
        if (error === true) {
          angular.element(document.querySelector('article')).css('visibility', 'hidden');
          angular.element(document.querySelector('aside')).css('visibility', 'hidden');
        }
      },
      getError: function() {
        return error;
      },
      startExport: function() {
        exporting = true;
      },
      stopExport: function() {
        exporting = false;
      },
      isExporting: function() {
        return exporting;
      }
    };
  });

  module.factory('Compatibility', function ($window) {

    function checkBase64() {
      if (!angular.isDefined($window.btoa)) {
        $window.btoa = base64.encode;
      }
      if (!angular.isDefined($window.atob)) {
        $window.atob = base64.decode;
      }
    }

    function checkLog() {
      if (!($window.console && console.log)) {
        $window.console = {
          log: function () {
          },
          debug: function () {
          },
          info: function () {
          },
          warn: function () {
          },
          error: function () {
          }
        };
      }
    }

    function checkTime() {
      if ($window.console && typeof($window.console.time === 'undefined')) {
        $window.console.time = function () {
        };
        $window.console.timeEnd = function () {
        };
      }
    }

    return {
      run: function () {
        checkBase64();
        checkLog();
        checkTime();
      }
    };
  });

  module.service('Settings', function () {
    Object.freeze(window.ICGC_SETTINGS);
    this.get = () => Promise.resolve(window.ICGC_SETTINGS);
  });

  module.service('ProjectCache', function(Projects) {
    var promise = null;
    var cache = {};

    function getData() {
      if (promise !== null)  {
        return promise;
      }
      promise = Projects.getMetadata().then(function(data) {
        data.hits.forEach(function(project) {
          cache[project.id] = project.name;
        });
        return cache;
      });
      return promise;
    }

    this.getData = getData;
  });

  module.factory ('RouteInfoService', function ($state, $log, gettextCatalog) {
    var href = $state.href;
    var routeInfo = {
      home: {
        href: href ('home'),
        title: gettextCatalog.getString('Home')
      },
      projects: {
        href: href ('projects'),
        title: gettextCatalog.getString('Cancer Projects')
      },
      advancedSearch: {
        href: href ('advanced'),
        title: gettextCatalog.getString('Advanced Search')
      },
      dataAnalysis: {
        href: href ('analysis'), // DCC-4594 default is launch analysis
        title: gettextCatalog.getString('Data Analysis')
      },
      dataReleases: {
        href: href ('dataReleases'),
        title: gettextCatalog.getString('DCC Data Releases')
      },
      dataRepositories: {
        href: href ('dataRepositories'),
        title: gettextCatalog.getString('Data Repositories')
      },
      pcawg: {
        href: href ('pancancer'),
        title: gettextCatalog.getString('PCAWG')
      },
      dataRepositoryFile: {
        href: href ('dataRepositoryFile'),
        title: gettextCatalog.getString('File')
      },
      drugCompound: {
        href: href ('compound'),
        title: gettextCatalog.getString('Compound')
      }
    };

    return {
      get: function (name) {
        if (! _.has (routeInfo, name)) {
          $log.error ('No route info is defined for %s.', name);
          return {};
        }

        return _.get (routeInfo, name, {});
      }
    };
  });

  /**
  * Centralized location for tooltip text
  */
  module.service('TooltipText', function(gettextCatalog) {
    this.ENRICHMENT = {
      OVERVIEW_GENES_OVERLAP: gettextCatalog.getString('Intersection between genes involved in Universe and input' +
        ' genes.'),
      INPUT_GENES: gettextCatalog.getString('Number of genes resulting from original query with upper limit. <br>' +
        'Input genes for this enrichment analysis result.'),
      FDR: gettextCatalog.getString('False Discovery Rate'),
      GENESET_GENES: gettextCatalog.getString('Number of genes involved in this gene set.'),
      GENESET_GENES_OVERLAP: gettextCatalog.getString('Intersection between genes involved in this gene set and' + 
        ' input genes.'),
      GENESET_DONORS: gettextCatalog.getString('Number of donors filtered by genes in overlap'),
      GENESET_MUTATIONS: gettextCatalog.getString('Number of simple somatic mutations filtered by genes in overlap.'),
      // GENESET_EXPECTED: 'Number of genes expected by chance',
      GENESET_EXPECTED: gettextCatalog.getString('Number of genes in overlap expected by chance'),
      GENESET_PVALUE: gettextCatalog.getString('P-Value using hypergeometric test'),
      GENESET_ADJUSTED_PVALUE: gettextCatalog.getString('Adjusted P-Value using the Benjamini-Hochberg procedure')
    };
  });


  // a function that returns an Angular 'structure' that represents a resuable
  // service which provides a simple hash/lookup function as well as fetching data via ajax.
  var KeyValueLookupServiceFactory = function ( fetch ) {

    return ['Restangular', '$log', function (Restangular, $log) {

      var _lookup = {};

      var _retrieve = function ( id ) {
        return _lookup[id];
      };
      var _echoOrDefault = function ( value, defaultValue ) {
        return ( value ) ?
          value :
          defaultValue || '';
      };
      var _noop = function () {};
      var _fetch = ( angular.isFunction (fetch) ) ? fetch : _noop;

      this.put = function ( id, name ) {
        if ( id && name ) {
          _lookup[id + ''] = name + '';

          $log.debug ( 'Updated lookup table is:' + JSON.stringify (_lookup) );
        }
      };
      this.get = function ( id ) {
        var result = _retrieve ( id );

        return _echoOrDefault ( result, id );
      };
      this.batchFetch = function ( ids ) {
        if ( angular.isArray (ids) ) {
          var missings = _.difference ( ids, _.keys (_lookup) );

          var setter = this.put;
          missings.forEach ( function (id) {
            _fetch ( Restangular, setter, id );
          });
        }
      };
    }];
  };

  // callback handler for gene-set name lookup
  var _fetchGeneSetNameById = function ( rest, setter, id ) {
    rest
      .one ( 'genesets', id )
      .get ( {field: ['id', 'name']} )
      .then ( function (geneSet) {

        if ( id === geneSet.id ) {
          setter ( id, geneSet.name );
        }
      });
  };

  module.service ( 'GeneSetNameLookupService', new KeyValueLookupServiceFactory (_fetchGeneSetNameById) );



  /**
   * Client side export of content using Blob and File, with fall back to server-side content echoing
   */
  module.service('ExportService', function() {
    this.exportData = function(filename, data) {
      if (window.Blob && window.File) {
        saveAs(new Blob([data], {type: 'text/plain;charset=utf-8'}), filename);
      } else {
        // Fallback (IE and other browsers that lack support), create a form and
        // submit a post request to bounce the download content against the server
        jQuery('<form method="POST" id="htmlDownload" action="/api/v1/ui/echo" style="display:none">' +
               '<input type="hidden" name="fileName" value="' + filename + '"/>' +
               '<input type="hidden" name="text" value="' + data + '"/>' +
               '<input type="submit" value="Submit"/>' +
               '</form>').appendTo('body');
        jQuery('#htmlDownload').submit();
        jQuery('#htmlDownload').remove();
      }

    };

    this.exportDataUri = (name, uri) => {
      if (navigator.msSaveOrOpenBlob) {
        navigator.msSaveOrOpenBlob(uriToBlob(uri), name);
      } else {
        var saveLink = document.createElement('a');
        var downloadSupported = 'download' in saveLink;
        if (downloadSupported) {
          saveLink.download = name;
          saveLink.href = uri;
          saveLink.style.display = 'none';
          document.body.appendChild(saveLink);
          saveLink.click();
          document.body.removeChild(saveLink);
        }
        else {
          window.open(uri, '_temp', 'menubar=no,toolbar=no,status=no');
        }
      }
    };
  });
})();
