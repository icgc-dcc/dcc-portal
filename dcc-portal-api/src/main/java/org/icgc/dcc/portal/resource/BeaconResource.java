/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.common.core.model.Chromosome;
import org.icgc.dcc.portal.model.AlleleParam;
import org.icgc.dcc.portal.model.Beacon;
import org.icgc.dcc.portal.model.BeaconInfo;
import org.icgc.dcc.portal.service.BeaconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.primitives.Doubles;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Component
@Api(value = "/beacon", description = "Resources relating to the ICGC GA4GH Beacon")
@Path("/v1/beacon")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class BeaconResource extends BaseResource {

  private final BeaconService beaconService;
  private static final String ANY_DATASET = " ";

  @GET
  @ApiOperation(value = "Query the Beacon", nickname = "query", response = Beacon.class, notes = "<strong>Answers the question: \"Have you observed this genotype?\"</strong>"
      + "<br>A GA4GH Beacon based off of the v0.2 specification. Given a position in a chromosome and an alllele,"
      + " the beacon looks for matching mutations at that location and returns a response accordingly.<br/><br/> Read "
      + "more about beacons and see other beacons at <a href=http://ga4gh.org/#/beacon>GAG4GH's Beacon Project Site</a>.")
  @Path("/query")
  @Timed
  public Beacon query(

  @ApiParam(value = "Chromosome ID: 1-22, X, Y, MT", required = true) @QueryParam("chromosome") String chromosome,

  @ApiParam(value = "Position (1-based)", required = true) @QueryParam("position") String position,

  @ApiParam(value = "Genome ID: GRCh\\d+", required = true) @QueryParam("reference") String reference,

  @ApiParam(value = "Alleles: [ACTG]+", required = true) @QueryParam("allele") AlleleParam allele,

  @ApiParam(value = "Dataset to be queried (Project ID)") @QueryParam("dataset") String dataset

  ) {
    // Validate
    val parsedChromosome = Chromosome.byExpression(chromosome);
    val parsedPosition = parsedChromosome.parsePosition(position);

    checkRequest(!isValidReference(reference), "Reference param ('%s') is empty or invalid (must be GRCh\\d+)",
        reference);

    if (isNullOrEmpty(dataset)) {
      dataset = ANY_DATASET;
    }

    return beaconService.query(parsedChromosome.getName(), parsedPosition,
        reference.trim(),
        allele.get(),
        dataset.trim());
  }

  @GET
  @ApiOperation(value = "Get information about the Beacon", nickname = "info", response = BeaconInfo.class, notes = "Provides information about the Beacon")
  @Path("/info")
  @Timed
  public BeaconInfo info() {
    return new BeaconInfo();
  }

  private Boolean isValidReference(String ref) {
    if (isNullOrEmpty(ref)) {
      return false;
    }
    ref = ref.trim();
    return ref.startsWith("GRCh") && Doubles.tryParse(ref.substring(4)) != null;
  }

}
