var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var paths = require('./paths');

module.exports = {
  bail: true,
  devtool: 'source-map',
  entry: {
    app: [
      require.resolve('./polyfills'),
      path.join(paths.appSrc, 'scripts/index')
    ],
    vendor: path.join(paths.appSrc, 'scripts/vendor'),
  },
  output: {
    path: paths.appBuild,
    filename: 'static/js/[name].[chunkhash:8].js',
    chunkFilename: 'static/js/[name].[chunkhash:8].chunk.js',
    publicPath: '/'
  },
  resolve: {
    extensions: ['', '.js', '.json'],
    modulesDirectories: ['node_modules', 'bower_components'],
  },
  resolveLoader: {
    root: paths.ownNodeModules,
    moduleTemplates: ['*-loader']
  },
  module: {
    // preLoaders: [
    //   {
    //     test: /\.js$/,
    //     loader: 'eslint',
    //     include: paths.appSrc
    //   }
    // ],
    loaders: [
      {
        test: /\.js$/,
        include: paths.appSrc,
        loader: 'ng-annotate',
      },
      {
        test: /\.js$/,
        include: paths.appSrc,
        loader: 'babel',
        query: require('./babel.prod'),
      },
      {
        test: /\.css$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: ExtractTextPlugin.extract('style', 'css!sass')
      },
      {
        test: /\.scss$/,
        include: [paths.appSrc, paths.appNodeModules],
        loaders: ['style', 'css?sourceMap', 'sass?sourceMap']
      },
      {
        test: /\.json$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'json'
      },
      {
        test: /\.(jpg|png|gif|eot|svg|ttf|woff|woff2)(\?.*)?$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'file',
        query: {
          name: 'static/media/[name].[hash:8].[ext]'
        }
      },
      {
        test: /\.(mp4|webm)(\?.*)?$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'url',
        query: {
          limit: 10000,
          name: 'static/media/[name].[hash:8].[ext]'
        }
      }
    ]
  },
  eslint: {
    // TODO: consider separate config for production,
    // e.g. to enable no-console and no-debugger only in prod.
    configFile: path.join(__dirname, 'eslint.js'),
    useEslintrc: false
  },
  plugins: [
    new CopyWebpackPlugin([
      {from: 'app/styles/fonts', to: paths.appBuild + '/styles/fonts'},
      {from: 'app/styles/images', to: paths.appBuild + '/styles/images'},

      // TODO: this is only necessary due to templateUrls, we should compile templates
      {from: 'app/scripts', to: paths.appBuild + '/scripts'},
    ]),
    new HtmlWebpackPlugin({
      inject: true,
      template: paths.appHtml,
      favicon: paths.appFavicon,
      minify: {
        removeComments: true,
        collapseWhitespace: true,
        removeRedundantAttributes: true,
        useShortDoctype: true,
        removeEmptyAttributes: true,
        removeStyleLinkTypeAttributes: true,
        keepClosingSlash: true,
        minifyJS: true,
        minifyCSS: true,
        minifyURLs: true
      }
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': '"production"',
      'process.env.GENOME_VIEWER_REQUIRE_STRING': JSON.stringify(require('./shims/genome-viewer')),
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        screw_ie8: true,
        warnings: false
      },
      mangle: {
        screw_ie8: true
      },
      output: {
        comments: false,
        screw_ie8: true
      }
    }),
    new ExtractTextPlugin('static/css/[name].[contenthash:8].css'),
  ]
};
