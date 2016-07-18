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
package org.icgc.dcc.portal.service;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;

import java.util.UUID;

import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.OncogridAnalysis;
import org.icgc.dcc.portal.repository.EntitySetRepository;
import org.icgc.dcc.portal.repository.OncogridAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OncogridAnalysisService {

  @NonNull
  private final OncogridAnalysisRepository oncogridRepository;
  @NonNull
  private final EntitySetRepository entitySetRepository;
  @NonNull
  private final PortalProperties properties;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  public OncogridAnalysis createAnalysis(@NonNull final UUID geneSet, @NonNull final UUID donorSet) {
    val dataVersion = getCurrentDataVersion();

    val donors = entitySetRepository.find(donorSet);
    val genes = entitySetRepository.find(geneSet);

    val newAnalysis = new OncogridAnalysis(
        UUID.randomUUID(),
        geneSet,
        genes.getCount(),
        genes.getName(),
        donorSet,
        donors.getCount(),
        donors.getName());

    val insertCount = oncogridRepository.save(newAnalysis, dataVersion);
    checkState(insertCount == 1, "Could not save analysis. Insert count: %s", insertCount);

    return newAnalysis;
  }

  public OncogridAnalysis getAnalysis(@NonNull final UUID id) {
    return oncogridRepository.find(id);
  }

  private int resolveDataVersion() {
    return properties.getRelease().getDataVersion();
  }

}
