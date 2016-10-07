package org.icgc.dcc.portal.server.repository;

import static java.util.Collections.singletonMap;
import static org.dcc.portal.pql.meta.Type.PROJECT;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.isRepositoryDonorInProject;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProjectRepository {

  private static final String TYPE_ID = IndexType.PROJECT.getId();
  private static final Map<String, String> FIELD_MAP = FIELDS_MAPPING.get(EntityType.PROJECT);

  private final Client client;
  private final String indexName;
  private final String repoIndexName;

  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();
  private final QueryEngine queryEngine;

  @Autowired
  ProjectRepository(Client client, QueryEngine engine, @Value("#{indexName}") String indexName,
      @Value("#{repoIndexName}") String repoIndexName) {
    this.indexName = indexName;
    this.repoIndexName = repoIndexName;
    this.client = client;
    this.queryEngine = engine;
  }

  public SearchResponse findAll(Query query) {

    val pql = converter.convert(query, PROJECT);
    val search = queryEngine.execute(pql, PROJECT);
    return search.getRequestBuilder().execute().actionGet();
  }

  public long count(Query query) {
    val pql = converter.convertCount(query, PROJECT);
    val search = queryEngine.execute(pql, PROJECT);
    return search.getRequestBuilder().execute().actionGet().getHits().getTotalHits();
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, TYPE_ID, id)
        .setFields(getFields(query, EntityType.PROJECT));
    setFetchSourceOfGetRequest(search, query, EntityType.PROJECT);

    val response = search.execute().actionGet();

    if (response.isExists()) {
      val result = createResponseMap(response, query, EntityType.PROJECT);
      log.debug("Found project: '{}'.", result);

      return result;
    }

    if (!isRepositoryDonorInProject(client, id, repoIndexName)) {
      // We know this is guaranteed to throw a 404, since the 'id' was not found in the first query.
      checkResponseState(id, response, EntityType.PROJECT);
    }

    return singletonMap(FIELD_MAP.get("id"), id);
  }
}
