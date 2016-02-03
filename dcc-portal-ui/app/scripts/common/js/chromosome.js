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

  var module = angular.module('icgc.common.chromosome', []);

  module.service('Chromosome', function() {

    // Chromosome lengths
    var lookup = {
      '1': 249250621,
      '2': 243199373,
      '3': 198022430,
      '4': 191154276,
      '5': 180915260,
      '6': 171115067,
      '7': 159138663,
      '8': 146364022,
      '9': 141213431,
      '10': 135534747,
      '11': 135006516,
      '12': 133851895,
      '13': 115169878,
      '14': 107349540,
      '15': 102531392,
      '16': 90354753,
      '17': 81195210,
      '18': 78077248,
      '19': 59128983,
      '20': 63025520,
      '21': 48129895,
      '22': 51304566,
      'X': 155270560,
      'Y': 59373566,
      'MT': 16569
    };

    this.get = function() {
      return lookup;
    };

    this.length = function(chromosome) {
      return lookup[chromosome];
    };

    /**
     * Validate against chromosome range
     *  Params: chr, start, end
     */
    this.validate = function() {
      var range, chr;

      if (arguments.length < 1 || arguments.length > 3) {
        return false;
      }
      chr = arguments[0].toUpperCase();
      if (lookup.hasOwnProperty(chr) === false) {
        return false;
      }
      range = lookup[chr];

      if (angular.isDefined(arguments[1]) && (arguments[1] > range || arguments[1] < 1)) {
        return false;
      }
      if (angular.isDefined(arguments[2]) && (arguments[2] > range || arguments[2] < 1)) {
        return false;
      }
      return true;
    };

  });

})();
