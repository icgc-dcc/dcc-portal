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

import static com.sun.jersey.api.client.ClientResponse.Status.ACCEPTED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.PENDING;
import static org.icgc.dcc.portal.model.Universe.REACTOME_PATHWAYS;
import static org.icgc.dcc.portal.util.Filters.emptyFilter;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import lombok.val;

import org.icgc.dcc.portal.mapper.BadRequestExceptionMapper;
import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.EnrichmentParams;
import org.icgc.dcc.portal.service.EnrichmentAnalysisService;
import org.icgc.dcc.portal.test.ContextInjectableProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.representation.Form;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class EnrichmentAnalysisResourceTest extends ResourceTest {

  /**
   * Endpoint.
   */
  private final static String RESOURCE = "/v1/analysis/enrichment";

  /**
   * Dependencies.
   */
  @Mock
  private EnrichmentAnalysisService service;
  @Mock
  private HttpContext context;

  /**
   * Subject
   */
  @InjectMocks
  private EnrichmentAnalysisResource resource;

  @Override
  protected final void setUpResources() {
    addResource(resource);
    addProvider(new ContextInjectableProvider<HttpContext>(HttpContext.class, context));
    addProvider(BadRequestExceptionMapper.class);
  }

  @Test
  public void testGet() {
    // Resource id
    val analysisId = UUID.randomUUID();
    val state = FINISHED;
    when(service.getAnalysis(analysisId))
        .thenReturn(new EnrichmentAnalysis().setId(analysisId).setState(state));

    // Execute
    val response = client()
        .resource(RESOURCE)
        .path(analysisId.toString())
        .accept(MediaType.APPLICATION_JSON)
        .get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

    val analysis = response.getEntity(EnrichmentAnalysis.class);
    assertThat(analysis.getState()).isEqualTo(state);
    assertThat(analysis.getId()).isEqualTo(analysisId);
  }

  @Test
  public void testSubmit() throws JsonProcessingException {
    // Analysis
    val formData = new Form();
    formData.add("params", MAPPER.writeValueAsString(new EnrichmentParams().setUniverse(REACTOME_PATHWAYS)));

    // Query
    formData.add("filters", emptyFilter());
    formData.add("sort", "affectedDonorCountFiltered");
    formData.add("order", "asc");

    // Mock
    val state = PENDING;

    // Execute
    val response = client()
        .resource(RESOURCE)
        .post(ClientResponse.class, formData);

    assertThat(response.getStatus()).isEqualTo(ACCEPTED.getStatusCode());

    val analysis = response.getEntity(EnrichmentAnalysis.class);
    assertThat(analysis.getState()).isEqualTo(state);
    assertThat(analysis.getQuery()).isNotNull();
  }

}
