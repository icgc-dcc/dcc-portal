import React, { Component } from 'react';
import { object } from 'prop-types';
import { connect } from 'react-redux';
import LolliplotChart from './LolliplotChart';
import Toolbar from './Toolbar';
import Tooltip from './Tooltip';
import Backbone from './Backbone';
import Minimap from './Minimap';
import Loading from './Loading';
import { updateChartState } from '../redux/OncoLolliplot/redux';

class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      cursorPos: {
        x: 0,
        y: 0,
      },
    };
  }

  _onMouseMove(e) {
    this.setState({
      ...this.state,
      cursorPos: { x: e.nativeEvent.clientX, y: e.nativeEvent.clientY },
    });
  }

  render() {
    const { d3, lolliplotState, displayWidth, updateChartState, tooltip, loading } = this.props;

    const { cursorPos } = this.state;

    return (
      <div onMouseMove={this._onMouseMove.bind(this)}>
        <h3>Protein</h3>
        <Toolbar />
        {loading ? (
          <Loading />
        ) : (
          <div>
            <LolliplotChart
              {...lolliplotState}
              d3={d3}
              width={displayWidth}
              update={updateChartState}
            />
            <Backbone d3={d3} />
            <Minimap d3={d3} />
          </div>
        )}
        {tooltip ? <Tooltip cursorPos={cursorPos} /> : null}
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
    updateChartState: state => dispatch(updateChartState(state)),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Lolliplot);
