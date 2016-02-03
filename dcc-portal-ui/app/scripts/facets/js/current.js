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

  var module = angular.module ('icgc.facets.current', []);

  module.controller('currentCtrl',
    function ($scope, Facets, LocationService, FilterService, FiltersUtil, Extensions, SetService, Page, GeneSymbols) {

    $scope.Page = Page;
    $scope.Facets = Facets;
    $scope.Extensions = Extensions;

    var donorSetFiltersInRepositoryFile = [{
      term: 'Uploaded donor set',
      controlTerm: 'Uploaded donor set',
      controlFacet: 'donorId',
      controlType: 'file'
    }, {
      term: 'Input donor set',
      controlTerm: 'Input donor set',
      controlFacet: 'donorId',
      controlType: 'file'
    }];

    $scope.prepositionBuilder = function (typeName, facet, terms) {
      if ($scope.isNot({type:typeName, facet:facet})) {
        if ($scope.inPluralForm (terms)) {
          return 'NOT IN (';
        } else {
          return 'IS NOT';
        }
      } else {
        if ($scope.inPluralForm (terms)) { 
          return 'IN ('; 
        } else { 
          return 'IS';
        } 
      }
    };

    /*
     * This function determines the opening or closing for a human-readable JQL expression,
     * displayed in UI (usually a top panel above a data table).
     * The main conditions is the number of items in the 'terms' variable,
     * with additional logic for special cases.
     */
    $scope.inPluralForm = function (terms) {
      var filters = _.get (terms, 'is', _.get (terms, 'not', []));

      if (_.isEmpty (filters)) {return false;}
      if (_.size (filters) > 1) {return true;}

      var filter = _.first (filters);

      /*
       * This handles a special case for 'Uploaded Donor Set' in External Repository.
       * In this scenario, we cannot compare the value of 'controlFacet' to Extensions.ENTITY,
       * due to the transformation applied in adjustExternalFileFilters() of
       * FiltersUtil service (/scripts/common/display.js). See comments in
       * adjustExternalFileFilters() for more details.
       * The strict comparison to elements in donorSetFiltersInRepositoryFile is for extra
       * caution only, ensuring we only apply this when we're in External Repository page.
       */
      if (_.some (donorSetFiltersInRepositoryFile, function (o) {return _.isEqual (filter, o);})) {
        return true;
      }

      return _.contains (_.get (filter, 'controlFacet', ''), Extensions.ENTITY);
    };
    
    $scope.isNot = function(terms) {
      var currentFilters = LocationService.filters();
      if (terms.facet === 'id') {
        return _.has(currentFilters, terms.type+'.'+terms.facet+'.not') || 
          _.has(currentFilters, terms.type+'.entitySetId.not');
      } else if (terms.type === 'go_term') {
        return _.has(currentFilters, ['gene',terms.facet,'not']);
      } else {
        return _.has(currentFilters, terms.type+'.'+terms.facet+'.not');
      }
    };
    
    $scope.activeClass = function(terms) {     
      return $scope.isNot(terms) ? 't_facets__facet__not' : '';
    };

    function resolveActiveGeneIds (filters) {
      var activeGeneIds;
      if (_.has(filters, 'gene.id.not')) {
          activeGeneIds = _(_.get (filters, 'gene.id.not', []))
          .filter ({controlFacet: 'id', controlType: 'gene'})
          .map ('term')
          .value();
      } else if (_.has(filters, 'gene.id.is')) {
        activeGeneIds = _(_.get (filters, 'gene.id.is', []))
          .filter ({controlFacet: 'id', controlType: 'gene'})
          .map ('term')
          .value();
      }
      
      if (_.isEmpty (activeGeneIds)) {
        $scope.ensemblIdGeneSymbolMap = {};
        return;
      }

      GeneSymbols.resolve (activeGeneIds).then (function (ensemblIdGeneSymbolMap) {
        $scope.ensemblIdGeneSymbolMap = ensemblIdGeneSymbolMap.plain();
      });
    }

    $scope.resolveGeneSymbols = function (type, term) {
      if ('gene' !== type) {
        return term;
      }

      return _.get ($scope.ensemblIdGeneSymbolMap, term, term);
    };

    function refresh() {
      var currentFilters = FilterService.filters();
      var ids = LocationService.extractSetIds(currentFilters);

      if (ids.length > 0) {
        SetService.getMetaData(ids).then(function(results) {
          $scope.filters = FiltersUtil.buildUIFilters (currentFilters,
            SetService.lookupTable (results.plain()));
          resolveActiveGeneIds ($scope.filters);
        });
      } else {
        $scope.filters = FiltersUtil.buildUIFilters(currentFilters, {});
        resolveActiveGeneIds ($scope.filters);
      }

      // If we have filters then show the filter query directive
      $scope.isActive = ! _.isEmpty(currentFilters);
    }

    /**
     * Proxy to Facets service, it does addtional handling of fields that behaves like
     * like facets but are structured in different ways
     */
    $scope.removeFacet = function (type, facet) {
      // Remove primary facet
      Facets.removeFacet({
        type: type,
        facet: facet
      });

      // Remove secondary facet - entity
      if (_.contains(['gene', 'donor', 'mutation'], type) === true && facet === 'id') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      if ('file' === type && facet === 'donorId') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      // Remove secondary facet - existing conditions
      if (type === 'gene' && facet === 'pathwayId') {
        Facets.removeFacet({
          type: type,
          facet: 'hasPathway'
        });
      }

    };

    /**
     * Proxy to Facets service, it does addtional handling of fields that behaves like
     * like facets but are structured in different ways
     */
    $scope.removeTerm = function (type, facet, term) {
      if (type === 'gene' && facet === 'hasPathway') {
        Facets.removeFacet({
          type: type,
          facet: facet
        });
      } else {
        if ('file' === type && 'donorId' === facet &&
          _.endsWith ((term || '').toLowerCase(), ' donor set')) {
          facet = Extensions.ENTITY;
        }

        Facets.removeTerm({
          type: type,
          facet: facet,
          term: term
        });
      }

    };

    refresh();
    $scope.$on('$locationChangeSuccess', function (evt, next) {
      // FIXME: Only applicable on search page. Should have a cleaner solution
      if (next.indexOf('search') !== -1 || next.indexOf('projects') !== -1 || next.indexOf('repositories') ) {
        refresh();
      }
    });

    $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, refresh);

  });

  module.directive('current', function () {
    return {
      restrict: 'E',
      templateUrl: '/scripts/facets/views/current.html',
      controller: 'currentCtrl'
    };
  });

})();
