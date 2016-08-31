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

  var module = angular.module('icgc.common.pcawg', []);

  // TODO: This service needs to be generalized as this does not deal with
  // just PCAWG alone it is also used in the general Repositories Data file
  // service. I am leaving (MMoncada) this alone right now because other
  // code in the application depends on this service.
  module.service('PCAWG', function(gettextCatalog) {

    var data = [
      {id: 'DNA-Seq', shortLabel: gettextCatalog.getString('Whole Genomes')},
      {id: 'RNA-Seq', shortLabel: gettextCatalog.getString('Whole Transcriptomes')},
      {id: 'SSM', shortLabel: gettextCatalog.getString('Simple Somatic Mutations')},
      {id: 'CNSM', shortLabel: gettextCatalog.getString('Copy Number Somatic Mutations')},
      {id: 'StSM', shortLabel: gettextCatalog.getString('Structural Somatic Mutations')}
    ];

    var shortLabelMap = {};
    data.forEach(function(datatype) {
      shortLabelMap[datatype.id] = datatype.shortLabel;
    });


    this.translate = function(id) {
      return _.get (shortLabelMap, id, id);
    };

    this.precedence = function() {
      return _.pluck(data, 'id');
    };


    this.isPCAWGStudy = function(term) {
      return term === 'PCAWG';
    };

  });

})();

