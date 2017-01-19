/*
 * Copyright (c) 2012-2014 The Ontario Institute for Cancer Research. All rights reserved.
 */

/* Script for setting up the dcc-portal schema using PostgreSQL 9.2.4 */

-- CREATE DATABASE dcc_portal;
-- CREATE USER dcc WITH PASSWORD 'dcc';
-- GRANT ALL PRIVILEGES ON DATABASE dcc_portal to dcc;
-- 
-- GRANT SELECT, INSERT ON user_gene_set TO dcc;

CREATE TABLE IF NOT EXISTS user_gene_set(
   id   UUID NOT NULL,
   data TEXT NOT NULL,
   
   PRIMARY KEY(id) 
);
GRANT SELECT, INSERT, UPDATE, DELETE ON user_gene_set TO dcc;

--------------------------------------------------------------------------------
 
CREATE TABLE IF NOT EXISTS enrichment_analysis(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,
   
   PRIMARY KEY(id) 
);
GRANT SELECT, INSERT, UPDATE, DELETE ON enrichment_analysis TO dcc;

-------------------------------------------------------------------------------- 

CREATE TABLE IF NOT EXISTS union_analysis(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON union_analysis TO dcc;

-------------------------------------------------------------------------------- 

CREATE TABLE IF NOT EXISTS entity_set(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON entity_set TO dcc;

-------------------------------------------------------------------------------- 

CREATE TABLE IF NOT EXISTS phenotype_analysis(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON phenotype_analysis TO dcc;

-------------------------------------------------------------------------------- 

CREATE TABLE IF NOT EXISTS survival_analysis(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON survival_analysis TO dcc;

-------------------------------------------------------------------------------- 

CREATE TABLE IF NOT EXISTS manifest(
   id      UUID NOT NULL,
   version INT,   
   data    TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON manifest TO dcc;

--------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS oncogrid_analysis(
   id   UUID NOT NULL,
   version INT,
   data TEXT NOT NULL,

   PRIMARY KEY(id)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON oncogrid_analysis TO dcc;

--------------------------------------------------------------------------------
 
