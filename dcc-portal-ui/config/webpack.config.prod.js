var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var paths = require('./paths');

module.exports = {
  bail: true,
  devtool: 'eval',
  entry: {
    app: [require.resolve('./polyfills'), path.join(paths.appSrc, 'scripts/index')],
    vendor: path.join(paths.appSrc, 'scripts/vendor'),
  },
  output: {
    path: paths.appBuild,
    filename: 'static/js/[name].[chunkhash:8].js',
    chunkFilename: 'static/js/[name].[chunkhash:8].chunk.js',
    publicPath: '/',
  },
  resolve: {
    extensions: ['', '.js', '.json'],
  },
  resolveLoader: {
    root: paths.ownNodeModules,
    moduleTemplates: ['*-loader'],
  },
  module: {
    preLoaders: [
      {
        test: /\.js$/,
        loader: 'eslint',
        include: paths.appSrc,
      },
    ],
    noParse: /node_modules\/lodash\/lodash\.js/,
    loaders: [
      {
        test: /\.html$/,
        loader: 'raw',
      },
      {
        test: /index.html$/,
        loader: 'string-replace',
        query: {
          multiple: [
            {
              search: "'COPYRIGHT_YEAR'",
              replace: new Date().getUTCFullYear(),
            },
          ],
        },
      },
      {
        test: /\.js$/,
        include: paths.appSrc,
        exclude: [paths.internalVendorModules],
        loader: 'ng-annotate',
      },
      {
        test: /\.js$/,
        include: paths.appSrc,
        exclude: [paths.internalVendorModules],
        loader: 'babel',
        query: require('./babel.prod'),
      },
      {
        test: /\.scss$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: ExtractTextPlugin.extract('style', 'css?-autoprefixer!postcss!sass'),
      },
      {
        test: /\.json$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'json',
      },
      {
        test: /\.(swf|jpg|png|gif|eot|svg|ttf|woff|woff2)(\?.*)?$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'file',
        query: {
          name: 'static/media/[name].[hash:8].[ext]',
        },
      },
      {
        test: /\.(mp4|webm)(\?.*)?$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'url',
        query: {
          limit: 10000,
          name: 'static/media/[name].[hash:8].[ext]',
        },
      },
    ],
  },
  eslint: {
    // TODO: consider separate config for production,
    // e.g. to enable no-console and no-debugger only in prod.
    configFile: path.join(__dirname, 'eslint.js'),
    useEslintrc: false,
  },
  postcss: function() {
    return [require('autoprefixer')];
  },
  plugins: [
    new CopyWebpackPlugin([
      { from: 'app/_redirects', to: paths.appBuild },
      { from: 'app/robots.txt', to: paths.appBuild + '/robots.txt' },
      { from: 'app/sitemap.xml', to: paths.appBuild + '/sitemap.xml' },
      { from: 'app/opensearch.xml', to: paths.appBuild + '/opensearch.xml' },
      { from: 'app/favicon.ico', to: paths.appBuild + '/favicon.ico' },
      { from: 'app/styles/fonts', to: paths.appBuild + '/styles/fonts' },
      { from: 'app/styles/images', to: paths.appBuild + '/styles/images' },
      { from: 'vendor/styles', to: paths.appBuild + '/vendor/styles' },
      { from: 'app/styles/icgc-icons.css', to: paths.appBuild + '/styles/icgc-icons.css' },
      { from: paths.cgpBackup, to: paths.appBuild + '/cgp' }, // backup of CGP nodes, DACO retirement. Ticket#712

      // TODO: this is only necessary due to templateUrls, we should compile templates
      { from: 'app/scripts', to: paths.appBuild + '/scripts' },
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
        minifyURLs: true,
      },
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production'),
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false,
        screw_ie8: true,
        conditionals: true,
        unused: true,
        comparisons: true,
        sequences: true,
        dead_code: true,
        evaluate: true,
        if_return: true,
        join_vars: true,
      },
      output: {
        comments: false,
      },
    }),
    new webpack.optimize.CommonsChunkPlugin(
      /* chunkName= */ 'vendor',
      /* filename= */ 'vendor.bundle.js'
    ),
    new ExtractTextPlugin('static/css/[name].[contenthash:8].css'),
  ],
};
