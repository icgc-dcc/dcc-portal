# ICGC DCC - Portal

Parent project of the portal modules

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/29bb5857a70d4861b46cbcc94d569009)](https://www.codacy.com/app/icgc-dcc/dcc-portal?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=icgc-dcc/dcc-portal&amp;utm_campaign=Badge_Grade)

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

See [submodule](#modules) documentation

## Modules

Sub-system modules:

- [Portal Server](dcc-portal-server/README.md)
- [Portal UI](dcc-portal-ui/README.md)
- [Portal PQL](dcc-portal-pql/README.md)

## Changes

Change log for the user-facing system modules may be found [here](CHANGES.md).

## Copyright and License

* [License](LICENSE.md)
