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

import static com.google.common.collect.Iterables.get;
import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.model.EnrichmentParams.MAX_INPUT_GENES;
import static org.icgc.dcc.portal.model.EnrichmentParams.MAX_OUTPUT_GENE_SETS;
import static org.icgc.dcc.portal.model.Universe.REACTOME_PATHWAYS;

import java.util.Set;

import javax.validation.ConstraintViolation;

import lombok.val;

import org.icgc.dcc.portal.model.EnrichmentParams;
import org.icgc.dcc.portal.test.AbstractValidationTest;
import org.junit.Test;

public class EnrichmentParamsTest extends AbstractValidationTest<EnrichmentParams> {

  @Test
  public void testTooBigMaxGeneCount() {
    val violations = validate(validParams().setMaxGeneCount(MAX_INPUT_GENES + 1));

    assertSingleViolation(violations, "must be less than or equal to " + MAX_INPUT_GENES);
  }

  @Test
  public void testTooBigMaxGeneSetCount() {
    val violations = validate(validParams().setMaxGeneSetCount(MAX_OUTPUT_GENE_SETS + 1));

    assertSingleViolation(violations, "must be less than or equal to " + MAX_OUTPUT_GENE_SETS);
  }

  @Test
  public void testTooSmallFdr() {
    val violations = validate(validParams().setFdr(0.0f));

    assertSingleViolation(violations, "must be in range [0.005000, 0.500000] inclusive");
  }

  @Test
  public void testTooBigFdr() {
    val violations = validate(validParams().setFdr(1.0f));

    assertSingleViolation(violations, "must be in range [0.005000, 0.500000] inclusive");
  }

  private static EnrichmentParams validParams() {
    return new EnrichmentParams().setUniverse(REACTOME_PATHWAYS);
  }

  private static void assertSingleViolation(Set<ConstraintViolation<EnrichmentParams>> violations,
      String expectedMessage) {
    assertThat(violations).hasSize(1);
    assertEquals(expectedMessage, get(violations, 0).getMessage());
  }

}
