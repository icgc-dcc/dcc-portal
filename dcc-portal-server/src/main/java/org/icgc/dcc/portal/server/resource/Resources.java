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
package org.icgc.dcc.portal.server.resource;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.NoArgsConstructor;

/**
 * Documentation for API endpoints.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Resources {

  public static final Set<String> ORDER_VALUES = ImmutableSet.of("asc", "desc");

  public static final String RETURNS_LIST = "Returns a list of ";
  public static final String RETURNS_COUNT = "Returns a count of ";
  public static final String GROUPED_BY = " grouped by ";
  public static final String DONOR = "Donor";
  public static final String MUTATION = "Mutation";
  public static final String TOTAL = "Total";
  public static final String GENE = "Gene";
  public static final String PROJECT = "Project";
  public static final String OCCURRENCE = "Occurrence";
  public static final String S = "(s)";
  public static final String AFFECTED_BY_THE = " affected by the ";
  public static final String FOR_THE = " for the ";
  public static final String FIND_BY_ID_ERROR =
      "Returns information of a mutation by ID. If the mutation ID is not found, this returns a 404 error.";
  public static final String FIND_BY_ID = "Find by Identifiable";
  public static final String NOT_FOUND = " not found";
  public static final String MULTIPLE_IDS = ". Multiple IDs can be separated by a comma";

  public static final String API_DONOR_VALUE = "Donor ID";
  public static final String API_DONOR_PARAM = "donorId";
  public static final String API_MUTATION_VALUE = "Mutation ID";
  public static final String API_MUTATION_PARAM = "mutationId";
  public static final String API_GENE_VALUE = "Gene ID";
  public static final String API_GENE_PARAM = "geneId";
  public static final String API_GENE_SET_PARAM = "geneSetId";
  public static final String API_GENE_SET_VALUE = "Gene Set ID";
  public static final String API_PROJECT_VALUE = "Project ID";
  public static final String API_PROJECT_PARAM = "projectId";
  public static final String API_OCCURRENCE_VALUE = "Occurrence ID";
  public static final String API_OCCURRENCE_PARAM = "occurrenceId";
  public static final String API_ORDER_ALLOW = "asc,desc";
  public static final String API_ORDER_PARAM = "order";
  public static final String API_ORDER_VALUE = "Order to sort the column";
  public static final String API_SORT_FIELD = "sort";
  public static final String API_SORT_VALUE = "Column to sort results on";
  public static final String API_SIZE_ALLOW = "range[1,100]";
  public static final String API_SIZE_PARAM = "size";
  public static final String API_SIZE_VALUE = "Number of results returned";
  public static final String API_FROM_PARAM = "from";
  public static final String API_FROM_VALUE = "Start index of results";
  public static final String API_INCLUDE_PARAM = "include";
  public static final String API_INCLUDE_VALUE = "Include addition data in the response";
  public static final String API_FIELD_PARAM = "field";
  public static final String API_FIELD_VALUE = "Select fields returned";
  public static final String API_FILTER_PARAM = "filters";
  public static final String API_FILTER_VALUE = "Filter the search results";
  public static final String API_TYPE_PARAM = "type";
  public static final String API_TYPE_VALUE = "Type of file export";
  public static final String API_QUERY_TYPE_PARAM="queryType";
  public static final String API_QUERY_TYPE_VALUE="Query Type";
  public static final String API_QUERY_VALUE = "PQL Query";
  public static final String API_QUERY_PARAM = "query";
  public static final String API_SCORE_FILTERS_PARAM = "scoreFilters";
  public static final String API_SCORE_FILTER_VALUE = "Used to filter scoring differently from results";
  public static final String API_ANALYSIS_VALUE = "Analysis";
  public static final String API_ANALYSIS_PARAM = "analysis";
  public static final String API_ANALYSIS_ID_VALUE = "Analysis ID";
  public static final String API_ANALYSIS_ID_PARAM = "analysisId";
  public static final String API_PARAMS_VALUE = "EnrichmentParams";
  public static final String API_PARAMS_PARAM = "params";
  public static final String API_FILE_IDS_PARAM = "fileIds";
  public static final String API_FILE_IDS_VALUE = "Limits the file manifest archive to this list of file IDs";
  public static final String API_FILE_REPOS_PARAM = "repositories";
  public static final String API_FILE_REPOS_VALUE =
      "Limits the file manifest archive to this list of file repositories";
  public static final String API_FILE_REPO_CODE_PARAM = "repoCode";
  public static final String API_FILE_REPO_CODE_VALUE = "File Repository Code";
  public static final String API_FACETS_ONLY_PARAM = "facetsOnly";
  public static final String API_FACETS_ONLY_DESCRIPTION = "Retrieves facet results only";

  public static final String API_ENTITY_SET_ID_VALUE = "Entity Set ID";
  public static final String API_ENTITY_SET_ID_PARAM = "entitySetId";

  public static final String API_ENTITY_SET_DEFINITION_VALUE = "Entity Set Definition";
  public static final String API_ENTITY_SET_DEFINITION_PARAM = "entitySetDefinition";
  public static final String API_ENTITY_SET_UPDATE_NAME = "Entity Set Name";
  public static final String API_ENTITY_SET_UPDATE_PARAM = "name";
  public static final String API_SET_ANALYSIS_DEFINITION_VALUE = "Set Analysis Definition";

  public static final String API_ASYNC = "Asyncronous API Request";

}
