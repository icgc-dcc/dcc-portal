import React from 'react';

const Overlapping = () =>
  (<div>
    <i
      className="fa fa-warning"
      style={{
        color: 'rgb(215, 175, 33)',
      }}
    />
    <span style={{ fontSize: '0.8em' }}>
      Some overlapping domains are not shown by default.&nbsp;
      <div onClick={() => console.log('expand')}>
        Click here to show / hide them.
      </div>
    </span>
  </div>);

Overlapping.displayName = 'Overlapping';

export default Overlapping;