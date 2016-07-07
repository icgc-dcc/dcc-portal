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
package org.icgc.dcc.portal.analysis;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.icgc.dcc.portal.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;

import com.google.common.base.Joiner;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import com.google.common.collect.Lists;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class EnrichmentReporter {

  /**
   * Constants.
   */
  private static final Joiner REPORT_GENES_JOINER = COMMA;
  private static final String[] REPORT_HEADERS =
      { "ID", "Name", "# Genes", "# Genes in overlap", "# Donors affected", "# Mutations", "Expected", "P-Value", "Adjusted P-Value", "Gene IDs in overlap"
      };

  /**
   * Dependencies.
   */
  @NonNull
  private final GeneRepository geneRepository;

  public void report(@NonNull EnrichmentAnalysis analysis, @NonNull OutputStream outputStream) throws IOException {
    val results = analysis.getResults();

    @Cleanup
    val writer = new CsvListWriter(new OutputStreamWriter(outputStream, UTF_8), TAB_PREFERENCE);
    writer.writeHeader(REPORT_HEADERS);

    // Shorthands
    val inputGeneListId = analysis.getId();

    for (int i = 0; i < results.size(); i++) {
      val result = results.get(i);

      log.info("[{}/{}] Reporting {}", new Object[] { i + 1, results.size(), result.getGeneSetId() });
      val overlapGeneSetGeneIds =
          geneRepository.findGeneSymbolsByGeneListIdAndGeneSetId(inputGeneListId, result.getGeneSetId());

      writer.write(new Object[] {
          result.getGeneSetId(),
          result.getGeneSetName(),
          result.getGeneCount(),
          result.getOverlapGeneSetGeneCount(),
          result.getOverlapGeneSetDonorCount(),
          result.getOverlapGeneSetMutationCount(),
          result.getExpectedValue(),
          result.getPValue(),
          result.getAdjustedPValue(),
          formatGenes(overlapGeneSetGeneIds)
      });
    }
  }

  private static String formatGenes(Map<String, String> genes) {
    // RQ12
    val values = Lists.<String> newArrayList();
    for (val gene : genes.entrySet()) {
      String symbol = gene.getValue();
      String geneId = gene.getKey();

      val value = symbol + ":" + geneId;

      values.add(value);
    }

    return REPORT_GENES_JOINER.join(values);
  }

}
