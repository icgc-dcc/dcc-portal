import React, { Component } from 'react';
import { object } from 'prop-types';
import { Lolliplot as ReactLolliplot } from '@oncojs/react-lolliplot/dist/lib';

export default class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
    data: object.isRequired,
  };

  constructor(props) {
    super(props);
    console.log('NEW LOLLIPLOT');
    this.state = this.initState(props.data);
  }

  initState(data) {
    return {
      min: 0,
      max: this.processMax(data),
      domainWidth: this.processDomainWidth(data),
      width: this.processWidth(data),
      highlightedPointId: this.processHighlightedPointId(data),
      data: this.processData(data),
      collisions: this.processCollisions(data),
    };
  }

  // State Management
  stateReducer(state, action, payload) {
    return state;
  }

  // Data Processing
  processMax(data) {
    console.log('processMax');
    return 766;
  }

  processDomainWidth(data) {
    console.log('processDomainWidth');
    return 766;
  }

  processWidth(data) {
    console.log('processWidth');
    return 900;
  }

  processHighlightedPointId(data) {
    console.log('processHighlightedPointId');
    return 0;
  }

  processData(data) {
    console.log('processData');
    // Array of
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
    return [];
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
