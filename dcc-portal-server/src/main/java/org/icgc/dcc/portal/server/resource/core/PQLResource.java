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

import com.yammer.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.resource.Resource;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;

import java.util.List;
import static java.lang.String.format;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.server.resource.Resources.*;

/**
 * Fetch application settings
 */
@Slf4j
@Component
@Api("/PQL")
@Path("/v1/PQL")
@Produces(APPLICATION_JSON)
public class PQLResource extends Resource {
  private static final Jql2PqlConverter QUERY_CONVERTER = Jql2PqlConverter.getInstance();
  @GET
  @Timed
  @ApiOperation(value = "Returns the elastic search query converted to PQL")
  public String getPQL(
    @ApiParam(value=API_QUERY_TYPE_VALUE) @QueryParam(API_QUERY_TYPE_PARAM)
      @DefaultValue(DEFAULT_QUERY_TYPE) Type queryType,
    @ApiParam(value = API_FIELD_VALUE, allowMultiple = true)
      @QueryParam(API_FIELD_PARAM) List<String> fields,
    @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM)
      @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
    @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
    @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM)
      @DefaultValue(DEFAULT_SIZE) IntParam size,
    @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
    @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM)
      @DefaultValue(DEFAULT_ORDER) String order ) {
    val filters = filtersParam.get();
    val query = query(fields, include, filters, from, size, sort, order);
    log.debug(FIND_ALL_TEMPLATE, size, queryType.toString(), from, sort, order, filters);

    val pql=QUERY_CONVERTER.convert(query, queryType);
    log.debug(PQL_TEMPLATE,pql);
    return format("{ %s : %s }",quote("PQL"),quote(pql));
  }

  private String quote(String s) {
    return "\"" + s + "\"";
  }
}