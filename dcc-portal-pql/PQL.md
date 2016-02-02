# Portal Query Language

1. [Indended Audience](#indended-audience)
2. [Learning Objectives](#learning-objectives)
3. [Terms](#terms)
4. [Querying In The Portal](#querying-in-the-portal)
 1. [The Query Languages](#the-query-languages) 
 2. [Converter to Query Engine](#converter-to-query-engine)
5. [Design of PQL](#design-of-pql)
 1. [Inspiration](#inspiration)
 2. [ANTLR](#antlr)
 3. [Visitors](#visitors)
 4. [Type Model](#type-model)
 5. [Limitations](#limitations)
 6. [Adding a new Type](#adding-a-new-type)
6. [Additional Reading](#additional-reading)
7. [Useful Links](#useful-links)

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

![High Level](docs/high-level.png)
This diagram provides a high level view of the steps a query takes before being executed 
against elastic search. The visitors will be described in a later section. 

#### Converter
`org.icgc.dcc.portal.pql.convert.Jql2PqlConverter`

As queries come into the portal in the form of JQL they are converted to PQL in order to be
used by the query engine. This class will convert the incoming Query object into a PQL String.

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
The grammar and syntax of PQL is largely inspired by RQL. 
Looking at the operators like `eq`, `ne`, `gt`, `lt`, and functions such as `in`, `sort`, `select` the similarities 
are quite clear. 

Also, very importantly, the readability of PQL is much greater than that of Elasticsearch. 

There was the intention to one day allow the UI to utilize PQL for its queries in a sort of 'advanced' mode. Similar to how JIRA allows a user
to type in a manual query rather then relying on the UI controls, it may be possible one day for advanced users
of the portal to enter direct PQL queries.

### ANTLR
ANTRL is the tool used for generating the lexer and parser classes which are based off of the grammar file we defined. 

The grammar file [Pql.g4](src/main/antl4/org/icgc/dcc/portal/pql/antrl4/Pql.g4) contains the definition the language. 
It is worthwhile to give the file a quick read to gain a basic understanding of how statements are constructed in PQL. 

### Visitors
The visitors are at the heart of how the PQL module functions. 
Their function is to visit the nodes of an AST and to convert them into an AST for the next language. 

* `CreatePqlAstVisitor` - Responsible for taking the generated parse tree from a PQL String and to convert it into an object
representing our PQL AST. The parse tree is created from functionality provided by ANTLR.
* `CreateEsAstVisitor` -  Responsible for traversing the PQL AST and returning an Elasticsearch AST representation of our query.

Once the Query Engine has the Elasticsearch AST, extra steps are performed on it to ensure it fits the requirements imposed by our data
model:
* Resolving special cases
* Resolving nested filters
* Resolving facets
* Adding score scripts
* Optimizing

Only then is the Elasticsearch AST
resolved into a SearchRequestBuilder used by the Elasticsearch API.

### Type Model
`org.dcc.portal.pql.meta`

The classes in this package represents a document type that the PQL infrastructure can query. These classes extend the abstract class
`TypeModel`. All of these classes have to provide some important information about their document type. It can be summarized as:
* The `Type` of the document
* The available fields
* The aliases for the fields
* The available facets

### Limitations
Since PQL is purpose designed for our data model it is inherently resistant to large changes to that data model. Changes to field names, nesting, 
and certain special case fields can all require code changes. Introducing new types also requires a new class to be written. 

Furthermore, not all Elasticsearch queries can be represented in PQL. Most notably multisearch and multi-index queries will need to be
written directly with the Elasticsearch API rather than using PQL. 

### Adding a new Type
This section will provide a high level explenation of how to add a new type to the Type Model.

1. Add your new type to the enum `org.dcc.portal.pql.meta.Type`
```java
public enum Type {
  ...
  FOO("foo","foo")
}
```

2. Create a new class in `org.dcc.portal.pql.meta` 
```java
/**
 * Type model of Foo index type
 */
public class FooTypeModel extends TypeModel {...
```

3. Implement the constructure and abstract methods.
```java
public FooTypeModel() {
  super(fields,internalAliases,allowedAliases,includeFields);
}
public Type getType()
public String prefix()
public List<String> getFacets()
```

4. Make your new type available by adding it to the `org.dcc.portal.pql.meta.IndexModel` class.
```java
...
private static final FooTypeModel FOO_TYPE_MODEL = new FooTypeModel();
...
case FOO:
      return FOO_TYPE_MODEL;
```

## Additional Reading
 
* [Data Model](../MODEL.md)
* [UI](../dcc-portal-ui/UI.md)
* [API](../dcc-portal-api/API.md)

## Useful Links

* [Elasticsearch](https://www.elastic.co/products/elasticsearch)
* [ANTRL](http://www.antlr.org/)
* [Wikipedia: Abstract Syntax Tree](https://en.wikipedia.org/wiki/Abstract_syntax_tree)
* [DCC Documentation](http://docs.dcc.icgc.org/)