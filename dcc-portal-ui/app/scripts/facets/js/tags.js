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

import deepmerge from 'deepmerge';

(function () {
  'use strict';

  var module = angular.module('icgc.facets.tags', ['icgc.portalfeature', 'icgc.ui.suggest']);

  module.controller('tagsFacetCtrl',
    function ($scope, $modal, Facets, FilterService, LocationService, HighchartsService, FiltersUtil,
      Extensions, GeneSets, Genes, GeneSetNameLookupService, SetService, GeneSymbols, CompoundsService, Page) {

    $scope.Extensions = Extensions;
    $scope.isInRepositoryFile = Page.page() === 'repository';

    var _fetchNameForSelections = function ( selections ) {

      if ( selections ) {
        [ 'goTermId', 'pathwayId' ].forEach ( function (s) {
          if ( selections[s] ) {
            GeneSetNameLookupService.batchFetch ( selections[ s ].is );
          }
        });
      }
    };

    $scope.uploadEntityFn = () => {
      if($scope.type === 'donor' || $scope.type === 'file-donor'){
        return $scope.uploadDonorSet();
      }
      if($scope.type === 'gene'){
        return $scope.uploadGeneSet();
      }
      if($scope.type === 'mutation' || $scope.type === 'file'){
        return $scope.uploadEntitySet();
      }
    }

    // This function is called by tags.html to prevent the File input box in
    // External Repo File page from displaying the "Uploaded donor set" label.
    $scope.shouldDisplayEntitySetId = function () {
      if ($scope.type === 'mutation' && $scope.facetName === 'id') {
        return true;
      }
      if ($scope.type === 'file' && $scope.facetName === 'donorId') {
        return true;
      }
      return $scope.type === 'file' && $scope.facetName === 'id';
    };

    var isGeneType = $scope.isGeneType = function() {
      return $scope.type === 'gene';
    };

    function resolveActiveGeneIds() {
      var activeGeneIds = $scope.actives;

      if (_.isEmpty (activeGeneIds)) {
        $scope.activeGenes = [];
        return;
      }

      GeneSymbols.resolve (activeGeneIds).then (function (ensemblIdGeneSymbolMap) {
        var map = ensemblIdGeneSymbolMap.plain();

        $scope.activeGenes = _.map (activeGeneIds, function (id) {
          // This returns a custom Gene object that the ng-repeater will consume.
          return {
            uiText: _.get (map, id, id),
            tooltip: id,
            dataId: id
          };
        });
      });
    }

    /**
     * Sets the active class if this facet is 'not' 
     */
    function setActiveClass() {
      var params = {type: $scope.type,facet: $scope.facetName};
      $scope.isNot = Facets.isNot(params);
      $scope.activeClass = Facets.isNot(params) ? 
        't_facets__facet__not' : '';
    }
    
    function activeEntityHelper(type) {
      var fileDonor = [];
      var other = [];
      
      fileDonor = Facets.getActiveFromTags({
        type: 'file',
        facet:'donorId'
      });

      other = Facets.getActiveFromTags({
        type: type,
        facet: 'id'
      });

      return other.concat(fileDonor);
    }

    function resolveActiveCompoundNames(activeCompoundIds) {
      $scope.compoundIdToNameMap = {};
      _.forEach(activeCompoundIds, function (compoundId) {
        CompoundsService.getCompoundByZincId(compoundId).then(function(compound) {
          $scope.compoundIdToNameMap[compoundId] = compound.name;
        });
      });
    }

    function setup() {
      /*jshint maxcomplexity:false */
      var type = $scope.proxyType || $scope.type,
          facet = $scope.proxyFacetName || $scope.facetName,
          filters = FilterService.filters(),
          activeIds = [];

      _fetchNameForSelections ( filters.gene );


      $scope.actives = Facets.getActiveFromTags({
        type: type,
        facet: facet
      });

      setActiveClass();

      // There are only 'active' entity ids
      $scope.activeEntityIds = activeEntityHelper(type);
      
      // Fetch display names for entity lists
      $scope.entityIdMap = {};

      // Find any entity set ids among the entity ids in order to query for their names
      var setIds = _($scope.activeEntityIds)
        .filter(function(id) { return id.indexOf(Extensions.ENTITY_PREFIX) === 0})
        .map(function(id) { return id.replace('ES:', '')})
        .value();

      if (setIds.length > 0) {
        SetService.getMetaData (setIds).then (function (results) {
          $scope.entityIdMap = SetService.lookupTable (results);
        });
      }

      // Check if we are in the context of advanced search. 
      var asContext = LocationService.path().indexOf('/search') >= 0;
      
      // Grab predefined geneset fields: each gene set type require specialized logic
      //   go has predefined Ids, searchable Ids, and Id counts
      //   pathway has predefined type, searchableIds, Id counts and type counts
      //   curated_set has predefined Ids and Id counts
      if ($scope.type === 'go_term' && asContext) {
        $scope.predefinedGO = _.filter(Extensions.GENE_SET_ROOTS, function(set) {
          return set.type === 'go_term';
        });
        $scope.predefinedGOIds = _.pluck($scope.predefinedGO, 'id');

        activeIds = $scope.actives.concat($scope.predefinedGOIds);

        GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
          $scope.GOIdCounts = result;
        });
      } else if ($scope.type === 'pathway' && asContext) {
        var pathwayTypeFilters = {};

        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('hasPathway')) {
          $scope.hasPathwayTypePredicate = true;
        } else {
          $scope.hasPathwayTypePredicate = false;
        }

        activeIds = $scope.actives;
        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('pathwayId')) {
          pathwayTypeFilters = FilterService.filters();
        } else {
          pathwayTypeFilters = FilterService.mergeIntoFilters({'gene':{'hasPathway':true}});
        }

        Genes.handler.one('count').get({filters:pathwayTypeFilters}).then(function (result) {
          $scope.allPathwayCounts = result || 0;
        });
        if (activeIds && activeIds.length > 0) {
          GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
            $scope.pathwayIdCounts = result;
          });
        }
      } else if ($scope.type === 'compound' && asContext) {
        activeIds = $scope.actives;
        resolveActiveCompoundNames(activeIds);
        
        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('hasCompound')) {
          $scope.hasCompoundTypePredicate = true;
        } else {
          $scope.hasCompoundTypePredicate = false;
        }

        var compoundTypeFilters = {};
        if (filters.hasOwnProperty('gene') && filters.gene.hasOwnProperty('compoundId')) {
          compoundTypeFilters = FilterService.filters();
        } else {
          compoundTypeFilters = FilterService.mergeIntoFilters({'gene':{'hasCompound':true}});
        }
        Genes.handler.one('count').get({filters:compoundTypeFilters}).then(function (result) {
          $scope.allCompoundCounts = result || 0;
        });

        if (activeIds && activeIds.length > 0) {
          $scope.compoundIdCounts = {};
          _.forEach(activeIds, function(activeId) {
            var activeIdFilter = FilterService.filters();
            delete activeIdFilter.gene.hasCompound;
            delete activeIdFilter.gene.compoundId;
            _.merge(activeIdFilter, {'gene':{'compoundId':{'is': [activeId]}}});

            Genes.handler.one('count').get({filters:activeIdFilter}).then(function (result) {
              $scope.compoundIdCounts[activeId] = result || 0; 
            });
          });
        }
      } else if ($scope.type === 'curated_set' && asContext) {
        $scope.predefinedCurated = _.filter(Extensions.GENE_SET_ROOTS, function(set) {
          return set.type === 'curated_set';
        });
        $scope.predefinedCuratedIds = _.pluck($scope.predefinedCurated, 'id');

        activeIds = $scope.predefinedCuratedIds;

        GeneSets.several(activeIds.join(',')).get('genes/counts', {filters: filters}).then(function(result) {
          $scope.curatedIdCounts = result;
        });
      } else if (isGeneType()) {
        resolveActiveGeneIds();
      }

      // Check if there are extended element associated with this facet
      // i.e. : GeneList is a subset of Gene
      $scope.hasExtension = false;

      if (isGeneType()) {
        if (FiltersUtil.hasGeneListExtension(filters)) {
          $scope.hasExtension = true;
        }
      }
    }

    $scope.addGeneSetType = function(type) {
      var filters = FilterService.filters();
      if (! filters.hasOwnProperty('gene')) {
        filters.gene = {};
      }
      filters.gene[type] = true;
      LocationService.setFilters(filters);
    };

    $scope.removeGeneSetType = function(type) {
      var filters = FilterService.filters();
      if (filters.hasOwnProperty('gene')) {
        delete filters.gene[type];
        if (_.isEmpty(filters.gene)) {
          delete filters.gene;
        }
      }
      LocationService.setFilters(filters);
    };

    var _captureTermInfo = function ( term ) {
      if ( ! term ) { return }

      var _type = term.type;
      var _id = term.id;
      var _name = term.name;

      var isGeneSet = function () {
        return _.contains ( ['go_term', 'pathway'], _type );
      };

      if ( isGeneSet () ) {
        GeneSetNameLookupService.put ( _id, _name );
      }
    };

    $scope.addTerm = function (term) {

      _captureTermInfo ( term );

      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.addTerm({
        type: type,
        facet: facet,
        term: term.id
      });
    };

    $scope.removeTerm = function (term) {
      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.removeTerm({
        type: type,
        facet: facet,
        term: term
      });
    };
    
    $scope.notFacet = function() {
      Facets.notFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
    };
    
    $scope.isFacet = function() {
      Facets.isFacet({
        type: $scope.type,
        facet: $scope.facetName
      });
    };

    /* Used for special cases where the relation is one-to-many instead of one-to-one */
    $scope.removeSpecificTerm = function(type, facet, term) {
      // Special remapping for the 'Upload Donor Set' control in the External Repository File page.
      if ('file-donor' === type && Extensions.ENTITY === facet) {
        type = 'file';
      }

      try {
        Facets.removeTerm({
          type: type,
          facet: facet,
          term: term
        });
      } catch (err) {
        Facets.removeTerm({
          type: type,
          facet: 'id',
          term: term
        });
      }

    };

    $scope.removeFacet = function () {
      var type = $scope.proxyType? $scope.proxyType : $scope.type;
      var facet = $scope.proxyFacetName? $scope.proxyFacetName: $scope.facetName;

      Facets.removeFacet({
        type: type,
        facet: facet
      });

      // Remove secondary facet - entity
      if (_.contains(['gene', 'donor', 'mutation'], type) === true && $scope.facetName === 'id') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }

      if ($scope.type === 'pathway') {
        Facets.removeFacet({
          type: type,
          facet: 'hasPathway'
        });
      }

      if ('file' === type && facet === 'donorId') {
        Facets.removeFacet({
          type: type,
          facet: Extensions.ENTITY
        });
      }
    };


    /* Add a gene set term to the search filters */
    $scope.uploadGeneSet = () => {
      $modal.open({
        templateUrl: '/scripts/genelist/views/upload.html',
        controller: 'GeneListController'
      });
    };

    $scope.uploadDonorSet = () => {
      $modal.open({
        templateUrl: '/scripts/donorlist/views/upload.html',
        controller: 'DonorListController'
      });
    };

     $scope.uploadEntitySet = () => {
      $modal.open({
        templateUrl: '/scripts/entitysetupload/views/upload.html',
        controller: 'EntitySetUploadController',
        resolve: {
          entityType: () => {
            return $scope.type;
          }
        }
      });
    };

    $scope.selectSet = (set) => {
      let term = {};
      if(!set.selected){
        if($scope.isInRepositoryFile && $scope.type === 'file-donor') {
          term.id = _.head(set.repoFilters.file.donorId.is);
        } else{
          term.id = _.head(set.advFilters[$scope.type].id.is);
        }
        
        term.type = $scope.type;
        term.name = set.name;
        $scope.addTerm(term);
        event.stopPropagation();
      } else if(set.selected) {
        $scope.removeTerm(`ES:${set.id}`);
        event.stopPropagation();
      }
    }

    // Needed if term removed from outside scope
    $scope.$on(FilterService.constants.FILTER_EVENTS.FILTER_UPDATE_EVENT, setup);

    setup();
  });

  module.directive('tagsFacet', function () {
    return {
      restrict: 'A',
      scope: {
        facetName: '@',
        label: '@',
        type: '@',
        example: '@',
        placeholder: '@',
        entitySets: '=',
        proxyType: '@',
        proxyFacetName: '@',
        showEntitySetFacet: '@'
      },
      templateUrl: function (elem, attr) {
        var path_ = function (s) {
          return 'scripts/facets/views/' + s + '.html';
        };
        var type = attr.type;

        if (type === 'go_term') {
          return path_ ('gotags');
        }
        if (type === 'pathway') {
          return path_ ('pathwaytags');
        }
        if (type === 'curated_set') {
          return path_ ('curatedtags');
        }
        if (type === 'compound') {
          return path_ ('compoundtags');
        }
        
        return path_ ('tags');
      },
      controller: 'tagsFacetCtrl'
    };
  });
})();
