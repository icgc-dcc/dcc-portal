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

(function() {
  'use strict';

  var module = angular.module('icgc.common.translator', []);

  /**
   * Translating value/code to human readable text, this bascially acts as a 
   */
  module.service('ValueTranslator', function(Consequence, DataType, CodeTable) {
     
     function getTranslatorModule(type) {

       if (type === 'consequenceType') {
         return Consequence;
       } else if (type === 'availableDataTypes' || type === 'dataType' ) {
         return DataType;
       } 
       return CodeTable;
     }

     function humanReadable(str) {
       var res = str;
       if (_.isEmpty(res) === true) { return res }
       res = res.replace(/_/g, '\u00A0').replace(/^\s+|\s+$/g, '');
       res = res.charAt(0).toUpperCase() + res.slice(1);
       return res;
     }

     this.translate = function(id, type) {
       if (!id) { return '' }
       if (id === '_missing') { return 'No Data' }

       return getTranslatorModule(type).translate(id) || humanReadable(id);
     };

     this.tooltip = function(id, type) {
       if (!id) { return '' }
       if (id === '_missing') { return 'No Data' }

       return getTranslatorModule(type).tooltip(id) || id;
     };

     this.readable = humanReadable;

  });

  module.filter('readable', function(ValueTranslator) {
    return function (id) {
      return ValueTranslator.readable(id);
    };
  });


  module.filter('trans', function (ValueTranslator) {
    return function (id, type) {
      return ValueTranslator.translate(id, type);
    };
  });

  module.filter('datatype', function (ValueTranslator) {
    return function (id) {
      return ValueTranslator.tooltip(id, 'availableDataTypes');
    };
  });

  module.filter('universe', function (Extensions) {
    return function (id) {
      return _.find(Extensions.GENE_SET_ROOTS, function(u) {
        return u.universe === id;
      }).name;
    };
  });


  module.filter('define', function (ValueTranslator) {
    return function (id, type) {
      return ValueTranslator.tooltip(id, type);
    };
  });





})();
