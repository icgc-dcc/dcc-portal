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

import { emptyActionGenerator, payloadActionGenerator } from '../helpers';
import { generateLolliplotChartState, resetLolliplotChartState, separateOverlapping } from './services';

/*
* Actions
*/
const LOAD_TRANSCRIPT_START = 'oncoLolliplot/LOAD_TRANSCRIPT_START';
const LOAD_TRANSCRIPT_SUCCESS = 'oncoLolliplot/LOAD_TRANSCRIPT_SUCCESS';
const LOAD_TRANSCRIPT_FAILURE = 'oncoLolliplot/LOAD_TRANSCRIPT_FAILURE';

const RESIZE_WIDTH = 'oncoLolliplot/RESIZE_WIDTH';
const UPDATE_CHART_STATE = 'oncoLolliplot/UPDATE_CHART_STATE';
const SELECT_COLLISIONS = 'oncoLolliplot/SELECT_COLLISIONS';
const SET_TOOLTIP = 'oncoLolliplot/SET_TOOLTIP';
const CLEAR_TOOLTIP = 'oncoLolliplot/CLEAR_TOOLTIP';
const RESET = 'oncoLolliplot/RESET';

const fetchMutationsStart = emptyActionGenerator(LOAD_TRANSCRIPT_START);
const fetchMutationsSuccess = payloadActionGenerator(LOAD_TRANSCRIPT_SUCCESS);
const fetchMutationsError = payloadActionGenerator(LOAD_TRANSCRIPT_FAILURE);

/*
* Public non-async actions (mapped to component props)
*/
export const resizeWidth = payloadActionGenerator(RESIZE_WIDTH);
export const updateChartState = payloadActionGenerator(UPDATE_CHART_STATE);
export const selectCollisions = payloadActionGenerator(SELECT_COLLISIONS);
export const setTooltip = payloadActionGenerator(SET_TOOLTIP);
export const clearTooltip = emptyActionGenerator(CLEAR_TOOLTIP);
export const reset = emptyActionGenerator(RESET);

/*
* Public async thunk actions (mapped to component props)
*/
export function loadTranscript(dispatch, { selectedTranscript, mutationService, filters }) {
  dispatch(fetchMutationsStart());
  return mutationService(selectedTranscript.id)
    .then(mutations => {
      const { chartState, proteinFamilies } = generateLolliplotChartState(mutations.hits, selectedTranscript, filters);
      const payload = {
        selectedTranscript,
        mutations: mutations.hits,
        filters: filters,
        lolliplotState: chartState,
        proteinFamilies
      };
      dispatch(fetchMutationsSuccess(payload));
    })
    .catch(error => {
      dispatch(fetchMutationsError(error));
    });
}

/*
* Reducer
*/

export const _defaultState = {
  loading: true,
  mutations: [],
  mutationService: null,
  transcripts: [],
  selectedTranscript: {},
  lolliplotState: {},
  proteinFamilies: [],
  filters: {},
  displayWidth: 900,
  tooltip: null,
  error: null,
};

export const reducer = (state = _defaultState, action) => {
  switch (action.type) {
    case LOAD_TRANSCRIPT_START:
      return {
        ...state,
        loading: true,
      };
    case LOAD_TRANSCRIPT_SUCCESS:
      return {
        ...state,
        loading: false,
        mutations: action.payload.mutations,
        selectedTranscript: action.payload.selectedTranscript,
        filters: action.payload.filters,
        proteinFamilies: action.payload.proteinFamilies,
        lolliplotState: {
          ...state.lolliplotState,
          ...action.payload.lolliplotState,
        },
      };
    case LOAD_TRANSCRIPT_FAILURE:
      return {
        ...state,
        loading: false,
        error: action.payload,
      };
    case UPDATE_CHART_STATE:
      return {
        ...state,
        lolliplotState: {
          ...state.lolliplotState,
          ...action.payload,
        },
      };
    case RESIZE_WIDTH: {
      return {
        ...state,
        displayWidth: action.payload
      };
    }
    case SELECT_COLLISIONS:
      return {
        ...state,
        lolliplotState: {
          selectedCollisions: action.payload
        }
      };
    case SET_TOOLTIP:
      return {
        ...state,
        tooltip: action.payload
      };
    case CLEAR_TOOLTIP:
      return {
        ...state,
        tooltip: null
      }
    case RESET:
      return {
        ...state,
        lolliplotState: {
          ...state.lolliplotState,
          ...resetLolliplotChartState(state)
        },
      };
    default:
      return state;
  }
};
