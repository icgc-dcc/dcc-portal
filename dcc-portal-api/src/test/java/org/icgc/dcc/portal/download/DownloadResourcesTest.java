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
package org.icgc.dcc.portal.download;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.common.core.model.DownloadDataType;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class DownloadResourcesTest {

  @Test
  public void testNormalizeSizes() throws Exception {
    val downloadSizes = ImmutableMap.<DownloadDataType, Long> builder()
        .put(DownloadDataType.SPECIMEN, 4315134L)
        .put(DownloadDataType.SAMPLE, 3134504L)
        .put(DownloadDataType.DONOR_FAMILY, 124690L)
        .put(DownloadDataType.DONOR, 1950045L)
        .put(DownloadDataType.DONOR_THERAPY, 175000L)
        .put(DownloadDataType.DONOR_EXPOSURE, 219529L)
        .build();

    val normalizedClinical = DownloadResources.normalizeSizes(downloadSizes);
    assertThat(normalizedClinical).hasSize(1);
    assertThat(normalizedClinical.get(DownloadDataType.DONOR)).isEqualTo(9918902L);
  }

}
