import React from 'react';
import { string } from 'prop-types';

const ChartTooltip = ({ style, id, numDonors, aaChange, functionalImpact }) =>
  (<div className="lolliplot-tooltip right in fade" style={style}>
    <div className="tooltip-arrow"></div>
    <div className="tooltip-inner">
      Mutation ID: {id}<br />
      Number of donors: {numDonors}<br />
      Amino acid change: {aaChange}<br />
      Functional Impact: {functionalImpact}
    </div>
  </div>);

ChartTooltip.displayName = 'ChartTooltip';

ChartTooltip.propTypes = {
  id: string.isRequired,
  numDonors: string.isRequired,
  aaChange: string.isRequired,
  functionalImpact: string.isRequired,
};

export default ChartTooltip;