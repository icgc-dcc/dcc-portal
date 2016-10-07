package org.icgc.dcc.portal.server.service;

import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Pagination;
import org.icgc.dcc.portal.server.model.Project;
import org.icgc.dcc.portal.server.model.Projects;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.server.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class ProjectService {

  private final AggregationToFacetConverter aggregationsConverter = AggregationToFacetConverter.getInstance();

  private final ProjectRepository projectRepository;

  public Projects findAll(Query query) {

    val response = projectRepository.findAll(query);
    val hits = response.getHits();
    val projectList = getProjectList(hits, query);

    Projects projects = new Projects(projectList);
    projects.addFacets(aggregationsConverter.convert(response.getAggregations()));
    projects.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return projects;
  }

  private ImmutableList<Project> getProjectList(SearchHits hits, Query query) {
    val projectList = ImmutableList.<Project> builder();
    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, query, EntityType.PROJECT);
      projectList.add(new Project(fieldMap));
    }

    return projectList.build();
  }

  public long count(Query query) {
    return projectRepository.count(query);
  }

  public Project findOne(String projectId, Query query) {
    return new Project(projectRepository.findOne(projectId, query));
  }
}
