/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.config;

import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.PhenotypeAnalysisRepository;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.repository.UserGeneSetRepository;
import org.skife.jdbi.v2.DBI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.DatabaseConfiguration;
import com.yammer.dropwizard.jdbi.DBIFactory;

import lombok.SneakyThrows;
import lombok.val;

@Lazy
@Configuration
public class RepositoryConfig {

  @Bean
  public UserGeneSetRepository userGeneSetRepository(DBI dbi) {
    return dbi.open(UserGeneSetRepository.class);
  }

  @Bean
  public EnrichmentAnalysisRepository enrichmentAnalysisRepository(DBI dbi) {
    return dbi.open(EnrichmentAnalysisRepository.class);
  }

  @Bean
  public EntityListRepository entityListRepository(DBI dbi) {
    return dbi.open(EntityListRepository.class);
  }

  @Bean
  public UnionAnalysisRepository unionAnalysisRepository(DBI dbi) {
    return dbi.open(UnionAnalysisRepository.class);
  }

  @Bean
  public PhenotypeAnalysisRepository phenotypeAnalysisRepository(DBI dbi) {
    return dbi.open(PhenotypeAnalysisRepository.class);
  }

  @Bean
  @SneakyThrows
  public DBI dbi(PortalProperties properties, Environment environment, DatabaseConfiguration database) {
    val name = "postgresql";
    val factory = new DBIFactory();

    return factory.build(environment, database, name);
  }

}
