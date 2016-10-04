var genomeViewerExports = require('./exports');

var genomeViewerImplicitGlobals = require('./implicit-globals');
var importsParams = genomeViewerImplicitGlobals.map(x => x + '=>undefined').join('&');
var exportsParams = genomeViewerExports.join('&');
var requireString = `imports?${importsParams}!exports?${exportsParams}!../vendor/scripts/genome-viewer/genome-viewer.js`;

module.exports = requireString;
