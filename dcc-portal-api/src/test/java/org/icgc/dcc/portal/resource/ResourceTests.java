package org.icgc.dcc.portal.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.portal.model.Donor;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.Gene;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.Mutation;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Occurrence;
import org.icgc.dcc.portal.model.Occurrences;
import org.icgc.dcc.portal.model.Project;
import org.icgc.dcc.portal.model.Projects;
import org.icgc.dcc.portal.model.Query;
import org.mockito.Matchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.ClientResponse;

public class ResourceTests {

  public static final LinkedHashMap<String, Long> COUNT_MAP_REQUEST = Maps.newLinkedHashMap();
  public static final LinkedHashMap<String, LinkedHashMap<String, Long>> NESTED_MAP_REQUEST = Maps.newLinkedHashMap();
  static {
    COUNT_MAP_REQUEST.put("A", 1L);
    COUNT_MAP_REQUEST.put("B", 1L);
    NESTED_MAP_REQUEST.put("A", Maps.<String, Long> newLinkedHashMap());
    NESTED_MAP_REQUEST.put("B", Maps.<String, Long> newLinkedHashMap());
    NESTED_MAP_REQUEST.get("A").put("AA", 1L);
    NESTED_MAP_REQUEST.get("A").put("BB", 1L);
    NESTED_MAP_REQUEST.get("B").put("AA", 1L);
    NESTED_MAP_REQUEST.get("B").put("BB", 1L);
  }

  public static final LinkedHashMap<String, Long> COUNT_MAP_RESULT = Maps.newLinkedHashMap();
  public static final LinkedHashMap<String, LinkedHashMap<String, Long>> NESTED_MAP_RESULT = Maps.newLinkedHashMap();
  static {
    COUNT_MAP_RESULT.put("A", 1L);
    COUNT_MAP_RESULT.put("B", 1L);
    COUNT_MAP_RESULT.put("Total", 2L);
    NESTED_MAP_RESULT.put("A", Maps.<String, Long> newLinkedHashMap());
    NESTED_MAP_RESULT.put("B", Maps.<String, Long> newLinkedHashMap());
    NESTED_MAP_RESULT.get("A").put("AA", 1L);
    NESTED_MAP_RESULT.get("A").put("BB", 1L);
    NESTED_MAP_RESULT.get("A").put("Total", 2L);
    NESTED_MAP_RESULT.get("B").put("AA", 1L);
    NESTED_MAP_RESULT.get("B").put("BB", 1L);
    NESTED_MAP_RESULT.get("B").put("Total", 2L);
  }

  @SuppressWarnings("unchecked")
  public static LinkedHashMap<String, LinkedHashMap<String, Query>> anyNestedCountQuery() {
    return Matchers.any(LinkedHashMap.class);
  }

  @SuppressWarnings("unchecked")
  public static LinkedHashMap<String, Query> anyCountQuery() {
    return Matchers.any(LinkedHashMap.class);
  }

  public static final Project PROJECT = new Project(Maps.<String, Object> newHashMap());

  public static final Projects PROJECTS = new Projects(Lists.<Project> newArrayList(PROJECT));

  public static final Donor DONOR = new Donor(Maps.<String, Object> newHashMap());

  public static final Donors DONORS = new Donors(Lists.<Donor> newArrayList(DONOR));

  public static final Gene GENE = new Gene(Maps.<String, Object> newHashMap());

  public static final Genes GENES = new Genes(Lists.<Gene> newArrayList(GENE));

  public static final Mutation MUTATION = new Mutation(Maps.<String, Object> newHashMap());

  public static final Mutations MUTATIONS = new Mutations(Lists.<Mutation> newArrayList(MUTATION));

  public static final Occurrence OCCURRENCE = new Occurrence(Maps.<String, Object> newHashMap());

  public static final Occurrences OCCURRENCES = new Occurrences(Lists.<Occurrence> newArrayList(OCCURRENCE));

  public static final void assertOK(ClientResponse response) {
    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
  }

  public static final <T> void assertEntityInstanceOf(ClientResponse response, Class<T> expected) throws IOException {
    assertThat(MAPPER.readValue(response.getEntity(String.class), expected)).isInstanceOf(expected);
  }

  public static final <T> void assertEntityEquals(ClientResponse response, T expected) throws IOException {
    assertThat(MAPPER.readTree(response.getEntity(String.class))).isEqualTo(MAPPER.valueToTree(expected));
  }

}
