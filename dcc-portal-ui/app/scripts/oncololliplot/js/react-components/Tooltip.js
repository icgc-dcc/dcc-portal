import React from 'react';
import { string } from 'prop-types';

const Tooltip = ({ style, id, numDonors, aaChange, functionalImpact }) =>
  (<div className="lolliplot-tooltip right in fade" style={style}>
    <div className="tooltip-arrow"></div>
    <div className="tooltip-inner">
      Mutation ID: {id}<br />
      Number of donors: {numDonors}<br />
      Amino acid change: {aaChange}<br />
      Functional Impact: {functionalImpact}
    </div>
  </div>);

Tooltip.displayName = 'Tooltip';

Tooltip.propTypes = {
  id: string.isRequired,
  numDonors: string.isRequired,
  aaChange: string.isRequired,
  functionalImpact: string.isRequired,
};

export default Tooltip;