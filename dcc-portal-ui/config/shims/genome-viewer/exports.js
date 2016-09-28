/*

To generate this array, 

- open a browser window and go to "about:blank"
- paste the contents of genome-viewer.js into the js console
- run the following snippet:

var objects = [];
var functions = [];

for(var i in this) {
  try{
    if(this[i] instanceof Object && this[i].constructor.toString() === 'function Object() { [native code] }' && this[i].toString().indexOf("native")==-1) {
      objects.push(i);
    } else if ((typeof this[i]).toString()=="function"&&this[i].toString().indexOf("native")==-1) {
      functions.push(this[i].name);
    }
  } catch(e) {
    console.warn('error:', e);
  }
}

console.log(objects.concat(functions));

*/

module.exports = [
  'Region',
  'Grid',
  'FeatureBinarySearchTree',
  'FileWidget',
  'BEDFileWidget',
  'GFFFileWidget',
  'GTFFileWidget',
  'TrackSettingsWidget',
  'UrlWidget',
  'VCFFileWidget',
  'InfoWidget',
  'GeneInfoWidget',
  'GeneOrangeInfoWidget',
  'MirnaInfoWidget',
  'ProteinInfoWidget',
  'SnpInfoWidget',
  'TFInfoWidget',
  'TranscriptInfoWidget',
  'TranscriptOrangeInfoWidget',
  'VCFVariantInfoWidget',
  'ConsequenceTypeFilterFormPanel',
  'FormPanel',
  'GoFilterFormPanel',
  'MafFilterFormPanel',
  'PositionFilterFormPanel',
  'SegregationFilterFormPanel',
  'StudyFilterFormPanel',
  'VariantBrowserGrid',
  'VariantEffectGrid',
  'VariantFileBrowserPanel',
  'VariantGenotypeGrid',
  'VariantStatsPanel',
  'VariantWidget',
  'CheckBrowser',
  'GenericFormPanel',
  'HeaderWidget',
  'JobListWidget',
  'LoginWidget',
  'OpencgaBrowserWidget',
  'ProfileWidget',
  'ResultTable',
  'ResultWidget',
  'UploadWidget',
  'CircosVertexRenderer',
  'DefaultEdgeRenderer',
  'DefaultVertexRenderer',
  'Edge',
  'Vertex',
  'DataSource',
  'FileDataSource',
  'StringDataSource',
  'TabularDataAdapter',
  'UrlDataSource',
  'FeatureDataAdapter',
  'BamAdapter',
  'BEDDataAdapter',
  'CellBaseAdapter',
  'DasAdapter',
  'EnsemblAdapter',
  'FeatureTemplateAdapter',
  'GFF2DataAdapter',
  'GFF3DataAdapter',
  'GTFDataAdapter',
  'OpencgaAdapter',
  'VCFDataAdapter',
  'AttributeNetworkDataAdapter',
  'DOTDataAdapter',
  'JSONNetworkDataAdapter',
  'SIFNetworkDataAdapter',
  'TextNetworkDataAdapter',
  'XLSXNetworkDataAdapter',
  'IndexedDBStore',
  'MemoryStore',
  'FeatureChunkCache',
  'FileFeatureCache',
  'BamCache',
  'NavigationBar',
  'ChromosomePanel',
  'KaryotypePanel',
  'StatusBar',
  'TrackListPanel',
  'Track',
  'AlignmentTrack',
  'FeatureTrack',
  'GeneTrack',
  'Renderer',
  'AlignmentRenderer',
  'ConservedRenderer',
  'FeatureClusterRenderer',
  'FeatureRenderer',
  'GeneRenderer',
  'HistogramRenderer',
  'SequenceRenderer',
  'VariantRenderer',
  'VcfMultisampleRenderer',
  'GenomeViewer',
  'Point',
  'IndexedDBTest',

  'Utils',
  'SVG',
  'CellBaseManager',
  'OpencgaManager',
  'EnsemblManager',
  'GraphLayout',
];
