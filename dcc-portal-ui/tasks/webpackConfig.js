'use strict';

// var fs = require('fs');
var path = require('path');

// var Express = require('express');
var webpack = require('webpack');

// var config = {};

// var host = 'localhost';
// var port = 3001;

var webpackConfig = {
  context: path.resolve(__dirname, '../app/scripts'),
  entry: {
    app: './index.js',
    vendor: require('./readVendors')()
  },
  output: {
    path: path.resolve(__dirname, '../target/dist'),
    filename: 'bundle.js'
  },
  resolve: {
    modulesDirectories: ['bower_components']
  },
  node: {
    fs: 'empty'
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin(/* chunkName= */"vendor", /* filename= */"vendor.bundle.js")
  ]
};

module.exports = webpackConfig;