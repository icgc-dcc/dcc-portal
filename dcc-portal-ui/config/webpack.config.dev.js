var path = require('path');
var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
var paths = require('./paths');

module.exports = {
  devtool: 'source-map',
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
        include: paths.appSrc,
      }
    ],
    loaders: [
      {
        test: /\.js$/,
        include: paths.appSrc,
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
