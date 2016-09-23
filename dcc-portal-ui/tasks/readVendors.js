'use strict';

var fs = require('fs');
var path = require('path');

module.exports = function () {
  var vendorFile = fs.readFileSync(path.resolve(__dirname, '../app/scripts/vendor.js'), 'utf8');

  var regex = /require\('(.+)'\)/g;
  var arr;
  var results = [];

  while ((arr = regex.exec(vendorFile)) !== null) {
      results.push(arr[1]);
  }

  // console.log(results);
  // process.exit();

  return results;
};
