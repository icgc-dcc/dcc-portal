# DCC Portal Application UI Documentation #




## Who this Document is for? ##

* A developer, or team of developers who wish to understand the general frontend
components in order to extend, modify or provide fixes towards the UI.




## Learning Objectives ##

* Understand the general frameworks used within the frontend.
* Be able to identify the responsibilities of various portions of the UI codebase.
* Learn to customize the DCC Banner, Logo and Footer.
* Theming the DCC Portal frontend.
* In general, gain enough knowledge from prior sections to work on your own feature enhancements in the frontend code.


## The Frontend Stack ##

### Build ###
*Please note:* The below dependendencies are managed using the [NodeJS's](https://nodejs.org) [NPM](https://www.npmjs.com/) tool. Please
refer to the *package.json* file in the current directory to lookup which versions the portal is depending on.


* NodeJS with NPM
* Grunt Tools (NodeJS) for
 * Development
    * Connect (Grunt HTTP Server)
    * Watch (File Watcher) w/LiveReload and SASS Regeneration
 * JS Linting
   * JSHint
 * Frontend Tests
   * Karma w/PhantomJS
 * Concatenation/Minification/Source Map Generation
   * Usemin/CSSmin/HTMLmin/Imagemin
   * Concat
   * Uglify
   * Filerev
   * etc...
   * Please Refer to the Frontend *Gruntfile.js* for More Details)
* SASS Preprocessor (w/Compass) - CSS Development
* Bower (Frontend Library Package Management)
 


### UI ###
*Please note:* The below dependendencies are managed using the [Bower](http://bower.io/) package manager. Please
refer to the *bower.json* file in the current directory to lookup which versions of the illustrated libraries the portal is depending on.

* *[jQuery](https://jquery.com/)* - For DOM Manipulation it is used by various 3rd party dependencies and the application itself)
* *[lodash](https://lodash.com/)* - A Common JS utility library.
* *[AngularJS](https://angularjs.org/)* - Version 1.x.y - DCC Portal frontend's primary JS framework.
* *[Angular-ui-router](https://github.com/angular-ui/ui-router)*  - Route Management in AngularJS.
* *[Restangular](https://github.com/mgonto/restangular)* - REST Resource Management in AngularJS.
* *[Angular-bootstrap](https://angular-ui.github.io/bootstrap/)* Bootstrap integration in AngularJS.
* *[D3.js](https://d3js.org/)* - Visualization (Preferred) Library
* *[Highcharts](http://www.highcharts.com/)*  - Visualization Library
* *Other Various Third Party Libs*

## Frontend Breakdown ##

### File Organization ###
This section provides a high-level outline of the modules/file structure in the Portal UI project.

* ```/scripts/advanced``` - Advanced Page ([https://dcc.icgc.org/search](https://dcc.icgc.org/search))
* ```/scripts/analysis``` - Set Analysis Page ([https://dcc.icgc.org/analysis](https://dcc.icgc.org/analysis))
* *```/scripts/app```* - The main entry point in the application. This is the file that provides the definition of the main application module and it's dependencies.
It also contains bootstrap code and some configuration for some of the dependencies (i.e. Restangular) used within the application. 
* ```/scripts/auth``` - Login module
* ```/scripts/beacon``` - Global alliance beacon page ([https://dcc.icgc.org/ga4gh/beacon](https://dcc.icgc.org/ga4gh/beacon))
* ```/scripts/browser``` - Genome Viewer Page ([https://dcc.icgc.org/browser](https://dcc.icgc.org/browser))
* ```/scripts/common``` - Houses common angular services, filters, and other utilities that are shared among application modules. 
This is also the build area for the PQL grammar file.
* ```/scripts/compounds``` - Drug Entity Page
* ```/scripts/donors``` - Donor Page
* ```/scripts/downloader``` - Dynamic downloader modules. Both request submitter and viewer.
* ```/scripts/enrichment``` - Gene set enrichment module
* ```/scripts/facets``` - Faceting modules, used in Advanced Page and Projects Page
* ```/scripts/genelist``` - Upload genelist module
* ```/scripts/genes``` - Gene Page. For example - [https://dcc.icgc.org/genes/ENSG00000141510](https://dcc.icgc.org/genes/ENSG00000141510)
* ```/scripts/genesets``` - Gene Set Page. Housing Reactome, Gene-Ontology and other gene annotations. For example - [https://dcc.icgc.org/genesets/R-HSA-5625970](https://dcc.icgc.org/genesets/R-HSA-5625970)
* ```/scripts/keyword``` - Quick text search module
* ```/scripts/mutations``` - Mutation Page
* ```/scripts/pancancer``` - Pancancer landing page
* ```/scripts/pathwayviewer``` - Pathway Viewer Page for Reactome pathways (linked to from enrichment analysis results)
* ```/scripts/phenotype``` - Phenotype analysis page
* ```/scripts/projectmutationviewer``` - Project mutation viewer component.
* ```/scripts/projects``` - Projects Page and Project Page ([https://dcc.icgc.org/projects](https://dcc.icgc.org/projects), [https://dcc.icgc.org/projects/BRCA-US](https://dcc.icgc.org/projects/BRCA-US))
* ```/scripts/proteinstructureviewer``` - Protein structure viewer component.
* ```/scripts/releases``` - Release module. Carries portal release summary.
* ```/scripts/repositories ``` - Pages for cloud documentation and repository stats (AWS/Collaboratory)
* ```/scripts/repository ``` - Static download Page ([https://dcc.icgc.org/repository](https://dcc.icgc.org/repository))
* ```/scripts/sets``` - Set upload, set operations, set CRUD
* ```/scripts/share``` - Sharing, URL shortening module
* ```/scripts/software``` - DCC software page which publishes various tools for working with ICGC Data ([https://dcc.icgc.org/software](https://dcc.icgc.org/software))
* ```/scripts/stackedareachart``` - Stacked bar chart UI component.
* ```/scripts/stackedbarchart``` - Stacked area chart UI component.
* ```/scripts/tokens``` - Token management for Collaboratory project via the portal login, not to be confused with the auth module with deals with ICGC authentication and tokens.
* ```/scripts/ui``` - Mostly smaller UI components tied to the ICGC portal
* ```/scripts/venn23``` - Venn diagram UI component used in analysis module.
* ```/scripts/visualization``` - Visualization module definition for the project mutation viewer, project stacked bar and area bar.

## Got Questions/Feeback? ##

 * We are always happy to help - should you have any questions or feedback
please feel free to contact us at [dcc-support@icgc.org](mailto:dcc-support@icgc.org) 