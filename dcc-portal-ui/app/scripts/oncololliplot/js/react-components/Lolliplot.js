import React, { Component } from 'react';
import { object } from 'prop-types';
import { connect } from 'react-redux';
import LolliplotChart from './LolliplotChart';
import Toolbar from './Toolbar';
import { updateChartState } from '../redux/OncoLolliplot/redux';

class Lolliplot extends Component {
  static displayName = 'Lolliplot';

  static propTypes = {
    d3: object.isRequired,
  };

  _renderLoading() {
    return <div>Loading ...</div>;
  }

  render() {
    const {
      d3,
      lolliplotState,
      displayWidth,
      updateChartState,
      selectCollisions,
      loading,
    } = this.props;

    return (
      <div>
        <Toolbar />
        {loading ?
          this._renderLoading()
          : <LolliplotChart
            {...lolliplotState}
            d3={d3}
            width={displayWidth}
            update={updateChartState}
            selectCollisions={selectCollisions}
          />}
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
    selectCollisions: collision => dispatch(selectCollisions(collision)),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Lolliplot);
