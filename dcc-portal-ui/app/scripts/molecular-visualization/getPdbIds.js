const queryPDB = uniprotId => fetch('https://www.rcsb.org/pdb/rest/search/?req=browser&sortfield=Resolution', {
  method: 'POST',
  headers: {
    'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
  },
  body: `<orgPdbQuery>
    <queryType>org.pdb.query.simple.UpAccessionIdQuery</queryType>
    <accessionIdList>${uniprotId}</accessionIdList>
  </orgPdbQuery>`,
}).then(response => response.text());

const getPdbIds = uniprotId =>
  queryPDB(uniprotId)
    .then(response => {
      return response.trim() === 'null'
        ? []
        : response
          .replace(/:(?:\d+)\n/g, '\n')
          .split('\n');
    });

export default getPdbIds;