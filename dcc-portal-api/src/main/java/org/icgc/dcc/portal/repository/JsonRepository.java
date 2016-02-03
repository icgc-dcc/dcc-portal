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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.PreDestroy;

import lombok.SneakyThrows;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.model.Identifiable;
import org.icgc.dcc.portal.repository.JsonRepository.JsonMapperFactory;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Data access for abstraction for working with user defined {@link EnrichmentAnalysis}.
 */
@RegisterMapperFactory(JsonMapperFactory.class)
public interface JsonRepository {

  /**
   * Constants.
   */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Schema constatns.
   */
  public static final String ID_FIELD_NAME = "id";
  public static final String DATA_FIELD_NAME = "data";

  /**
   * {@code close} with no args is used to close the connection.
   */
  @PreDestroy
  void close();

  /**
   * Implementation
   */

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static class JsonMapperFactory implements ResultSetMapperFactory {

    @Override
    public boolean accepts(Class type, StatementContext ctx) {
      return true;
    }

    @Override
    public ResultSetMapper mapperFor(final Class type, StatementContext ctx) {
      return new ResultSetMapper() {

        @Override
        @SneakyThrows
        public Object map(int index, ResultSet resultSet, StatementContext ctx) throws SQLException {
          return MAPPER.readValue(resultSet.getString(DATA_FIELD_NAME), type);
        }
      };
    }

  }

  @BindingAnnotation(BindValue.Factory.class)
  @Retention(RUNTIME)
  @Target({ PARAMETER })
  public @interface BindValue {

    public static class Factory implements BinderFactory {

      @Override
      public Binder<BindValue, Identifiable<?>> build(Annotation annotation) {
        return new Binder<BindValue, Identifiable<?>>() {

          @Override
          @SneakyThrows
          public void bind(SQLStatement<?> statement, BindValue bind, Identifiable<?> value) {
            statement.bind(ID_FIELD_NAME, value.getId());
            statement.bind(DATA_FIELD_NAME, MAPPER.writeValueAsString(value));
          }

        };
      }

    }

  }

}
