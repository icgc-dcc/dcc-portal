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

  var module = angular.module('icgc.common.external', []);

  /**
   * Provides links to external websites for
   * - Genes
   * - Gene Sets
   * - Gene Transcripts
   * - Project experimental type
   *
   * This is largely for URLs that needs to be dynamically constructed, tend to change over time,
   * or used in multiple places.
   * No sanity checks are performed on the IDs, we assume that they are valid
   */
  module.service('ExternalLinks', function () {

    // Gene Sets related links
    this.geneSetReactomeDiagram = function(diagramId, reactomeId) {
      return 'http://www.reactome.org/PathwayBrowser/#DIAGRAM=' + diagramId + '&ID=' + reactomeId;
    };
    this.geneSetReactome = function(reactomeId) {
      return 'http://www.reactome.org/content/detail/' + reactomeId;
    };
    this.geneSetAmigo = function(goTermId) {
      return 'http://amigo.geneontology.org/amigo/term/' + goTermId;
    };
    this.geneSetEBI = function(goTermId) {
      return 'http://www.ebi.ac.uk/QuickGO/GTerm?id=' + goTermId;
    };
    this.geneSetSanger = function() {
      return 'http://cancer.sanger.ac.uk/cancergenome/projects/census/';
    };
    this.geneSetGeneOntology = function() {
      return 'http://geneontology.org/';
    };


    // Gene related links
    this.geneHGNC = function(hgncId) {
      return 'http://www.genenames.org/data/hgnc_data.php?hgnc_id=' + hgncId;
    };
    this.geneEnsembl = function(geneId) {
      return 'http://feb2014.archive.ensembl.org/Homo_sapiens/Gene/Summary?db=core;g=' + geneId;
    };
    this.geneCOSMIC = function(geneSymbol) {
      return 'http://cancer.sanger.ac.uk/cosmic/gene/analysis?ln=' + geneSymbol;
    };
    this.geneEntrez = function(entrezId) {
      return 'http://www.ncbi.nlm.nih.gov/gene/' + entrezId;
    };
    this.geneOMIM = function(omimId) {
      return 'http://omim.org/entry/' + omimId;
    };
    this.geneUniProt = function(uniProtId) {
      return 'http://www.uniprot.org/uniprot/' + uniProtId;
    };
    this.geneTranscript = function(transcriptId) {
      return 'http://feb2014.archive.ensembl.org/Homo_sapiens/Transcript/Summary?db=core;t=' + transcriptId;
    };


    // Project related links
    this.projectICGC = function(icgcId) {
      return 'http://icgc.org/node/' + icgcId;
    };
    this.projectSNP = function() {
      return 'http://www.ncbi.nlm.nih.gov/SNP/';
    };
    this.projectGDCLegacy = () => 'https://gdc-portal.nci.nih.gov/legacy-archive';
    this.projectGEO = function() {
      return 'http://www.ncbi.nlm.nih.gov/geo/';
    };
    this.projectEGA = function() {
      return 'https://ega-archive.org/';
    };
    this.projectGDCActive = () => 'https://gdc-portal.nci.nih.gov';
  });


})();
