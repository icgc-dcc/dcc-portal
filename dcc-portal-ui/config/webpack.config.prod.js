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
    preLoaders: [
      {
        test: /\.js$/,
        loader: 'eslint',
        include: paths.appSrc
      }
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
              search: '\'COPYRIGHT_YEAR\'',
              replace: new Date().getUTCFullYear()
            },
          ]
        }
      },
      {
        test: /\.js$/,
        include: paths.appSrc,
        exclude: [paths.bowerModules, paths.internalVendorModules],
        loader: 'ng-annotate',
      },
      {
        test: /\.js$/,
        include: paths.appSrc,
        exclude: [paths.bowerModules, paths.internalVendorModules],
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
  postcss: function() {
    return [
      require('autoprefixer'),
    ];
  },
  plugins: [
    new CopyWebpackPlugin([
      {from: 'app/favicon.ico', to: paths.appBuild + '/favicon.ico'},
      {from: 'app/styles/fonts', to: paths.appBuild + '/styles/fonts'},
      {from: 'app/styles/images', to: paths.appBuild + '/styles/images'},
      {from: 'vendor/styles', to: paths.appBuild + '/vendor/styles'},
      {from: 'app/styles/icgc-icons.css', to: paths.appBuild + '/styles/icgc-icons.css'},

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
    new webpack.optimize.CommonsChunkPlugin(/* chunkName= */"vendor", /* filename= */"vendor.bundle.js"),
    new ExtractTextPlugin('static/css/[name].[contenthash:8].css'),
  ]
};
