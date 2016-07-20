# ICGC DCC - Portal

Parent project of the portal modules

## Build

To compile, test and package the system, execute the following from the root of the repository:

```shell
$ mvn
```

The main artifact will be created at:

```shell
dcc-portal-server/target/dcc-portal-server-[version].jar
dcc-portal-server/target/dcc-portal-server-[version]-dist.tar.gz
```

## Running

### Command-line

To run the server without building the UI or packaging the API into a jar, issue the following from the root of the project:

```shell
mvn -pl dcc-portal-server spring-boot:run -Drun.profiles=test
```

One can append `-Drun.arguments="arg1,arg2,arg3"` to pass arguments to the application. See the [spring-boot:run](http://docs.spring.io/spring-boot/docs/current/maven-plugin/run-mojo.html) documentations for details.

### IDE

From Eclipse or IntelliJ:

| Property   | Value                                   |
| ---------- | --------------------------------------- |
| Main Class | `org.icgc.dcc.portal.server.ServerMain` |
| VM Options | `-Xmx6G`                                |
| Arguments  | `--spring.profiles.active=test --spring.config.location=/path/to/application.yml` |

*Note: If Eclipse or IntelliJ are taking a long time to build before running, try excluding
the dcc-portal-ui as a module.*
    
## Modules

Sub-system modules:

- [Portal Server](dcc-portal-server/README.md)
- [Portal UI](dcc-portal-ui/README.md)
- [Portal PQL](dcc-portal-pql/README.md)

## Changes

Change log for the user-facing system modules may be found [here](CHANGES.md).

## Copyright and License

* [License](LICENSE.md)
