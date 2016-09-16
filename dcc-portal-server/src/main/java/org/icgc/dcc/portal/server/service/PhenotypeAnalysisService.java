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
package org.icgc.dcc.portal.server.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.portal.server.analysis.PhenotypeAnalyzer;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.PhenotypeAnalysis;
import org.icgc.dcc.portal.server.repository.PhenotypeAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;

/**
 * A service to create and retrieve results of phenotype analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PhenotypeAnalysisService {

  @NonNull
  private final PhenotypeAnalysisRepository sqlRepository;
  @NonNull
  private final PhenotypeAnalyzer analyzer;
  @NonNull
  private final ServerProperties properties;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  public PhenotypeAnalysis createAnalysis(@NonNull final List<UUID> entitySetIds) {
    val dataVersion = getCurrentDataVersion();
    val newAnalysis = new PhenotypeAnalysis(UUID.randomUUID(), entitySetIds, entitySetIds.size(), dataVersion);

    val insertCount = sqlRepository.save(newAnalysis, dataVersion);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    return newAnalysis;
  }

  public PhenotypeAnalysis getAnalysisResult(@NonNull final UUID analysisId) {
    val analysis = sqlRepository.find(analysisId);

    if (null == analysis) {
      log.error("No phenotype analysis is found for id: '{}'.", analysisId);
      throw new NotFoundException(analysisId.toString(), "phenotype analysis");
    }

    val results = analyzer.getPhenotypeAnalysisResult(analysis.getEntitySetIds());
    analysis.setResults(results);

    return analysis;
  }

  private int resolveDataVersion() {
    return properties.getRelease().getDataVersion();
  }

}
