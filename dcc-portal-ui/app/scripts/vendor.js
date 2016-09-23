global.jQuery = require('jquery');
global._ = require('lodash');

// Angular Libs
require('angular/angular.js');
require('angular-sanitize/angular-sanitize.js');
require('angular-animate/angular-animate.js');
require('angular-cookies/angular-cookies.js');
require('angular-loading-bar/build/loading-bar.js');
require('restangular/dist/restangular.js');
require('angular-ui-router/release/angular-ui-router.js');
require('ngInfiniteScroll/build/ng-infinite-scroll.js');
require('angular-local-storage/dist/angular-local-storage.js');
require('angularytics/dist/angularytics.js');
require('angular-ui-utils/scrollfix.js');
require('angular-markdown-directive/markdown.js');
require('angular-lodash/angular-lodash.js');
require('angular-bootstrap/ui-bootstrap-tpls.min.js');
require('angularjs-toaster/toaster.js');
require('angular-drag-and-drop-lists/angular-drag-and-drop-lists.js');
require('zeroclipboard/dist/ZeroClipboard.js');
require('angular-gettext/dist/angular-gettext.min.js');


// Other App Dependencies
window.Highcharts = require('highcharts/highcharts.js');
require('highcharts/modules/no-data-to-display.src.js');
require('d3/d3.min.js');
require('Blob/Blob.js');
require('FileSaver/FileSaver.min.js');
global.showdown = require('showdown');
require('x2js/xml2json.js');
require('bootstrap');

require('./ui/js/table2CSV.js');
require('../vendor/scripts/invariant.js');


// BAM iobio
// require('../vendor/scripts/bamiobio/class.js');
window.Class = require('class.js')();
require('../vendor/scripts/bamiobio/rdp.js');
require('../vendor/scripts/bamiobio/queue.min.js');
require('../vendor/scripts/bamiobio/donut.d3.js');
require('../vendor/scripts/bamiobio/histogram.d3.js');
require('../vendor/scripts/bamiobio/histogramViewFinder.d3.js');
require('../vendor/scripts/bamiobio/movingLine.d3.js');
require('../vendor/scripts/bamiobio/bam.iobio.js/bam.iobio.js');
require('../vendor/scripts/bamiobio/bam.iobio.js/bam.js');
require('../vendor/scripts/bamiobio/bam.iobio.js/bin.js');
require('../vendor/scripts/bamiobio/binary.js');
require('../vendor/scripts/bamiobio/flatui-checkbox.js');


// VCF iobio
require('../vendor/scripts/vcfiobio/vcf.iobio.js');
require('../vendor/scripts/vcfiobio/histogram.d3.vcf.js');
require('../vendor/scripts/vcfiobio/barChartAlt.d3.js');
require('../vendor/scripts/vcfiobio/donutChooser.d3.js');
require('../vendor/scripts/vcfiobio/groupedBar.d3.js');
require('../vendor/scripts/vcfiobio/line.d3.js');

// Genome Viewer
window.Backbone = require('backbone');
// require('../vendor/scripts/backbone.min-1.0.js');
require('script!../vendor/scripts/bootstrap-slider-2.0.0.js');
require('script!../vendor/scripts/genome-viewer/vendor/qtip2/jquery.qtip.min.js');
window.URI = require('urijs');
// require('../vendor/scripts/genome-viewer/vendor/uri.js/src/URI.min.js');

// Genome Viewer Config and Initialization
require('../vendor/scripts/genome-viewer/gv-config.js');
require('../vendor/scripts/genome-viewer/genome-viewer.js');

// Highcharts Rounded Corners
require('../vendor/scripts/highcharts-rounded-corners.js');

// Custom ICGC Genome Viewer Components
require('../vendor/scripts/genome-viewer/icgc-gene-adapter.js');
require('../vendor/scripts/genome-viewer/icgc-gene-track.js');
require('../vendor/scripts/genome-viewer/icgc-mutation-adapter.js');
require('../vendor/scripts/genome-viewer/icgc-mutation-track.js');
require('../vendor/scripts/genome-viewer/icgc-navigation-bar.js');

// OncoGrid
require('../vendor/scripts/oncogrid/oncogrid-debug.js');
