# ICGC DCC - Portal Server

Server for ICGC DCC Data Portal. 

## Documentation

Technical Documentation: [SERVER.md](./SERVER.md)

Executable API documentation is available at:

	http://localhost:8080/docs

## Administration

Administration is available at:

	http://localhost:8081
  
## Development

The Portal Server is built with maven:

```shell
cd dcc-portal/dcc-portal-server
mvn -am
```

## Running

### Command-line

The following commands assume the current directory is the root directory, `dcc-portal`.

To run a basic test setup, execute the following:

```shell
mvn -pl dcc-portal-server spring-boot:run -Drun.profiles=test
```

Note that you may need to tweak `application.yml` to point to various systems for this to work.

To run a basic development setup, execute the following supplying information about the config server:

```shell
mvn -pl dcc-portal-server spring-boot:run -Drun.profiles=development -Drun.arguments='--spring.cloud.config.uri=http://<user>:<password>@<config-server-host>'
```

See the [spring-boot:run](http://docs.spring.io/spring-boot/docs/current/maven-plugin/run-mojo.html) documentations for further configuration details.

### IDE

From Eclipse or IntelliJ:

| Property   | Value                                   |
| ---------- | --------------------------------------- |
| Main Class | `org.icgc.dcc.portal.server.ServerMain` |
| VM Options | `-Xmx6G`                                |
| Arguments  | `--spring.profiles.active=test` |

*Note: If Eclipse or IntelliJ are taking a long time to build before running, try excluding
the dcc-portal-ui as a module.*
    

## Configuration

To configure the portal for running, the `elastic` and `icgc` portions of the `settings.yml` file
to be used must be set.

The `elastic` portion of the configuration must point to an existing and running elasticsearch index. 

For the `icgc` portion, the API endpoints and credentials must be configured. You must provide substitutes to any
ICGC systems and APIs you do not have access to should that case arise, such as your own Centralized User Directory. 

## Deployment

To run the application once built:

```shell
cd dcc-portal/dcc-portal-server
java -jar target/dcc-portal-server-[version].jar --spring.profiles.active=test --spring.config.location=src/main/resources/application.yml
``` 
  
## Keystore Management

To import certs generated from letsencrypt:
 

```shell
# Create new letsencrypt.jks keystore
openssl pkcs12 -export -in cert.pem -inkey privkey.pem -out cert_and_key.p12 -name tomcat -CAfile chain.pem -caname root
keytool -importkeystore -deststorepass password -destkeypass password -destkeystore letsencrypt.jks -srckeystore cert_and_key.p12 -srcstoretype PKCS12 -srcstorepass password -alias tomcat
```
Based from: [gist](https://gist.github.com/mihkels/6e30e8e21acc68a55482#file-letsencrypt-sh-L9-L12)
