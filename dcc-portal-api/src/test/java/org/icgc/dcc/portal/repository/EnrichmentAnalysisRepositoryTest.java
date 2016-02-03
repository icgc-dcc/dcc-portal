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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import lombok.val;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

public class EnrichmentAnalysisRepositoryTest {

  EnrichmentAnalysisRepository repository;

  @Before
  public void setUp() {
    val dbi = new DBI("jdbc:h2:mem:genelist;MODE=PostgreSQL;INIT=runscript from 'src/test/sql/schema.sql'");
    this.repository = dbi.open(EnrichmentAnalysisRepository.class);
  }

  @After
  public void tearDown() {
    repository.close();
  }

  @Test
  public void testAll() {
    val id1 = UUID.randomUUID();

    val analysis1 = new EnrichmentAnalysis().setId(id1);
    val count1 = repository.save(analysis1, analysis1.getVersion());
    assertThat(count1).isEqualTo(1);

    val id2 = UUID.randomUUID();
    val analysis2 = new EnrichmentAnalysis().setId(id2);
    val count2 = repository.save(analysis2, analysis2.getVersion());
    assertThat(count2).isEqualTo(1);

    assertThat(id1).isNotEqualTo(id2);

    val actualData1 = repository.find(id1).getId();
    assertThat(id1).isEqualTo(actualData1);

    val actualData2 = repository.find(id2).getId();
    assertThat(id2).isEqualTo(actualData2);

    repository.update(analysis1, analysis1.getVersion());
    val actualData3 = repository.find(id1).getId();
    assertThat(actualData3).isEqualTo(actualData1);
  }

}
