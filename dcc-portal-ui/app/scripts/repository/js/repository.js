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

import {ensureArray, ensureString} from '../../common/js/ensure-input';
import './file-finder';

(function() {
  'use strict';

  var isEmptyArray = _.flow (ensureArray, _.isEmpty);

  var isEmptyString = _.flow (ensureString, _.isEmpty);

  function ensureObject (o) {
    return _.isPlainObject (o) ? o : {};
  }

  var defaultString = '--';
  function stringOrDefault (s) {
    return isEmptyString (s) ? defaultString : s;
  }

  var toJson = angular.toJson;
  var commaAndSpace = ', ';

  var module = angular.module('icgc.repository.controllers', [
    'icgc.repository.services',
    'file-finder',
    ]);

  var cloudRepos = ['AWS - Virginia', 'Collaboratory - Toronto', 'PDC - Chicago'];

  /**
   * ICGC static repository controller
   */
  module.controller('ICGCRepoController', function($scope, $stateParams, Restangular, FileService,
    ProjectCache, API, Settings, Page, RouteInfoService) {
    var _ctrl = this;
    var dataReleasesRouteInfo = RouteInfoService.get ('dataReleases');

    Page.setTitle (dataReleasesRouteInfo.title);
    Page.setPage ('dataReleases');
    // Prevent double encoding issues when reloading state on login
    _ctrl.path = $stateParams.path ? decodeURIComponent($stateParams.path) : '';
    _ctrl.slugs = [];
    _ctrl.API = API;
    _ctrl.deprecatedReleases = ['release_15'];
    _ctrl.downloadEnabled = true;
    _ctrl.dataReleasesTitle = dataReleasesRouteInfo.title;
    _ctrl.dataReleasesUrl = dataReleasesRouteInfo.href;

    _ctrl.fileQuery = '';
    _ctrl.handleFileQueryKeyup = ($event) => {
      if (event.keyCode === 27) {
        _ctrl.fileQuery = '';
        $event.currentTarget.blur();
      }
    };

    function buildBreadcrumbs() {
      var i, s, slug, url;

      url = '';
      s = _ctrl.path.split('/').filter(Boolean); // removes empty cells

      for (i = 0; i < s.length; ++i) {
        slug = s[i];
        url += slug + '/';
        _ctrl.slugs.push({name: slug, url: url});
      }
    }

    /**
     * Additional information for rendering
     */
    function annotate(file) {
      var name, tName, extension;

      // For convienence
      file.baseName = file.name.split('/').pop();

      // Check if there is a translation code for directories (projects)
      if (file.type === 'd') {
        name = (file.name).split('/').pop();

        ProjectCache.getData().then(function(cache) {
          tName = cache[name];
          if (tName) {
            file.translation = tName;
          }
        });
      }

      // Check file extension
      extension = file.name.split('.').pop();
      if (_.contains(['txt', 'md'], extension.toLowerCase())) {
        file.isText = true;
      } else {
        file.isText = false;
      }
    }

    function getFiles() {
      FileService.folder(_ctrl.path).then(function (response) {
        var files = response;

        files.forEach(annotate);

        _ctrl.files = FileService.sortFiles(files, _ctrl.slugs.length);


        // Grab text file (markdown)
        _ctrl.textFiles = _.filter(files, function(f) {
          return f.type === 'f' && f.isText === true;
        });
        _ctrl.textFiles.forEach(function(f) {
          Restangular.one('download').get( {'fn':f.name}).then(function(data) {
            f.textContent = data;
          }).then(function(){

            // Workaround for links in README file on Releases page

            angular.element('.markdown-container').delegate('a', 'click', function(){
              var _elem = jQuery(this),
                _href = _elem.attr('href');
              
              if(_href.indexOf('@') !== -1){
                window.location.href = 'mailto:' + _href;
                return false;
              }
              else if(_href.indexOf('http') === -1) {
                window.location.href = 'http://' + _href;
                return false;
              }
            });
          });
        });

      },function (error) {
        if(error.status === 503){
          _ctrl.downloadEnabled = false;
        }
      });
    }

    // Check if download is disabled or not
    function refresh() {
      Settings.get().then(function(settings) {
        if (settings.downloadEnabled && settings.downloadEnabled === true) {
          buildBreadcrumbs();
          getFiles();
          _ctrl.downloadEnabled = true;
        } else {
          _ctrl.downloadEnabled = false;
        }
      });
    }

    // Initialize
    $scope.$watch (function() {
      return $stateParams.path;
    }, function() {
      // Prevent double encoding issues when reloading state on login
      _ctrl.path = $stateParams.path ? decodeURIComponent($stateParams.path) : '';
      _ctrl.slugs = [];
      refresh();
    });

  });

  module.controller('ExternalIobioController',
    function($scope, $document, $modalInstance, params) {

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.$on('bamready.event', function() {
      $scope.bamId = params.fileObjectId;
      $scope.bamName = params.fileObjectName;
      $scope.bamFileName = params.fileName;
      $scope.$apply();
    });

  });

  module.controller('ExternalVcfIobioController',
    function($scope, $document, $modalInstance, params) {

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.$on('bamready.event', function() {
      $scope.vcfId = params.fileObjectId;
      $scope.vcfName = params.fileObjectName;
      $scope.vcfFileName = params.fileName;
      $scope.$apply();
    });

  });

  module.controller('ExternalFileIcgcGetController', function (
    $scope,
    ExternalRepoService,
    params,
    FilterService,
    LoadState,
    $modalInstance
  ) {
    var vm = this;
    var loadState = new LoadState();

    var filters = _.extend({},
      FilterService.filters(),
      !_.isEmpty(params.selectedFiles) ? {
        file: {
          id: {
            is: params.selectedFiles
          }
        }
      } : {}
    );

    var requestManifestId = ExternalRepoService.getRelevantRepos(filters)
      .then(function (repos) {
        return ExternalRepoService.createManifest({
          filters: filters,
          repos: repos,
          format: 'json'
        });
      })
      .then(function (manifestId) {
        vm.manifestId = manifestId;
      });

    loadState.loadWhile(requestManifestId);

    _.extend(this, {
      manifestId: undefined,
      loadState: loadState,
      close: _.partial($modalInstance.dismiss, 'cancel'),
    });
  });

  module.controller('ExternalFileDownloadController',
    function ($scope, $location, $window, $document, $modalInstance, ExternalRepoService, SetService, FilterService,
      Extensions, params, Restangular, $filter, RepositoryService) {

    $scope.selectedFiles = params.selectedFiles;
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.filters = FilterService.filters;
    $scope.$filter = $filter;
    $scope.shouldDeduplicate = false;
    $scope.summary = {};

    $scope.getRepoFieldValue = function (repoName, fieldName) {
      var repoData = $scope.shouldDeduplicate ? $scope.summary[repoName] : _.findWhere($scope.repos, { repoName: repoName });
      return repoData && repoData[fieldName];
    };

    $scope.handleNumberTweenStart = function (tween) {
      jQuery(tween.elem).closest('td').addClass('tweening');
    };

    $scope.handleNumberTweenEnd = function (tween) {
      jQuery(tween.elem).closest('td').removeClass('tweening');
    };

    var p = {};
    p.size = 0;
    p.filters = FilterService.filters();
    if ($scope.selectedFiles && !_.isEmpty($scope.selectedFiles)) {
      if (! p.filters.file) {
        p.filters.file = {};
      }
      p.filters.file.id = {is: $scope.selectedFiles};
    }
    p.include = 'facets';

    function findRepoData(list, term) {
      return _.find(list, function(t) { return t.term === term }).count || 0;
    }

    Promise.all([
      ExternalRepoService.getList(p),
      RepositoryService.getRepos(),
    ]).then(function ([data, reposFromService]) {
      var facets = data.termFacets;
      var activeRepos = [];

      if (p.filters.file && p.filters.file.repoName) {
        activeRepos = p.filters.file.repoName.is;
      }

      // Build the repo table
      var repos = {};
      facets.repoName.terms.forEach(function(term) {
        var repoName = term.term;
        const repo = _.find(reposFromService, {name: repoName});

        // Restrict to active repos if it is available
        if (!_.isEmpty(activeRepos) && !_.contains(activeRepos, repoName)) {
          return;
        }

        if (! repos[repoName]) {
          repos[repoName] = {};
        }

        repos[repoName].repoName = repoName;
        repos[repoName].repoCode = repo.code;
        repos[repoName].fileSize = findRepoData(facets.repositorySizes.terms, repoName);
        repos[repoName].donorCount = findRepoData(facets.repositoryDonors.terms, repoName);
        repos[repoName].fileCount = term.count;
        repos[repoName].hasManifest = RepositoryService.isCloudRepo(repo);
        repos[repoName].isCloud = RepositoryService.isCloudRepo(repo);
      });

      $scope.repos = _(repos).values().sortBy('fileSize').value().reverse();
      $scope.selectedRepos = Object.keys(repos);

      var manifestSummaryQuery = {
        query: p,
        repoNames: _.map($scope.repos, 'repoName')
      };

      return ExternalRepoService.getManifestSummary(manifestSummaryQuery).then(
        function (summary) {
          $scope.summary = summary;
        });
    });

    $scope.movedCallback = function(index) {
      $scope.repos.splice(index, 1);
      var manifestSummaryQuery = {
        query: p,
        repoNames: _.map($scope.repos, 'repoName')
      };
      return ExternalRepoService.getManifestSummary(manifestSummaryQuery).then(
        function (summary) {
          $scope.summary = summary;
        });
    };

    $scope.getRepoManifestUrl = ExternalRepoService.getRepoManifestUrl;

    $scope.getRepoManifestShortUrl = function (repoData) {
      var longUrl = $scope.getRepoManifestUrl({
        repoCodes: [repoData.repoCode],
        filters: FilterService.filters()
      });

      repoData.isGeneratingManifestShortUrl = true;

      return Restangular.one('short', '').get({ url: longUrl, shouldUseParamsOnlyForRequest: true })
        .then(function (response) {
          repoData.isGeneratingManifestShortUrl = false;
          repoData.shortUrl = response.plain().shortUrl;
        });
    };

    $scope.closeDropdowns = function () {
      jQuery('.btn-group.open').trigger('click');
    };

    $scope.download = function() {
      if (_.isEmpty($scope.selectedFiles)) {
        var filters = FilterService.filters();

        var manifestUrl = $scope.getRepoManifestUrl({
          repoCodes: _.map($scope.repos, 'repoCode'),
          filters: filters,
          format: 'tarball',
          unique: $scope.shouldDeduplicate,
        });

        window.location.href = manifestUrl;
      } else {
        ExternalRepoService.downloadSelected(
                $scope.selectedFiles, 
        		$scope.shouldDeduplicate ? _.map($scope.repos, 'repoCode') : $scope.selectedRepos, 
        		$scope.shouldDeduplicate);
      }
      $scope.cancel();
    };

    $scope.createManifestId = function (repoName, repoData) {

      repoData.isGeneratingManifestID = true;
      repoData.manifestID = false;

      var selectedFiles = $scope.selectedFiles;
      var filters = FilterService.filters();

      if (! _.isEmpty (selectedFiles)) {
        filters = _.set (filters, 'file.id.is', selectedFiles);
      }

      filters = _.set (filters, 'file.repoName.is', [repoName]);

      // TODO: Externalize the mapping from repo codes to names
      var params = {
        format: 'files',
        repos: [repoName === 'AWS - Virginia' ? 'aws-virginia' : 'collaboratory'],
        filters: filters
      };

      ExternalRepoService.createManifest(params).then(function (id) {
        if (! id) {
          throw new Error('No Manifest UUID is returned from API call.');
        }
        repoData.isGeneratingManifestID = false;
        repoData.manifestID = id;
     });
    };

  });

  /**
   * Controller for File Entity page
   */
  module.controller('ExternalFileInfoController',
    function (Page, ExternalRepoService, CodeTable, ProjectCache, PCAWG, fileInfo, PortalFeature, SetService,
      gettextCatalog) {

    Page.setTitle(gettextCatalog.getString('Repository File'));
    Page.setPage('externalFileEntity');

    var slash = '/';
    var projectMap = {};

    function refresh () {
      ProjectCache.getData().then (function (cache) {
        projectMap = ensureObject (cache);
      });
    }
    refresh();

    this.fileInfo = fileInfo;
    this.uiDonorInfo = getUiDonorInfoJSON(fileInfo.donors);
    this.stringOrDefault = stringOrDefault;
    this.isEmptyString = isEmptyString;
    this.defaultString = defaultString;
    
    // Defaults for client side pagination 
    this.currentDonorsPage = 1;
    this.defaultDonorsRowLimit = 10;
    this.rowSizes = [10, 25, 50];

    function convertToString (input) {
      return _.isString (input) ? input : (input || '').toString();
    }

    function toUppercaseString (input) {
      return convertToString (input).toUpperCase();
    }

    function removeBookendingSlash (input) {
      var inputString = convertToString (input);

      if (inputString.length < 1) {
        return input;
      }

      return inputString.replace (/^\/+|\/+$/g, '');
    }

    function equalsIgnoringCase (test, expected) {
      return toUppercaseString (test) === toUppercaseString (expected);
    }

    function isS3 (repoType) {
      return equalsIgnoringCase (repoType, 'S3');
    }

    function isGDC (repoType) {
      return equalsIgnoringCase (repoType, 'GDC');
    }

    function isPDC (repoType) {
      return equalsIgnoringCase (repoType, 'PDC');
    }

    function isGnos (repoType) {
      return equalsIgnoringCase (repoType, 'GNOS');
    }

    function isEGA (repoType) {
      return equalsIgnoringCase (repoType, 'EGA');
    }

    function isCollab (repoCode) {
      return equalsIgnoringCase (repoCode, 'collaboratory');
    }

    /**
     * View single file with many donors in Advanced Search
     */
    this.viewFileInSearch = function () {
      SetService.createSetFromSingleFile(fileInfo.id, fileInfo.donors.length);
    };

    // Public functions
    function projectName (projectCode) {
      return _.get (projectMap, projectCode, '');
    }

    this.buildUrl = function (baseUrl, dataPath, entityId) {
      // Removes any opening and closing slash in all parts then concatenates.
      return _.map ([baseUrl, dataPath, entityId], removeBookendingSlash)
        .join (slash);
    };

    this.buildMetaDataUrl = function (fileCopy, fileInfo) {
      var parts = [];
      var metaId;
      if (isS3 (fileCopy.repoType) && isCollab(fileCopy.repoCode)) {
        metaId = fileCopy.repoMetadataPath.substr(fileCopy.repoMetadataPath.lastIndexOf('/')+1);
        parts = ['api/v1/ui/collaboratory/metadata/', metaId];
      } else if (isS3 (fileCopy.repoType) && !isCollab(fileCopy.repoCode)) {
        metaId = fileCopy.repoMetadataPath.substr(fileCopy.repoMetadataPath.lastIndexOf('/')+1);
        parts = ['api/v1/ui/aws/metadata/', metaId];
      } else if (isEGA (fileCopy.repoType)) {
        parts = ['api/v1/ui/ega/metadata/', fileCopy.repoDataSetIds[0]];
      } else if (isGDC (fileCopy.repoType)) {
        // See https://wiki.oicr.on.ca/pages/viewpage.action?pageId=66946440
        var expands = [
          'analysis',
          'annotations',
          'cases',
          'cases.samples',
          'cases.samples.annotations',
          'cases.samples.portions',
          'cases.samples.portions.analytes',
          'cases.samples.portions.analytes.aliquots',
          'cases.samples.portions.analytes.aliquots.annotations',
          'cases.samples.portions.analytes.annotations',
          'cases.samples.portions.slides',
          'cases.samples.portions.slides.annotations',
          'cases.samples.portions.annotations',
          'cases.annotations',
          'cases.files',
          'cases.summary.experimental_strategies',
          'associated_entities'
        ];

        parts = [fileCopy.repoBaseUrl, fileCopy.repoMetadataPath, fileCopy.repoFileId, '?expand=' + expands.join(',')];
      } else if (isPDC (fileCopy.repoType)) {
        parts = ['https://griffin-objstore.opensciencedatacloud.org', fileCopy.repoMetadataPath];
      } else {
        parts = [fileCopy.repoBaseUrl, fileCopy.repoMetadataPath, fileInfo.dataBundle.dataBundleId];
      }

      return _.map (parts, removeBookendingSlash)
        .join (slash);
    };

    this.buildManifestUrl = function (fileId, repos) {
       return ExternalRepoService.getManifestUrlByFileIds(fileId, repos);
    };

    this.equalsIgnoringCase = equalsIgnoringCase;

    this.downloadManifest = function (fileId, repo) {
      ExternalRepoService.downloadSelected ([fileId], [repo], true);
    };

    function noNullConcat (values) {
      var flattened = _.flatten(values);
      var result = isEmptyArray (flattened) ? '' : _.filter (flattened, _.negate (isEmptyString)).join (commaAndSpace);
      return stringOrDefault (result);
    }

    this.shouldShowMetaData = function (repoType) {
      /* JJ: Quality is too low: || isEGA (repoType) */
      return isGnos (repoType) || isS3 (repoType) || isGDC (repoType) || isPDC(repoType);
    };

    this.isS3 = isS3;
    this.isEGA = isEGA;

    this.translateDataType = function (dataType) {
      var longName = PCAWG.translate (dataType);

      return (longName === dataType) ? dataType : longName + ' (' + dataType + ')';
    };

    this.translateCountryCode = CodeTable.translateCountryCode;
    this.countryName = CodeTable.countryName;

    this.awsOrCollab = function(fileCopies) {
       return _.includes(_.pluck(fileCopies, 'repoCode'), 'aws-virginia') ||
         _.includes(_.pluck(fileCopies, 'repoCode'), 'collaboratory');
    };

    function getUiDonorInfoJSON(donors){
      return donors.map(function(donor){
        return _.extend({}, {
          uiProjectCode: donor.projectCode,
          uiStringProjectCode: stringOrDefault(donor.projectCode),
          uiProjectName: projectName(donor.projectCode),
          uiPrimarySite: stringOrDefault(donor.primarySite),
          uiStudy: donor.study,
          uiDonorId: donor.donorId,
          uiStringDonorId: stringOrDefault(donor.donorId),
          uiSubmitterId: noNullConcat([donor.otherIdentifiers.tcgaParticipantBarcode, donor.submittedDonorId]),
          uiSpecimentId: noNullConcat(donor.specimenId),
          uiSpecimentSubmitterId: noNullConcat([donor.otherIdentifiers.tcgaSampleBarcode, donor.submittedSpecimenId]),
          uiSpecimenType: noNullConcat(donor.specimenType),
          uiSampleId: noNullConcat(donor.sampleId),
          uiSampleSubmitterId: noNullConcat([donor.otherIdentifiers.tcgaAliquotBarcode, donor.submittedSampleId]),
          uiMatchedSampleId: stringOrDefault(donor.matchedControlSampleId)
        });
      });
    }

  });

  /**
   * External repository controller
   */
  module.controller ('ExternalRepoController', function ($scope, $window, $modal, LocationService, Page,
    ExternalRepoService, SetService, ProjectCache, CodeTable, RouteInfoService, $rootScope, PortalFeature,
    FacetConstants, Facets, LoadState) {

    var dataRepoTitle = RouteInfoService.get ('dataRepositories').title,
        FilterService = LocationService.getFilterService();

    Page.setTitle (dataRepoTitle);
    Page.setPage ('repository');

    var tabNames = {
      files: 'Files',
      donors: 'Donors'
    };
    var currentTabName = tabNames.files;
    var projectMap = {};
    var _ctrl = this;

    this.handleOperationSuccess = () => { this.selectedFiles = [] };

    _ctrl.showIcgcGet = PortalFeature.get('ICGC_GET');
    _ctrl.selectedFiles = [];
    _ctrl.summary = {};
    _ctrl.facetCharts = {};
    _ctrl.dataRepoTitle = dataRepoTitle;
    _ctrl.dataRepoFileUrl = RouteInfoService.get ('dataRepositoryFile').href;
    _ctrl.advancedSearchInfo = RouteInfoService.get ('advancedSearch');
    _ctrl.repoChartConfigOverrides = {
      chart: {
          type: 'column',
          marginTop: 20,
          marginBottom: 20,
          backgroundColor: 'transparent',
          spacingTop: 1,
          spacingRight: 20,
          spacingBottom: 20,
          spacingLeft: 10
      },
      xAxis: {
        labels: {
          rotation: 0,
          align: 'left',
          x: -5,
          y: 12,
          formatter: function () {
            var isCloudRepo = _.includes(cloudRepos, this.value);
            return isCloudRepo ? '\ue844' : '';
          }
        },
        gridLineColor: 'transparent',
        minorGridLineWidth: 0
      },
      yAxis: {
        gridLineColor: 'transparent',
        endOnTick: false,
        maxPadding: 0.01,
        labels: {
          formatter: function () {
            return this.value / 1000 + 'k';
          }
        },
        lineWidth: 1,
        title: {
          align: 'high',
          offset: 0,
          margin: -20,
          y: -10,
          rotation: 0,
          text: '# of Files'
        }
      },
      plotOptions: {
        series: {
          minPointLength: 2,
          pointPadding: 0,
          maxPointWidth: 100,
          borderRadiusTopLeft: 2,
          borderRadiusTopRight: 2,
          cursor: 'pointer',
          stickyTracking: false,
          point: {
            events: {
              click: function () {
                Facets.toggleTerm({
                  type: 'file',
                  facet: 'repoName',
                  term: this.category
                });
                $scope.$apply();
              },
              mouseOut: $scope.$emit.bind($scope, 'tooltip::hide')
            }
          }
        }
      }
    };

    _ctrl.donorSetsForRepo = () => 
      _.map(_.cloneDeep(SetService.getAllDonorSets()), (set) => {
        set.repoFilters = {};
        set.repoFilters.file = {};
        set.repoFilters.file.donorId = set.advFilters.donor.id;
        return set;
      });

    // Adding filters for repository to the donor set
    _ctrl.donorSets = _ctrl.donorSetsForRepo();

    _ctrl.fileSets = _.cloneDeep(SetService.getAllFileSets());

    function toSummarizedString (values, name) {
      var size = _.size (values);
      return (size > 1) ? '' + size + ' ' + name + 's' :
        _.first (values);
    }

    function createFilter (category, ids) {
      return encodeURIComponent (toJson (_.set ({}, '' + category + '.id.is', ids)));
    }

    function buildDataInfo (data, property, paths, category, toolTip) {
      var ids = _(ensureArray (data))
        .map (property)
        .unique()
        .value();

      return isEmptyArray (ids) ? {} : {
        text: toSummarizedString (ids, category),
        tooltip: toolTip (ids),
        href: _.size (ids) > 1 ?
          paths.many + createFilter (category, ids) :
          paths.one + _.first (ids)
      };
    }

    _ctrl.setTabToFiles = function() {
      currentTabName = tabNames.files;
    };
    _ctrl.setTabToDonors = function() {
      currentTabName = tabNames.donors;
    };

    _ctrl.isOnFilesTab = function() {
      return currentTabName === tabNames.files;
    };
    _ctrl.isOnDonorsTab = function() {
      return currentTabName === tabNames.donors;
    };

    _ctrl.donorInfo = function (donors) {
      var toolTipMaker = function () {
        return '';
      };
      return buildDataInfo (donors, 'donorId', {one: '/donors/', many: '/search?filters='},
        'donor', toolTipMaker);
    };

    _ctrl.buildProjectInfo = function (donors) {
      var toolTipMaker = function (ids) {
        return _.size (ids) === 1 ? _.get (projectMap, _.first (ids), '') : '';
      };
      return buildDataInfo (donors, 'projectCode', {one: '/projects/', many: '/projects?filters='},
        'project', toolTipMaker);
    };

    function uniquelyConcat (fileCopies, property) {
      return _(fileCopies)
        .map (property)
        .unique()
        .join(commaAndSpace);
    }

    /**
     * Tablular display
     */

    _ctrl.fileFormats = function (fileCopies) {
      return uniquelyConcat (fileCopies, 'fileFormat');
    };

    function tooltipList (objects, property, oneItemHandler) {
      var uniqueItems = _(objects)
        .map (property)
        .unique();

      if (uniqueItems.size() < 2) {
        return _.isFunction (oneItemHandler) ? oneItemHandler() :
          '' + oneItemHandler;
      }
      return uniqueItems.map (function (s) {
          return '<li>' + s;
        })
        .join ('</li>');
    }

    _ctrl.fileNames = function (fileCopies) {
      return tooltipList (fileCopies, 'fileName', function () {
          return _.get (fileCopies, '[0].fileName', '');
        });
    };

    _ctrl.repoNamesInTooltip = function (fileCopies) {
      return tooltipList (fileCopies, 'repo.name', '');
    };

    _ctrl.awsOrCollab = function(fileCopies) {
       return _.includes(_.pluck(fileCopies, 'repoCode'), 'aws-virginia') ||
         _.includes(_.pluck(fileCopies, 'repoCode'), 'collaboratory');
    };

    _ctrl.fileAverageSize = function (fileCopies) {
      var count = _.size (fileCopies);
      return (count > 0) ? _.sum (fileCopies, 'fileSize') / count : 0;
    };

    _ctrl.flagIconClass = function (projectCode) {
      var defaultValue = '';
      var last3 = _.takeRight (ensureString (projectCode), 3);

      if (_.size (last3) < 3 || _.first (last3) !== '-') {
        return defaultValue;
      }

      var last2 = _.rest (last3).join ('');

      return 'flag flag-' + CodeTable.translateCountryCode (last2.toLowerCase());
    };

    _ctrl.repoIconClass = function (repoName) {
      return _.includes(cloudRepos, repoName) ? 'icon-cloud' : '';
    };

    /**
     * Export table
     */
    _ctrl.export = function() {
      ExternalRepoService.export (FilterService.filters());
    };

    /**
     * View in Advanced Search
     */
    _ctrl.viewInSearch = function (limit) {
      var params = {};
      params.filters = FilterService.filters();
      params.size = limit;
      params.isTransient = true;
      SetService.createForwardRepositorySet ('donor', params, '/search');
    };

    /**
     * View single file with many donors in Advanced Search
     */
    _ctrl.viewFileInSearch = function (fileId, limit) {
      SetService.createSetFromSingleFile(fileId, limit);
    };

    /**
     * Save a donor set from files
     */
    _ctrl.saveDonorSet = function (type, limit) {
      _ctrl.setLimit = limit;
      _ctrl.setType = type;

      $modal.open ({
        templateUrl: '/scripts/sets/views/sets.upload.external.html',
        controller: 'SetUploadController',
        resolve: {
          setType: function() {
            return _ctrl.setType;
          },
          setLimit: function() {
            return _ctrl.setLimit;
          },
          setUnion: function() {
            return undefined;
          },
          selectedIds: function() {
            return undefined;
          }
        }
      });
    };

    /**
     * Save a file set from files
     */
    _ctrl.saveFileSet = function (type, limit) {
      _ctrl.setLimit = limit;
      _ctrl.setType = type;

      $modal.open ({
        templateUrl: '/scripts/sets/views/sets.upload.external.html',
        controller: 'SetUploadController',
        resolve: {
          setType: function() {
            return _ctrl.setType;
          },
          setLimit: function() {
            return _ctrl.setLimit;
          },
          setUnion: function() {
            return undefined;
          },
          selectedIds: function() {
            return _ctrl.selectedFiles;
          }
        }
      });
    };

    /**
     * Download manifest
     */
    _ctrl.downloadManifest = function() {
      $modal.open ({
        templateUrl: '/scripts/repository/views/repository.external.submit.html',
        controller: 'ExternalFileDownloadController',
        size: 'lg',
        resolve: {
          params: function() {
            return {
              selectedFiles: _ctrl.selectedFiles
            };
          }
        }
      });
    };

    _ctrl.showIcgcGetModal = function() {
      $modal.open ({
        templateUrl: '/scripts/repository/views/repository.external.icgc-get.html',
        controller: 'ExternalFileIcgcGetController as vm',
        size: 'lg',
        resolve: {
          params: function() {
            return {
              selectedFiles: _ctrl.selectedFiles
            };
          }
        }
      });
    };
    
    _ctrl.isSelected = (row) => _ctrl.selectedFiles.includes(row.id);

    _ctrl.toggleRow = (row) => { _ctrl.selectedFiles = _.xor(_ctrl.selectedFiles, [row.id]) };

    /**
     * Undo user selected files
     */
    _ctrl.undo = function() {
      _ctrl.selectedFiles = [];

      _ctrl.files.hits.forEach (function (f) {
        delete f.checked;
      });
    };

    function removeCityFromRepoName (repoName) {
      if (_.contains (repoName, 'CGHub')) {
        return 'CGHub';
      }

      if (_.contains (repoName, 'TCGA DCC')) {
        return 'TCGA DCC';
      }

      return repoName;
    }

    function fixRepoNameInTableData (data) {
      _.forEach (data, function (row) {
        _.forEach (row.fileCopies, function (fileCopy) {
          fileCopy.repoName = removeCityFromRepoName (fileCopy.repoName);
        });
      });
    }

    function processRepoData (data) {
      var filteredRepoNames = _.get(LocationService.filters(), 'file.repoName.is', []);
      var selectedColor = [253, 179, 97 ];
      var unselectedColor = [22, 147, 192];
      var minAlpha = 0.3;

      var transformedItems = data.s.map(function (item, i, array) {
        var isSelected = _.includes(filteredRepoNames, data.x[i]);
        var baseColor = isSelected ? selectedColor : unselectedColor;
        var alpha = array.length ?
          1 - (1 - minAlpha) / array.length * i :
          0;
        var rgba = 'rgba(' + baseColor.concat(alpha).join(',') + ')';
        return _.extend({}, item, {
          color: rgba,
          fillOpacity: 0.5
        });
      });

      return _.extend({}, data, {
        s: transformedItems
      });
    }

    var loadState = new LoadState();
    _ctrl.loadState = loadState;

    function refresh() {
      var params = {};
      var filesParam = LocationService.getJqlParam ('files');

      // Default
      params.size = 25;

      if (filesParam.from || filesParam.size) {
        params.from = filesParam.from;
        params.size = filesParam.size || 25;
      }

      if (filesParam.sort) {
        params.sort = filesParam.sort;
        params.order = filesParam.order;
      }

      params.include = 'facets';
      params.filters = FilterService.filters();

      // Get files that match query
      var listRequest = ExternalRepoService.getList (params).then (function (data) {
        // Vincent asked to remove city names from repository names for CGHub and TCGA DCC.
        fixRepoNameInTableData (data.hits);
        _ctrl.files = data;

        _ctrl.facetCharts = ExternalRepoService.createFacetCharts(data.termFacets);
        _ctrl.facetCharts.repositories = processRepoData(_ctrl.facetCharts.repositories);
        // Sanity check, just reset everything
        _ctrl.undo();
      });

      // Get summary
      var summaryRequest = ExternalRepoService.getSummary (params).then (function (summary) {
        _ctrl.summary = summary;
      });

      // Get index creation time
      var metaDataRequeset = ExternalRepoService.getMetaData().then (function (metadata) {
        _ctrl.repoCreationTime = metadata.creation_date || '';
      });

      var cacheReqeust = ProjectCache.getData().then (function (cache) {
        projectMap = ensureObject (cache);
      });

      loadState.loadWhile([listRequest, summaryRequest, metaDataRequeset, cacheReqeust]);
    }

    // to check if a set was previously selected and if its still in effect
    const updateSetSelection = (entity, entitySets) => {
      let filters = FilterService.filters();

      entitySets.forEach( (set) =>
        set.selected = filters.file && filters.file[entity] &&  _.includes(filters.file[entity].is, `ES:${set.id}`)
      );
    };

    refresh();
    updateSetSelection('donorId', _ctrl.donorSets);
    updateSetSelection('id', _ctrl.fileSets);

    // Pagination watcher, gets destroyed with scope.
    $scope.$watch(function() {
        return JSON.stringify(LocationService.search('files'));
      },
      function(newVal, oldVal) {
        if (newVal !== oldVal) {
          refresh();
      }
    });

    $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, function(e, filterObj) {
      var filters = filterObj.currentFilters,
          hasFilters = ! _.isEmpty(filters);

      if (hasFilters && _ctrl.selectedFiles.length > 0) {
        _ctrl.undo();
      }
      else {
        refresh();
      }
      updateSetSelection('donorId', _ctrl.donorSets);
      updateSetSelection('id', _ctrl.fileSets);
    });

    // Remove any pagination on facet change: see DCC-4589
    $scope.$on(FacetConstants.EVENTS.FACET_STATUS_CHANGE, function() {
      var filesParam = LocationService.getJqlParam('files');
      if (!_.isEmpty(filesParam)) {
        var newParam = {
          from: 1,
          size: filesParam.size || 25
          };
        LocationService.setJsonParam('files', newParam);
      }
    });

    $rootScope.$on(SetService.setServiceConstants.SET_EVENTS.SET_ADD_EVENT, () => {
      _ctrl.donorSets = _ctrl.donorSetsForRepo();
      _ctrl.fileSets = _.cloneDeep(SetService.getAllFileSets());
    });

  });

})();
