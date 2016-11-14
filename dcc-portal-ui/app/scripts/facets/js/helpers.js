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

  var toJson = angular.toJson;
  var isDefined = angular.isDefined;

  var module = angular.module('icgc.facets.helpers', ['icgc.facets']);

  module.factory('Facets', function (FilterService, FacetConstants, Extensions, $rootScope) {

    function _broadcastFacetStatusChange(facet, isActive, changeType) {
      $rootScope.$broadcast(FacetConstants.EVENTS.FACET_STATUS_CHANGE, {
        facet: facet || '*',
        isActive: isActive,
        changeType: (changeType || FacetConstants.FACET_CHANGE_TYPE.SINGLE)
      });
    }

    function ensurePath(filters, params) {
      if (!filters.hasOwnProperty(params.type)) {
        filters[params.type] = {};
      }
      if (!_.has(filters, [params.type, params.facet])) {
        if (isNot(params)) {
          filters[params.type][params.facet] = {not: []};
        } else {
          filters[params.type][params.facet] = {is: []};
        }
      }
    }

    /*
     * Set Terms
     */
    function setTerms(params) {
      if (invalidParams (params)) {
        throw new Error ('Missing property in params: ' + toJson (params));
      }

      var filters = FilterService.filters();
      ensurePath(filters, params);
      var operation = isNot(params) ? 'not' : 'is';

      filters[params.type][params.facet][operation] = angular.isArray(params.terms) ? params.terms : [params.terms];

      FilterService.filters(filters);
    }

    /*
     * Add a Term
     */
    function addTerm(params) {
      if (invalidParams (params)) {
        throw new Error ('Missing property in params: ' + toJson (params));
      }

      var filters = FilterService.filters();
      var operation = isNot(params) ? 'not' : 'is';

      ensurePath(filters, params);
      var facetFilter = filters[params.type][params.facet];

      if (!_.includes(facetFilter[operation], params.term)) {
        facetFilter[operation].push(params.term);
        _broadcastFacetStatusChange(params.term, true);
      }

      FilterService.filters(filters);
    }
    
    /**
     * Helper for walking filter and switching between 'is' and 'not'
     */
    function notFilterHelper(params, makeNot) {
      // Check for required parameters
      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });
      
      var which = 'not';
      var into = 'is';
      if (makeNot) {
        which = 'is';
        into = 'not';
      }
      
      var filters = FilterService.filters();
      if (_.has(filters, [params.type, params.facet, which])) {
        filters[params.type][params.facet][into] = filters[params.type][params.facet][which];
        delete filters[params.type][params.facet][which];
      } else if (params.type === 'go_term') {
        filters.gene[params.facet][into] = filters.gene[params.facet][which];
        delete filters.gene[params.facet][which];
      }
      
      _broadcastFacetStatusChange(params.facet, true);
      FilterService.filters(filters);
    }
    
    /** 
     * Make the facet an IS NOT   
     */
    function notFacet(params) {      
      notFilterHelper(params, true);
    }
    
    /**
     * Remove the IS NOT
     */
    function isFacet(params) {
      notFilterHelper(params, false);
    }

    /*
     * Remove a Term
     */
    function removeTerm (params) {
      if (invalidParams (params)) {
        throw new Error ('Missing property in params: ' + toJson (params));
      }

      // Check for required parameters
      [ 'type', 'facet', 'term'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      var filters = FilterService.filters();
      var operation = isNot(params) ? 'not' : 'is';
      var facetFilter = filters[params.type][params.facet];
      facetFilter[operation] = _.without(facetFilter[operation], params.term);
      if (!facetFilter[operation].length) {
        removeFacet(params);
      } else {
        _broadcastFacetStatusChange(params.facet, false);
        FilterService.filters(filters);
      }
    }

    function invalidParams (params) {
      var properties = ['type', 'facet', 'term'];

      return anyIs (false, propertyValues (params, properties), isDefined);
    }

    function propertyValues (obj, properties) {
      return _(obj).pick (properties)
        .values().value();
    }

    function anyIs (truthy, collection, predicate) {
      return _.some (collection,
        truthy ? predicate : _.negate (predicate));
    }

    /*
     * Removes a facet
     */
    function removeFacet(params) {
      var filters;

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();

      if (! filters.hasOwnProperty(params.type)) {
        return;
      }

      delete filters[params.type][params.facet];
      _broadcastFacetStatusChange(params.facet, false);

      if (_.isEmpty(filters[params.type])) {
        delete filters[params.type];
      }

      if (_.isEmpty(filters)) {
        FilterService.removeFilters();
      } else {
        FilterService.filters(filters);
      }
    }

    /*
     * Removes all filters
     */
    function removeAll() {
      FilterService.removeFilters();
      _broadcastFacetStatusChange(null, FacetConstants.FACET_CHANGE_TYPE.ALL);
    }

    /*
     * Toggle Term
     */
    function toggleTerm(params) {
      var filters;

      [ 'type', 'facet', 'term'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();
  
      if (_.has(filters, [params.type, params.facet, 'is']) && 
          filters[params.type][params.facet].is.indexOf(params.term)  !== -1) {
        removeTerm(params);
      } else if (_.has(filters, [params.type, params.facet, 'not']) && 
          filters[params.type][params.facet].not.indexOf(params.term)  !== -1) {
        removeTerm(params);
      } else {
        addTerm(params);
      }
    }

    /*
     * Get list of active terms
     */
    function getActiveTerms(params) {
      var filters, list = [];

      [ 'type', 'facet', 'terms'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();

      function filterFn(active) {
        return _.find(params.terms, function (term) {
          return term.term === active;
        }) || { term: active, count: 0};
      }

      if (_.has(filters, [params.type, params.facet, 'is'])) {
        list = _.map(filters[params.type][params.facet].is, filterFn);
      } else if (_.has(filters, [params.type, params.facet, 'not'])){
        list = _.map(filters[params.type][params.facet].not, filterFn);
      }

      return list;
    }
    
    /**
     * Determine if this facet is an "IS NOT"
     */
    function isNot(params) { 
      var filters;

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();
      if (params.type === 'go_term') {
        return _.has(filters, ['gene',params.facet,'not']);
      } else {
        return _.has(filters, params.type+'.'+params.facet+'.not');
      }
    }

    /*
     * Get list of inactive terms
     */
    function getInactiveTerms(params) {
      [ 'actives', 'terms'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      return _.difference(params.terms, params.actives);
    }

    function getActiveFromTags(params) {
      var list = getActiveTags(params);
      var retList = [];
      _.forEach(list, function(item) {
        retList.push(item);
      });
      
      return retList; 
    }

    /* Get a list of active tags */
    function getActiveTags(params) {
      var filters, list = [];

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();
      if (_.has(filters, [params.type, params.facet, 'is'])) {
        list = filters[params.type][params.facet].is || [];
      } else if (_.has(filters, [params.type, params.facet, 'not'])) {
        list = filters[params.type][params.facet].not || [];
      }

      return list;
    }

    /* Get a list of active tags */
    function getActiveLocations(params) {
      var filters, list = [];

      [ 'type', 'facet'].forEach(function (rp) {
        if (!params.hasOwnProperty(rp)) {
          throw new Error('Missing required parameter: ' + rp);
        }
      });

      filters = FilterService.filters();

      if (_.has(filters, [params.type, params.facet, 'is'])) {
        list = filters[params.type][params.facet].is;
      } else if (_.has(filters, [params.type, params.facet, 'not'])) {
        list = filters[params.type][params.facet].not;
      }

      return list;
    }

    return {
      setTerms: setTerms,
      toggleTerm: toggleTerm,
      addTerm: addTerm,
      removeTerm: removeTerm,
      notFacet: notFacet,
      isFacet: isFacet,
      removeFacet: removeFacet,
      removeAll: removeAll,
      getActiveTerms: getActiveTerms,
      isNot: isNot,
      getInactiveTerms: getInactiveTerms,
      getActiveTags: getActiveTags,
      getActiveLocations: getActiveLocations,
      getActiveFromTags: getActiveFromTags
    };
  });

})();
