ICGC DCC - Portal API
===

RESTful API for the ICGC DCC Data Portal. 

Documentation
---

Executable API documentation is available at:

	http://localhost:8080/docs

Administration
---

Administration is available at:

	http://localhost:8081

Development
---

For development data, port forward to the dev cluster:

	ssh -NL 9200:***REMOVED***:9200 -NL 9300:***REMOVED***:9300 hproxy-dev***REMOVED***

To run the application:
	
	cd dcc-portal/dcc-portal-api
	mvn -am
	java -jar target/dcc-portal-api-[version].jar server src/test/conf/settings.yml

