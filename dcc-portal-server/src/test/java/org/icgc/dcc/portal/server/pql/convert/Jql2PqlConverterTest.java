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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Jql2PqlConverterTest {

  private static final Jql2PqlConverter CONVERTER = Jql2PqlConverter.getInstance();

  @Test
  public void fieldsTest() {
    val query = Query.builder().fields(ImmutableList.of("id", "age")).build();
    assertResponse(query, "select(id,age)");
  }

  @Test
  public void allFieldsWithIncludesTest() {
    val query = Query.builder()
        .includes(ImmutableList.of("transcript"))
        .build();
    assertResponse(query, "select(*),select(transcript)");
  }

  @Test
  public void fieldsWithFilterTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{is:1}}}").get())
        .build();
    assertResponse(query, "select(id,age),eq(donor.id,1)");
  }

  @Test
  public void fieldsWithFilterNotTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{not:1}}}").get())
        .build();
    assertResponse(query, "select(id,age),not(eq(donor.id,1))");
  }

  @Test
  public void allFieldsTest() {
    val query = Query.builder()
        .build();
    assertResponse(query, "select(*)");
  }

  @Test
  public void sizeTest() {
    val query = Query.builder().size(100).build();
    assertResponse(query, "limit(100)");
  }

  @Test
  public void fromSizeTest() {
    val query = Query.builder().size(100).from(10).build();
    assertResponse(query, "limit(9,100)");
  }

  @Test
  public void sortTest() {
    val query = Query.builder().sort("id").order("asc").build();
    assertResponse(query, "sort(+id)");
  }

  @Test
  public void includeFacetsTest() {
    val query = Query.builder().includes(singletonList("facets")).build();
    val result = CONVERTER.convert(query, Type.DONOR_CENTRIC);
    log.debug("{}", result);
    assertThat(result).contains("facets(*)");
  }

  @Test
  public void existsTest() {
    val query = Query.builder()
        .filters(new FiltersParam("{donor:{hasPathway:true}}").get())
        .build();
    assertResponse(query, "select(*),exists(gene.pathwayId)");
  }

  @Test
  public void emptyIncludesTest() {
    val query = Query.builder()
        .sort("donorId")
        .order("desc")
        .fields(emptyList())
        .includes(emptyList())
        .build();
    assertResponse(query, "select(*),sort(-donorId)");
  }

  @Test
  public void fieldsAndFilterAndFacetTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{is:1}}}").get())
        .includes(singletonList("facets"))
        .build();
    assertResponse(query, "select(id,age),facets(*),eq(donor.id,1)");
  }

  @Test
  public void filterAndCountTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{is:1}}}").get())
        .build();
    assertCountQueryResponse(query, "count(),eq(donor.id,1)");
  }

  @Test
  public void filterAndCountNotTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{not:1}}}").get())
        .build();
    assertCountQueryResponse(query, "count(),not(eq(donor.id,1))");
  }

  @Test
  public void filterAndFacetOnlyTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{is:1}}}").get())
        .includes(singletonList("facets"))
        .build();
    assertCountQueryResponse(query, "count(),facets(*),eq(donor.id,1)");
  }

  @Test
  public void filterAndFacetOnlyNotTest() {
    val query = Query.builder()
        .fields(ImmutableList.of("id", "age"))
        .filters(new FiltersParam("{donor:{id:{not:1}}}").get())
        .includes(singletonList("facets"))
        .build();
    assertCountQueryResponse(query, "count(),facets(*),not(eq(donor.id,1))");
  }

  private void assertResponse(Query query, String exectedResult) {
    val result = CONVERTER.convert(query, Type.DONOR_CENTRIC);
    log.debug("{}", result);

    if (query.hasFields()) {
      assertThat(result).isEqualTo(exectedResult);
    } else {
      assertThat(result).contains(exectedResult);
    }
  }

  private void assertCountQueryResponse(Query query, String exectedResult) {
    val result = CONVERTER.convertCount(query, Type.DONOR_CENTRIC);
    log.debug("{}", result);

    assertThat(result).isEqualTo(exectedResult);
  }

}
