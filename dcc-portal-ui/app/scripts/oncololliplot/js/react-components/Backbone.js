import React from 'react';
import { connect } from 'react-redux';
import { Backbone as OncoBackbone } from '@oncojs/react-lolliplot/dist/lib';
import { updateChartState, setTooltip, clearTooltip } from '../redux/OncoLolliplot/redux';

const Backbone = ({ min, max, d3, domainWidth, displayWidth, updateChartState, data, selectedTranscript, setTooltip, clearTooltip }) =>
  <OncoBackbone
    d3={d3}
    data={data}
    min={min}
    max={max}
    domainWidth={domainWidth}
    width={displayWidth}
    update={updateChartState}
    onProteinClick={d => {
      if (min === d.start && max === d.end) {
        updateChartState({
          min: 0,
          max: selectedTranscript.length_amino_acid,
        });
        clearTooltip();
      } else {
        updateChartState({ min: d.start, max: d.end });
        setTooltip(
          <span>
            <div>
              <b>{d.id}</b>
            </div>
            <div>{d.description}</div>
            <div>
              <b>Click to reset zoom</b>
            </div>
          </span>,
        );
      }
    }}
    onProteinMouseover={d => {
      setTooltip({
        type: 'backbone',
        data: {
          id: d.id,
          description: d.description
        }
      });
    }}
    onProteinMouseout={() => clearTooltip()}
  />;

const mapStateToProps = state => {
  return {
    min: state.oncoLolliplot.lolliplotState.min,
    max: state.oncoLolliplot.lolliplotState.max,
    domainWidth: state.oncoLolliplot.lolliplotState.domainWidth,
    displayWidth: state.oncoLolliplot.displayWidth,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    updateChartState: state => dispatch(updateChartState(state)),
    setTooltip: ({ type, data }) => dispatch(setTooltip({ type, data })),
    clearTooltip: () => dispatch(clearTooltip()),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Backbone);
