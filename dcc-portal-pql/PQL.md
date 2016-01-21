# Portal Query Language

## Intended Audience
This document is intended for developers working on or planning on contributing to the dcc-portal
project which powers the [ICGC Data Portal](https://dcc.icgc.org).

This document will go over the main concepts, code, and architecture of the PQL Module powering
the Portal's query engine.

## Learning Objectives
* Familiarity with the design of the PQL module.
* Exposure to and some familiarity with the grammar.
* Understanding the intent and purpose of PQL. 

## Terms
* ICGC - International Cancer Genome Consortium
* DCC - The ICGC Data Coordination Center
* JQL - JSON Query Language
* PQL - Portal Query Language
* RQL - Resource Query Language
* AST - Abstract Syntax Tree
* ANTLR - ANother Tool for Language Recognition

## Querying In The Portal
This section will provide a fairly high level representation of how querying works in the Data Portal.

### The Query Languages
The Data Portal primarily concerns itself with two query languages.

#### JQL
JSON Query Language. JSON representation of a filter to be applied to our data model.
Primarily used by the UI to communicate the desired query to the API.

Example of JQL query where we are filtering for donors where the primary cancer site is blood,
gene type is protein coding, and functional impact is high.
```json
{"donor":{"primarySite":{"is":["Blood"]}},"gene":{"type":{"is":["protein_coding"]}},"mutation":{"functionalImpact":{"is":["High"]}}}
```

#### PQL
Portal Query Language. Consumed by the PQL Module to generate Elasticsearch queries.

An example query where we want the id and age of a donor with id of 1:
```
select(id,age),eq(donor.id,1)
```

### Converter to Query Engine

#### Converter
`org.icgc.dcc.portal.pql.convert.Jql2PqlConverter`

As queries come into the portal in the form of JQL they are converted to PQL in order to be
used by the query engine. This class will converting the incoming Query object into a PQL String.

#### Parser
 `org.dcc.portal.pql.query.PqlParser`
 
This class is responsible for taking a PQL String and generating an object representation of the AST described
by the string. 

#### Query Engine
`org.dcc.portal.pql.query.QueryEngine`

The query engine consumes the PQL AST object and is responsible for creating the `SearchRequestBuilder` object
used for elasticsearch. It encapsulates this object inside a `QueryRequest` object. 

## Design of PQL

### Inspiration

### ANTLR

### Limitation