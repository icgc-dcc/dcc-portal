/*
 * Copyright (c) 2012-2014 The Ontario Institute for Cancer Research. All rights reserved.
 */

/* Script for setting up the dcc-portal schema using H2 1.3.x */

-- Create
CREATE TABLE IF NOT EXISTS user_gene_set(
   id   UUID PRIMARY KEY,
   data TEXT 
);
CREATE TABLE IF NOT EXISTS enrichment_analysis(
   id       UUID PRIMARY KEY,
   version  INT,
   data     TEXT 
);
CREATE TABLE IF NOT EXISTS union_analysis (
   id       UUID PRIMARY KEY,
   version  INT,
   data     TEXT
);
CREATE TABLE IF NOT EXISTS entity_set(
   id       UUID PRIMARY KEY,
   version  INT,
   data     TEXT
);
CREATE TABLE IF NOT EXISTS phenotype_analysis(
   id       UUID PRIMARY KEY,
   version  INT,
   data     TEXT
);
