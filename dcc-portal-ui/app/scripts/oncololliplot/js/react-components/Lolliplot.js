import React, { Component } from 'react';
import { object } from 'prop-types';
import { connect } from 'react-redux';
import LolliplotChart from './LolliplotChart';
import Toolbar from './Toolbar';
import Tooltip from './Tooltip';
import Backbone from './Backbone';
import Overlapping from './Overlapping';
import { updateChartState, selectCollisions } from '../redux/OncoLolliplot/redux';

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
        y: 0
      }
    };
  }

  _onMouseMove(e) {
    this.setState({
      ...this.state,
      cursorPos: { x: e.nativeEvent.offsetX, y: e.nativeEvent.offsetY }
    });
  }

  _renderLoading() {
    return <div>Loading ...</div>;
  }

  render() {
    const {
      d3,
      lolliplotState,
      proteinFamilies,
      displayWidth,
      updateChartState,
      selectCollisions,
      tooltip,
      loading,
    } = this.props;

    const { cursorPos } = this.state;

    return (
      <div onMouseMove={this._onMouseMove.bind(this)} style={{ position: 'relative' }}>
        <Toolbar />
        {loading ?
          this._renderLoading()
          : <div>
            <LolliplotChart
              {...lolliplotState}
              d3={d3}
              width={displayWidth}
              update={updateChartState}
              selectCollisions={selectCollisions}
            />
            <Backbone
              d3={d3}
              data={proteinFamilies}
              style={{ position: 'relative' }}
            />
          </div>}
        {tooltip ? <Tooltip cursorPos={cursorPos} /> : null}
      </div>);
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
    selectCollisions: collision => dispatch(selectCollisions(collision)),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Lolliplot);
