import React from 'react';
import { object, array, func } from 'prop-types';

const Toolbar = ({ transcripts, selectedTranscript, selectTranscript, reset }) => {};

Toolbar.displayName = 'Toolbar';

Toolbar.propTypes = {
  transcripts: array.isRequired,
  selectedTranscript: object.isRequired,
  selectTranscript: func.isRequired,
  reset: func.isRequired,
};
