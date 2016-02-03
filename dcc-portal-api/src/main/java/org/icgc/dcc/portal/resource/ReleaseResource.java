/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Release;
import org.icgc.dcc.portal.model.Releases;
import org.icgc.dcc.portal.service.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Path("/v1/releases")
@Produces(APPLICATION_JSON)
@Api(value = "/releases", description = "Resources relating to releases")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ReleaseResource {

  private static final String DEFAULT_FILTERS = "{}";
  protected static final String DEFAULT_SIZE = "10";
  protected static final String DEFAULT_FROM = "1";
  protected static final String DEFAULT_SORT = "releasedOn";
  protected static final String DEFAULT_ORDER = "desc";

  private final ReleaseService releaseService;

  @GET
  @Timed
  @ApiOperation(value = "Returns a list of releases", response = Releases.class)
  public Releases findAll(
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters,
      @ApiParam(value = "Start index of results") @QueryParam("from") @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]") @QueryParam("size") @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = "Column to sort results on") @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
      @ApiParam(value = "Order to sort the column", allowableValues = "asc,desc") @QueryParam("order") @DefaultValue(DEFAULT_ORDER) String order
      ) {
    log.info("Request for all Releases: {}");

    Releases releases =
        releaseService.findAll(Query.builder().fields(fields).from(from.get()).filters(filters.get())
            .size(size.get()).sort(sort).order(order).build());

    log.info("Returning {}", releases);

    return releases;
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = "Returns a count of releases")
  public long count(
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters
      ) {
    log.info("Request for Release count");

    return releaseService.count(Query.builder().filters(filters.get()).build());
  }

  @Path("/current")
  @GET
  @Timed
  @ApiOperation(value = "Returns the current Release", response = Release.class)
  public Release findAll(
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields
      ) {
    log.info("Request for current Releases: {}");

    Releases releases =
        releaseService
            .findAll(Query.builder().fields(fields).from(0).size(1).sort(DEFAULT_SORT).order("desc").build());

    Release release = releases.getHits().get(0);

    log.info("Returning {}", release);

    return release;
  }
}
