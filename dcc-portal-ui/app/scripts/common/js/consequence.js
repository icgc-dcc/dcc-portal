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

  var module = angular.module('icgc.common.consequence', []);

  /**
   * Binds mutation consequence mappings from SnpEff
   */
  module.service('Consequence', function() {
    var dataOrdered = [
      {id: 'frameshift_variant', label: 'Frameshift'},
      {id: 'missense_variant', label: 'Missense'},
      {id: 'start_lost', label: 'Start Lost'},
      {id: 'initiator_codon_variant', label: 'Initiator Codon'},
      {id: 'stop_gained', label: 'Stop Gained'},
      {id: 'stop_lost', label: 'Stop Lost'},
      {id: 'exon_loss_variant', label: 'Exon Loss'},
      {id: 'splice_acceptor_variant', label: 'Splice Acceptor'},
      {id: 'splice_donor_variant', label: 'Splice Donor'},
      {id: 'splice_region_variant', label: 'Splice Region'},
      {id: 'rare_amino_acid_variant', label: 'Rare Amino Acid'},
      {id: '5_prime_UTR_premature_start_codon_gain_variant', label: 'Start Gained'},
      {id: 'coding_sequence_variant', label: 'Coding Sequence'},
      {id: '5_prime_UTR_truncation', label: '5 UTR Truncation'},
      {id: '3_prime_UTR_truncation', label: '3 UTR Truncation'},
      {id: 'non_canonical_start_codon', label: 'Non ATG Start'},
      {id: 'disruptive_inframe_deletion', label: 'Disruptive Inframe Deletion'},
      {id: 'inframe_deletion', label: 'Inframe Deletion'},
      {id: 'disruptive_inframe_insertion', label: 'Disruptive Inframe Insertion'},
      {id: 'inframe_insertion', label: 'Inframe Insertion'},
      {id: 'regulatory_region_variant', label: 'Regulatory Region'},
      {id: 'miRNA', label: 'miRNA'},
      {id: 'conserved_intron_variant', label: 'Conserved Intron'},
      {id: 'conserved_intergenic_variant', label: 'Conserved Intergenic'},
      {id: '5_prime_UTR_variant', label: '5 UTR'},
      {id: 'upstream_gene_variant', label: 'Upstream'},
      {id: 'synonymous_variant', label: 'Synonymous'},
      {id: 'stop_retained_variant', label: 'Stop Retained'},
      {id: '3_prime_UTR_variant', label: '3 UTR'},
      {id: 'exon_variant', label: 'Exon'},
      {id: 'downstream_gene_variant', label: 'Downstream'},
      {id: 'intron_variant', label: 'Intron'},
      {id: 'transcript_variant', label: 'Transcript'},
      {id: 'gene_variant', label: 'Gene'},
      {id: 'intragenic_variant', label: 'Intragenic'},
      {id: 'intergenic_region', label: 'Intergenic'},
      {id: 'chromosome', label: 'Chromosome'},
      {id: '_missing', label: 'Missing'}
    ];

    var map = {};
    dataOrdered.forEach(function(consequence) {
      map[consequence.id] = consequence.label;
    });


    this.precedence = function() {
      return _.pluck(dataOrdered, 'id');
    };

    this.translate = function(id) {
      return map[id]; 
    };

    this.tooltip = function(id) {
      return 'SO term: ' + id;
    };



  });

})();
