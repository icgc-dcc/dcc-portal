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

import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import org.icgc.dcc.portal.server.model.Keyword;
import org.icgc.dcc.portal.server.model.Keywords;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Pagination;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.repository.SearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class SearchService {

  private static final String DONOR_TYPE = "donor";

  private final SearchRepository searchRepository;

  @NonNull
  public Keywords findAll(Query query, String type) {
    val hits = searchRepository.findAll(query, type).getHits();
    val donors = Lists.<String> newArrayList();
    val results = ImmutableList.<Keyword> builder();

    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, query, EntityType.KEYWORD);
      val keyword = new Keyword(fieldMap);

      if (keyword.getType().equals(DONOR_TYPE)) {
        val donorId = keyword.getId();

        donors.add(donorId);
      }

      results.add(keyword);
    }

    val keywords = new Keywords(results.build());
    keywords.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return keywords;
  }

}
