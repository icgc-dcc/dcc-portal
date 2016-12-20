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
package org.icgc.dcc.portal.server.resource.set;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.icgc.dcc.portal.server.resource.Resources.API_ASYNC;
import static org.icgc.dcc.portal.server.resource.Resources.API_ENTITY_SET_DEFINITION_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_ENTITY_SET_ID_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_ENTITY_SET_ID_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_ENTITY_SET_UPDATE_NAME;
import static org.icgc.dcc.portal.server.resource.Resources.API_ENTITY_SET_UPDATE_PARAM;
import static org.icgc.dcc.portal.server.util.MediaTypes.TEXT_TSV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.server.model.BaseEntitySet.Type;
import org.icgc.dcc.portal.server.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.server.model.EntitySet;
import org.icgc.dcc.portal.server.model.EntitySetDefinition;
import org.icgc.dcc.portal.server.model.UnionUnit;
import org.icgc.dcc.portal.server.model.param.UUIDSetParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.service.EntitySetService;
import org.icgc.dcc.portal.server.service.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * End-points that provide various functionalities for entity sets.
 */
@Slf4j
@Component
@Api("/entityset")
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

  @GET
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet getSet(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId,
      @ApiParam(value = "Include items in the response?", required = true) @QueryParam("includeItems") @DefaultValue("false") final boolean includeItems) {
    val entitySet = this.getEntitySet(entitySetId);

    if (includeItems) {
      val items = service.getSetItems(entitySet);
      entitySet.setItems(items);
    }

    return entitySet;
  }

  /**
   * Updates an entityset.
   * 
   * @param entitySetId path param holding the set id to update.
   * @param setDefinition definition of the set with updated info.
   * @return updated entityset.
   */
  @PUT
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet updateSet(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId,
      @ApiParam(value = API_ENTITY_SET_UPDATE_NAME) @FormParam(API_ENTITY_SET_UPDATE_PARAM) final String newName) {
    val updatedSet = service.updateEntitySet(entitySetId, newName);
    if (updatedSet == null) {
      log.warn("updateEntitySet returns empty. The entitySetId '{}' is most likely invalid.", updatedSet);
      throw new NotFoundException(entitySetId.toString(), API_ENTITY_SET_ID_VALUE);
    }

    return updatedSet;
  }

  /**
   * Updates an entityset.
   * 
   * @param entitySetId path param holding the set id to update.
   * @param setDefinition definition of the set with updated info.
   * @return updated entityset.
   */
  @POST
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}/unions")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet addToSet(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId,
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final EntitySetDefinition modifierSetDefinition) {

    val currentSet = this.getEntitySet(entitySetId);
    val modifierSet = createModifierSet(modifierSetDefinition);
    UnionUnit unionUnit1 = new UnionUnit(ImmutableSet.of(modifierSet.getId()), Collections.emptySet());
    UnionUnit unionUnit2 = new UnionUnit(ImmutableSet.of(currentSet.getId()), Collections.emptySet());
    DerivedEntitySetDefinition derivedSetDefinition =
        new DerivedEntitySetDefinition(Arrays.asList(unionUnit1, unionUnit2), currentSet.getName(),
            currentSet.getDescription(), currentSet.getType(), false);
    service.updateEntitySet(entitySetId, derivedSetDefinition);

    return this.getEntitySet(entitySetId);
  }

  public EntitySet createModifierSet(final EntitySetDefinition modifierSetDefinition) {
    return modifierSetDefinition.getType() == Type.FILE ? service
        .createFileEntitySet(modifierSetDefinition) : service.createEntitySet(modifierSetDefinition, false);
  }

  /**
   * Updates an entityset.
   * 
   * @param entitySetId path param holding the set id to update.
   * @param setDefinition definition of the set with updated info.
   * @return updated entityset.
   */
  /**
   * @param entitySetId
   * @param modifierSetDefinition
   * @return
   */
  @POST
  @Path("/{" + API_ENTITY_SET_ID_PARAM + "}/differences")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an entity set by its ID.", response = EntitySet.class)
  public EntitySet removeFromSet(
      @ApiParam(value = API_ENTITY_SET_ID_VALUE, required = true) @PathParam(API_ENTITY_SET_ID_PARAM) final UUID entitySetId,
      @ApiParam(value = API_ENTITY_SET_DEFINITION_VALUE) final EntitySetDefinition modifierSetDefinition) {

    val currentSet = this.getEntitySet(entitySetId);
    val modifierSet = createModifierSet(modifierSetDefinition);
    UnionUnit unionUnit1 = new UnionUnit(ImmutableSet.of(currentSet.getId()), ImmutableSet.of(modifierSet.getId()));
    DerivedEntitySetDefinition derivedSetDefinition =
        new DerivedEntitySetDefinition(Arrays.asList(unionUnit1), currentSet.getName(),
            currentSet.getDescription(), currentSet.getType(), false);
    service.updateEntitySet(entitySetId, derivedSetDefinition);

    return this.getEntitySet(entitySetId);
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

  private EntitySet getEntitySet(UUID entitySetId) {
    val entitySet = service.getEntitySet(entitySetId);
    if (entitySet == null) {
      log.warn("getEntitySet for the updated set returns empty. The entitySetId '{}' is most likely invalid.",
          entitySetId);
      throw new NotFoundException(entitySetId.toString(), API_ENTITY_SET_ID_VALUE);
    }
    return entitySet;
  }

}
