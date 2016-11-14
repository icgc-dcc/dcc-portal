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
