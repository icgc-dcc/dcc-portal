import React from 'react';
import { connect } from 'react-redux';
import { Backbone as OncoBackbone } from '@oncojs/react-lolliplot/dist/lib';
import { updateChartState } from '../redux/OncoLolliplot/redux';

const setTooltip = tooltip => {
  console.log(tooltip);
}

const Backbone = ({ min, max, d3, domainWidth, displayWidth, updateChartState, data, selectedTranscript }) =>
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
        setTooltip(null);
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
      setTooltip(
        <span>
          <div>
            <b>{d.id}</b>
          </div>
          <div>{d.description}</div>
          {min === d.start &&
            max === d.end && (
              <div>
                <b>Click to reset zoom</b>
              </div>
            )}
          {(min !== d.start || max !== d.end) && (
            <div>
              <b>Click to zoom</b>
            </div>
          )}
        </span>,
      );
    }}
    onProteinMouseout={() => setTooltip(null)}
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
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Backbone);
