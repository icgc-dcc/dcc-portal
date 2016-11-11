import keyBy from 'lodash.keyby';
import memoize from 'memoizee';

const memoizeAsync = fn => memoize(fn, {promise: true});

class Repo {
  constructor(params) {
    Object.assign(this, ...params);
  }
};

export default function (Restangular) {
  "ngInject";
  
  const repos = [];
  const repoCodeMap = undefined;

  const fetchRepos = () => Restangular.one('/repositories').get();

  this.getRepos = memoizeAsync(() => fetchRepos());
  this.getRepoCodeMap = memoizeAsync(() => this.getRepos().then(repos => keyBy(repos, 'code')));

  fetchRepos();
};
