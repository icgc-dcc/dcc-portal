/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal.server.service;

import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.model.*;
import org.icgc.dcc.portal.server.repository.OccurrenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class OccurrenceService {

  private final OccurrenceRepository occurrenceRepository;

  public Occurrences findAll(Query query) {
    val response = occurrenceRepository.findAll(query);
    return buildOccurences(response, query.getIncludes(), PaginationRequest.of(query));
  }

  public Occurrences findAll(String pqlString, List<String> fieldsNotToFlatten) {
    val response = occurrenceRepository.findAll(pqlString);
    val pql = parse(pqlString);
    return buildOccurences(response, fieldsNotToFlatten,PaginationRequest.of(pql));
  }

  public Occurrences buildOccurences(SearchResponse response, List<String> fieldsToNotFlatten,
      PaginationRequest pagination) {
    val list = ImmutableList.<Occurrence> builder();
    val hits = response.getHits();
    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, fieldsToNotFlatten, EntityType.OCCURRENCE);
      list.add(new Occurrence(fieldMap));
    }

    val occurrences = new Occurrences(list.build());
    occurrences.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), pagination));

    return occurrences;
  }

  public long count(Query query) {
    return occurrenceRepository.count(query);
  }

  public Occurrence findOne(String occurrenceId, Query query) {
    return new Occurrence(occurrenceRepository.findOne(occurrenceId, query));
  }

  public Map<String, Map<String, Long>> getProjectMutationDistribution() {
    val result = occurrenceRepository.getProjectDonorMutationDistribution();
    return result;
  }
}
