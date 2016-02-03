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
package org.icgc.dcc.portal.service;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.analysis.UnionAnalyzer;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A service to create and retrieve results of entity set union analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UnionAnalysisService {

  @NonNull
  private final UnionAnalysisRepository repository;
  @NonNull
  private final UnionAnalyzer analyzer;
  @NonNull
  private final PortalProperties properties;

  @Getter(lazy = true)
  private final int currentDataVersion = loadDataVersion();

  public UnionAnalysisResult getAnalysis(@NonNull final UUID analysisId) {
    val result = repository.find(analysisId);

    if (null == result) {
      log.error("No analysis is found for id: '{}'.", analysisId);
    } else {
      log.debug("Got analysis: '{}'", result);
    }

    return result;
  }

  public UnionAnalysisResult submitAnalysis(@NonNull final UnionAnalysisRequest request) {
    val entityType = request.getType();
    val inputCount = request.getInputCount();
    val dataVersion = getCurrentDataVersion();
    val newAnalysis = UnionAnalysisResult.createForNewlyCreated(entityType, inputCount, dataVersion);

    val insertCount = repository.save(newAnalysis, dataVersion);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    analyzer.calculateUnionUnitCounts(newAnalysis.getId(), request);

    return newAnalysis;
  }

  public List<String> previewSetUnion(@NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    return analyzer.previewSetUnion(entitySetDefinition);
  }

  private int loadDataVersion() {
    return properties.getRelease().getDataVersion();
  }

}
