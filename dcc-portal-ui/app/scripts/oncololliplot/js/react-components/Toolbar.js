import React from 'react';
import { object, array, func } from 'prop-types';

const Toolbar = ({ transcripts, selectedTranscript, selectTranscript, reset }) => {
  return (
    <div>
      <strong>Transcript:</strong>
      <select onChange={e => selectTranscript(e.target.value)} value={selectedTranscript.id}>
        {transcripts.map(t => (
          <option key={t.id} value={t.id}>
            {t.name} ({t.lengthAminoAcid} aa)
          </option>
        ))}
      </select>
    </div>
  );
};

Toolbar.displayName = 'Toolbar';

Toolbar.propTypes = {
  transcripts: array.isRequired,
  selectedTranscript: object.isRequired,
  selectTranscript: func.isRequired,
  reset: func.isRequired,
};

export default Toolbar;
