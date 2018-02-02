import React, { Component } from 'react';
import { object, func, number } from 'prop-types';
import { Lolliplot as ReactLolliplot } from '@oncojs/react-lolliplot/dist/lib';

export default class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
    transcript: object.isRequired,
    locationService: func.isRequired,
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
    const { mutations, transcript, displayWidth } = props;
    const data = this.processData(mutations, transcript);
    return {
      min: 0,
      max: this.getTranscriptMax(transcript),
      domainWidth: this.processDomainWidth(transcript),
      width: displayWidth,
      highlightedPointId: this.processHighlightedPointId(data),
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

  processHighlightedPointId(data) {
    console.log('processHighlightedPointId');
    return 0;
  }

  processData(mutations, transcript) {
    const transcriptId = transcript.id;
    const result = mutations.hits
      .filter(x => {
        const co = x.transcripts.find(c => c.id === transcriptId);
        return co && this._getAaStart(co.consequence.aaMutation);
      })
      .map(mutation => this._mapToResult(mutation, transcriptId));

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

  getPointColor() {
    console.log('getPointColor');
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
