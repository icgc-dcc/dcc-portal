import React, { Component } from 'react';
import { object, number } from 'prop-types';
import { groupBy, pickBy } from 'lodash';
import { Lolliplot as ReactLolliplot } from '@oncojs/react-lolliplot/dist/lib';

export default class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
    transcript: object.isRequired,
    filters: object.isRequired,
    mutations: object.isRequired,
    displayWidth: number.isRequired,
  };

  constructor(props) {
    super(props);

    // State
    this.state = this.stateFromProps(props);
  }

  componentWillReceiveProps(nextProps) {
    this.setState(this.stateFromProps(nextProps, this.state));
  }

  stateFromProps(props, oldState = {}) {
    const { mutations, transcript, displayWidth, filters } = props;
    const data = this.processData(mutations, transcript, filters);
    return {
      min: 0,
      max: this.getTranscriptMax(transcript),
      domainWidth: this.processDomainWidth(transcript),
      width: displayWidth,
      data,
      collisions: this.processCollisions(data),
      selectedCollisions: oldState.selectedCollisions || null,
    };
  }

  // Data Getters and Processing
  getTranscriptMax(transcript) {
    return transcript.lengthAminoAcid;
  }

  processDomainWidth(transcript) {
    return transcript.lengthAminoAcid;
  }

  processData(mutations, transcript, filters) {
    const transcriptId = transcript.id;
    const result = mutations.hits
      .filter(x => {
        const co = x.transcripts.find(c => c.id === transcriptId);
        return co && this._getAaStart(co.consequence.aaMutation);
      })
      .map(mutation => this._mapToResult(mutation, transcriptId))
      .filter(m => this._filterResults(m, filters));

    return result;
  }

  processCollisions(data) {
    return pickBy(groupBy(data, d => `${d.x},${d.y}`), d => d.length > 1);
  }

  // Bound Methods used by Component
  update(payload) {
    this.setState({
      ...this.state,
      ...payload,
    });
  }

  getPointColor(point) {
    const colourMapping = {
      low: '#4d4',
      high: '#d44',
      unknown: '#bbb',
    };
    return colourMapping[point.impact.toLowerCase()];
  }

  onPointClick(d) {
    const collisions = this.state.collisions;
    if (collisions[`${d.x},${d.y}`]) {
      this._selectCollisions(collisions[`${d.x},${d.y}`]);
    } else {
      window.location = `/mutations/${d.id}`;
    }
  }

  onPointMouseover({ y: cases = 0, ...d }) {
    if (this.state.collisions[`${d.x},${cases}`]) {
      this._setTooltip('There are multiple mutations at this coordinate. Click to view.');
    } else {
      this._setTooltip(
        <span>
          <div>
            <b>DNA Change: {d.genomic_dna_change}</b>
          </div>
          <div>ID: {d.id}</div>
          <div>AA Change: {d.aa_change}</div>
          <div># of Cases: {cases.toLocaleString()}</div>
          <div>VEP Impact: {d.impact}</div>
        </span>
      );
    }
  }

  onPointMouseout() {
    this._setTooltip(null);
  }

  // Private Methods

  /**
   * Maps to object for protein viewer
   */
  _mapToResult(mutation, transcriptId) {
    const transcript = mutation.transcripts.find(c => c.id === transcriptId);

    // Example Return
    // {
    //   aa_change: 'V600E';
    //   consequence: 'missense';
    //   genomic_dna_change: 'chr7:g.140753336A>T';
    //   id: '84aef48f-31e6-52e4-8e05-7d5b9ab15087';
    //   impact: 'MODERATE';
    //   polyphen_impact: 'probably_damaging';
    //   polyphen_score: 0.967;
    //   sift_impact: 'deleterious';
    //   sift_score: 0;
    //   x: 600;
    //   y: 565;
    // }

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

  _getAaStart(aaMutation) {
    return aaMutation.replace(/[^\d]/g, '');
  }

  _setTooltip(tooltip) {
    console.log(tooltip);
  }

  _selectCollisions(collisions) {
    this.setState({
      ...this.state,
      selectedCollisions: collisions,
    });
  }

  render() {
    return (
      <ReactLolliplot
        {...this.state}
        d3={this.props.d3}
        update={this.update.bind(this)}
        getPointColor={this.getPointColor.bind(this)}
        onPointClick={this.onPointClick.bind(this)}
        onPointMouseover={this.onPointMouseover.bind(this)}
        onPointMouseout={this.onPointMouseout.bind(this)}
      />
    );
  }
}
