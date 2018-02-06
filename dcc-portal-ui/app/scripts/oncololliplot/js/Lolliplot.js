import React, { Component } from 'react';
import { func, object, array, number } from 'prop-types';
import { groupBy, pickBy } from 'lodash';
import LolliplotChart from './react-components/LolliplotChart';
import Toolbar from './react-components/Toolbar';

export default class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
    transcripts: array.isRequired,
    filters: object.isRequired,
    getMutations: func.isRequired,
    displayWidth: number.isRequired,
  };

  constructor(props) {
    super(props);

    this.defaultState = {
      loading: true,
      mutations: [],
      transcripts: props.transcripts,
      selectedTranscript: props.transcripts[0],
      lolliplotState: {},
    };

    // Init State
    this.state = this.defaultState;
  }

  componentDidMount() {
    // Async load of transcript
    this.loadTranscript(this.state.selectedTranscript.id);
  }

  componentWillReceiveProps(nextProps) {
    const newState = {
      ...this.state,
      lolliplotState: this.generateLolliplotState(
        nextProps,
        this.state.lolliplotState,
        this.state.mutations,
        this.state.selectedTranscript
      ),
    };
    this.setState(newState);
  }

  /**
   * Regenerate state with new transcript
   * @param {string} transcriptId
   */
  loadTranscript(transcriptId) {
    const selectedTranscript = this.state.transcripts.filter(t => t.id === transcriptId)[0];

    // Set loading to true
    this._setStatePromise({
      ...this.state,
      selectedTranscript,
      loading: true,
    }).then(newState => {
      // Load new transcript mutations
      this.props.getMutations(selectedTranscript.id).then(mutations => {
        this.setState({
          ...newState,
          loading: false,
          mutations: mutations.hits,
          lolliplotState: this.generateLolliplotState(
            this.props,
            newState.lolliplotState,
            mutations.hits,
            selectedTranscript
          ),
        });
      });
    });
  }

  /**
   * Generate component state using props passed in from OncoLolliplotController (ng)
   * @param {object} props
   * @param {object} oldState - if calling a second time and we may want to maintain state
   * @param {array} mutations - mutations for selected transcript
   * @returns {object} - new state
   */
  generateLolliplotState(props, oldState = {}, mutations, transcript) {
    const { filters, displayWidth } = props;
    const data = this.processData(mutations, transcript, filters);

    return {
      min: oldState.min || 0,
      max: oldState.max || this.processDomainWidth(transcript),
      domainWidth: this.processDomainWidth(transcript),
      width: displayWidth,
      data,
      collisions: this.processCollisions(data),
      selectedCollisions: oldState.selectedCollisions || null,
    };
  }

  //
  /** Data Getters and Processing **/
  //

  /**
   * Get the donain width for the transcript (used for max value on load)
   * @param {object} transcript - transcript we are getting from
   * @returns {number} - max length
   */
  processDomainWidth(transcript) {
    return transcript.lengthAminoAcid;
  }

  /**
   * Using provided params, compute data for lolliplot
   * @param {array} mutations - mutations to be filtered on
   * @param {object} transcript - selected transcript
   * @param {object} filters - facets selected from outside component
   * @returns {array} - data for lolliplot component
   */
  processData(mutations, transcript, filters) {
    const transcriptId = transcript.id;
    const result = mutations
      .filter(x => {
        const co = x.transcripts.find(c => c.id === transcriptId);
        return co && this._getAaStart(co.consequence.aaMutation);
      })
      .map(mutation => this._mapToResult(mutation, transcriptId))
      .filter(m => this._filterResults(m, filters));

    return result;
  }

  /**
   * Process overlapping data points
   * @param {array} data - data in lolliplot component
   * @returns {object} - collisions
   */
  processCollisions(data) {
    return pickBy(groupBy(data, d => `${d.x},${d.y}`), d => d.length > 1);
  }

  //
  /** Private/Protected Methods **/
  //

  _setStatePromise(state) {
    return new Promise(resolve => {
      this.setState(state, () => resolve(state));
    });
  }

  /**
   * Update function passed to Lolliplot chart to update chart based on user actions
   * @param {object} payload - new state
   */
  _updateLolliplotChartState(payload) {
    this.setState({
      ...this.state,
      lolliplotState: {
        ...this.state.lolliplotState,
        ...payload,
      },
    });
  }

  /**
   * Set's selected collisions state
   * @param {object} collisions - new state
   */
  _selectCollisions(collisions) {
    this.setState({
      ...this.state,
      selectedCollisions: collisions,
    });
  }

  _selectTranscript(value) {}

  _reset() {
    console.log(reset);
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
  _mapToResult(mutation, transcriptId) {
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
      x: this._getAaStart(transcript.consequence.aaMutation),
      y: mutation.affectedDonorCountTotal,
    };
  }

  /**
   * Filters results based on valid start and criteria of filter (fixtures)
   * @param {object} m - mutation
   * @param {object} filters - active search fixtures
   * @returns {boolean} - true/false value for Array.prototype.filter()
   */
  _filterResults(m, filters) {
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
  _getAaStart(m) {
    return m.replace(/[^\d]/g, '');
  }

  _renderLoading() {
    return <div>Loading ...</div>;
  }

  render() {
    const { lolliplotState, transcripts, selectedTranscript } = this.state;
    return this.state.loading ? (
      this._renderLoading()
    ) : (
      <div>
        <Toolbar
          transcripts={transcripts}
          selectedTranscript={selectedTranscript}
          selectTranscript={this.loadTranscript.bind(this)}
          reset={this._reset.bind(this)}
        />
        <LolliplotChart
          {...lolliplotState}
          d3={this.props.d3}
          update={this._updateLolliplotChartState.bind(this)}
          selectCollisions={this._selectCollisions.bind(this)}
        />
      </div>
    );
  }
}
