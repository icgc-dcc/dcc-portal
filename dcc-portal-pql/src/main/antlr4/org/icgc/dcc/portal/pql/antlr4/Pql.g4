// Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
//
// Description:
//  Grammar definition for Portal Query Language.

grammar Pql;

statement
  : (filter | function) (COMMA (filter | function))*
  | count (COMMA (filter | aggs))*
  | (filter | function) (COMMA (filter | function))* COMMA order
  | (filter | function) (COMMA (filter | function))* COMMA range
  | (filter | function) (COMMA (filter | function))* COMMA order COMMA range
  ;
	
count
  : 'count' OPAR CPAR
  ;

function
  : selects
  | aggs
  ;
  
selects
  : 'select' OPAR ID (COMMA ID)* CPAR       # select
  | 'select' OPAR ASTERISK CPAR             # select
  ;

aggs
  : 'facets' OPAR ID (COMMA ID)* CPAR       # facets
  | 'facets' OPAR ASTERISK CPAR             # facets
  ;

filter
  : 'nested' OPAR ID (COMMA filter)+ CPAR   # nested
  | 'not' OPAR filter CPAR                  # not
  | 'exists' OPAR ID CPAR                   # exists
  | 'missing' OPAR ID CPAR                  # missing
  | eq                                      # equal
  | ne                                      # notEqual
  | gt                                      # greaterThan
  | ge                                      # greaterEqual
  | lt                                      # lessThan
  | le                                      # lessEqual
  | in                                      # inArray
  | OPAR filter CPAR                        # group
  | 'and' OPAR filter (COMMA filter)+ CPAR  # and
  | filter '&' filter                       # and
  | 'or' OPAR filter (COMMA filter)+ CPAR   # or
  | filter '|' filter                       # or
  ;

eq
  : 'eq' OPAR ID COMMA value CPAR
  | ID '=' value
  ;

ne
  : 'ne' OPAR ID COMMA value CPAR
  | ID '!=' value
  ;

gt
  : 'gt' OPAR ID COMMA value CPAR
  | ID '=gt=' value
  ;

ge
  : 'ge' OPAR ID COMMA value CPAR
  | ID '=ge=' value
  ;

lt
  : 'lt' OPAR ID COMMA value CPAR
  | ID '=lt=' value
  ;

le
  : 'le' OPAR ID COMMA value CPAR
  | ID '=le=' value
  ;

in
  : 'in' OPAR ID (COMMA value)+ CPAR
  | ID '=' value (COMMA value)+
  ;

order
  : 'sort' OPAR (SIGN)? ID (COMMA (SIGN)? ID)* CPAR
  ;

range
  : 'limit' OPAR INT CPAR
  | 'limit' OPAR INT COMMA INT CPAR
  ;

 
value
  : STRING
  | FLOAT
  | INT
  ;
 
/*------------------------------------------------------------------------------
 * LEXER RULES
 *----------------------------------------------------------------------------*/

ASTERISK
  : '*'
  ;

COMMA
  : ','
  ;

OPAR 
  : '(' 
  ;

CPAR
  : ')' 
  ;

ID
  : [a-zA-Z_] [a-zA-Z_0-9\.]*
  ;

SIGN
  : '-'
  | '+'
  ;

STRING
  : '"' ('*'|'%')? (~[*%"\r\n])* ('*'|'%')? '"'
  | '\'' ('*'|'%')? (~[*%'\r\n])* ('*'|'%')? '\''
  ;

FLOAT
  : (SIGN)? INT '.' [0-9]* 
  | (SIGN)? '.' [0-9]+
  ;
    
INT
  : (SIGN)? [0-9]+
  ;

WS
  : [ \t\r\n]+ -> skip
  ;
