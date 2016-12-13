var _ = require('lodash');

var webpackProdConfig = require('./config/webpack.config.prod.js');

var webpackTestConfig = Object.assign({}, webpackProdConfig, {
  entry: {},
  devtool: 'cheap-source-map',
  // Note: plugins that are currently unsupported by karma-webpack need to be removed
  // https://github.com/webpack/karma-webpack/issues/149
  plugins: webpackProdConfig.plugins.filter(x => !_.includes([
    'CommonsChunkPlugin',
    // Note: not able to exclude test files from uglification due to https://github.com/webpack/webpack/issues/1079
    'UglifyJsPlugin',
    ], x.constructor.name))
});

module.exports = function(config){
    config.set({
    //  root path location that will be used to resolve all relative paths in files and exclude sections, should be the root of your project
    // basePath : '../',
    basePath : '.',

    // files to include, ordered by dependencies
    files : [
      './node_modules/es6-shim/es6-shim.js',
      'app/scripts/vendor.js',
      'app/scripts/index.js',
      // === Test, mock files ===
      'app/bower_components/angular-mocks/angular-mocks.js',
      {pattern: 'test/unit/**/*.js', watched: false},
    ],

    preprocessors: {
      'app/scripts/vendor.js': ['webpack'],
      'app/scripts/index.js': ['webpack'],
      'test/unit/**/*.js': ['webpack'],
    },

    // files to exclude
    exclude : [
        'app/lib/angular/angular-loader.js'
      , 'app/lib/angular/*.min.js'
      , 'app/lib/angular/angular-scenario.js'
    ],

    singleRun: true,

    // testing framework, be sure to install the karma plugin
    frameworks: ['jasmine'],

    // browsers to test against, be sure to install the correct karma browser launcher plugin
    // browsers : ['Chrome', 'PhantomJS', 'Firefox'],
    // browsers : ['Chrome'],
    browsers : ['PhantomJS'],

    // progress is the default reporter
    // reporters: ['progress'],
    reporters: ['spec'],

    webpack: webpackTestConfig,

    webpackMiddleware: {
      // webpack-dev-middleware configuration
      // i. e.
      stats: 'errors-only'
    }
})}
