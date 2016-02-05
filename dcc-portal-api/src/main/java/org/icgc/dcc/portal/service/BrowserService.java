/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.service;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.common.core.model.ChromosomeLocation;
import org.icgc.dcc.portal.repository.BrowserRepository;
import org.icgc.dcc.portal.util.BrowserParsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.UtilityClass;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class BrowserService {

  private final BrowserRepository browserRepository;

  @UtilityClass
  private class ParameterNames {

    private final String SEGMENT = "segment";
    private final String RESOURCE = "resource";
  }

  /**
   * Retrieves histogram.
   */
  public List<Object> getHistogram(Map<String, String> queryMap) {

    ChromosomeLocation chromosome = getChromosomeLocation(queryMap.get(ParameterNames.SEGMENT));

    if (queryMap.get(ParameterNames.RESOURCE).equals("mutation")) {
      return getHistogramSegmentMutation(chromosome.getChromosome().getName(),
          Long.valueOf(chromosome.getStart()),
          Long.valueOf(chromosome.getEnd()),
          queryMap);
    } else if (queryMap.get(ParameterNames.RESOURCE).equals("gene")) {
      return getHistogramSegmentGene(chromosome.getChromosome().getName(),
          Long.valueOf(chromosome.getStart()),
          Long.valueOf(chromosome.getEnd()),
          queryMap);
    } else {
      throw new IllegalArgumentException("Invalid Resource: " + queryMap.get(ParameterNames.RESOURCE));
    }
  }

  /**
   * Retrieves complete data.
   */
  @SneakyThrows
  public List<List<Object>> getRecords(Map<String, String> queryMap) {

    val segmentRegion = queryMap.get(ParameterNames.SEGMENT);
    List<List<Object>> result = newArrayList();

    for (val chromosomeString : segmentRegion.split(",")) {
      ChromosomeLocation chromosome = getChromosomeLocation(chromosomeString);

      if (queryMap.get(ParameterNames.RESOURCE).equals("mutation")) {
        result.add(getSegmentMutation(chromosome.getChromosome().getName(),
            Long.valueOf(chromosome.getStart()),
            Long.valueOf(chromosome.getEnd()),
            queryMap));
      } else if (queryMap.get(ParameterNames.RESOURCE).equals("gene")) {
        result.add(getSegmentGene(chromosome.getChromosome().getName(),
            Long.valueOf(chromosome.getStart()),
            Long.valueOf(chromosome.getEnd()),
            queryMap));
      } else {
        throw new IllegalArgumentException("Invalid Resource: " + queryMap.get(ParameterNames.RESOURCE));
      }
    }

    return result;
  }

  private List<Object> getSegmentMutation(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    val consequenceValue = queryMap.get("consequence_type");
    val consequenceTypes = parseList(consequenceValue);

    val projectFilterValue = queryMap.get("consequence_type");
    val projectFilters = parseList(projectFilterValue);
    
    val impactFilterValue = queryMap.get("functional_impact");
    val impactFilters = parseList(impactFilterValue);

    val searchResponse = browserRepository.getMutation(segmentId, start, stop, consequenceTypes, projectFilters, impactFilters);
    
    return BrowserParsers.parseMutations(segmentId, start, stop, consequenceTypes, projectFilters, searchResponse);
  }

  private List<Object> getHistogramSegmentMutation(String segmentId, Long start, Long stop,
      Map<String, String> queryMap) {
    val consequenceValue = queryMap.get("consequence_type");
    val consequenceTypes = parseList(consequenceValue);

    val projectFilterValue = queryMap.get("consequence_type");
    val projectFilters = parseList(projectFilterValue);

    val impactFilterValue = queryMap.get("functional_impact");
    val impactFilters = parseList(impactFilterValue);

    val intervalValue = queryMap.get("interval");
    val interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : 0;

    val searchResponse =
        browserRepository.getMutationHistogram(interval, segmentId, start, stop, consequenceTypes, projectFilters,
            impactFilters);
    return BrowserParsers.parseHistogramMutation(segmentId, start, stop, interval, consequenceTypes, projectFilters,
        searchResponse);
  }

  private List<Object> getSegmentGene(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    val biotypeValue = queryMap.get("biotype");
    val biotypes = parseList(biotypeValue);

    val withTranscripts = nullToEmpty(queryMap.get("dataType")).equals("withTranscripts");

    val searchResponse = browserRepository.getGene(segmentId, start, stop, biotypes, withTranscripts);
    return BrowserParsers.parseGenes(segmentId, start, stop, biotypes, withTranscripts, searchResponse);
  }

  private List<Object> getHistogramSegmentGene(String segmentId, Long start, Long stop, Map<String, String> queryMap) {
    val biotypeValue = queryMap.get("biotype");
    val biotypes = parseList(biotypeValue);

    val intervalValue = queryMap.get("interval");
    val interval = intervalValue != null ? Math.round(Double.parseDouble(intervalValue)) : 0;

    val searchResponse = browserRepository.getGeneHistogram(interval, segmentId, start, stop, biotypes);
    return BrowserParsers.parseHistogramGene(segmentId, start, stop, interval, biotypes, searchResponse);
  }

  private static ChromosomeLocation getChromosomeLocation(String segmentRegion) {
    try {
      return ChromosomeLocation.parse(segmentRegion);
    } catch (Exception e) {
      val message = "Value of the '" + ParameterNames.SEGMENT +
          "' parameter (" + segmentRegion + ") is not valid. Reason: " + e.getMessage();
      throw new BadRequestException(message);
    }
  }

  private static List<String> parseList(String value) {
    if (value != null) {
      return asList(value.split(","));
    } else {
      return emptyList();
    }
  }

}