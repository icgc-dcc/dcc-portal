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
package org.icgc.dcc.portal.server.pql.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;

import org.dcc.portal.pql.meta.Type;
import org.junit.Test;

import lombok.val;

public class EntitySetFiltersConverterTest {

  FiltersConverter converter = new FiltersConverter();

  @Test
  public void donorEntitySetTest_donor() {
    assertFilterConversion("{donor:{id:{is:['ES:abc']}}}", "in(donor.id,'ES:abc')", DONOR_CENTRIC);
  }

  @Test
  public void donorEntitySetNotTest_donor() {
    assertFilterConversion("{donor:{id:{not:['ES:abc']}}}", "not(in(donor.id,'ES:abc'))", DONOR_CENTRIC);
  }

  @Test
  public void donorEntitySetTest_gene() {
    assertFilterConversion("{donor:{id:{is:['ES:abc']}}}", "nested(donor,in(donor.id,'ES:abc'))", GENE_CENTRIC);
  }

  @Test
  public void donorEntitySetNotTest_gene() {
    assertFilterConversion("{donor:{id:{not:['ES:abc']}}}", "not(nested(donor,in(donor.id,'ES:abc')))",
        GENE_CENTRIC);
  }

  @Test
  public void donorEntitySetTest_mutation() {
    assertFilterConversion("{donor:{id:{is:['ES:abc']}}}", "nested(ssm_occurrence,in(donor.id,'ES:abc'))",
        MUTATION_CENTRIC);
  }

  @Test
  public void donorEntitySetNotTest_mutation() {
    assertFilterConversion("{donor:{id:{not:['ES:abc']}}}", "not(nested(ssm_occurrence,in(donor.id,'ES:abc')))",
        MUTATION_CENTRIC);
  }

  @Test
  public void geneEntitySetTest_donor() {
    assertFilterConversion("{gene:{id:{is:['ES:abc']}}}", "nested(gene,in(gene.id,'ES:abc'))", DONOR_CENTRIC);
  }

  @Test
  public void geneEntitySetNotTest_donor() {
    assertFilterConversion("{gene:{id:{not:['ES:abc']}}}", "not(nested(gene,in(gene.id,'ES:abc')))",
        DONOR_CENTRIC);
  }

  @Test
  public void geneEntitySetTest_gene() {
    assertFilterConversion("{gene:{id:{is:['ES:abc']}}}", "in(gene.id,'ES:abc')", GENE_CENTRIC);
  }

  @Test
  public void geneEntitySetNotTest_gene() {
    assertFilterConversion("{gene:{id:{not:['ES:abc']}}}", "not(in(gene.id,'ES:abc'))", GENE_CENTRIC);
  }

  @Test
  public void geneEntitySetTest_mutation() {
    assertFilterConversion("{gene:{id:{is:['ES:abc']}}}", "nested(transcript,in(gene.id,'ES:abc'))",
        MUTATION_CENTRIC);
  }

  @Test
  public void geneEntitySetNotTest_mutation() {
    assertFilterConversion("{gene:{id:{not:['ES:abc']}}}", "not(nested(transcript,in(gene.id,'ES:abc')))",
        MUTATION_CENTRIC);
  }

  @Test
  public void mutationEntitySetTest_donor() {
    assertFilterConversion("{mutation:{id:{is:['ES:abc']}}}", "nested(gene.ssm,in(mutation.id,'ES:abc'))",
        DONOR_CENTRIC);
  }

  @Test
  public void mutationEntitySetNotTest_donor() {
    assertFilterConversion("{mutation:{id:{not:['ES:abc']}}}", "not(nested(gene.ssm,in(mutation.id,'ES:abc')))",
        DONOR_CENTRIC);
  }

  @Test
  public void mutationEntitySetTest_gene() {
    assertFilterConversion("{mutation:{id:{is:['ES:abc']}}}", "nested(donor.ssm,in(mutation.id,'ES:abc'))",
        GENE_CENTRIC);
  }

  @Test
  public void mutationEntitySetNotTest_gene() {
    assertFilterConversion("{mutation:{id:{not:['ES:abc']}}}", "not(nested(donor.ssm,in(mutation.id,'ES:abc')))",
        GENE_CENTRIC);
  }

  @Test
  public void mutationEntitySetTest_mutation() {
    assertFilterConversion("{mutation:{id:{is:['ES:abc']}}}", "in(mutation.id,'ES:abc')", MUTATION_CENTRIC);
  }

  @Test
  public void mutationEntitySetNotTest_mutation() {
    assertFilterConversion("{mutation:{id:{not:['ES:abc']}}}", "not(in(mutation.id,'ES:abc'))", MUTATION_CENTRIC);
  }

  private void assertFilterConversion(String jql, String pql, Type type) {
    val filters = FiltersConverterTest.createFilters(jql);
    val result = converter.convertFilters(filters, type);
    assertThat(result).isEqualTo(pql);
  }

}
