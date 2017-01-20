require('expose?jQuery!expose?$!jquery');
global._ = require('lodash');

// Polyfills
const includes = require('array-includes');
includes.shim();

// Angular Libs
require('expose?angular!angular');
require('angular-sanitize');
require('angular-animate');
require('angular-cookies');
require('angular-loading-bar');
require('restangular');
require('angular-ui-router');
require('ng-infinite-scroll');
require('angular-local-storage');
require('angularytics');
require('angular-ui-scrollpoint');
require('expose?marked!marked');
require('angular-marked');
require('angular-ui-bootstrap');
require('angularjs-toaster');
require('angular-drag-and-drop-lists');
require('expose?ZeroClipboard!zeroclipboard');
require('angular-gettext');
require('angular-xeditable');

// Other App Dependencies
require('expose?Highcharts!highcharts');
require('highcharts/modules/no-data-to-display.src.js');
require('d3/d3.min.js');
require('blob');
require('expose?saveAs!exports?saveAs!file-saver/FileSaver.min.js');
require('expose?X2JS!x2js');
require('bootstrap');

require('./ui/js/table2CSV.js');
require('../vendor/scripts/invariant.js');

// BAM iobio
window.Class = require('!class.js')();
require('expose?properRDP!imports?this=>window!exports?properRDP!../vendor/scripts/bamiobio/rdp.js');
require('imports?this=>window!../vendor/scripts/bamiobio/queue.min.js');
require('expose?donutD3!imports?this=>window&klass=>undefined!exports?donutD3!../vendor/scripts/bamiobio/donut.d3.js');
require('expose?histogramD3!imports?this=>window!exports?histogramD3!../vendor/scripts/bamiobio/histogram.d3.js');
require('expose?histogramViewFinderD3!imports?this=>window!exports?histogramViewFinderD3!../vendor/scripts/bamiobio/histogramViewFinder.d3.js');
require('expose?movingLineD3!imports?this=>window!exports?movingLineD3!../vendor/scripts/bamiobio/movingLine.d3.js');
require('expose?Bam!imports?this=>window!exports?Bam!../vendor/scripts/bamiobio/bam.iobio.js/bam.iobio.js');
require('imports?this=>window!../vendor/scripts/bamiobio/bam.iobio.js/bam.js');
require('imports?this=>window!../vendor/scripts/bamiobio/bam.iobio.js/bin.js');
require('expose?BinaryClient!exports?BinaryClient!binaryjs-client');
require('imports?this=>window!../vendor/scripts/bamiobio/flatui-checkbox.js');

// VCF iobio
require('expose?Vcfiobio!imports?this=>window&refIndex=>undefined!exports?Vcfiobio!../vendor/scripts/vcfiobio/vcf.iobio.js');
require('expose?histogramD3VCF!imports?this=>window!exports?histogramD3VCF!../vendor/scripts/vcfiobio/histogram.d3.vcf.js');
require('expose?barChartAltD3!imports?this=>window!exports?barChartAltD3!../vendor/scripts/vcfiobio/barChartAlt.d3.js');
require('expose?donutChooserD3!imports?this=>window&outerRadius=>undefined!exports?donutChooserD3!../vendor/scripts/vcfiobio/donutChooser.d3.js');
require('expose?groupedBarD3!imports?this=>window&colorList=>undefined!exports?groupedBarD3!../vendor/scripts/vcfiobio/groupedBar.d3.js');
require('expose?lineD3!imports?this=>window!exports?lineD3!../vendor/scripts/vcfiobio/line.d3.js');

// Highcharts Rounded Corners
require('../vendor/scripts/highcharts-rounded-corners.js');

// OncoGrid
require('script!../vendor/scripts/oncogrid/oncogrid-debug.js');
