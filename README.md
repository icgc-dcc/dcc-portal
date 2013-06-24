ICGC DCC - System
===

Parent project of the ICGC DCC system.

Setup
---

Clone the repository:

`git clone https://github.com/icgc-dcc/dcc.git`

Install Maven 3.0.3:

[http://maven.apache.org/download.cgi](http://maven.apache.org/download.cgi)
	
Install MongoDB 2.4.1:

[http://www.mongodb.org/downloads](http://www.mongodb.org/downloads)

Install ElasticSearch 0.90.1:
	
[http://www.elasticsearch.org/downloads](http://www.elasticsearch.org/downloads)

Install UI development environment:
	
- [dcc-submission-ui](dcc-submission/dcc-submission-ui/README.md)
- [dcc-portal-ui](dcc-portal/dcc-portal-ui/README.md)


Build
---

To build, test and install _all_ modules in the system:

`mvn`
	
To build, test and install _only_ the Submission sub-system modules:

`mvn -amd -pl dcc-submission`

To build, test and install _only_ the ETL sub-system modules:

`mvn -amd -pl dcc-etl`

To build, test and install _only_ the Download sub-system modules:

`mvn -amd -pl dcc-downloader`
	
To build, test and install _only_ the Portal sub-system modules:

`mvn -amd -pl dcc-portal`
	
Run
---

See module documentation

Modules
---
Top level system modules:

- [Submission](dcc-submission/README.md)
- [ETL](dcc-etl/README.md)
- [Download](dcc-downloader/README.md)
- [Portal](dcc-portal/README.md)
	

