var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
var paths = require('./paths');

module.exports = {
  devtool: process.env.SOURCE_MAP ? process.env.SOURCE_MAP : 'eval-source-map',
  cache: true,
  context: path.resolve(__dirname, '../app/scripts'),
  entry: {
    app: [
      require.resolve('webpack-dev-server/client') + '?/',
      require.resolve('webpack/hot/only-dev-server'),
      require.resolve('./polyfills'),
      path.join(paths.appSrc, 'scripts/index')
    ],
    vendor: path.join(paths.appSrc, 'scripts/vendor'),
  },
  output: {
    // Next line is not used in dev but WebpackDevServer crashes without it:
    path: paths.appBuild,
    pathinfo: true,
    filename: 'static/js/[name].js',
    publicPath: 'http://local.dcc.icgc.org:9000/',
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
        include: paths.appSrc,
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
              search: '\<portal-settings\>\<\/portal-settings\>',
              replace: `<script>window.ICGC_SETTINGS = ${JSON.stringify(require('./ICGC_SETTINGS.dev.js'))}</script>`
            },
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
        loaders: ['babel?' + JSON.stringify(require('./babel.dev'))],
      },
      {
        test: /\.css$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'style!css'
      },
      {
        test: /\.scss$/,
        include: [paths.appSrc, paths.appNodeModules],
        loaders: [
          'style',
          'css?sourceMap&-autoprefixer',
          'postcss',
          'sass?sourceMap'
        ]
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
          name: 'static/media/[path][name].[ext]'
        }
      },
      {
        test: /\.(mp4|webm)(\?.*)?$/,
        include: [paths.appSrc, paths.appNodeModules],
        loader: 'url',
        query: {
          limit: 10000,
          name: 'static/media/[name].[ext]'
        }
      }
    ]
  },
  eslint: {
    configFile: path.join(__dirname, 'eslint.js'),
    useEslintrc: false
  },
  postcss: function() {
    return [
      require('autoprefixer'),
    ];
  },
  plugins: [
    new HtmlWebpackPlugin({
      inject: true,
      template: paths.appHtml,
      favicon: paths.appFavicon,
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': '"development"',
      'process.env.GENOME_VIEWER_REQUIRE_STRING': JSON.stringify(require('./shims/genome-viewer')),
    }),
    // Note: only CSS is currently hot reloaded
    new webpack.HotModuleReplacementPlugin(),
    new CaseSensitivePathsPlugin(),
    new webpack.optimize.CommonsChunkPlugin(/* chunkName= */"vendor", /* filename= */"vendor.bundle.js"),
  ]
};
