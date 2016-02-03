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

import static org.icgc.dcc.portal.resource.ResourceTestUtils.COUNT_MAP_REQUEST;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.COUNT_MAP_RESULT;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.DONOR;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.DONORS;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.GENES;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.MUTATIONS;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.NESTED_MAP_REQUEST;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.NESTED_MAP_RESULT;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.anyCountQuery;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.anyNestedCountQuery;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertEntityEquals;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertEntityInstanceOf;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertOK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import lombok.val;

import org.icgc.dcc.portal.mapper.NotFoundExceptionMapper;
import org.icgc.dcc.portal.model.Donor;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class DonorResourceTest extends ResourceTest {

  private final static String RESOURCE = "/v1/donors";

  @Mock
  DonorService donorService;

  @Mock
  MutationService mutationService;

  @Mock
  GeneService geneService;

  @InjectMocks
  DonorResource donorResource;

  @Override
  protected final void setUpResources() {
    addResource(donorResource);
    addProvider(NotFoundExceptionMapper.class);
  }

  private ClientResponse resource(String path) {
    return client().resource(RESOURCE).path(path).get(ClientResponse.class);
  }

  @Test
  public final void test_findAll() throws IOException {
    when(donorService.findAllCentric(any(Query.class), any(Boolean.class))).thenReturn(DONORS);

    val response = resource("");

    assertOK(response);
    assertEntityInstanceOf(response, Donors.class);
  }

  @Test
  public void test_count() {
    when(donorService.count(any(Query.class))).thenReturn(1L);

    val response = resource("count");

    assertOK(response);
  }

  @Test
  public final void test_find() throws IOException {
    when(donorService.findOne(any(String.class), any(Query.class))).thenReturn(DONOR);

    val response = resource("A");

    assertOK(response);
    assertEntityInstanceOf(response, Donor.class);
  }

  @Test
  public void test_findGenes() throws IOException {
    when(geneService.findAllCentric(any(Query.class))).thenReturn(GENES);

    val response = resource("A/genes");

    assertOK(response);
    assertEntityInstanceOf(response, Genes.class);
  }

  @Test
  public void test_countGenes() throws IOException {
    when(geneService.count(any(Query.class))).thenReturn(1L);

    val response = resource("A/genes/count");

    assertOK(response);
  }

  @Test
  public void test_countsGenes() throws IOException {
    when(geneService.counts(anyCountQuery())).thenReturn(COUNT_MAP_REQUEST);
    when(geneService.count(any(Query.class))).thenReturn(2L);

    val response = resource("A,B/genes/counts");

    assertOK(response);
    assertEntityEquals(response, COUNT_MAP_RESULT);
  }

  @Test
  public void test_countGeneMutations() throws IOException {
    when(mutationService.count(any(Query.class))).thenReturn(1L);

    val response = resource("A/genes/AA/mutations/count");

    assertOK(response);
  }

  @Test
  public void test_countsGeneMutations() throws IOException {
    when(mutationService.nestedCounts(anyNestedCountQuery())).thenReturn(NESTED_MAP_REQUEST);
    when(mutationService.count(any(Query.class))).thenReturn(2L);

    val response = resource("A,B/genes/AA,BB/mutations/counts");

    assertOK(response);
    assertEntityEquals(response, NESTED_MAP_RESULT);
  }

  @Test
  public void test_findMutations() throws IOException {
    when(mutationService.findAllCentric(any(Query.class))).thenReturn(MUTATIONS);

    val response = resource("A/mutations");

    assertOK(response);
    assertEntityInstanceOf(response, Mutations.class);
  }

  @Test
  public void test_countMutations() {
    when(mutationService.count(any(Query.class))).thenReturn(1L);

    val response = resource("A/mutations/count");

    assertOK(response);
  }

  @Test
  public void test_countsMutations() throws IOException {
    when(mutationService.counts(anyCountQuery())).thenReturn(COUNT_MAP_REQUEST);
    when(mutationService.count(any(Query.class))).thenReturn(2L);

    val response = resource("A,B/mutations/counts");

    assertOK(response);
    assertEntityEquals(response, COUNT_MAP_RESULT);
  }

  @Test
  public void test_countMutationGenes() throws IOException {
    when(geneService.count(any(Query.class))).thenReturn(1L);

    val response = resource("A/mutations/AA/genes/count");

    assertOK(response);
  }

  @Test
  public void test_countsMutationGenes() throws IOException {
    when(geneService.nestedCounts(anyNestedCountQuery())).thenReturn(NESTED_MAP_REQUEST);
    when(geneService.count(any(Query.class))).thenReturn(2L);

    val response = resource("A,B/mutations/AA,BB/genes/counts");

    assertOK(response);
    assertEntityEquals(response, NESTED_MAP_RESULT);
  }
}
