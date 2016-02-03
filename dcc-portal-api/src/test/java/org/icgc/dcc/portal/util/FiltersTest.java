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
package org.icgc.dcc.portal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.test.JsonNodes.$;
import static org.icgc.dcc.portal.util.Filters.andFilter;
import lombok.val;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class FiltersTest {

  @Test
  public void testAndFilterMulti() throws Exception {
    val and = andFilter(
        parse("{x: {y : [1]}}"),
        parse("{x: {y : [2]}}"),
        parse("{x: {y : [2]}}"));

    assertThat(parse(and)).isEqualTo(
        parse("{x: {y : [1, 2]}}"));
  }

  @Test
  public void testAndFilterCombineValues() throws Exception {
    val and = andFilter(
        parse("{x: 1}"),
        parse("{x: 2}"));

    assertThat(parse(and)).isEqualTo(
        parse("{x: [1, 2]}"));
  }

  @Test
  public void testAndFilterMissing() throws Exception {
    val and = andFilter(
        parse("{x: 1}"),
        parse("{y: 2}"));

    assertThat(parse(and)).isEqualTo(
        parse("{x: 1, y: 2}"));
  }

  private static ObjectNode parse(String json) {
    return (ObjectNode) $(json);
  }

  private static ObjectNode parse(ObjectNode objectNode) {
    return (ObjectNode) $(objectNode);
  }

}
