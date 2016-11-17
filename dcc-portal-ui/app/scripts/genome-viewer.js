// Genome Viewer Dependencies
require('expose?Backbone!backbone');
require('script!../vendor/scripts/bootstrap-slider-2.0.0.js');
require('script!../vendor/scripts/genome-viewer/vendor/qtip2/jquery.qtip.min.js');
require('expose?URI!urijs');

// Genome Viewer Config and Initialization
// require('script!../vendor/scripts/genome-viewer/gv-config.js');
Object.assign(window, require('imports?key=>undefined!exports?CODON_CONFIG&GENE_BIOTYPE_COLORS&SNP_BIOTYPE_COLORS&SEQUENCE_COLORS&FEATURE_TYPES!../vendor/scripts/genome-viewer/gv-config.js'));
Object.assign(window, require(process.env.GENOME_VIEWER_REQUIRE_STRING));

// Custom ICGC Genome Viewer Components
require('expose?IcgcGeneAdapter!exports?IcgcGeneAdapter!../vendor/scripts/genome-viewer/icgc-gene-adapter.js');
require('expose?IcgcGeneTrack!exports?IcgcGeneTrack!../vendor/scripts/genome-viewer/icgc-gene-track.js');
require('expose?IcgcMutationAdapter!exports?IcgcMutationAdapter!../vendor/scripts/genome-viewer/icgc-mutation-adapter.js');
require('expose?IcgcMutationTrack!exports?IcgcMutationTrack!../vendor/scripts/genome-viewer/icgc-mutation-track.js');
require('expose?IcgcNavigationBar!exports?IcgcNavigationBar!../vendor/scripts/genome-viewer/icgc-navigation-bar.js');
