require('expose?jQuery!expose?$!jquery');
require('expose?_!../bower_components/lodash/lodash.js');

// Angular Libs
require('angular/angular.js');
require('angular-sanitize/angular-sanitize.js');
require('angular-animate/angular-animate.js');
require('angular-cookies/angular-cookies.js');
require('angular-loading-bar/build/loading-bar.js');
require('restangular');
require('angular-ui-router/release/angular-ui-router.js');
require('ngInfiniteScroll/build/ng-infinite-scroll.js');
require('angular-local-storage/dist/angular-local-storage.js');
require('angularytics/dist/angularytics.js');
require('angular-ui-utils/scrollfix.js');
require('expose?marked!marked');
require('angular-marked');
require('angular-lodash/angular-lodash.js');
require('angular-bootstrap/ui-bootstrap-tpls.min.js');
require('angularjs-toaster/toaster.js');
require('angular-drag-and-drop-lists/angular-drag-and-drop-lists.js');
require('expose?ZeroClipboard!zeroclipboard/dist/ZeroClipboard.js');
require('angular-gettext/dist/angular-gettext.min.js');
require('angular-xeditable');

// Other App Dependencies
require('expose?Highcharts!highcharts');
require('highcharts/modules/no-data-to-display.src.js');
require('d3/d3.min.js');
require('Blob/Blob.js');
require('expose?saveAs!exports?saveAs!FileSaver/FileSaver.min.js');
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
