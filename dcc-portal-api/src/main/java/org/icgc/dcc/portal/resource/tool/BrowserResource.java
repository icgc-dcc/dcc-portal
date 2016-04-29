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
package org.icgc.dcc.portal.resource.tool;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.commons.lang.StringUtils.isBlank;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.BrowserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.wordnik.swagger.annotations.ApiOperation;
import com.yammer.metrics.annotation.Timed;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Genome browser endpoints that mimic the Cellbase API style.
 */
@Component
@Path("/browser")
@Produces(TEXT_PLAIN)
@Setter
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BrowserResource extends Resource {

  private final BrowserService browserService;

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper().setSerializationInclusion(NON_NULL);

  private static final class ParameterNames {

    private static final String SEGMENT = "segment";
    private static final String HISTOGRAM = "histogram";
    private static final String DATATYPE = "dataType";
    private static final String INTERVAL = "interval";
    private static final String RESOURCE = "resource";
    private static final String BIOTYPE = "biotype";
    private static final String CONSEQUENCE_TYPE = "consequence_type";
    private static final String FUNCTIONAL_IMPACT = "functional_impact";
  }

  /**
   * Handles requests for gene objects
   */
  @GET
  @Path("/gene")
  @Timed
  @ApiOperation(value = "Retrieves a list of genes")
  public String getGene(
      @QueryParam(ParameterNames.SEGMENT) String segment,
      @QueryParam(ParameterNames.HISTOGRAM) String histogram,
      @QueryParam(ParameterNames.DATATYPE) String dataType,
      @QueryParam(ParameterNames.INTERVAL) String interval,
      @QueryParam(ParameterNames.RESOURCE) String resource,
      @QueryParam(ParameterNames.BIOTYPE) String bioType,
      @QueryParam(ParameterNames.CONSEQUENCE_TYPE) String consequenceType,
      @QueryParam(ParameterNames.FUNCTIONAL_IMPACT) String functionalImpact) {
    return getData(segment, histogram, dataType, interval, resource, bioType, consequenceType, functionalImpact);
  }

  /**
   * Handles requests for mutation objects.
   */
  @GET
  @Path("/mutation")
  @Timed
  @ApiOperation(value = "Retrieves a list of mutations")
  public String getMutation(
      @QueryParam(ParameterNames.SEGMENT) String segment,
      @QueryParam(ParameterNames.HISTOGRAM) String histogram,
      @QueryParam(ParameterNames.DATATYPE) String dataType,
      @QueryParam(ParameterNames.INTERVAL) String interval,
      @QueryParam(ParameterNames.RESOURCE) String resource,
      @QueryParam(ParameterNames.BIOTYPE) String bioType,
      @QueryParam(ParameterNames.CONSEQUENCE_TYPE) String consequenceType,
      @QueryParam(ParameterNames.FUNCTIONAL_IMPACT) String functionalImpact) {
    return getData(segment, histogram, dataType, interval, resource, bioType, consequenceType, functionalImpact);
  }

  /**
   * Common method used to retrieve data of all types.
   */
  @SneakyThrows
  String getData(String segment, String histogram, String dataType,
      String interval, String resource, String bioType, String consequenceType, String functionalImpact) {

    checkRequest(isBlank(resource), "'resource' parameter is required but missing.");

    val isHistogram = histogram != null && "true".equals(histogram);
    val errorMessage = "Histogram request requires '%s' parameter.";
    checkRequest(isHistogram && isBlank(interval), errorMessage, ParameterNames.INTERVAL);
    checkRequest(isHistogram && Doubles.tryParse(interval) == null, "Bad value '%s' for '%s'", interval,
        ParameterNames.INTERVAL);
    checkRequest(isHistogram && Doubles.tryParse(interval) <= 0, "Historgram requires %s > 0", ParameterNames.INTERVAL);
    checkRequest(!isHistogram && isBlank(segment), "'%s' parameter is required but missing.", ParameterNames.SEGMENT);

    val queryMap = Maps.<String, String> newHashMap();
    queryMap.put(ParameterNames.SEGMENT, segment);
    queryMap.put(ParameterNames.HISTOGRAM, histogram);
    queryMap.put(ParameterNames.DATATYPE, dataType);
    queryMap.put(ParameterNames.INTERVAL, interval);
    queryMap.put(ParameterNames.RESOURCE, resource);
    queryMap.put(ParameterNames.BIOTYPE, bioType);
    queryMap.put(ParameterNames.CONSEQUENCE_TYPE, consequenceType);
    queryMap.put(ParameterNames.FUNCTIONAL_IMPACT, functionalImpact);

    return MAPPER
        .writeValueAsString(isHistogram ? browserService.getHistogram(queryMap) : browserService.getRecords(queryMap));
  }
}