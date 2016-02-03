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

  _.mixin ({
    pairUp: function (arrays) {
      return _.zip.apply (_, arrays);
    }
  });

  function ensureArray (array) {
    return _.isArray (array) ? array : [];
  }

  function ensureString (string) {
    return _.isString (string) ? string.trim() : '';
  }

  // Higher-order function to return a partial function (to perform substring) for a sentence (captured).
  function subStringer (sentence) {
    var f = _.spread (_.partial (_.method, 'substr'));

    return function (positions) {
      return f (positions) (sentence);
    };
  }

  function words (phrase) {
    return _.words (phrase, /[^, ]+/g);
  }

  // Abridger class - trying to keep this class lightweight
  // so not using prototype (will do so if there's need to subclass)
  function Abridger (maxLength) {
    var ellipsis = '...';

    // TODO: re-joining like this actually doesn't necessarily
    // reproduce the original partial sentence as _.words() strips out punctuations.
    function reconstruct (fragments, sentence) {
      var joined = fragments.join (' ').trim();
      var dots = function (f) {
        return f (sentence, joined) ? '' : ellipsis;
      };

      return dots (_.startsWith) + joined + dots (_.endsWith);
    }

    function anyMatch (sentence, keyword) {
      if (_.isEmpty (sentence)) {
        return {};
      }

      var sentence2 = sentence.toUpperCase();
      var keyword2 = keyword.toUpperCase();

      var tokens = [keyword2].concat (words (keyword2));
      var matchKeyword = _(tokens)
        .unique()
        .find (function (token) {
          return _.contains (sentence2, token);
        });

      return _.isUndefined (matchKeyword) ? {} : {
        sentence: sentence,
        keyword: matchKeyword
      };
    }

    //
    var _this = this;

    this.maxLength = maxLength;

    this.find = function (sentence, keyword) {
      var sentence2 = sentence.toUpperCase();
      var keyword2 = keyword.toUpperCase();

      var index = sentence2.indexOf (keyword2);
      var start = _.lastIndexOf (sentence2, ' ', index) + 1;
      var end = _.indexOf (sentence2, ' ', index + keyword2.length);
      end = (end < 0) ? sentence2.length : end;

      var substring = subStringer (sentence);
      return {
        target: [substring ([start, end - start])],
        left: words (substring ([0, start])),
        right: words (substring ([end]))
      };
    };

    var withinLimit = this.withinLimit = function (newElements) {
      var combined = _(_this.resultArray).concat (newElements);

      var numberOfCharacters = combined.map ('length').sum();
      var numberOfSpaces = combined.size() - 1;

      return (numberOfCharacters + numberOfSpaces) <= _this.maxLength;
    };

    this.processLeftAndRight = function (newElements) {
      var left = _.first (newElements);
      var right = _.last (newElements);

      if (withinLimit (newElements)) {
        _this.resultArray = [left].concat (_this.resultArray, right);
        return true;
      } else {

        // This mutates currentProcessor.
        if (_.size (left) >= _.size (right)) {
          if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          }
        } else {
          if (withinLimit (right)) {
            _this.currentProcessor = _this.processRightOnly;
            return _this.currentProcessor (newElements);
          } else if (withinLimit (left)) {
            _this.currentProcessor = _this.processLeftOnly;
            return _this.currentProcessor (newElements);
          }
        }

      }

      return false;
    };

    this.processLeftOnly = function (newElements) {
      var left = _.first (newElements);

      if (withinLimit (left)) {
        _this.resultArray = [left].concat (_this.resultArray);
        return true;
      }

      return false;
    };

    this.processRightOnly = function (newElements) {
      var right = _.last (newElements);

      if (withinLimit (right)) {
        _this.resultArray = _this.resultArray.concat (right);
        return true;
      }

      return false;
    };

    this.hasRoom = function (newElements) {
      return _this.currentProcessor (newElements);
    };

    this.init = function (initResult) {
      _this.resultArray = initResult;
      _this.currentProcessor = _this.processLeftAndRight;
    };

    this.abridge = function (sentences, keyword) {
      keyword = ensureString (keyword);

      if (_.isEmpty (keyword)) {
        return '';
      }

      var matchingPair = _.reduce (ensureArray (sentences), function (result, sentence) {
        return _.isEmpty (result) ? anyMatch (ensureString (sentence), keyword) : result;
      }, {});

      if (_.isEmpty (matchingPair)) {
        return '';
      }

      var finding = _this.find (matchingPair.sentence, matchingPair.keyword);

      if (_.isEmpty (finding)) {
        return '';
      }

      _this.init (finding.target);

      // This is a hack as it violates FP. If this hurts, turn it into a for-loop with guards.
      _([finding.left, finding.right])
        .pairUp()
        .takeWhile (_this.hasRoom)
        .value();

      return reconstruct (_this.resultArray, matchingPair.sentence);
    };

    // Declaring these fields here is more for the purpose of documentation,
    // less about initialization, as init() resets these two fields.
    this.currentProcessor = this.processLeftAndRight;
    this.resultArray = [];
  }

  // Expose Abridger as an NG service
  var namespace = 'icgc.common.text.utils';
  var serviceName = 'Abridger';

  var module = angular.module (namespace, []);

  module.factory (serviceName, function () {
    return {
      of: function (maxLength) {
        return new Abridger (maxLength);
      }
    };
  });
})();
