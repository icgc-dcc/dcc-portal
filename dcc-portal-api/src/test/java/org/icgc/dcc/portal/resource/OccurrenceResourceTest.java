package org.icgc.dcc.portal.resource;

import static org.icgc.dcc.portal.resource.ResourceTestUtils.OCCURRENCE;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.OCCURRENCES;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertEntityInstanceOf;
import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertOK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import lombok.val;

import org.icgc.dcc.portal.mapper.NotFoundExceptionMapper;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.Gene;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.OccurrenceService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class OccurrenceResourceTest extends ResourceTest {

  private final static String RESOURCE = "/v1/occurrences";

  @Mock
  OccurrenceService occurrenceService;

  @InjectMocks
  OccurrenceResource occurrenceResource;

  @Override
  protected final void setUpResources() {
    addResource(occurrenceResource);
    addProvider(NotFoundExceptionMapper.class);
  }

  private ClientResponse resource(String path) {
    return client().resource(RESOURCE).path(path).get(ClientResponse.class);
  }

  @Test
  public final void test_findAll() throws IOException {
    when(occurrenceService.findAll(any(Query.class))).thenReturn(OCCURRENCES);

    val response = resource("");

    assertOK(response);
    assertEntityInstanceOf(response, Donors.class);
  }

  @Test
  public void test_count() {
    when(occurrenceService.count(any(Query.class))).thenReturn(1L);

    val response = resource("count");

    assertOK(response);
  }

  @Test
  public final void test_find() throws IOException {
    when(occurrenceService.findOne(any(String.class), any(Query.class))).thenReturn(OCCURRENCE);

    val response = resource("A");

    assertOK(response);
    assertEntityInstanceOf(response, Gene.class);
  }

}
