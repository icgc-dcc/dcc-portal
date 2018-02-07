import React from 'react';

const TooltipMulti = ({ style }) =>
  (<div className="lolliplot-tooltip right in fade" style={style}>
    <div className="tooltip-arrow"></div>
    <div className="tooltip-inner">
      There are multiple mutations at this coordinate. Click to view.
    </div>
  </div>);

TooltipMulti.displayName = 'TooltipMulti';

export default TooltipMulti;