import React from 'react';

const ChartTooltipMulti = ({ style }) =>
  (<div className="lolliplot-tooltip right in fade" style={style}>
    <div className="tooltip-arrow"></div>
    <div className="tooltip-inner">
      There are multiple mutations at this coordinate. Click to view.
    </div>
  </div>);

ChartTooltipMulti.displayName = 'ChartTooltipMulti';

export default ChartTooltipMulti;