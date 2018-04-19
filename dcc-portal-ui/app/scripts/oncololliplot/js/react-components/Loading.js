import React from 'react';

const Loading = () => (
  <div className="empty">
    <h3 style={{ display: 'inline-block' }}>
      <i className="icon-spinner icon-spin" />Loading Protein...
    </h3>
  </div>
);

Loading.displayName = 'Loading';

export default Loading;
