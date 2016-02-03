/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.repository;

import java.util.UUID;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.repository.JsonRepository.JsonMapperFactory;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;

/**
 * Data access for abstraction for working with user defined {@link EnrichmentAnalysis}.
 */
@RegisterMapperFactory(JsonMapperFactory.class)
public interface EnrichmentAnalysisRepository extends JsonRepository {

  /**
   * Schema constants.
   */
  static final String TABLE_NAME = "enrichment_analysis";
  static final String VERSION_FIELD_NAME = "version";

  @SqlQuery("SELECT  " + DATA_FIELD_NAME + " FROM " + TABLE_NAME + " WHERE " + ID_FIELD_NAME + " = :id")
  EnrichmentAnalysis find(@Bind(ID_FIELD_NAME) UUID id);

  @SqlUpdate("INSERT INTO " + TABLE_NAME + " (" + ID_FIELD_NAME + " , " + DATA_FIELD_NAME + ", " + VERSION_FIELD_NAME
      + ") VALUES (:id, :data, :version)")
  int save(@BindValue EnrichmentAnalysis analysis, @Bind(VERSION_FIELD_NAME) int dataVersion);

  @SqlUpdate("UPDATE " + TABLE_NAME + " SET  " + DATA_FIELD_NAME + " = :data, " + VERSION_FIELD_NAME
      + " = :version WHERE " + ID_FIELD_NAME + " = :id")
  int update(@BindValue EnrichmentAnalysis analysis, @Bind(VERSION_FIELD_NAME) int dataVersion);

}
