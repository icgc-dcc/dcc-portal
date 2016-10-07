package org.icgc.dcc.portal.server.repository;

import java.util.LinkedHashMap;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.IndexType;

public interface Repository {

  SearchResponse findAllCentric(Query query);

  SearchResponse findAll(Query query);

  long count(Query query);

  MultiSearchResponse counts(LinkedHashMap<String, Query> queries);

  MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries);

  // Needed for tests
  SearchRequestBuilder buildFindAllRequest(Query query, IndexType type);

  NestedQueryBuilder buildQuery(Query query);
}
