/* eslint-disable quotes */

module.exports = {
  releaseDate: 'April 25th, 2016',
  dataVersion: 2,
  downloadEnabled: true,
  authEnabled: true,
  maxNumberOfHits: 20000,
  maxMultiplier: 3,
  mirror: {
    enabled: false,
    countryCode: '',
    name: '',
    countryLocation: '',
  },
  featureFlags: {
    AUTH_TOKEN: true,
    ICGC_CLOUD: true,
    VCF_IOBIO: true,
    SOFTWARE_PAGE: true,
  },
  jupyter: {
    enabled: true,
    url: 'https://jupyterhub.cancercollaboratory.org',
  },
};
