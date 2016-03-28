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
  
  
Keystore Management
---
To import certs generated from letsencrypt:
 

```
# Create new letsencrypt.jks keystore
openssl pkcs12 -export -in cert.pem -inkey privkey.pem -out cert_and_key.p12 -name tomcat -CAfile chain.pem -caname root
keytool -importkeystore -deststorepass password -destkeypass password -destkeystore letsencrypt.jks -srckeystore cert_and_key.p12 -srcstoretype PKCS12 -srcstorepass password -alias tomcat
```
Based from: [gist](https://gist.github.com/mihkels/6e30e8e21acc68a55482#file-letsencrypt-sh-L9-L12)
