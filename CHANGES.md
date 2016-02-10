ICGC DCC - Portal - Change Log
===

Change log for the DCC 2.0 Portal

4.0.5
--
 - Functional Impact filter now applies to gene and mutation tracks in the Genome Viewer.
 - Increased size of protein viewer and made it resize based on page width.

4.0.0
--
 - Refactored the repositories API to support changes in document structure in Elasticsearch.
 - Added `IS NOT` support for Facets, this functionality is hidden behind a feature flag. 
 - EntitySetIds are now part of entity id list in JQL. Olds style JQL for entitySetIds is still supported. 
 - Various UI Bug Fixes.
 - Added UI.md (UI Getting Started Developer Guide), CONTRIBUTING.md (Making Contributions to the Source Guide) and GPL3 Licenses to UI code. Changed links to point to newly created ICGC Document site developed in parallel to this release.
 - Filters are now managed by a seperate Angular Service to simplify interaction with UI logic.
 - Changed directory structure of UI (flattened out unneccessary nesting) to simplify it for future extension.

3.9.3
--
 - Refactored Advanced Search Front end to render tabs and facets only when required - performance boost is the result.
 - Decorated Restangular to provide support for cancelling ajax requests.
 - Draft of front end developer conventions in progress - README.md
 - Updated build tools on front end.
 - Added JS Source Maps Support for extra debugging convenience.
 
3.8.20
--
 - New drug/compound entity page which correlates targeted genes and provides context into clinical trials.
 - Drug/Compound integration in gene info pages.
 - Quick search support added for Compounds/Drugs.
 - Bug fixes for genome map viewer including support for tooltip descriptions in fullscreen.
 - Reactome Pathway viewer from enrichment analysis now supports the ability to identify overlapping genes (ICGC vs input gene sets).
 - Refactored Pathway Viewer into more angular-ized components.
 - iobio integration added to AWS/Collaboratory repository pages.
 
3.8.18.x
--
  - Added tooltip information for Data Type in External Repository
  - Added Analysis Software facet in External Repository
  - Updated Genome Viewer to handle new API format
  - Bug fixes to mutation counts and text rendering in Pathway Viewer
  - Custom Copy to Clipboard Component added throuhgout application.
  - Improved performance of the Advanced Search page.
  - Released ICGC in the Cloud Universal Guide for AWS and Collaboratory. 
  - Released ICGC in the Cloud Repository showcase pages for AWS and Collaboratory.
  - Separated DCC data releases from the external file repository page.
  - Various portal UI bug fixes.
  - Fixed mailer for error reporting on the data portal.
  - Enrichment Analysis enhancements utilizing the Pathway Viewer for Reactome Pathways.
  - Some additional improvements and optimizations introduced to front-end build process.

3.8.16.1
--
 - Updated d3 to fix minor error with Pathway Viewer
 - Fixed issue that caused Pie Chart colouring from not being correctly assigned to new Projects on Cancer Projects page.
 - In Phenotype Analysis, results of 0 should no longer link to advanced search
 - When navigating to Genome Viewer from Donor Entity page, the mutation sorting and filter will be carried over
 - Corrected the filter used in querying mutations for the Reactome Pathway Viewer
 - Fixed bug that caused the Project Sample Sheet to return empty
 - Refactored Browser API to fix various problems that could result in error 500s being returned
 - Significantly reduced the load time of stacked bar chart in Project Summary page.
 - Enhanced data repositories.
 - Introduced in Pathway Viewer page relating to Enrichment Analysis
 - Added filtering on Reactome ID’s in new Pathway Viewer page.
 - Promoted featurePortal service to a Provider so it can be used in config angular modules (useful for altering angular routes or angular ui-router state templates).
  - Added ICGC in the Cloud pages including (currently disabled as changes are being done to the documents):
  - Logo addition on ICGC Portal Home Page
  - ICGC In the Cloud Landing Page 
  - Preliminary User Guide for AWS-Virginia
 - Added a generalized page to be able to model the info regarding associated ICGC Repositories (similar to the PCAWG Page).
 - Fixed Grunt Bower service from running multiple times during build
 - Added injectable Grunt Front-end Developer Profile so there is no more need to modify the application’s app.js when doing front-end development via ‘grunt server’.
 - Updated ICGC Data Portal Team Page to reflect the current Project Team as well as to publicly acknowledge some important contributing parties to the ICGC Portal.

3.8.14.4
--
 - Added functionality to view files in external repo based on filter from advanced search
 - Added functionality to download and view donor sets in external repository from analysis
 - Added full screen support for Genome Viewer and Pathway Viewer
 - Bug fix for gene query in the case of no donor analysis
 - Disease pathways should no longer throw errors
 - QOL improvements and various bug fixes for Pathway Viewer and Genome Viewer
 - Added Tumor Types facet in Projects page

3.8.9.0
--
 - Added Pancancer pages
 - Fix minor issues with protein viewer and enrichment analysis

3.8.8.0
--
 - Upgraded core javascript libraries
 - Reduced project colour space to primary sites
 - Added facet / filter / badge support for PanCancer (PCAWG) donors
 - Added support for clinical supplementary files (family, exposure, therapy)
 - Added external repositories to Data Repository page
 - Added Data Repository search and manifest export
 - Added support for non-Molecular / non-Clinical project / donor entity pages
 - Added preliminary UI / API support for "donor states"
 - Added miscellaneous bug fixes.

3.8.2.4
---
 - Added support for HTTP range headers for static downloads

3.8.2.3
---

- Added phenotype comparison to data analysis
- Redesigned data analysis to better model user activities
- Added functionality to save custom uploaded gene list
- Added version control and version deprecation for data analyses
- Added demo analyses
- Upgrade libraries
- Fix bugs and usability issues.

3.8.2.2
---

- Added "Donor Count History" chart to the Projects Page
- Added versioning in relational models for compatibility detection in data releases.

3.8.1.3
---

- Added ability to disable downloader in the API and UI
- Harmonized tooltips across application

3.7.6.4
---

- New functions to save donor, gene and mutation sets
- Gene set enrichment analysis
- Set operations analysis
- Projects page layout optimization
- Charts optimization
- Global alliance beacon
- Bug fixes

3.7.6.3
---

- Health checks for CUD and Hazelcast
- Dynamically configurable logging
- Connection pooling with health check

3.7.6.2
---

- Gene Ontology integration
- Update Reactome pathway 
- Update Cancer Gene Census
- Route change: Pathway to GeneSet
- Various small fixes


3.7.4.1
---

- Plot showing exome mutations across projects
- Upload custom gene list
- Markdown support for data repository


3.5.8
---

- Support multiple observations
- Genome Maps update

3.5.2
---

- Added a url shortening service for search queries
- Updated to latest Genome Maps

3.2.3
---

- Added "Data Repository HREF" feature

3.2
---

- Added "Aggregated SSM VCF" feature
- Added "Pathway Entity Page" feature
- Added "Pathway Search" feature
- Added "Gene Sets" feature
- Added "Functional Impact" feature
- Added "Project Search" feature
- Added "Authentication and Authorization" feature
- Added "Controlled Access Download" feature
- Added "Asynchronous Download" feature
- Added "Public RESTful API" feature
