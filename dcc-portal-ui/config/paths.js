var path = require('path');

function resolveApp(relativePath) {
  return path.resolve(relativePath);
}

module.exports = {
  appBuild: resolveApp('target/classes/app'),
  appHtml: resolveApp('app/index.html'),
  appFavicon: resolveApp('favicon.ico'),
  appPackageJson: resolveApp('package.json'),
  appSrc: resolveApp('app'),
  appNodeModules: resolveApp('node_modules'),
  cgpBackup: resolveApp('app/assets/cgp'),
  internalVendorModules: resolveApp('app/vendor'),
  ownNodeModules: resolveApp('node_modules')
};
