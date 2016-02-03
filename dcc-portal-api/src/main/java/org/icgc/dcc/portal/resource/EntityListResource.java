/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

package org.icgc.dcc.portal.resource;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ASYNC;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_DEFINITION_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_ID_VALUE;
import static org.icgc.dcc.portal.util.MediaTypes.TEXT_TSV;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySetDefinition;
import org.icgc.dcc.portal.model.UuidListParam;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.EntityListService;
import org.icgc.dcc.portal.service.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * DropWizard end-points that provide various functionalities for entity sets.
 */

@Slf4j
@Component
@Path("/v1/entityset")
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class EntityListResource {

  private final static String TYPE_ATTACHMENT = "attachment";
  private static final String EXPORT_FILE_EXTENSION = ".tsv";

  @NonNull
  private final EntityListService service;

  private List<EntitySet> getEntityListsByIds(@NonNull final Set<UUID> ids) {
    val result = new ArrayList<EntitySet>(ids.size());
    for (val id : ids) {
      // Should implement @BindIn in the EntityListRepository to allow the IN clause instead of doing a loop here.
      val list = service.getEntityList(id);
      if (null != list) {
        result.add(list);
      }
    }
    return result;
  }

  @GET
  @Path("/sets/{" + API_ENTITY_LIST_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves a list of entity sets by their IDs.", response = EntitySet.class, responseContainer = "List")
  public List<EntitySet> getEntityLists(
      @ApiParam(value = API_ENTITY_LIST_ID_VALUE, required = true) @PathParam(API_ENTITY_LIST_ID_PARAM) final UuidListParam entityListIds) {
    Set<UUID> listIds = null;
    try {
      listIds = entityListIds.get();

    } catch (Exception e) {
      log.error("Exception occurred while parsing the UUID list from web request: '{}'", entityListIds);
      log.error("The exception while parsing the UUID list is: ", e);
      throw new BadRequestException("Unable to parse the entitySetId parameter.");
    }
    log.debug("Received a getEntityLists request for these lists: '{}'", listIds);

    return getEntityListsByIds(listIds);
  }

  @GET
  @Path("/{" + API_ENTITY_LIST_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet getEntityList(
      @ApiParam(value = API_ENTITY_LIST_ID_VALUE, required = true) @PathParam(API_ENTITY_LIST_ID_PARAM) final UUID entityListId) {
    val result = getEntityListsByIds(Sets.newHashSet(entityListId));
    if (result.isEmpty()) {
      log.error("Error: getEntityListsByIds returns empty. The entityListId '{}' is most likely invalid.",
          entityListId);
      throw new NotFoundException(entityListId.toString(), API_ENTITY_LIST_ID_VALUE);
    } else {
      return result.get(0);
    }
  }

  /**
   * This hits the root path of /v1/entityset
   * 
   * @param listDefinition EntitySet definition from client.
   * @param async Defaults to true. Set to false if a synchronous request is needed.
   * @return JSON representation of new entity set.
   */
  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Advanced Search query.", response = EntitySet.class)
  public Response createList(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final EntitySetDefinition listDefinition,
      @ApiParam(value = API_ASYNC) @QueryParam("async") @DefaultValue("true") final boolean async) {
    val newList = service.createEntityList(listDefinition, async);

    return newListResponse(newList);
  }

  /**
   * Endpoint used for creating an entity set from the external repository.
   * 
   * @param listDefinition EntitySet definition from client.
   * @return JSON representation of new entity set.
   */
  @POST
  @Path("/external")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Advanced Search query.", response = EntitySet.class)
  public Response createExternalList(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final EntitySetDefinition listDefinition) {
    val newList = service.createExternalEntityList(listDefinition);

    return newListResponse(newList);
  }

  /**
   * Endpoint for creating an entity set from files from a single repository
   * 
   * @param listDefinition EntitySet definition from client.
   * @return JSON representation of new entity set.
   */
  @POST
  @Path("/file")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Repository Query.", response = EntitySet.class)
  public Response createFileSet(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final EntitySetDefinition listDefinition) {
    val filters = listDefinition.getFilters();
    val repoList = (ArrayNode) filters.path("file").path("repoName").path("is");
    if (!repoList.isMissingNode() && repoList.size() == 1) {
      val newList = service.createFileEntitySet(listDefinition);
      return newListResponse(newList);
    } else {
      throw new BadRequestException("Need to filter by exactly one Repository.");
    }
  }

  @POST
  @Path("/union")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set by combining two or more existing sets.", response = EntitySet.class)
  public Response deriveList(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final DerivedEntitySetDefinition listDefinition,
      @ApiParam(value = API_ASYNC) @QueryParam("async") @DefaultValue("true") final boolean async) {
    val newList = service.computeEntityList(listDefinition, async);

    return newListResponse(newList);
  }

  private static Response newListResponse(final EntitySet newList) {
    return Response.status(CREATED)
        .entity(newList)
        .build();
  }

  private static String getFileName(final EntitySet list) {
    return list.getType().getName() + "-ids-for-set-" + list.getName() + EXPORT_FILE_EXTENSION;
  }

  @GET
  @Path("/{" + API_ENTITY_LIST_ID_PARAM + "}/export")
  @Produces(TEXT_TSV)
  @ApiOperation(value = "Exports the data of a set as a download in TSV (tab-delimited) format.", response = EntitySet.class)
  public Response exportListItems(
      @ApiParam(value = API_ENTITY_LIST_ID_VALUE, required = true) @PathParam(API_ENTITY_LIST_ID_PARAM) final UUID entityListId) {
    val list = getEntityList(entityListId);

    if (EntitySet.State.FINISHED != list.getState()) {
      // We return a 204 if the list is not ready.
      return null;
    }

    val streamingHandler = new StreamingOutput() {

      @Override
      public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        service.exportListItems(list, outputStream);
      }
    };
    val attechmentType = type(TYPE_ATTACHMENT)
        .fileName(getFileName(list))
        .creationDate(new Date())
        .build();

    return Response
        .ok(streamingHandler)
        .header(CONTENT_DISPOSITION, attechmentType)
        .build();
  }
}
