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
package org.dcc.portal.pql.ast.builder;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.dcc.portal.pql.ast.builder.FilterBuilders;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.ExistsNode;
import org.dcc.portal.pql.ast.filter.GeNode;
import org.dcc.portal.pql.ast.filter.GtNode;
import org.dcc.portal.pql.ast.filter.InNode;
import org.dcc.portal.pql.ast.filter.LeNode;
import org.dcc.portal.pql.ast.filter.LtNode;
import org.dcc.portal.pql.ast.filter.MissingNode;
import org.dcc.portal.pql.ast.filter.NeNode;
import org.dcc.portal.pql.ast.filter.NestedNode;
import org.dcc.portal.pql.ast.filter.NotNode;
import org.dcc.portal.pql.ast.filter.OrNode;
import org.junit.Test;

public class FilterBuildersTest {

  @Test
  public void eqTest() {
    val result = (EqNode) FilterBuilders.eq("id", 1).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(1);
  }

  @Test
  public void neTest() {
    val result = (NeNode) FilterBuilders.ne("id", 1).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(1);
  }

  @Test
  public void andTest() {
    AndNode result = (AndNode) FilterBuilders.and(FilterBuilders.eq("id", 1), FilterBuilders.ne("gene", 2)).build();
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
    assertThat(result.getChild(1)).isInstanceOf(NeNode.class);

    result = (AndNode) FilterBuilders.and(FilterBuilders.eq("id", 1)).build();
    assertThat(result.childrenCount()).isEqualTo(1);
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
  }

  @Test
  public void existsTest() {
    val result = (ExistsNode) FilterBuilders.exists("id").build();
    assertThat(result.getField()).isEqualTo("id");
  }

  @Test
  public void geTest() {
    val result = (GeNode) FilterBuilders.ge("id", 5).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(5);
  }

  @Test
  public void gtTest() {
    val result = (GtNode) FilterBuilders.gt("id", 5).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(5);
  }

  @Test
  public void leTest() {
    val result = (LeNode) FilterBuilders.le("id", 5).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(5);
  }

  @Test
  public void ltTest() {
    val result = (LtNode) FilterBuilders.lt("id", 5).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValue()).isEqualTo(5);
  }

  @Test
  public void inTest() {
    val result = (InNode) FilterBuilders.in("id", 5, 7).build();
    assertThat(result.getField()).isEqualTo("id");
    assertThat(result.getValues()).containsOnly(5, 7);
  }

  @Test
  public void missingTest() {
    val result = (MissingNode) FilterBuilders.missing("id").build();
    assertThat(result.getField()).isEqualTo("id");
  }

  @Test
  public void nestedTest() {
    val result = (NestedNode) FilterBuilders.nested("PATH",
        FilterBuilders.eq("id", 1),
        FilterBuilders.ne("gene", 2))
        .build();
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(result.getPath()).isEqualTo("PATH");
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
    assertThat(result.getChild(1)).isInstanceOf(NeNode.class);
  }

  @Test
  public void notTest() {
    val result = (NotNode) FilterBuilders.not(FilterBuilders.eq("id", 1)).build();
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
  }

  @Test
  public void orTest() {
    OrNode result = (OrNode) FilterBuilders.or(FilterBuilders.eq("id", 1), FilterBuilders.ne("gene", 2)).build();
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
    assertThat(result.getChild(1)).isInstanceOf(NeNode.class);

    result = (OrNode) FilterBuilders.or(FilterBuilders.eq("id", 1)).build();
    assertThat(result.childrenCount()).isEqualTo(1);
    assertThat(result.getFirstChild()).isInstanceOf(EqNode.class);
  }

}
