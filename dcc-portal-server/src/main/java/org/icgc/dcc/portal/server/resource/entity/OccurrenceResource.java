/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.server.resource.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yammer.metrics.annotation.Timed;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icgc.dcc.portal.server.model.Occurrence;
import org.icgc.dcc.portal.server.model.Occurrences;
import org.icgc.dcc.portal.server.model.Projects;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.OccurrenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.server.resource.Resources.*;

@Component
@Api("/occurrences")
@Path("/v1/occurrences")
@Produces(APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class OccurrenceResource extends Resource {

  private final OccurrenceService occurrenceService;

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + OCCURRENCE + S, response = Occurrence.class)
  public Occurrences findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_OCCURRENCE_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();

    log.debug(FIND_ALL_TEMPLATE, new Object[] { size, OCCURRENCE, from, sort, order, filters });

    return occurrenceService.findAll(query().fields(fields).filters(filters).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());

  }

  @GET
  @Timed
  @Path("/pql")
  @ApiOperation(value = RETURNS_LIST + OCCURRENCE + S, response = Occurrence.class)
  public  Occurrences findPQL(
      @ApiParam(value = API_QUERY_VALUE) @QueryParam(API_QUERY_PARAM) @DefaultValue(DEFAULT_QUERY) String pql
  ) {

    log.debug(PQL_TEMPLATE,  pql);

    return occurrenceService.findAll(pql, Collections.emptyList());
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + OCCURRENCE + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filters) {
    log.debug(COUNT_TEMPLATE, OCCURRENCE, filters.get());

    return occurrenceService.count(query().filters(filters.get()).build());
  }

  @Path("/{" + API_OCCURRENCE_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Occurrence.class)
  @ApiResponses(value = { @ApiResponse(code = 404, message = OCCURRENCE + NOT_FOUND) })
  public Occurrence find(
      @ApiParam(value = API_OCCURRENCE_VALUE, required = true) @PathParam(API_OCCURRENCE_PARAM) String occurrenceId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.info(FIND_ONE_TEMPLATE, occurrenceId);

    return occurrenceService.findOne(occurrenceId, query().fields(fields).includes(include).build());
  }
}
