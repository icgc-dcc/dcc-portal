package org.icgc.dcc.portal.util;

import static com.google.common.base.Strings.repeat;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.test.AbstractSpringIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
@Ignore
public class MappingsReaderTest extends AbstractSpringIntegrationTest {

  /**
   * Dependencies.
   */
  @Autowired
  Client client;

  @Test
  public void testGenerate() throws JsonProcessingException, IOException {
    val indexName = "test17-trim";
    val resolver = new MappingsSourceResolver(client);
    val reader = new MappingsReader(indexName, resolver);

    val mappings = reader.read();
    for (val mapping : mappings) {
      log.info(repeat("-", 100));
      log.info("Mapping '{}'", mapping.getType());
      log.info(repeat("-", 100));

      log.info("{}", mapping.getFields());
    }

    log.info("json: {}", MAPPER.writeValueAsString(mappings));
    log.info("donor-centric = {}", mappings.getMapping("donor-centric"));
    log.info("project._project_id = {}", mappings.getPath("donor-centric", "project._project_id"));
  }

}
