import React from 'react';
import { string } from 'prop-types';

const BackboneTooltip = ({ style, id, description }) =>
  (<div className="lolliplot-tooltip top in fade" style={style}>
    <div className="tooltip-arrow"></div>
    <div className="tooltip-inner">
      {id}<br />
      {description}<br />
      <strong>Click to Zoom</strong>
    </div>
  </div>);

BackboneTooltip.displayName = 'BackboneTooltip';

BackboneTooltip.propTypes = {
  id: string.isRequired,
  description: string.isRequired,
};

export default BackboneTooltip;