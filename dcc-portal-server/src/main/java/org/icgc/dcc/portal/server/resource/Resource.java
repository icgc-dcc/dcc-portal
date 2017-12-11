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
package org.icgc.dcc.portal.server.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.icgc.dcc.portal.server.util.Collections.isEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

import javax.validation.Validation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.icgc.dcc.common.core.json.JsonNodeBuilders.ObjectNodeBuilder;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.portal.server.model.Error;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.Query.QueryBuilder;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.entity.MutationResource;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.util.JsonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Base classes for all API resources.
 */
@Slf4j
public abstract class Resource {

  /**
   * Default constants.
   */
  protected static final String DEFAULT_FIELDS = "";
  protected static final String DEFAULT_FILTERS = "{}";
  protected static final String DEFAULT_QUERY = "select(*)";
  protected static final String DEFAULT_TYPE = "tsv";
  protected static final String DEFAULT_FACETS = "true";
  protected static final String DEFAULT_SIZE = "10";
  protected static final String DEFAULT_FROM = "1";
  protected static final String DEFAULT_SORT = "_score";
  protected static final String DEFAULT_ORDER = "desc";
  protected static final String DEFAULT_MIN_SCORE = "0";
  protected static final String DEFAULT_QUERY_TYPE="DONOR_CENTRIC";

  protected static final String DEFAULT_PROJECT_SORT = "totalLiveDonorCount";
  protected static final String DEFAULT_OCCURRENCE_SORT = "donorId";
  protected static final String DEFAULT_DONOR_SORT = "ssmAffectedGenes";
  protected static final String DEFAULT_GENE_MUTATION_SORT = "affectedDonorCountFiltered";

  /**
   * Logging template constants.
   */
  protected static final String PQL_TEMPLATE = "PQL query = '{}'";
  protected static final String COUNT_TEMPLATE = "Request for a count of {} with filters '{}'";
  protected static final String FIND_ALL_TEMPLATE =
      "Request for '{}' {} from index '{}', sorted by '{}' in '{}' order with filters '{}'";
  protected static final String FIND_ONE_TEMPLATE = "Request for '{}'";
  protected static final String NESTED_FIND_TEMPLATE = "Request {} for '{}'";
  protected static final String NESTED_COUNT_TEMPLATE = "Request {} count for '{}'";
  protected static final String NESTED_NESTED_COUNT_TEMPLATE = "Request count of '{}' in '{}' affected by '{}'";

  /**
   * Other constants.
   */
  protected static final Joiner COMMA_JOINER = Joiners.COMMA.skipNulls();
  protected static final Splitter COMMA_SPLITTER = Splitters.COMMA.omitEmptyStrings().trimResults();

  /**
   * JAX-RS URI information for building mutation links.
   */
  @Context
  protected UriInfo uriInfo;

  /**
   * Creates a mutation URL from the supplied mutation id.
   * 
   * @param mutationId - the mutation id
   * @return the mutation URL
   */
  protected URI mutationUrl(String mutationId) {
    return uriInfo
        .getBaseUriBuilder()
        .path(MutationResource.class)
        .path(mutationId)
        .build();
  }

  protected boolean hasParam(String name) {
    return uriInfo.getQueryParameters().containsKey(name);
  }

  /**
   * Readability methods for map building.
   * 
   * @return the map builder
   */
  protected static Builder<String, Object> response() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static Builder<String, Object> hit() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static Builder<String, Object> record() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static Builder<String, Object> mutation() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static Builder<String, Object> consequence() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static Builder<String, Object> fields() {
    return ImmutableMap.<String, Object> builder();
  }

  protected static FiltersParam filtersParam(ObjectNode objectNode) {
    return new FiltersParam(objectNode.toString());
  }

  protected static FiltersParam filtersParam(ObjectNodeBuilder builder) {
    return filtersParam(builder.end());
  }

  protected static ObjectNode mergeFilters(ObjectNode filters, String template, Object... objects) {
    return JsonUtils.merge(filters, (new FiltersParam(String.format(template, objects)).get()));
  }

  protected static List<String> commaValues(String text) {
    return COMMA_SPLITTER.splitToList(text);
  }

  protected static QueryBuilder query() {
    return Query.builder();
  }

  protected static Query query(FiltersParam filters) {
    return query(filters.get());
  }

  protected static Query query(ObjectNode filters) {
    return query().filters(filters).build();
  }

  protected static Query query(List<String> fields, List<String> include, ObjectNode filters,
      IntParam from, IntParam size, String sort, String order) {
    val query = query()
        .fields(fields).filters(filters)
        .from(from.get()).size(size.get())
        .sort(sort).order(order);

    clean(include);
    if (!include.isEmpty()) {
      query.includes(include);
    }

    return query.build();
  }

  protected static LinkedHashMap<String, Query> queries(ObjectNode filters, String filterTemplate,
      List<String> ids) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  protected static LinkedHashMap<String, Query> queries(ObjectNode filters, String filterTemplate,
      List<String> ids,
      String anchorId) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id, anchorId);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  protected static LinkedHashMap<String, LinkedHashMap<String, Query>> queries(ObjectNode filters,
      String filterTemplate,
      List<String> ids,
      List<String> anchorIds) {
    val queries = Maps.<String, LinkedHashMap<String, Query>> newLinkedHashMap();

    for (String anchorId : anchorIds) {
      queries.put(anchorId, queries(filters, filterTemplate, ids, anchorId));
    }

    return queries;
  }

  protected static void validate(@NonNull Object object) {
    val errorMessages = new ArrayList<String>();
    val validator = Validation.buildDefaultValidatorFactory().getValidator();

    val violations = validator.validate(object);
    if (!violations.isEmpty()) {
      for (val violation : violations) {
        errorMessages.add("'" + violation.getPropertyPath() + "' " + violation.getMessage());
      }

      throw new BadRequestException(COMMA_JOINER.join(errorMessages));
    }
  }

  protected static Response error(Status status, String message) {
    return Response
        .status(status)
        .type(APPLICATION_JSON_TYPE)
        .entity(new Error(status, message))
        .build();
  }

  protected static <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String enumName) {
    if (enumName == null) {
      return false;
    }
    try {
      Enum.valueOf(enumClass, enumName);
      return true;
    } catch (final IllegalArgumentException ex) {
      return false;
    }
  }

  protected static void checkRequest(boolean errorCondition, String formatTemplate, Object... args) {
    if (errorCondition) {
      // We don't want exception within an exception-handling routine.
      final Supplier<String> errorMessageProvider = () -> {
        try {
          return format(formatTemplate, args);
        } catch (Exception e) {
          final String errorDetails = "message: '" + formatTemplate +
              "', parameters: '" + COMMA_JOINER.join(args) + "'";
          log.error("Error while formatting message - " + errorDetails, e);

          return "Invalid web request - " + errorDetails;
        }
      };

      throw new BadRequestException(errorMessageProvider.get());
    }
  }

  protected static List<String> clean(List<String> source) {
    if (isEmpty(source)) {
      return source;
    }

    source.removeAll(newArrayList("", null));

    return source;
  }

  protected CacheControl noCache() {
    val cacheControl = new CacheControl();
    cacheControl.setNoCache(true);

    return cacheControl;
  }

}
