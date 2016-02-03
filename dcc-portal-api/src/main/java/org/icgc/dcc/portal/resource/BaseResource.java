/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.resource;

import static org.icgc.dcc.portal.util.ListUtils.mapList;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public abstract class BaseResource {

  /**
   * Default constants.
   */
  protected static final String DEFAULT_FIELDS = "";
  protected static final String DEFAULT_FILTERS = "{}";
  protected static final String DEFAULT_FACETS = "true";
  protected static final String DEFAULT_SIZE = "10";
  protected static final String DEFAULT_FROM = "1";
  protected static final String DEFAULT_SORT = "_score";
  protected static final String DEFAULT_ORDER = "desc";
  protected static final String DEFAULT_MIN_SCORE = "0";

  /**
   * JAX-RS uri information for building mutation links.
   */
  @Context
  UriInfo uriInfo;

  /**
   * Returns the total matching count independent of paging.
   * 
   * @param response - the search results
   * @return the total matching count
   */
  static long count(SearchResponse response) {
    return response.getHits().totalHits();
  }

  /**
   * Utility method.
   * 
   * @param hit
   * @param fieldName
   * @return
   */
  static Object field(SearchHit hit, String fieldName) {
    return hit.field(fieldName).value();
  }

  /**
   * Utility method to access a search hit partial field in a type safe manner.
   * 
   * @param hit - the search hit
   * @param fieldName - the partial field name
   * @return
   */
  static List<Map<String, Object>> partialField(SearchHit hit, String fieldName) {
    SearchHitField field = hit.field(fieldName);
    if (field == null) {
      return null;
    }

    Map<String, Object> value = field.value();

    return mapList(value.get(fieldName));
  }

  /**
   * Creates a mutation URL from the supplied mutation id.
   * 
   * @param mutationId - the mutation id
   * @return the mutation URL
   */
  URI mutationUrl(String mutationId) {
    return uriInfo //
        .getBaseUriBuilder() //
        .path(MutationResource.class) //
        .path(mutationId) //
        .build();
  }

  /**
   * Readability methods for map building.
   * 
   * @return the map builder
   */
  static Builder<String, Object> response() {
    return ImmutableMap.<String, Object> builder();
  }

  static Builder<String, Object> hit() {
    return ImmutableMap.<String, Object> builder();
  }

  static Builder<String, Object> record() {
    return ImmutableMap.<String, Object> builder();
  }

  static Builder<String, Object> mutation() {
    return ImmutableMap.<String, Object> builder();
  }

  static Builder<String, Object> consequence() {
    return ImmutableMap.<String, Object> builder();
  }

  static Builder<String, Object> fields() {
    return ImmutableMap.<String, Object> builder();
  }

}
