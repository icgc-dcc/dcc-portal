/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.resource.set;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.icgc.dcc.portal.resource.Resources.API_ASYNC;
import static org.icgc.dcc.portal.resource.Resources.API_ENTITY_SET_DEFINITION_VALUE;
import static org.icgc.dcc.portal.resource.Resources.API_ENTITY_SET_ID_PARAM;
import static org.icgc.dcc.portal.resource.Resources.API_ENTITY_SET_ID_VALUE;
import static org.icgc.dcc.portal.util.MediaTypes.TEXT_TSV;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.model.BaseEntitySet.Type;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySetDefinition;
import org.icgc.dcc.portal.model.UUIDSetParam;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.EntitySetService;
import org.icgc.dcc.portal.service.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * End-points that provide various functionalities for entity sets.
 */
@Slf4j
@Component
@Path("/v1/entityset")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EntitySetResource extends Resource {

  /**
   * Constants.
   */
  private final static String TYPE_ATTACHMENT = "attachment";
  private static final String EXPORT_FILE_EXTENSION = ".tsv";

  /**
   * Dependencies.
   */
  @NonNull
  private final EntitySetService service;

  @GET
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet getSet(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId,
      @ApiParam(value = "Include items in the response?", required = true) @QueryParam("includeItems") @DefaultValue("false") final boolean includeItems) {
    val entitySet = service.getEntitySet(entitySetId);
    if (entitySet == null) {
      log.warn("getSetsByIds returns empty. The entitySetId '{}' is most likely invalid.", entitySetId);
      throw new NotFoundException(entitySetId.toString(), API_ENTITY_SET_ID_VALUE);
    }

    if (includeItems) {
      val items = service.getSetItems(entitySet);
      entitySet.setItems(items);
    }

    return entitySet;
  }

  @GET
  @Path("/sets/{" + API_ENTITY_SET_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves a list of entity sets by their IDs.", response = EntitySet.class, responseContainer = "List")
  public List<EntitySet> getSets(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUIDSetParam entitySetIds) {
    Set<UUID> setIds = null;
    try {
      setIds = entitySetIds.get();
    } catch (Exception e) {
      log.error("Exception occurred while parsing the UUID list from web request: '{}'", entitySetIds);
      log.error("The exception while parsing the UUID list is: ", e);
      throw new BadRequestException("Unable to parse the entitySetId parameter.");
    }
    log.debug("Received a getSets request for these lists: '{}'", setIds);

    return getSetsByIds(setIds);
  }

  /**
   * This hits the root path of /v1/entityset
   * 
   * @param setDefinition EntitySet definition from client.
   * @param async Defaults to true. Set to false if a synchronous request is needed.
   * @return JSON representation of new entity set.
   */
  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Advanced Search query.", response = EntitySet.class)
  public Response createSet(
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final EntitySetDefinition setDefinition,
      @ApiParam(value = API_ASYNC) @QueryParam("async") @DefaultValue("true") final boolean async) {
    val newSet = service.createEntitySet(setDefinition, async);
    return newSetResponse(newSet);
  }

  /**
   * Endpoint used for creating an entity set from the external repository.
   * 
   * @param setDefinition EntitySet definition from client.
   * @return JSON representation of new entity set.
   */
  @POST
  @Path("/external")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Repository Browser query.", response = EntitySet.class)
  public Response createExternalSet(
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final EntitySetDefinition setDefinition) {
    if (setDefinition.getType() == Type.FILE) {
      val newSet = service.createFileEntitySet(setDefinition);
      return newSetResponse(newSet);
    } else {
      val newSet = service.createExternalEntitySet(setDefinition);
      return newSetResponse(newSet);
    }
  }

  /**
   * Endpoint for creating an entity set from files from a single repository
   * 
   * @param listDefinition EntitySet definition from client.
   * @return JSON representation of new entity set.
   */
  // TODO: Remove and use the other endpoint that now services both use cases.
  @POST
  @Path("/file")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set from an Repository Query.", response = EntitySet.class)
  public Response createFileSet(
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final EntitySetDefinition listDefinition) {
    val filters = listDefinition.getFilters();
    val repoList = filters.path("file").path("repoName").path("is");
    if (!repoList.isMissingNode() && repoList.size() == 1) {
      val newSet = service.createFileEntitySet(listDefinition);
      return newSetResponse(newSet);
    } else {
      throw new BadRequestException("Need to filter by exactly one Repository.");
    }
  }

  @POST
  @Path("/union")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates an entity set by combining two or more existing sets.", response = EntitySet.class)
  public Response unionSets(
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final DerivedEntitySetDefinition setDefinition,
      @ApiParam(value = API_ASYNC) @QueryParam("async") @DefaultValue("true") final boolean async) {
    val newSet = service.computeEntitySet(setDefinition, async);

    return newSetResponse(newSet);
  }

  @GET
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}/export")
  @Produces(TEXT_TSV)
  @ApiOperation(value = "Exports the data of a set as a download in TSV (tab-delimited) format.", response = EntitySet.class)
  public Response exportSetItems(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId) {
    val entitySet = getSet(entitySetId, false);

    if (EntitySet.State.FINISHED != entitySet.getState()) {
      // We return a 204 if the list is not ready.
      return null;
    }

    val attachmentType = type(TYPE_ATTACHMENT)
        .fileName(getFileName(entitySet))
        .creationDate(new Date())
        .build();

    return Response
        .ok((StreamingOutput) outputStream -> service.exportSetItems(entitySet, outputStream))
        .header(CONTENT_DISPOSITION, attachmentType)
        .build();
  }

  private List<EntitySet> getSetsByIds(@NonNull final Set<UUID> ids) {
    val result = new ArrayList<EntitySet>(ids.size());
    for (val id : ids) {
      // Should implement @BindIn in the EntitySetRepository to allow the IN clause instead of doing a loop here.
      val list = service.getEntitySet(id);
      if (null != list) {
        result.add(list);
      }
    }

    return result;
  }

  private static String getFileName(EntitySet list) {
    return list.getType().getName() + "-ids-for-set-" + list.getName() + EXPORT_FILE_EXTENSION;
  }

  private static Response newSetResponse(EntitySet newSet) {
    return Response.status(CREATED)
        .entity(newSet)
        .build();
  }

}
