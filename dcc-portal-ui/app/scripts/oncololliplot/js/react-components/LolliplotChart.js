import React from 'react';
import { connect } from 'react-redux';
import { Lolliplot } from '@oncojs/react-lolliplot/dist/lib';
import { object, array, func, number } from 'prop-types';
import { setTooltip, clearTooltip } from '../redux/OncoLolliplot/redux';

const LolliplotChart = props => {
  const { collisions, setTooltip, clearTooltip } = props;

  const getPointColor = point => {
    const colourMapping = {
      low: '#4d4',
      high: '#d44',
      unknown: '#bbb',
    };
    return colourMapping[point.impact.toLowerCase()];
  };

  const onPointClick = d => {
    const clickedPointCollisions = collisions[`${d.x},${d.y}`];

    if (clickedPointCollisions) {
      const mutations = JSON.stringify(clickedPointCollisions.map(mutation => mutation.id));
      const query = `{"mutation":{"id":{"is":${mutations}}}}`;
      window.location = `/search/m?filters=${encodeURIComponent(query)}`;
    } else {
      window.location = `/mutations/${d.id}`;
    }
  };

  const onPointMouseover = ({ y: cases = 0, ...d }) => {
    if (collisions[`${d.x},${cases}`]) {
      setTooltip({
        type: 'chartMulti',
        data: {},
      });
    } else {
      setTooltip({
        type: 'chartSingle',
        data: {
          id: d.id,
          numDonors: cases.toLocaleString(),
          aaChange: d.aa_change,
          functionalImpact: d.impact,
        },
      });
    }
  };

  const onPointMouseout = () => {
    clearTooltip();
  };

  return (
    <Lolliplot
      {...props}
      getPointColor={getPointColor}
      onPointClick={onPointClick}
      onPointMouseover={onPointMouseover}
      onPointMouseout={onPointMouseout}
    />
  );
};

LolliplotChart.displayName = 'LolliplotChart';

LolliplotChart.propTypes = {
  min: number.isRequired,
  max: number.isRequired,
  domainWidth: number.isRequired,
  width: number.isRequired,
  data: array.isRequired,
  collisions: object.isRequired,
  d3: object.isRequired,
  update: func.isRequired,
};

const mapDispatchToProps = dispatch => {
  return {
    setTooltip: ({ type, data }) => dispatch(setTooltip({ type, data })),
    clearTooltip: () => dispatch(clearTooltip()),
  };
};

export default connect(null, mapDispatchToProps)(LolliplotChart);
