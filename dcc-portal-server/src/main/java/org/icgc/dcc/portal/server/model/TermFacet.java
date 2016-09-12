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

package org.icgc.dcc.portal.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Value
@JsonInclude(NON_EMPTY)
public class TermFacet {

  private static final String FACET_TYPE = "terms";
  private static final long DEFAULT_COUNT = 0L;

  String type;
  @JsonInclude(ALWAYS)
  Long missing;
  Long total;
  Long other;
  ImmutableList<Term> terms;

  // FIXME: Temporary work around until PQL, we need emulate a term facet from ES aggregations
  public static TermFacet repoTermFacet(long total, long missing, ImmutableList<Term> terms) {
    return new TermFacet(total, missing, terms);
  }

  // FIXME: Temporary work around until PQL, we need emulate a term facet from ES aggregations
  // private TermFacet(long total, long missing, ImmutableList<Term> terms) {
  // this.type = "terms";
  // this.other = -1L;
  // this.total = total;
  // this.missing = missing;
  // this.terms = terms;
  // }

  public static TermFacet of(TermsFacet facet) {
    return new TermFacet(facet);
  }

  public static TermFacet of(long total, long missing, @NonNull ImmutableList<Term> terms) {
    return new TermFacet(total, missing, terms);
  }

  private TermFacet(TermsFacet facet) {
    this.type = facet.getType();
    this.missing = facet.getMissingCount();
    this.total = facet.getTotalCount();
    this.other = facet.getOtherCount();
    this.terms = buildTerms(facet.getEntries(), facet.getMissingCount());
  }

  private TermFacet(long total, long missing, ImmutableList<Term> terms) {
    this.type = FACET_TYPE;
    this.missing = missing;
    this.total = total;
    this.other = DEFAULT_COUNT;
    this.terms = terms;
  }

  private ImmutableList<Term> buildTerms(List<? extends TermsFacet.Entry> entries, Long missing) {
    val lst = ImmutableList.<Term> builder();
    for (val entry : entries) {
      Text name = entry.getTerm();
      long value = entry.getCount();
      Term term = new Term(name.string(), value);
      lst.add(term);
    }
    return lst.build();
  }

  @Value
  public static class Term {

    String term;
    Long count;
  }

}
