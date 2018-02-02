import React, { Component } from 'react';
import { object, number } from 'prop-types';
import { Lolliplot as ReactLolliplot } from '@oncojs/react-lolliplot/dist/lib';

export default class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
    transcript: object.isRequired,
    locationService: object.isRequired,
    mutations: object.isRequired,
    displayWidth: number.isRequired,
  };

  constructor(props) {
    super(props);

    // State
    this.state = this.initState(props);

    // Binding
  }

  initState(props) {
    const { mutations, transcript, displayWidth, locationService } = props;
    const filters = locationService.filters();
    const data = this.processData(mutations, transcript, filters);
    return {
      min: 0,
      max: this.getTranscriptMax(transcript),
      domainWidth: this.processDomainWidth(transcript),
      width: displayWidth,
      data,
      collisions: this.processCollisions(data),
    };
  }

  // State Management
  stateReducer(state, action, payload) {
    return state;
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
    console.log('processCollisions');
    return [];
  }

  // Bound Methods used by Component
  update(state) {
    console.log('update');
    return {
      ...state,
    };
  }

  getPointColor(point) {
    const colourMapping = {
      low: '#4d4',
      high: '#d44',
      unknown: '#bbb',
    };
    return colourMapping[point.impact.toLowerCase()];
  }

  onPointClick() {
    console.log('onPointClick');
  }

  onPointMouseover() {
    console.log('onPointMouseover');
  }

  onPointMouseout() {
    console.log('onPointMouseout');
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

  render() {
    return (
      <ReactLolliplot
        {...this.state}
        d3={this.props.d3}
        getPointColor={this.getPointColor.bind(this)}
        onPointClick={this.onPointClick.bind(this)}
        onPointMouseover={this.onPointMouseover.bind(this)}
        onPointMouseout={this.onPointMouseout.bind(this)}
      />
    );
  }
}
