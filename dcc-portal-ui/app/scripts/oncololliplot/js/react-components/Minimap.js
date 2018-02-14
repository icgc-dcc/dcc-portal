import React from 'react';
import { connect } from 'react-redux';
import { Minimap as OncoMinimap } from '@oncojs/react-lolliplot/dist/lib';
import { updateChartState } from '../redux/OncoLolliplot/redux';

const Minimap = ({
  min,
  max,
  d3,
  domainWidth,
  displayWidth,
  updateChartState,
  mutations,
  proteins,
}) => (
  <OncoMinimap
    min={min}
    max={max}
    d3={d3}
    domainWidth={domainWidth}
    width={displayWidth}
    update={updateChartState}
    data={{ mutations, proteins }}
  />
);

Minimap.displayName = 'Minimap';

const mapStateToProps = state => {
  return {
    min: state.oncoLolliplot.lolliplotState.min,
    max: state.oncoLolliplot.lolliplotState.max,
    domainWidth: state.oncoLolliplot.lolliplotState.domainWidth,
    displayWidth: state.oncoLolliplot.displayWidth,
    mutations: state.oncoLolliplot.lolliplotState.data,
    proteins: state.oncoLolliplot.proteinFamilies,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    updateChartState: state => dispatch(updateChartState(state)),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Minimap);
