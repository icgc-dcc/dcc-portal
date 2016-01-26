ICGC DCC - Portal API
===

RESTful API for the ICGC DCC Data Portal. 

Documentation
---

Technical Documentation: [API.md](./API.md)

Executable API documentation is available at:

	http://localhost:8080/docs

Administration
---

Administration is available at:

	http://localhost:8081

Development
---

To run the application:
	
	cd dcc-portal/dcc-portal-api
	mvn -am
	java -jar target/dcc-portal-api-[version].jar server src/test/conf/settings.yml

