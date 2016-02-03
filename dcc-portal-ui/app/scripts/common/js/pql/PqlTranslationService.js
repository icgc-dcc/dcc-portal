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
 * PqlTranslator translates a PQL into a parse tree in JSON. This can be used without AngularJS.
 */

(function () {
  'use strict';
  window.dcc = window.dcc || {};

  dcc.PqlTranslator = function (LOGGER) {
    var noNestingOperators = [
      'exists', 'missing', 'select', 'facets'
    ];

    function addQuotes (s) {
      return '"' + s + '"';
    }

    function appendCommaIfNeeded (s) {
      return s.length > 0 ? ',' : '';
    }

    function opValuesToString (op, values) {
      return '' + op + '(' + values.join() + ')';
    }

    function convertNodeToPqlString (unit) {
      if (! _.isPlainObject (unit)) {
        return _.isString (unit) ? addQuotes (unit) : '' + unit;
      }

      var op = unit.op;
      if ('limit' === op) {
        return limitUnitToPql (unit);
      }
      if ('sort' === op) {
        return sortUnitToPql (unit);
      }

      var vals = _.isArray (unit.values) ? unit.values : [];
      var values = (_.contains (noNestingOperators, op) ? vals : vals.map(convertNodeToPqlString)).join();

      var parameters = unit.field || '';

      if (values.length > 0) {
        parameters += appendCommaIfNeeded (parameters) + values;
      }

      var ending = ('count' === op) ?
        ')' + appendCommaIfNeeded (parameters) + parameters :
        parameters + ')';

      return '' + op + '(' + ending;
    }

    function positiveInteger (n) {
      return (n > 0) ? Math.floor (n) : 0;
    }

    function limitUnitToPql (limit) {
      if (! _.isPlainObject (limit)) {return '';}
      if (_.isEqual ({op: 'limit'}, limit)) {return '';}

      var size = _.isNumber (limit.size) ? positiveInteger (limit.size) : 0;
      var values = _.isNumber (limit.from) ? [positiveInteger (limit.from), size] : [size];

      return opValuesToString (limit.op, values);
    }

    function sortUnitToPql (sort) {
      if (! sort) {return '';}
      if (! _.isArray (sort.values)) {return '';}

      var sortArray = sort.values;
      if (sortArray.length < 1) {return '';}

      var values = sortArray.map (function (obj) {
        return '' + obj.direction + obj.field;
      });

      return opValuesToString (sort.op, values);
    }

    function parseResult (result) {
      return _.isError (result) ? {
        isValid: false,
        errorMessage: result.message
      } : {
        isValid: true,
        result: result
      };
    }

    return {
      fromPql: function (pql) {
        try {
          return PqlPegParser.parse (pql);
        } catch (e) {
          LOGGER.error ('Error parsing PQL [%s] with error message: [%s]', pql, e.message);
          throw e;
        }
      },
      // Exception-free version
      tryParse: function (pql) {
        return parseResult (_.attempt (PqlPegParser.parse, pql));
      },
      // Exception-free and function-chaining friendly
      parseOrDefault: function (pql, defaultValue) {
        var result = this.tryParse (pql);
        return result.isValid ? result : defaultValue;
      },
      toPql: function (parseTree) {
        var result = _.isArray (parseTree) ?
          _.filter (parseTree.map (convertNodeToPqlString), function (s) {
            return s.trim().length > 1;
          }).join() :
          _.isPlainObject (parseTree) ? convertNodeToPqlString (parseTree) : null;

        if (result === null) {
          LOGGER.warn ('The input is neither an array nor an object: [%s]. toPql() is returning an empty string.',
            JSON.stringify (parseTree));
          result = '';
        }

        return result;
      }
    };
  };
})();

/*
 * This is the Angular service that wraps around the PqlTranslator.
 */
(function () {
  'use strict';

  var namespace = 'icgc.common.pql.translation';
  var serviceName = 'PqlTranslationService';

  var module = angular.module (namespace, []);

  module.factory (serviceName, function ($log) {
    var translator = dcc.PqlTranslator ($log);

    return {
      fromPql: translator.fromPql,
      tryParse: translator.tryParse,
      toPql: translator.toPql
    };
  });
})();

