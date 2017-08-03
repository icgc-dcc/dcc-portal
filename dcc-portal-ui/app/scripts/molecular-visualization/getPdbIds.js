const queryPdb = uniprotId => fetch('https://www.rcsb.org/pdb/rest/search/?req=browser&sortfield=Resolution', {
  method: 'POST',
  headers: {
    'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
  },
  body: `<orgPdbQuery>
    <queryType>org.pdb.query.simple.UpAccessionIdQuery</queryType>
    <accessionIdList>${uniprotId}</accessionIdList>
  </orgPdbQuery>`,
}).then(response => response.text());

const isValidPdbCode = string =>
  string.match(/^[\d][\d\w]{3}$/) || string.match(/^pdb_[\d]{5}[\d\w]{3}$/);

const getPdbIds = uniprotId =>
  queryPdb(uniprotId)
    .then(response => {
      return response.trim() === 'null'
        ? []
        : response
          .replace(/:(?:\d+)\n/g, '\n')
          .split('\n')
          .filter(isValidPdbCode);
    });

export default getPdbIds;