ICGC DCC - Portal - Change Log
===

Change log for the DCC Data Portal.

[4.3.24](https://github.com/icgc-dcc/dcc-portal/compare/4.3.23...4.3.24)
--
- Feature: Added support for recursive download file listing in endpoint in DCC Data Releases
- Enhancement: Added instructions in Repository Browser modals
- Enhancement: Added polyfill.io for older browsers
- Enhancement: Added compression to API endpoints
- Fix: Remove one-time binding for Enrichment Analysis
- Build: Using `yarn` for dependency management
- Build: Make eslint extension agnostic
- Refactor: A bit of cleanup for gene set logic

[4.3.23](https://github.com/icgc-dcc/dcc-portal/compare/4.3.22...4.3.23)
--
- Bugfix: Race condition with cleanup in Saved Results
- Bugfix: Deactivating infinite scroll if there are no results
- Bugfix: Creating custom gene set should show up right away in the table
- Bugfix: Adds null check for returned entity set in phenotype analyzer
- Bugfix: Update copyright year on build
- Bugfix: Display only specific user token if user has multiple accounts

[4.3.22](https://github.com/icgc-dcc/dcc-portal/compare/4.3.21...4.3.22)
--
- Site: Remove uppercase transform and separator on action toolbar
- Site: Help icon changed to question circle icon
- Sets: Getting the set name based on the filter
- Sets: Names now includes filter values
- Entity Tables: Text wrapping long set name into new lines
- Launch Cohort Analysis from AS and Project page
- AS: Resetting the tables pagination on filter change based on the tab
- AS: Added OncoGrid to AS page for Donors and Genes tables.
- Token Manager: Conditionally show “days” in “Expires in” column
- OncoGrid: Adding mutation ID to crosshair tooltip
- OncoGrid: Fixed regression for set name change with demo	
- Survival Analysis: Show overall and disease free survival graphs side
- Donor: Add biomarker and surgery to model
- Genome Viewer: Account for zoom when getting start and end of mutation track

[4.3.21](https://github.com/icgc-dcc/dcc-portal/compare/4.3.20...4.3.21)
--
- Add oncogrid to advanced search 
- Survival layout improvements
- Files endpoint can use field param
- Legend on OncoGrid
- Have webpack dev server proxy to production api based on process.env
- Fix scrolling on pagination
- Handle error when creating token
- Add message for expired tokens instead of showing negative time 
- Reset pagination on filter change / pagination change
- Fix: A "id" to field mapping for file search
- Add "View in Data Repository" link in Projects page
- Add ‘drag to zoom’ prompt in Survival Analysis
- Fix: Missing version from oncogrid analysis table schema
- `RepositorySearchService` refactoring in UI to use new `/repositories` endpoint
- Fix: Add guard against no intervals in survival analysis
- Change header to "Mutation ID" instead of "ID" in AS mutation TSV download
- Fix: On auth server failure, portal will return a 502 bad gateway
- Don't destroy and recreate analysis results when switching tabs
- Add other useful npm scripts to readme
- Improvements to protein response, api, and code.
- Remove unused UI code and files
- Move genome-viewer dependencies to own module and lazy load on demand

[4.3.20](https://github.com/icgc-dcc/dcc-portal/compare/4.3.19...4.3.20)
--
- Removed notes from saved sets
- Removed primary load indicator when refreshing Advanced Search or Projects page
- Bugfix: Saving sets in different tabs overwrite sets saved in other tabs
- Bugfix: Uploading donor set in Repository Browser uses correct prefix
- Improvement: Prevent hard page refresh on filter change in Repository Browser
- Improvement: Changing query in AS should reset pagination
- Use subscripts on set identifiers
- Allow updating set names
- Enforce npm / node versions in build
- Elliding sequences in Gene page when too long
- Additional parameter checking in REST endpoints
- `TermsLookupService` refactoring
- Fixed display of Analysis Result page
- Fixed Collaboratory button URL
- Fixed border rendering in AS hits table

[4.3.19](https://github.com/icgc-dcc/dcc-portal/compare/4.3.18...4.3.19)
--
- Remove “notes” from saved sets
- Null check for cookies before attempting to get session token
- More information in exception emails
- Null check on donorIds for processing a donor set
- Bubbles up a 403 from the auth server on bad token scope

[4.3.18](https://github.com/icgc-dcc/dcc-portal/compare/4.3.17...4.3.18)
--
- Added support for injecting settings into <portal-settings></portal-settings> as window.ICGC_SETTINGS js object
- Added support for externalizign Google Analytics account ID
- Removed grunt
- Oncogrid zoom selection fix for right to left
- Showing loading blocks for Projects Details tab
- Added back publications section
- Refactoring of `IndexModel` to be leaner and more cohesive
- Server FindBug fixes and improvements to code clarity
- Fixed JS errors on tooltips
- Migrated jQuery from Bower to NPM

[4.3.17](https://github.com/icgc-dcc/dcc-portal/compare/4.3.16...4.3.17)
--
- Fixed API setting of Functional Impact on the consequence if provided
- Fixed ability to deselect Cancer Gene Census in Advanced Search
- Fixed sorting of Cancer Distribution table columns

[4.3.16](https://github.com/icgc-dcc/dcc-portal/compare/4.3.15...4.3.16)
--
- Added support for mirroring customization
- Fixed missing filter on table links in Gene Page to Advanced Search

[4.3.15](https://github.com/icgc-dcc/dcc-portal/compare/4.3.14...4.3.15)
--
- Added 404 page for entities and unknown pages
- Added human readable saved set names

[4.3.14](https://github.com/icgc-dcc/dcc-portal/compare/4.3.13...4.3.14)
--
- Fixed missing tokens / scopes when using ICGC.org logins
- Updated Browser Update library configuration
- Fixed bug with functional impact in summary on mutation page
- Refactored Phenotype Analyzer based on PR feedback

[4.3.13](https://github.com/icgc-dcc/dcc-portal/compare/4.3.12...4.3.13)
--
- Table filters added to entity pages
- VCF IOBIO statistics show user friendly message when file not available
- 'Files' section added to Donors entity page
- Dynamically setting name for saving a entity set
- 'Loading Files..' spinner added to repositories page
- Bug fixes

[4.3.12](https://github.com/icgc-dcc/dcc-portal/compare/4.3.11...4.3.12)
--
- Fixes regression for launching Oncogrid demo.
- Always download manifest as tarball
- Using HttpServletResponse to render manifests
- Detailed logging for manifest query/response
- Bug fixes

[4.3.11](https://github.com/icgc-dcc/dcc-portal/compare/4.3.10...4.3.11)
--
- Fixes regression for launching Survival Analysis demo. 
- Don’t open manifest downloads in new tab, always download tarball
- Now using HttpServletResponse to render manifests in order to eagerly commit status code and content disposition for user agents
- Fixed issues with manifest generation when unique selected and using individually selected files
- Fixed other UI issues related to manifest generation

[4.3.10](https://github.com/icgc-dcc/dcc-portal/compare/4.3.9...4.3.10)
--
- Added ability to filter by compounds in Advanced Search
- Added p-value calculations for Survival Analysis
- Added ability to launch OncoGrid from Project page
- Added ability in manifest dialog to include duplicates
- Added support for `icgc-get` in Repository Browser
- Changed Survival Analysis button to perform reset instead of undo
- Fixed hidden close button in pathway viewer legend
- Fixed text-wrapping issues
- Fixed token manager to not error on unknown scopes

[4.3.9](https://github.com/icgc-dcc/dcc-portal/compare/4.3.8...4.3.9)
--
- Fixed regression from Spring Boot upgrade: now ensures "_missing" count is present in response json, even when `null`

[4.3.8](https://github.com/icgc-dcc/dcc-portal/compare/4.3.7...4.3.8)
--
- Upgraded to Spring Boot `1.4.0-RELEASE` and added custom `FailureAnalyzer`s for Elasticsearch, etc.
- Bug fix for missing data in sample/specimen table when in a Donor Entity Page.
- Bug fix for exporting Files table to a file. 
- OncoGrid shows "Not Verified" for donors with missing diagnosis. 
- i18n implementation for Portal
- Bug fixes for alignment and text wrap issues

[4.3.7](https://github.com/icgc-dcc/dcc-portal/compare/4.3.6...4.3.7)
--
- Removed software page and moved to [docs](http://docs.icgc.org/software/binaries/).

[4.3.6](https://github.com/icgc-dcc/dcc-portal/compare/4.3.5...4.3.6)
--
  - Added icgc-get modal for file manifests page, currently behind a feature flag
  - OncoGrid shows spinner while loading

[4.3.5](https://github.com/icgc-dcc/dcc-portal/compare/4.3.4...4.3.5)
--
  -  Made information table in pathway viewer sortable
  -  Added mutation annotations to nodes, information table, and legend in pathway viewer
  -  Added exception mapper for 503 errors
  -  'Download unavailable' message while the service is down for maintenance 
  -  angular-animate and angular-cookies version update
  -  Added directive for animating number changes - `<number-tween>`  
    See [PR#74](https://github.com/icgc-dcc/dcc-portal/pull/74) for usage instructions
  - Changes to simplify manifest download modal
  - jshint will now run during development and grunt-notify will notify errors
  - `LoadState` added. [See PR#58](https://github.com/icgc-dcc/dcc-portal/pull/58) for usage instructions
  - Updated analysis results sidebar layout
  - New instances of `<tooltip-controls>` can now be created to be used localized within a component.  
    See [PR#52](https://github.com/icgc-dcc/dcc-portal/pull/52) for usage instructions
  - Improves the way entity sets are handleing in the UI for the File Repository
  - Bug fix for NPEs caused by invalid fields in keyword search
  - Updates to OncoGrid rendering so that it does not block sidebar animations in analysis page
  - Bug fix for File Repository summary numbers. Increased maximum number of term buckets to 300,000

[4.3.4](https://github.com/icgc-dcc/dcc-portal/compare/4.3.3...4.3.4)
--
  - Removed Publication section from Cancer projects
  - Cleanup of unused feature flags and associated logic

[4.3.3](https://github.com/icgc-dcc/dcc-portal/compare/4.3.2...4.3.3)
--
  - Added .settings to `dcc-portal-server`
  - Add option to use prod API for UI dev
  - Removed `dropdown.js`
  - `dcc-portal-server` pom.xml cleansing and refactoring
  - DCC-4996: Analysis tab now shows Saved Sets immediately
  - Updated Analysis result side panel's styling
  - Added "zoom", "undo" and "fullscreen" features to survival analysis graphs
  - Layout and feature updates for manifest download modal
  - Changed bower dependency on `angularjs` to `angular`, fixes intermittent build errors.
  
[4.3.2](https://github.com/icgc-dcc/dcc-portal/compare/4.3.1...4.3.2)
--
  - Migration of files from `dcc-portal-api` to `dcc-portal-server`
  - URL encoding fixes
  - Enhanced command line running using `mvn spring-boot:run`
  - Simplified classpath resolution of static files

[4.3.1](https://github.com/icgc-dcc/dcc-portal/compare/4.3.0...4.3.1)
--
  - Hox fix for `OncoGridResource` not extending from `Resource`

[4.3.0](https://github.com/icgc-dcc/dcc-portal/compare/4.2.12...4.3.0)
--
  - Migration to Spring Boot

4.2.12
--
  - Added new OncoGrid analysis using newly written OncoGrid library (https://github.com/icgc/oncogrid)
  - Set Analysis now supports operations on File Sets
  - Added Survival analysis into Phenotype Comparison analysis
  - Refactoring and consolidation of page all functionality
  - Minor bug fixes and style updates. 

4.2.11
--
  - Added missing CNSM data type to the public downloads
  
4.2.10
--
  - Downloader improvements
  - Bugfix: URL encodings 
  - Bugfix: / in URL of "download/info/{release}"
   
4.2.9
--
  - Analysis launcher page layout update
  
4.2.8
--
  - Bugfix: Fixed bad `this` in Pathway Viewer reference
  
4.2.7
--
  - Added drugs overlay in Pathway Viewer
  
4.2.6
--
  - Added support for new Download Service
  - Removed "downloader.html" screen as this is no longer needed
  - Added spinner while download dialog is populated
  - Added ability to download directly from the dialog

4.2.5
--
  - Added x of y donors on File Entity page
  - Added collapse / show on File Entity page when donors greater than 5 

4.2.4
--
  - Removed NOT facet on File in Repository Browser

4.2.3
--
  - Fixed PDC support
  - Upgraded Highcharts to 4.2.5
  - Added Charts to Data Repositories page
  - Unified behaviours for "copy to clipboard" widgets
  - Added copy to clipboard to user tokens
  - Updated search result layout. Added labels underneath the single-letter badges.
  - Enabled 'IS NOT' support for Donor ID, Gene ID, Mutation ID in the Advanced Search Page, and File ID and Donor ID in the Data Repositories Page
  - Switched to HTTPS mode by default for local development to enable local user-logins during dev.
    - API calls are now proxied via [grunt-proxy](https://github.com/tutukin/grunt-proxy)
    - Local development now needs to be run under https://local.dcc.icgc.org
  - Added NPM scripts.
    - Should now use `npm run dev` to start local UI development instead of `grunt server`. This will check if your host file contains the entry for local.dcc.icgc.org and prompt with instructions on how to add it if it doesn't find the entry.
    - `npm run sethost` will add the entry for local.dcc.icgc.org in your host file
  - Made tables more compact for clinical files containing many donors. 
  - Added Autoprefixer to UI build step
  - Added source for the DCC-DEV server, which is a test portal provisioner
  - Added `document.domain = document.domain` to index.html to allow interactions from DCC-DEV
  - Bugfix: capitalization of miRNA-Seq
  - Bugfix: tooltips from some D3 charts persisting between page navigations
  - Bugfix: main loading indicator not centered on load
  - Bugfix: Going to advanced search from files that have many donors. 

4.2.2
--
  - Added better tumor/normal support for GDC files. 
  - Added ability to search for files by manifest ID.
  - Bugfix: Keyword search paging was broken, now fixed. 
  - Bugfix: Downloading manifests sometimes errored out for collab. 


4.0.26
--
  - Fixed legend not showing in Pathway Viewer

4.0.25
--
  - Support for EGA files in Repository Browser
  - Support for multi-valued specimen / sample ids

4.0.24
-- 
  - Can search for files based on Bundle ID, and the Bundle ID will be shown for file results in keyword search.
  - The Gene entity page will now show all available drugs instead of limiting to a list of 10 results.
  - Clicking on clinical trials from the gene page will now scroll to the trials section of compound page.
  - Various bug fixes
  
4.0.23
--
  - Bug fix for paging when modifying filters in File Repository.
  - Bug fix for downloading and saving manifests when using an entity set in the repo filter.
  - Can search for files based on Bundle ID.
  - No longer trigger server errors when trying to retrieve mapping of repository index. 
  - Gene entity page will show all available drugs instead of limiting to 10 drugs. 

4.0.17
--
  - New endpoint for repository meta data.
  - Bug fix to keyword search which would continue to show the "show more" button when there were no results.
  - Various fixes to entity set logic now that migration to 'ES:' prefix for sets is complete. 

4.0.15
--
  - Upgraded from Swagger 1.3.13 to 1.5.8
  - Added enhanced manifest endpoint `/manifests` that allows `json`, `files` and `tarball` formats
  - Support for ICGC21 (history, projects, etc.)
  - Various bug fixes

4.0.14
--
  - Added iobio visualization for Collaboratory and AWS-Virginia hosted VCF files in File Repository. 

4.0.13
--
 - Retrieving software download versions from artifactory is now done over HTTPS.
 - Updates to the Collaboratory page. 
 - Elasticsearch Facet API has been completely replaced with the newer Aggregations API.
 - Fix to allow the enabling of experimental features through query parameters. 
 - Improvements to resizing behaviour of Genome Viewer.
 - Bug fix for Collaboratory manifest IDs mistakenly being resolved as AWS-Virginia manifest IDs.
 - Bug fixes to Advanced Search links in Phenotype and Enrichment Analysis.
 - Misc bug fixes to API, UI, and build process. 

4.0.5
--
 - Functional Impact filter now applies to gene and mutation tracks in the Genome Viewer.
 - Functional Imapct filter now also applies to the compound page.
 - Increased size of protein viewer and made it resize based on page width.
 - Various Front End Bug Fixes.
 - Removed SwaggerUI static assets used in the Portal API documetation (this is now centralized at http://docs.icgc.org/)
 - Updates to some Portal UI docs.

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
