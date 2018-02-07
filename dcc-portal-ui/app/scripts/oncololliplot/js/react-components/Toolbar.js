import React from 'react';
import { connect } from 'react-redux';
import { object, array, func } from 'prop-types';
import { loadTranscript, reset } from '../redux/OncoLolliplot/redux';


const Toolbar = ({ loadTranscript, reset, transcripts, selectedTranscript, mutationService, filters }) => {

  const selectTranscript = id => {
    const t = transcripts.filter(t => t.id === id)[0];
    loadTranscript({ selectedTranscript: t, mutationService, filters });
  }

  return (
    <div className="lolliplot-toolbar">
      <strong>Transcript: </strong>
      <select onChange={e => selectTranscript(e.target.value)} value={selectedTranscript.id}>
        {transcripts.map(t => (
          <option key={t.id} value={t.id}>
            {t.name} ({t.lengthAminoAcid} aa)
          </option>
        ))}
      </select>
      <button onClick={reset}>Reset</button>
    </div>
  );
};

Toolbar.displayName = 'Toolbar';

Toolbar.propTypes = {
  transcripts: array.isRequired,
  selectedTranscript: object.isRequired,
  loadTranscript: func.isRequired,
  reset: func.isRequired,
};

const mapStateToProps = state => {
  return {
    transcripts: state.oncoLolliplot.transcripts,
    selectedTranscript: state.oncoLolliplot.selectedTranscript,
    mutationService: state.oncoLolliplot.mutationService,
    filters: state.oncoLolliplot.filters
  };
};

const mapDispatchToProps = dispatch => {
  return {
    loadTranscript: ({ selectedTranscript, mutationService, filters }) =>
      loadTranscript(dispatch, { selectedTranscript, mutationService, filters }),
    reset: () => dispatch(reset()),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Toolbar);
