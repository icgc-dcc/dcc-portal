import React from 'react';
import { connect } from 'react-redux';
import { object } from 'prop-types';
import ChartTooltip from './ChartTooltip';
import ChartTooltipMulti from './ChartTooltipMulti';
import BackboneTooltip from './BackboneTooltip';

const Tooltip = ({ tooltip, cursorPos: { x = 0, y = 0 } }) => {
  const baseStyle = {
    position: 'absolute',
    display: 'block',
  }

  switch (tooltip.type) {
    case 'chartSingle':
      return <ChartTooltip
        style={{
          ...baseStyle,
          top: y - 21,
          left: x + 12
        }}
        {...tooltip.data}
      />;
    case 'chartMulti':
      return <ChartTooltipMulti
        style={{
          ...baseStyle,
          top: y + 10,
          left: x + 12
        }}
      />;
    case 'backbone':
      return <BackboneTooltip
        style={{
          ...baseStyle,
          top: y + 192, // temp
          left: x - 72 // temp
        }}
        {...tooltip.data}
      />;
    default:
      return null;
  }
}

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