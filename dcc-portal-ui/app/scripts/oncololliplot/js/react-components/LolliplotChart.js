import React from 'react';
import { Lolliplot } from '@oncojs/react-lolliplot/dist/lib';
import { object, array, func, number } from 'prop-types';

const LolliplotChart = props => {
  const { collisions, selectCollisions } = props;

  const getPointColor = point => {
    const colourMapping = {
      low: '#4d4',
      high: '#d44',
      unknown: '#bbb',
    };
    return colourMapping[point.impact.toLowerCase()];
  };

  const onPointClick = d => {
    if (collisions[`${d.x},${d.y}`]) {
      selectCollisions(collisions[`${d.x},${d.y}`]);
    } else {
      window.location = `/mutations/${d.id}`;
    }
  };

  const onPointMouseover = ({ y: cases = 0, ...d }) => {
    if (collisions[`${d.x},${cases}`]) {
      _setTooltip('There are multiple mutations at this coordinate. Click to view.');
    } else {
      _setTooltip(
        <span>
          <div>
            <b>DNA Change: {d.genomic_dna_change}</b>
          </div>
          <div>ID: {d.id}</div>
          <div>AA Change: {d.aa_change}</div>
          <div># of Cases: {cases.toLocaleString()}</div>
          <div>VEP Impact: {d.impact}</div>
        </span>
      );
    }
  };

  const onPointMouseout = () => {
    _setTooltip(null);
  };

  const _setTooltip = tooltip => {
    console.log(tooltip);
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
  selectCollisions: func.isRequired,
  selectedCollisions: array,
  d3: object.isRequired,
  update: func.isRequired,
};

export default LolliplotChart;
