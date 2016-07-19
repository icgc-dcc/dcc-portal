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
package org.icgc.dcc.portal.server.download;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.immutableEntry;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.model.DownloadDataType.CLINICAL;
import static org.icgc.dcc.common.core.model.DownloadDataType.DONOR;
import static org.icgc.dcc.common.core.model.DownloadDataType.hasClinicalDataTypes;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.common.core.model.DownloadDataType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@NoArgsConstructor(access = PRIVATE)
public final class DownloadResources {

  public static List<? extends Object> createGetDataSizeResponse(Map<String, Long> allowedDataTypeSizes) {
    val response = newArrayList();
    for (val entry : allowedDataTypeSizes.entrySet()) {
      val items = ImmutableMap.of(
          "label", entry.getKey(),
          "sizes", entry.getValue());
      response.add(items);
    }
    return response;
  }

  public static ImmutableMap<String, Long> getAllowedDataTypeSizes(Map<DownloadDataType, Long> dataTypeSizes,
      Collection<DownloadDataType> allowedDataTypes) {
    return allowedDataTypes.stream()
        .map(type -> immutableEntry(type.getCanonicalName(), dataTypeSizes.get(type)))
        .filter(e -> e.getValue() != null)
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue()));
  }

  public static Map<DownloadDataType, Long> normalizeSizes(Map<DownloadDataType, Long> typeSizes) {
    if (!hasClinicalDataTypes(typeSizes.keySet())) {
      return typeSizes;
    }

    val normalizesSizes = Maps.<DownloadDataType, Long> newHashMap();
    for (val entry : typeSizes.entrySet()) {
      val type = entry.getKey();
      val size = entry.getValue();
      if (CLINICAL.contains(type)) {
        val totalSize = firstNonNull(normalizesSizes.get(DONOR), 0L);
        normalizesSizes.put(DONOR, totalSize + size);
      } else {
        normalizesSizes.put(type, size);
      }
    }

    return ImmutableMap.copyOf(normalizesSizes);
  }

}
