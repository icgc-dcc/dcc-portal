/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

/*
 * This is the Angular service that translates a parse tree of PQL into an object model to be
 * consumed by UI-centric code.
*/

(function () {
  'use strict';

  var namespace = 'icgc.common.pql.queryobject';
  var serviceName = 'PqlQueryObjectService';

  var module = angular.module(namespace, []);

  module.factory(serviceName, function (PqlTranslationService) {
    var defaultProjection = ['*'];

    function getEmptyQueryObject () {
      return {
        params: {
          // For UI, selectAll should always be true. This is translated to 'select(*)' in PQL.
          selectAll: true,
          customSelects: [],
          facets: false,
          sort: [],
          limit: {}
        },
        filters: {}
      };
    }

    function ensureArray (array) {
      return _.isArray (array) ? array : [];
    }

    function ensureString (string) {
      return _.isString (string) ? string.trim() : '';
    }

    function ensureObject (o) {
      return _.isPlainObject (o) ? o : {};
    }

    function convertPqlToQueryObject (pql) {
      pql = ensureString (pql);

      if (pql.length < 1) {return getEmptyQueryObject();}

      var jsonTree = PqlTranslationService.fromPql (pql);
      return convertJsonTreeToQueryObject (jsonTree);
    }

    function isNode (nodeName, node) {
      node = node || {};
      return ensureString (node.op) === nodeName;
    }

    function createNodeDetector (nodeName) {
      return _.partial (isNode, nodeName);
    }

    var isAndNode = createNodeDetector ('and');
    var isSortNode = createNodeDetector ('sort');
    var isLimitNode = createNodeDetector ('limit');
    var isSelectNode = createNodeDetector ('select');
    var isFacetsNode = createNodeDetector ('facets');

    function allAre (truthy, collection, predicate) {
      return _.every (collection, truthy ? predicate : _.negate (predicate));
    }

    function anyIs (truthy, collection, predicate) {
      return _.some (collection, truthy ? predicate : _.negate (predicate));
    }

    function parseIdentifier (id) {
      var splits = ensureString(id).split ('.');

      // This attempts to read the first 2 elements from splits to create a tuple of {category, facet}.
      var result = _.zipObject (['category', 'facet'], splits);
      return (result.category && result.facet) ? result : null;
    }

    /*
     * A bunch of processors specific to certain operators in converting the parse tree to QueryObject.
     */
    function inOperatorProcessor (node, emptyValue) {
      var permittedOps = ['in', 'eq'];
      if (! _.contains (permittedOps, node.op)) {return emptyValue;}

      var identifier = parseIdentifier (node.field);
      if (! identifier) {return emptyValue;}

      var values = ensureArray (node.values);
      if (values.length < 1) {return emptyValue;}

      // Currently this service treats both 'in' and 'eq' nodes as terminal/leaf nodes.
      // No child node detection nor recursive processing here.
      var propertyPath = [identifier.category, identifier.facet, 'in'];
      return _.set ({}, propertyPath, values);
    }

    function notOperatorProcessor (node, emptyValue) {
      var permittedOps = ['not'];
      if (! _.contains (permittedOps, node.op)) {return emptyValue;}

      var values = ensureArray (node.values);
      if (values.length < 1) {return emptyValue;}

      // For the time being, we expect only one 'in' node in the values of an 'not' node.
      var inNode = values[0];
      var predicate = function (op) {return isNode (op, inNode);};

      if (allAre (false, ['in', 'eq'], predicate)) {
        return emptyValue;
      }

      var identifier = parseIdentifier (inNode.field);
      if (! identifier) {return emptyValue;}

      values = ensureArray (inNode.values);
      if (values.length < 1) {return emptyValue;}

      var propertyPath = [identifier.category, identifier.facet, 'not'];
      return _.set ({}, propertyPath, values);
    }

    function unaryOperatorProcessor (op, node, emptyValue) {
      var permittedOps = [op];
      if (! _.contains (permittedOps, node.op)) {return emptyValue;}

      /*
       * For unary operators such as 'exists' and 'missing' (supported here), the identifier is
       * stored in a single-element array in 'values' attribute, as opposed to the 'field' attribute.
       * The reason for that was this way a solution could be generalized for 'exists', 'missing' as well as
       * 'select' and 'facets' when converting the parse tree back to PQL. That's really an implementation detail for
       * PqlTranslationService.
       */
      var identifier = node.values[0];
      identifier = parseIdentifier (identifier);
      if (! identifier) {return emptyValue;}

      var propertyPath = [identifier.category, identifier.facet, op];
      return _.set ({}, propertyPath, true);
    }

    function orOperatorProcessor (node, emptyValue, accumulator) {
      var permittedOps = ['or'];
      if (! _.contains (permittedOps, node.op)) {return emptyValue;}

      return _.reduce (ensureArray (node.values), reduceFilterArrayToQueryFilters, accumulator);
    }

    function mapWithMultipleFuncs (collection, funcs, emptyValue, f) {
      var result =  _.map (collection, function (element) {
        return _.reduce (funcs, function (result, func) {
          /* We use this check, along with reduce(), to 'short-circuit' (kind of) this anonymous function here,
           * because mapWithMultipleFuncs() is meant to have each element processed exactly once by one processor only.
           * The functions in 'funcs' list should follow this rule: process the element, if matched, and
           * return a value other than the emptyValue; return the emptyValue to pass. It's like a map()
           * with an internal switch/case construct for calling the corresponding function.
           */
          if (! _.isEqual (result, emptyValue)) {return result;}

          return f (func, element, emptyValue);
        }, emptyValue);
      });

      return _.without (result, emptyValue);
    }

    var supportedOps = ['in', 'eq', 'or', 'exists', 'missing', 'not'];

    var parseTreeOperatorProcessors = [
      inOperatorProcessor,
      orOperatorProcessor,
      notOperatorProcessor,
      _.partial (unaryOperatorProcessor, 'exists'),
      _.partial (unaryOperatorProcessor, 'missing')
    ];

    function reduceFilterArrayToQueryFilters (result, node) {
      if (! node) {return result;}
      if (! _.contains (supportedOps, node.op)) {return result;}

      var values = ensureArray (node.values);
      if (values.length < 1) {return result;}

      var emptyValue = {};

      var filters = mapWithMultipleFuncs ([node], parseTreeOperatorProcessors, emptyValue,
        function (processor, valueNode, empty) {
          return processor (valueNode, empty, result);
        }
      );

      return _.isEmpty (filters) ? result : _.reduce (filters, _.merge, result);
    }

    function getSpecialNodeFromTreeArray (treeArray, filterFunc) {
      var nodes = _.filter (treeArray, filterFunc);
      return (nodes.length > 0) ? nodes [0] : null;
    }

    function removeOp (jsonObject) {
      return _.omit (jsonObject, 'op');
    }

    function convertJsonTreeToQueryObject (treeArray) {
      var result = getEmptyQueryObject();

      // In this context, we expect the input (treeArray) to be an array,
      // namely we don't expect 'count' in this case.
      if (! _.isArray (treeArray)) {return result;}

      var andNode = getSpecialNodeFromTreeArray (treeArray, isAndNode);
      // For our current need, there should be only one 'And' node at the top level if one exists.
      var filterValues = andNode ? ensureArray (andNode.values) : treeArray;
      result.filters = _.reduce (filterValues, reduceFilterArrayToQueryFilters, {});

      var customSelectsNode = getSpecialNodeFromTreeArray (treeArray, function (node) {
        return isSelectNode (node) &&
          _.isArray (node.values) &&
          ! _.isEqual (node.values, defaultProjection);
      });

      if (customSelectsNode) {
        result.params.customSelects = customSelectsNode.values;
      }

      // Again, currently the UI doesn't care about projection on facets so we treat any 'facets' as 'facets(*)'
      result.params.facets = (null !== getSpecialNodeFromTreeArray (treeArray, isFacetsNode));

      var sortNode = getSpecialNodeFromTreeArray (treeArray, isSortNode);
      // The values field in a 'sort' node contains an array of sort fields.
      result.params.sort = (sortNode && _.isArray (sortNode.values)) ? sortNode.values : [];

      var limitNode = getSpecialNodeFromTreeArray (treeArray, isLimitNode);
      result.params.limit = limitNode ? removeOp (limitNode) : {};

      return result;
    }

    function removeEmptyObject (collection) {
      return _.without (collection, {});
    }

    function createSelectTreeNodeObject (values) {
      return {
          op: 'select',
          values: values
        };
    }

    function createCountTreeNodeObject (values) {
      return {
        op: 'count',
        values: values
      };
    }

    // Here the 'func' param is optional. It should be a function that further transforms or
    // augments filter array.
    function toFilterOnlyStatement (pql, func) {
      var query = convertPqlToQueryObject (pql);
      var filters = query.filters;

      if (! _.isPlainObject (filters) || _.isEmpty (filters)) {return '';}

      var filterArray = convertQueryFilterToJsonTree (filters);
      var pqlJson = _.isFunction (func) ? func (filterArray) : filterArray;

      return PqlTranslationService.toPql (pqlJson);
    }

    function toCountStatement (pql) {
      return toFilterOnlyStatement (pql, createCountTreeNodeObject);
    }

    function convertQueryObjectToJsonTree (query) {
      // Result, in this context, should be an array. Count statement (which is represented as an object)
      // is handled/generated by toCountStatement().
      var result = [];
      var queryParams = ensureObject (query.params);

      // Selects
      if (queryParams.selectAll) {
        result.push (createSelectTreeNodeObject (defaultProjection));
      }

      var customProjection = queryParams.customSelects;
      if (_.isArray (customProjection) && ! _.isEmpty (customProjection)) {
        result.push (createSelectTreeNodeObject (customProjection));
      }

      // Facets
      if (queryParams.facets) {
        result.push ({
          op: 'facets',
          values: ['*']
        });
      }

      // Filters
      result = result.concat (convertQueryFilterToJsonTree (query.filters));

      // Sort
      var sort = ensureArray (queryParams.sort);
      if (! _.isEmpty (sort)) {
        result.push ({
          op: 'sort',
          values: sort
        });
      }

      // Limit
      var limit = ensureObject (queryParams.limit);
      if (! _.isEmpty (limit)) {
        limit.op = 'limit';
        result.push (limit);
      }

      return result;
    }

    /*
     * A list of processors (functions) that process a list of attributes under each 'facet' node
     * in the QueryObject model, converting the filters ('in', 'exists', 'missing') in QueryObject to parse tree nodes.
     */
    function inFacetPropertyProcessor (property, value, defaultValue, identifier) {
      if ('in' !== property) {return defaultValue;}

      var inArray = ensureArray (value);
      var inArrayLength = inArray.length;

      if (inArrayLength > 0) {
        return {
          op: (inArrayLength > 1) ? 'in' : 'eq',
          field: identifier,
          values: inArray
        };
      } else {
        return defaultValue;
      }
    }

    function notFacetPropertyProcessor (property, value, defaultValue, identifier) {
      if ('not' !== property) {return defaultValue;}

      var notArray = ensureArray (value);

      if (notArray.length > 0) {
        var inNode = inFacetPropertyProcessor ('in', notArray, defaultValue, identifier);

        if (_.isEqual (defaultValue, inNode)) {return defaultValue;}

        return {
          op: 'not',
          values: [inNode]
        };
      } else {
        return defaultValue;
      }
    }

    function booleanFacetPropertyProcessor (op, property, value, defaultValue, identifier) {
      op = ensureString (op);
      if (op !== property) {return defaultValue;}

      return (_.isBoolean (value) && value) ? {op: op, values: [identifier]} : defaultValue;
    }

    var facetPropertyProcessors = [
      inFacetPropertyProcessor,
      notFacetPropertyProcessor,
      _.partial (booleanFacetPropertyProcessor, 'exists'),
      _.partial (booleanFacetPropertyProcessor, 'missing')
    ];

    function getObjectProperties (o) {
      return _.keys (ensureObject (o));
    }

    function convertFacetInQueryFilterToJsonTree (facet, identifier) {
      var properties = getObjectProperties (facet);
      var emptyValue = {};

      if (_.isEmpty (properties)) {return emptyValue;}

      var result = mapWithMultipleFuncs (properties, facetPropertyProcessors, emptyValue,
        function (processor, property, empty) {
          return processor (property, facet [property], empty, identifier);
        }
      );

      if (_.isEmpty (result)) {return emptyValue;}

      return (result.length > 1) ? {op: 'or', values: result} : result[0];
    }

    function convertQueryFilterToJsonTree (queryFilter) {
      var categoryKeys = getObjectProperties (queryFilter);

      if (categoryKeys.length < 1) {return [];}

      var termArray = _.map (categoryKeys, function (categoryKey) {
        var category = queryFilter [categoryKey];
        var facetKeys = getObjectProperties (category);

        var termFilters = _.map (facetKeys, function (facetKey) {
          var identifier = '' + categoryKey + '.' + facetKey;

          return convertFacetInQueryFilterToJsonTree (category [facetKey], identifier);
        });

        return termFilters;
      });

      var values = removeEmptyObject (_.flatten (termArray));

      return (values.length > 1) ? [{op: 'and', values: values}] : values;
    }

    function addTermToQueryFilter (categoryName, facetName, term, queryFilter) {
      if (! term) {return queryFilter;}

      return addMultipleTermsToQueryFilter (categoryName, facetName, [term], queryFilter);
    }

    function addMultipleTermsToQueryFilter (categoryName, facetName, terms, queryFilter) {
      if (! _.isArray (terms)) {return queryFilter;}
      if (_.isEmpty (terms)) {return queryFilter;}

      var propertyPath = [categoryName, facetName, 'in'];
      var inValueArray = _.get (queryFilter, propertyPath, []);

      _.each (terms, function (term) {
        if (term && ! _.contains (inValueArray, term)) {
          inValueArray.push (term);
        }
      });

      // update the original filter.
      return _.set (queryFilter, propertyPath, inValueArray);
    }

    function excludeTerm (categoryName, facetName, term, queryFilter) {
      if (! term) {return queryFilter;}

      return excludeMultipleTerms (categoryName, facetName, [term], queryFilter);
    }

    function excludeMultipleTerms (categoryName, facetName, terms, queryFilter) {
      if (! _.isArray (terms)) {return queryFilter;}
      if (_.isEmpty (terms)) {return queryFilter;}

      var propertyPath = [categoryName, facetName, 'not'];
      var notArray = _.get (queryFilter, propertyPath, []);

      _.each (terms, function (term) {
        if (term && ! _.contains (notArray, term)) {
          notArray.push (term);
        }
      });

      return _.set (queryFilter, propertyPath, notArray);
    }

    function addBooleanPropertyToQueryFilter (propertyName, categoryName, facetName, notUsed, queryFilter) {
      var propertyPath = [categoryName, facetName, propertyName];
      return _.set (queryFilter, propertyPath, true);
    }

    function removeTermFromQueryFilter (categoryName, facetName, term, queryFilter) {
      var categoryKeys = getObjectProperties (queryFilter);

      if (_.contains (categoryKeys, categoryName)) {
        var facetKeys = getObjectProperties (queryFilter [categoryName]);
        var inField = 'in';

        if (_.contains (facetKeys, facetName)) {
          var propertyPath = [categoryName, facetName, inField];
          var inValueArray = _.get (queryFilter, propertyPath, []);
          queryFilter = _.set (queryFilter, propertyPath, _.without (inValueArray, term));

          if (_.isEmpty (_.get (queryFilter, propertyPath, []))) {
            queryFilter = removePropertyFromQueryFilterFacet (inField, categoryName, facetName, null, queryFilter);
          }
        }
      }

      return queryFilter;
    }

    function removePropertyFromQueryFilterFacet (propertyName, categoryName, facetName, notUsed, queryFilter) {
      var propertyPath = [categoryName, facetName];
      var facetProperty = _.omit (_.get (queryFilter, propertyPath, {}), propertyName);

      return _.isEmpty (facetProperty) ?
        removeFacetFromQueryFilter (categoryName, facetName, null, queryFilter) :
        _.set (queryFilter, propertyPath, facetProperty);
    }

    function removeFacetFromQueryFilter (categoryName, facetName, notUsed, queryFilter) {
      var categoryKeys = getObjectProperties (queryFilter);

      if (_.contains (categoryKeys, categoryName)) {
        var category = queryFilter [categoryName];
        var facetKeys = getObjectProperties (category);

        if (_.contains (facetKeys, facetName)) {
          delete category[facetName];

          if (_.isEmpty (getObjectProperties (category))) {
            delete queryFilter [categoryName];
          }
        }
      }

      return queryFilter;
    }

    function includesFacets (pql) {
      return updateQueryParam (pql, 'facets', true);
    }

    var validIncludeFields = ['transcripts', 'consequences', 'occurrences',
      'specimen', 'observation', 'projects'];

    function addProjections (pql, fields) {
      fields = ensureArray (fields);
      var selectFields = _.remove (fields, function (s) {
        return _.isString (s) && _.contains (validIncludeFields, s);
      });

      if (_.isEmpty (selectFields)) {return pql;}

      var query = convertPqlToQueryObject (pql);
      var paramName = 'customSelects';
      var customSelects = query.params [paramName];
      var updatedSelects = _.union (customSelects, selectFields);

      var asterisk = defaultProjection[0];
      return updateQueryParam (pql, paramName, _.without (updatedSelects, asterisk));
    }

    function addProjection (pql, selectField) {
      var asterisk = defaultProjection[0];
      if (asterisk === selectField) {return pql;}

      return addProjections (pql, [selectField]);
    }

    function includes (pql, field) {
      return (_.isArray (field) ? addProjections : addProjection) (pql, field);
    }

    function convertQueryObjectToPql (queryObject) {
      var jsonTree = convertQueryObjectToJsonTree (queryObject);
      var result = PqlTranslationService.toPql (jsonTree);
      return result;
    }

    function updateQueryFilter (pql, categoryName, facetName, term, updators) {
      return updateQueryWithCustomAction (pql, function (query) {
        query.filters = _.reduce (ensureArray (updators), function (result, f) {
          return _.isFunction (f) ? f (categoryName, facetName, term, result) : result;
        }, query.filters);
      });
    }

    function cleanUpArguments (args, func) {
      var argumentArray = Array.prototype.slice.call (args);
      return _.isFunction (func) ? _.map (argumentArray, func) : argumentArray;
    }

    function mergeQueryObjects (queryArray) {
      var queryObjects = removeEmptyObject (queryArray);
      var numberOfQueries = queryObjects.length;

      if (numberOfQueries < 1) {return {};}

      return (numberOfQueries < 2) ? queryObjects [0] : _.reduce (queryObjects, _.merge, {});
    }

    function mergeQueries () {
      var emptyValue = {};
      var args = cleanUpArguments (arguments, function (o) {
        return ensureObject (o);
      });

      return _.isEmpty (args) ? emptyValue : mergeQueryObjects (args);
    }

    function mergePqlStatements () {
      var emptyValue = '';
      var args = cleanUpArguments (arguments, function (s) {
        return ensureString (s);
      });

      var pqlArray = _.unique (_.without (args, emptyValue));
      var numberOfPql = pqlArray.length;

      if (numberOfPql < 1) {return emptyValue;}

      var isValid = _.flow (PqlTranslationService.tryParse, _.property ('isValid'));

      if (numberOfPql < 2) {
        var pql = pqlArray [0];
        return isValid (pql) ? pql : emptyValue;
      }

      // Making sure both PQL statements are valid.
      if (anyIs (false, pqlArray, isValid)) {
        return emptyValue;
      }

      var resultObject = mergeQueryObjects (_.map (pqlArray, convertPqlToQueryObject));

      return _.isEmpty (resultObject) ? emptyValue :
        PqlTranslationService.toPql (convertQueryObjectToJsonTree (resultObject));
    }

    function updateQueryWithCustomAction (pql, action) {
      var query = convertPqlToQueryObject (pql);
      action (query);
      return convertQueryObjectToPql (query);
    }

    function updateQueryParam (pql, param, value) {
      return updateQueryWithCustomAction (pql, function (query) {
        query.params [param] = value;
      });
    }

    return {
      addTerm: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term, [addTermToQueryFilter]);
      },
      addTerms: function (pql, categoryName, facetName, terms) {
        return updateQueryFilter (pql, categoryName, facetName, terms, [addMultipleTermsToQueryFilter]);
      },
      excludeTerm: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term, [excludeTerm]);
      },
      excludeTerms: function (pql, categoryName, facetName, terms) {
        return updateQueryFilter (pql, categoryName, facetName, terms, [excludeMultipleTerms]);
      },
      removeTerm: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term, [removeTermFromQueryFilter]);
      },
      removeFacet: function (pql, categoryName, facetName) {
        return updateQueryFilter (pql, categoryName, facetName, null, [removeFacetFromQueryFilter]);
      },
      has: function (pql, categoryName, existsField) {
        var propertyName = 'exists';
        var updator = _.partial (addBooleanPropertyToQueryFilter, propertyName);
        return updateQueryFilter (pql, categoryName, existsField, null, [updator]);
      },
      hasNo: function (pql, categoryName, existsField) {
        var propertyName = 'exists';
        var updator = _.partial (removePropertyFromQueryFilterFacet, propertyName);
        return updateQueryFilter (pql, categoryName, existsField, null, [updator]);
      },
      withMissing: function (pql, categoryName, missingField) {
        var propertyName = 'missing';
        var updator = _.partial (addBooleanPropertyToQueryFilter, propertyName);
        return updateQueryFilter (pql, categoryName, missingField, null, [updator]);
      },
      withoutMissing: function (pql, categoryName, missingField) {
        var propertyName = 'missing';
        var updator = _.partial (removePropertyFromQueryFilterFacet, propertyName);
        return updateQueryFilter (pql, categoryName, missingField, null, [updator]);
      },
      overwrite: function (pql, categoryName, facetName, term) {
        return updateQueryFilter (pql, categoryName, facetName, term,
          [removeFacetFromQueryFilter, _.isArray (term) ? addMultipleTermsToQueryFilter : addTermToQueryFilter]);
      },
      includesFacets: includesFacets,
      includes: includes,
      toCountStatement: toCountStatement,
      toFilterOnlyStatement: toFilterOnlyStatement,
      convertQueryToPql: convertQueryObjectToPql,
      convertPqlToQueryObject: convertPqlToQueryObject,
      mergePqls: mergePqlStatements,
      mergeQueries: mergeQueries,
      getSort: function (pql) {
        var query = convertPqlToQueryObject (pql);
        return query.params.sort;
      },
      setSort: function (pql, sortArray) {
        return updateQueryParam (pql, 'sort', sortArray);
      },
      getLimit: function (pql) {
        var query = convertPqlToQueryObject (pql);
        return query.params.limit;
      },
      setLimit: function (pql, limit) {
        return updateQueryParam (pql, 'limit', limit);
      },
      getFilters: function (pql) {
        var queryObject = convertPqlToQueryObject (pql);
        return queryObject.filters;
      }
    };

  });
})();
