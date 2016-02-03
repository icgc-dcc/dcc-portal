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
package org.dcc.portal.pql.meta;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import com.google.common.collect.Lists;

@Slf4j
public class DonorCentricTypeModelTest extends DonorCentricTypeModel {

  DonorCentricTypeModel model = new DonorCentricTypeModel();

  @Test
  public void nestedTest() {
    val nestedFields = Lists.<String> newArrayList();
    for (val entry : this.fieldsByFullPath.entrySet()) {
      if (entry.getValue().isNested()) nestedFields.add(entry.getKey());
    }
    log.debug("Nested Fields: {}", nestedFields);

    assertThat(nestedFields.size()).isEqualTo(4);
    assertThat(nestedFields).contains("gene");
    assertThat(nestedFields).contains("gene.ssm");
    assertThat(nestedFields).contains("gene.ssm.consequence");
    assertThat(nestedFields).contains("gene.ssm.observation");
  }

  @Test
  public void goTermTest() {
    assertThat(this.fieldsByAlias.get("gene.GoTerm")).isEqualTo("gene.go_term");
  }

  @Test
  public void getInternalAliasTest() {
    assertThat(getInternalField(BIOLOGICAL_PROCESS)).isEqualTo("gene.go_term.biological_process");
  }

  @Test
  public void hasPatthwayTest() {
    assertThat(getField("hasPathway")).isEqualTo("gene.pathwayId");
  }

}
