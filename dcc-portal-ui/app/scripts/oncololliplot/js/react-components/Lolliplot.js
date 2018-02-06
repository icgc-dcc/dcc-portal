import React, { Component } from 'react';
import { func, object, array, number } from 'prop-types';
import { connect } from 'react-redux';
import { groupBy, pickBy } from 'lodash';
import LolliplotChart from './LolliplotChart';
import Toolbar from './Toolbar';
import { loadTranscript, updateChartState } from '../redux/OncoLolliplot/redux';

class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
  };

  componentDidMount() {
    const { loadTranscript, selectedTranscript, mutationService, filters } = this.props;

    // Async load of transcript
    loadTranscript({ selectedTranscript, mutationService, filters });
  }

  componentWillReceiveProps(nextProps) {
    // const newState = {
    //   ...this.state,
    //   lolliplotState: this.generateLolliplotState(
    //     nextProps,
    //     this.state.lolliplotState,
    //     this.state.mutations,
    //     this.state.selectedTranscript
    //   ),
    // };
    // this.setState(newState);
  }

  //
  /** Data Getters and Processing **/
  //

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

  _selectTranscript(value) {
    console.log(value);
  }

  _reset() {
    console.log('reset');
  }

  _renderLoading() {
    return <div>Loading ...</div>;
  }

  render() {
    const {
      lolliplotState,
      transcripts,
      selectedTranscript,
      displayWidth,
      loadTranscript,
      updateChartState,
      loading,
    } = this.props;

    return loading ? (
      this._renderLoading()
    ) : (
      <div>
        <Toolbar
          transcripts={transcripts}
          selectedTranscript={selectedTranscript}
          selectTranscript={loadTranscript}
          reset={this._reset.bind(this)}
        />
        <LolliplotChart
          {...lolliplotState}
          d3={this.props.d3}
          width={displayWidth}
          update={updateChartState}
          selectCollisions={this._selectCollisions.bind(this)}
        />
      </div>
    );
  }
}

const mapStateToProps = state => {
  return {
    ...state.oncoLolliplot,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    loadTranscript: ({ selectedTranscript, mutationService, filters }) =>
      loadTranscript(dispatch, { selectedTranscript, mutationService, filters }),
    updateChartState: state => dispatch(updateChartState(state)),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Lolliplot);
