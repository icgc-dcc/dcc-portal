package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_OCCURRENCE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_OCCURRENCE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_FIELD;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.COUNT_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FILTERS;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FROM;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_OCCURRENCE_SORT;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_ORDER;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_SIZE;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_ALL_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_BY_ID;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_BY_ID_ERROR;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_ONE_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.NOT_FOUND;
import static org.icgc.dcc.portal.resource.ResourceUtils.OCCURRENCE;
import static org.icgc.dcc.portal.resource.ResourceUtils.RETURNS_COUNT;
import static org.icgc.dcc.portal.resource.ResourceUtils.RETURNS_LIST;
import static org.icgc.dcc.portal.resource.ResourceUtils.S;
import static org.icgc.dcc.portal.resource.ResourceUtils.query;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Occurrence;
import org.icgc.dcc.portal.model.Occurrences;
import org.icgc.dcc.portal.model.Projects;
import org.icgc.dcc.portal.service.OccurrenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Path("/v1/occurrences")
@Produces(APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class OccurrenceResource {

  private final OccurrenceService occurrenceService;

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + OCCURRENCE + S, response = Projects.class)
  public Occurrences findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_OCCURRENCE_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order
      ) {
    ObjectNode filters = filtersParam.get();

    log.info(FIND_ALL_TEMPLATE, new Object[] { size, OCCURRENCE, from, sort, order, filters });

    return occurrenceService.findAll(query().fields(fields).filters(filters).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());

  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + OCCURRENCE + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filters
      ) {
    log.info(COUNT_TEMPLATE, OCCURRENCE, filters.get());

    return occurrenceService.count(query().filters(filters.get()).build());
  }

  @Path("/{" + API_OCCURRENCE_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Occurrence.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = OCCURRENCE + NOT_FOUND) })
  public Occurrence find(
      @ApiParam(value = API_OCCURRENCE_VALUE, required = true) @PathParam(API_OCCURRENCE_PARAM) String occurrenceId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include
      ) {
    log.info(FIND_ONE_TEMPLATE, occurrenceId);

    return occurrenceService.findOne(occurrenceId, query().fields(fields).includes(include).build());
  }
}
