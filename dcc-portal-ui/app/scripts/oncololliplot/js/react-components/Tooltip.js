import React from 'react';
import { connect } from 'react-redux';
import { object } from 'prop-types';
import ChartTooltip from './ChartTooltip';
import ChartTooltipMulti from './ChartTooltipMulti';
import BackboneTooltip from './BackboneTooltip';

const Tooltip = ({ tooltip, cursorPos: { x = 0, y = 0 } }) => {
  const style = {
    position: 'fixed',
    display: 'block',
    top: y,
    left: x,
  };

  switch (tooltip.type) {
    case 'chartSingle':
      return <ChartTooltip style={style} {...tooltip.data} />;
    case 'chartMulti':
      return <ChartTooltipMulti style={style} />;
    case 'backbone':
      return <BackboneTooltip style={style} {...tooltip.data} />;
    default:
      return null;
  }
};

Tooltip.displayName = 'Tooltip';

Tooltip.propTypes = {
  tooltip: object.isRequired,
  cursorPos: object.isRequired,
};

const mapStateToProps = state => {
  return {
    tooltip: state.oncoLolliplot.tooltip,
  };
};

export default connect(mapStateToProps, null)(Tooltip);
