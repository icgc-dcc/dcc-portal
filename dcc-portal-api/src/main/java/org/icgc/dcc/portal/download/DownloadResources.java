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

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.immutableEntry;
import static java.lang.Long.parseLong;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.model.DownloadDataType.CLINICAL;
import static org.icgc.dcc.common.core.model.DownloadDataType.DONOR;
import static org.icgc.dcc.common.core.model.DownloadDataType.hasClinicalDataTypes;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.common.core.model.DownloadDataType;
import org.icgc.dcc.download.core.response.JobResponse;
import org.icgc.dcc.portal.model.param.IdsParam;
import org.icgc.dcc.portal.service.NotFoundException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@NoArgsConstructor(access = PRIVATE)
public final class DownloadResources {

  /**
   * Constants.
   */
  public static final String NOT_FOUND_STATUS = "NOT_FOUND";
  private static final String IS_CONTROLLED = "isControlled";
  private static final String FOUND_STATUS = "FOUND";

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

  public static Map<String, Object> createJobProgressResponse(JobResponse job) {
    val dataTypes = normalizeClinical(job.getDataType());
    val progress = dataTypes.stream()
        .map(dataType -> createProgressResponse(dataType))
        .collect(toImmutableList());

    return ImmutableMap.of(
        "downloadId", job.getId(),
        "status", "SUCCEEDED",
        "progress", progress);
  }

  public static Map<String, Object> createUiJobInfoResponse(JobResponse job) {
    val jobInfo = job.getJobInfo();
    val response = ImmutableMap.<String, Object> builder()
        .put("filter", jobInfo.getFilter())
        .put("uiQueryStr", jobInfo.getUiQueryStr())
        .put("startTime", job.getSubmissionTime())
        .put("et", job.getSubmissionTime())
        .put("hasEmail", "false")
        .put(DownloadResources.IS_CONTROLLED, String.valueOf(jobInfo.isControlled()))
        .put("status", DownloadResources.FOUND_STATUS)
        .put("fileSize", job.getFileSize());

    return response.build();
  }

  public static boolean hasControlledData(List<JobResponse> jobs) {
    return jobs.stream()
        .anyMatch(job -> job.getJobInfo().isControlled());
  }

  public static ImmutableMap<String, Long> getAllowedDataTypeSizes(Map<DownloadDataType, Long> dataTypeSizes,
      Collection<DownloadDataType> allowedDataTypes) {
    return allowedDataTypes.stream()
        .map(type -> immutableEntry(type.getCanonicalName(), dataTypeSizes.get(type)))
        .filter(e -> e.getValue() != null)
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue()));
  }

  public static long getFromByte(String range) {
    if (range == null) {
      return 0;
    }

    val ranges = range.split("=")[1].split("-");
    return parseLong(ranges[0]);
  }

  public static List<String> parseDownloadIds(IdsParam downloadIds) {
    val ids = downloadIds.get();
    if (ids == null || ids.isEmpty()) {
      throw new NotFoundException("Malformed request. Missing download IDs", "download");
    }
    return ids;
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

  private static Set<DownloadDataType> normalizeClinical(Set<DownloadDataType> dataTypes) {
    if (!hasClinicalDataTypes(dataTypes)) {
      return dataTypes;
    }

    val normalized = Sets.newHashSet(dataTypes);
    normalized.removeAll(DownloadDataType.CLINICAL);
    normalized.add(DownloadDataType.DONOR);

    return ImmutableSet.copyOf(normalized);
  }

  private static Map<String, String> createProgressResponse(DownloadDataType downloadDataType) {
    return ImmutableMap.<String, String> builder()
        .put("dataType", downloadDataType.getCanonicalName())
        .put("completed", "true")
        .put("numerator", "1")
        .put("denominator", "1")
        .put("percentage", "1.0")
        .build();
  }

}
