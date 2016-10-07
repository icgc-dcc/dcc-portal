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
package org.icgc.dcc.portal.server.resource.core;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.portal.server.model.Keywords;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.FileService;
import org.icgc.dcc.portal.server.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Path("/v1/keywords")
@Produces(APPLICATION_JSON)
@Api(value = "/keyword", description = "Resources relating to keyword search")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class SearchResource extends Resource {

  private static final Keywords EMPTY_RESULT = new Keywords(emptyList());
  private static final String DEFAULT_FILTERS = "{}";
  private static final ImmutableMap<String, String> FIELD_MAPPING = FIELDS_MAPPING.get(EntityType.KEYWORD);

  protected static final String DEFAULT_SIZE = "10";
  protected static final String DEFAULT_FROM = "1";
  protected static final String DEFAULT_SORT = "_score";
  protected static final String DEFAULT_ORDER = "desc";

  private final SearchService searchService;
  private final FileService repositoryService;

  @GET
  @Timed
  @ApiOperation(value = "Returns a list of keyword results")
  public Keywords findAll(
      @ApiParam(value = "Keyword search") @QueryParam("q") @DefaultValue("") String q,
      @ApiParam(value = "Type") @QueryParam("type") @DefaultValue("") String type,
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters,
      @ApiParam(value = "Start index of results") @QueryParam("from") @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]") @QueryParam("size") @DefaultValue(DEFAULT_SIZE) IntParam size) {
    log.debug("Request for keyword search: {}", q);

    if (q.isEmpty()) {
      return EMPTY_RESULT;
    }

    val unknownFields = fields.stream()
        .filter(f -> FIELD_MAPPING.get(f) == null)
        .collect(toImmutableList());

    checkRequest(unknownFields.size() > 0,
        "The following fields are not supported by keyword search: %s",
        unknownFields.toString());

    val query = Query.builder().query(q);
    val keywords = type.equals("file-donor") ? repositoryService.findRepoDonor(query.build()) : searchService.findAll(
        query.fields(fields)
            .from(from.get())
            .filters(filters.get())
            .size(size.get())
            .sort(DEFAULT_SORT)
            .order(DEFAULT_ORDER)
            .build(),
        type);

    log.debug("Returning keyword search result: '{}'.", keywords);

    return keywords;
  }
}
