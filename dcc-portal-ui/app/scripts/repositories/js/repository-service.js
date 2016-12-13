import keyBy from 'lodash.keyby';
import memoize from 'memoizee';

const memoizeAsync = fn => memoize(fn, {promise: true});

export default function (Restangular) {
  'ngInject';
  const fetchRepos = () => Restangular.one('/repositories').get().then(x => x.plain());
  this.getRepos = memoizeAsync(() => fetchRepos());
  this.getRepoCodeMap = memoizeAsync(() => this.getRepos().then(repos => keyBy(repos, 'code')));

  this.isCloudRepo = ({storage}) => _.includes(['icgc', 's3'], storage);

  fetchRepos();
};
