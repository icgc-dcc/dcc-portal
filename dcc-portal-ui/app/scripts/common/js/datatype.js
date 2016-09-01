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

  var module = angular.module('icgc.common.datatype', []);

  /**
   * Binds ICGC data types
   */
  module.service('DataType', function(gettextCatalog) {
    var data = [
      {id: 'donor', shortLabel: 'Clinical', label: gettextCatalog.getString('Clinical Data')},
      {id: 'clinical', shortLabel: 'Clinical', label: gettextCatalog.getString('Clinical Data')},
      {id: 'ssm', shortLabel: 'SSM', label: gettextCatalog.getString('Simple Somatic Mutation')},
      {id: 'sgv', shortLabel: 'SGV', label: gettextCatalog.getString('Simple Germline Variation')},
      {id: 'cnsm', shortLabel: 'CNSM', label: gettextCatalog.getString('Copy Number Somatic Mutation')},
      {id: 'stsm', shortLabel: 'STSM', label: gettextCatalog.getString('Structural Somatic Mutations')},
      {id: 'exp_array', shortLabel: 'EXP-A', label: gettextCatalog.getString('Array-based Gene Expression')},
      {id: 'exp_seq', shortLabel: 'EXP-S', label: gettextCatalog.getString('Sequencing-based Gene Expression')},
      {id: 'pexp', shortLabel: 'PEXP', label: gettextCatalog.getString('Protein Expression')},
      {id: 'mirna_seq', shortLabel: 'miRNA-S', label: gettextCatalog.getString('Sequence-based miRNA Expression')},
      {id: 'jcn', shortLabel: 'JCN', label: gettextCatalog.getString('Exon Junctions')},
      {id: 'meth_array', shortLabel: 'METH-A', label: gettextCatalog.getString('Array-based DNA Methylation')},
      {id: 'meth_seq', shortLabel: 'METH-S', label: gettextCatalog.getString('Sequencing-based DNA Methylation')},
      {id: 'aligned reads', shortLabel:'Aligned Reads', label: gettextCatalog.getString('Aligned Sequencing Reads')},
      {id: 'stgv' , shortLabel: 'StGV', label: gettextCatalog.getString('Structural Germline Variants')}
    ];

    var shortLabelMap = {}, labelMap = {};
    data.forEach(function(datatype) {
      shortLabelMap[datatype.id] = datatype.shortLabel;
      labelMap[datatype.id] = datatype.label;
    });

    this.get = function() {
      return data;
    };

    this.precedence = function() {
      return _.pluck(data, 'id');
    };

    this.translate = function(id) {
      return shortLabelMap[id]; 
    };

    this.tooltip = function(id) {
      return labelMap[id.toLowerCase()];
    };

  });

})();


