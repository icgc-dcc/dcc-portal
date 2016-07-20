/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.UUID;

import org.icgc.dcc.portal.server.analysis.SurvivalAnalyzer;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.SurvivalAnalysis;
import org.icgc.dcc.portal.server.repository.SurvivalAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SurvivalAnalysisService {

  /**
   * Dependencies
   */
  @NonNull
  private final SurvivalAnalysisRepository repository;
  @NonNull
  private final ServerProperties properties;
  @NonNull
  private final SurvivalAnalyzer analyzer;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  public SurvivalAnalysis createAnalysis(@NonNull final List<UUID> entitySetIds) {
    val dataVersion = getCurrentDataVersion();
    val analysis = new SurvivalAnalysis(UUID.randomUUID(), entitySetIds, entitySetIds.size(), dataVersion);

    val insertCount = repository.save(analysis, dataVersion);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    return analysis;
  }

  public SurvivalAnalysis getAnalysis(@NonNull final UUID analysisId) {
    val analysis = repository.find(analysisId);

    if (null == analysis) {
      log.error("No survival analysis is found for id: '{}'.", analysisId);
      throw new NotFoundException(analysisId.toString(), "survival analysis");
    }

    return analyzer.analyze(analysis);
  }

  private int resolveDataVersion() {
    return properties.getRelease().getDataVersion();
  }

}
