package org.icgc.dcc.portal.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.mapper.NotFoundExceptionMapper;
import org.icgc.dcc.portal.model.Gene;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.Mutation;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Transcript;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class TranscriptResourceTest extends ResourceTest {

  private final static String RESOURCE = "/v1/transcripts";

  @Mock
  MutationService mutationService;
  @Mock
  GeneService geneService;

  @SuppressWarnings("unchecked")
  Genes genes = new Genes(ImmutableList.of(
      new Gene(ImmutableMap.<String, Object> of("transcripts",
          Lists.newArrayList(ImmutableMap.<String, Object> of("id", "TR1"))))));

  Mutations mutations = new Mutations(ImmutableList.of(new Mutation(ImmutableMap.<String, Object> of())));

  @Override
  protected final void setUpResources() {
    addResource(new TranscriptResource(mutationService, geneService));
    addProvider(NotFoundExceptionMapper.class);
  }

  @Test
  public final void testFind() throws IOException {
    when(geneService.findAllCentric(any(Query.class))).thenReturn(genes);
    ClientResponse response = client().resource(RESOURCE).path("TR1").get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(MAPPER.readValue(response.getEntity(String.class), Transcript.class)).isInstanceOf(Transcript.class);
  }

  @Test
  public final void testFind404() {
    when(geneService.findAllCentric(any(Query.class))).thenReturn(new Genes(new ArrayList<Gene>()));
    ClientResponse response = client().resource(RESOURCE).path("TR1").get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public final void testFindMutations() throws IOException {
    when(mutationService.findAllCentric(any(Query.class))).thenReturn(mutations);
    ClientResponse response = client().resource(RESOURCE).path("TR1,TR2/mutations").get(ClientResponse.class);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(MAPPER.readValue(response.getEntity(String.class), Mutations.class)).isInstanceOf(Mutations.class);
  }
}
