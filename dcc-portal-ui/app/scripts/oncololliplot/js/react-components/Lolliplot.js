import React, { Component } from 'react';
import { object } from 'prop-types';
import { connect } from 'react-redux';
import LolliplotChart from './LolliplotChart';
import Toolbar from './Toolbar';
import Tooltip from './Tooltip';
import TooltipMulti from './TooltipMulti';
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

  _renderTooltip() {
    const { tooltip } = this.props;
    const { cursorPos: { x = 0, y = 0 } } = this.state;

    const baseStyle = {
      position: 'absolute',
      display: 'block',
    }

    switch (tooltip.type) {
      case 'single':
        return <Tooltip
          style={{
            ...baseStyle,
            top: y - 21,
            left: x + 12
          }}
          {...tooltip.data}
        />;
      case 'multi':
        return <TooltipMulti
          style={{
            ...baseStyle,
            top: y + 10,
            left: x + 12
          }}
        />;
      default:
        return null;
    }
  }

  _renderLoading() {
    return <div>Loading ...</div>;
  }

  render() {
    const {
      d3,
      lolliplotState,
      proteinTracks,
      displayWidth,
      updateChartState,
      selectCollisions,
      tooltip,
      loading,
    } = this.props;

    // Temp
    const expandDomains = true;

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
            <div style={{ marginTop: '-20px' }}>
              {proteinTracks.length > 1 ? <Overlapping /> : null}
              {proteinTracks.slice(0, expandDomains ? Infinity : 1).map((trackData, i) =>
                <Backbone
                  key={i}
                  d3={d3}
                  data={trackData}
                />
              )}
            </div>
            {tooltip ? this._renderTooltip() : null}
          </div>}
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
