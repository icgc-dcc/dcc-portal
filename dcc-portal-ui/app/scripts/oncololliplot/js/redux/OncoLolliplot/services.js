/*
 * Copyright 2018(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import { groupBy, pickBy } from 'lodash';

/**
 * Generates lolliplot chart component state
 * @param {array} mutations - mutations for selected transcript
 * @param {array} transcript - selected transcript
 * @param {array} filters - active filters (search facets)
 * @returns {object} - new state
 */
export function generateLolliplotChartState(mutations, transcript, filters) {
  const data = processData(mutations, transcript, filters);
  const proteinFamilies = processProteins(transcript);
  const domainWidth = processDomainWidth(transcript);

  return {
    chartState: {
      min: 0,
      max: domainWidth,
      domainWidth,
      data,
      collisions: {}, // no collisions for now
      // collisions: processCollisions(data),
    },
    proteinFamilies,
  };
}

export function resetLolliplotChartState(state) {
  const domainWidth = processDomainWidth(state.selectedTranscript);

  return {
    min: 0,
    max: domainWidth,
    domainWidth,
  };
}

//
/** Data Processing **/
//

/**
 * Get the donain width for the transcript (used for max value on load)
 * @param {object} transcript - transcript we are getting from
 * @returns {number} - max length
 */
function processDomainWidth(transcript) {
  return transcript.lengthAminoAcid;
}

/**
 * Using provided params, compute data for lolliplot
 * @param {array} mutations - mutations to be filtered on
 * @param {object} transcript - selected transcript
 * @param {object} filters - facets selected from outside component
 * @returns {array} - data for lolliplot component
 */
function processData(mutations, transcript, filters) {
  const transcriptId = transcript.id;
  const result = mutations
    .filter(x => {
      const co = x.transcripts.find(c => c.id === transcriptId);
      return co && getAaStart(co.consequence.aaMutation);
    })
    .map(mutation => mapToResult(mutation, transcriptId))
    .filter(m => filterResults(m, filters));

  return result;
}

/**
 * Process overlapping data points
 * @param {array} data - data in lolliplot component
 * @returns {object} - collisions
 */
function processCollisions(data) {
  return pickBy(groupBy(data, d => `${d.x},${d.y}`), d => d.length > 1);
}

/**
   * Maps to object for protein viewer
   * @param {object} - source mutation
   * @param {string} - selected transcript
   * @returns {object} - example return object:
    {
      aa_change: 'K117N',
      genomic_dna_change: 'T>G',
      id: 'MU153141',
      impact: 'High',
      x: 117,
      y: 5,
    }
   */
function mapToResult(mutation, transcriptId) {
  const transcript = mutation.transcripts.find(c => c.id === transcriptId);

  return {
    aa_change: transcript.consequence.aaMutation,
    // consequence: consequence.transcript.consequence_type.replace('_variant', ''), // missing
    genomic_dna_change: mutation.mutation,
    id: mutation.id,
    impact: transcript.consequence.functionalImpact,
    // polyphen_impact: (consequence.transcript.annotation || {}).polyphen_impact, // missing
    // polyphen_score: (consequence.transcript.annotation || {}).polyphen_score, // missing
    // sift_impact: (consequence.transcript.annotation || {}).sift_impact, // missing
    // sift_score: (consequence.transcript.annotation || {}).sift_score, // missing
    x: getAaStart(transcript.consequence.aaMutation),
    y: mutation.affectedDonorCountTotal,
  };
}

/**
 * Filters results based on valid start and criteria of filter (fixtures)
 * @param {object} m - mutation
 * @param {object} filters - active search fixtures
 * @returns {boolean} - true/false value for Array.prototype.filter()
 */
function filterResults(m, filters) {
  if (m.start < 0) return false;

  const fiFilter = _.get(filters, 'mutation.functionalImpact.is');
  if (fiFilter) {
    if (fiFilter.indexOf(m.impact) >= 0) {
      return true;
    }
  } else {
    return true;
  }
  return false;
}

/**
 * Extract mutation start
 * @param {object} m - mutation
 * @returns {string} - starting position
 */
function getAaStart(m) {
  return m.replace(/[^\d]/g, '');
}

/** Process proteins for backbone
 * @param {object} transcript - selected transcript that contains domains
 * @returns {object} - processed protein domains with colour attached
 */
function processProteins(transcript) {
  const colors = (transcript.domains || []).reduce(
    (acc, protein, i) => ({
      ...acc,
      [protein.hitName]: `hsl(${(i * 100) % 360}, 60%, 60%)`,
    }),
    {}
  );

  return (transcript.domains || []).map(protein => ({
    id: protein.hitName,
    start: protein.start,
    end: protein.end,
    description: protein.description,
    getProteinColor: () => colors[protein.hitName],
  }));
}
