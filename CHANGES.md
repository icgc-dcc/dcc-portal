ICGC DCC - Portal - Change Log
===

Change log for the DCC 2.0 Portal

3.8.15.x
--
	- Updated d3 to fix minor error with Pathway Viewer

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
 - Added miscelaneous bug fixes.

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

- New functions to save donor, gene and muation sets
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
- Connection pooling with healh check

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
