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
package org.icgc.dcc.portal.pql.convert;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.dcc.portal.pql.es.visitor.aggs.MissingAggregationVisitor.MISSING_SUFFIX;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.model.TermFacet.Term;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class AggregationToFacetConverter {

  private static final AggregationToFacetConverter INSTANCE = new AggregationToFacetConverter();

  public Map<String, TermFacet> convert(Aggregations aggregations) {
    if (aggregations == null) {
      return emptyMap();
    }

    val result = new ImmutableMap.Builder<String, TermFacet>();
    for (val aggregation : aggregations) {
      val aggName = aggregation.getName();
      if (aggName.endsWith(MISSING_SUFFIX)) {
        continue;
      }

      val termFacet = createTermFacet(aggName, aggregations);
      log.debug("Created TermFacet - {}", termFacet);
      result.put(aggregation.getName(), termFacet);
    }

    return result.build();
  }

  public static AggregationToFacetConverter getInstance() {
    return INSTANCE;
  }

  private static TermFacet createTermFacet(String termAggName, Aggregations aggregations) {
    val termsAgg = findTermsAggregation(aggregations.get(termAggName));
    val terms = new ImmutableList.Builder<Term>();
    long total = 0;
    for (val bucket : termsAgg.getBuckets()) {
      val docsCount = resolveDocCount(termAggName, bucket);
      terms.add(new Term(bucket.getKey(), docsCount));
      total += docsCount;
    }

    val missingAggName = termAggName + MISSING_SUFFIX;
    val missingAgg = findMissingAggregation(aggregations.get(missingAggName));

    return TermFacet.of(total, resolveMissingDocCount(missingAggName, missingAgg), terms.build());
  }

  private static long resolveMissingDocCount(String missingAggName, Missing missingAgg) {
    val aggregations = missingAgg.getAggregations();
    if (hasAggregations(aggregations)) {
      return 0L;
    }

    return missingAgg.getDocCount();
  }

  private static boolean hasAggregations(Aggregations aggregations) {
    return aggregations != null && !aggregations.asList().isEmpty();
  }

  private static long resolveDocCount(String aggName, Bucket bucket) {
    val aggregations = bucket.getAggregations();
    if (hasAggregations(aggregations)) {
      ReverseNested reverseNestedAggregation = aggregations.get(aggName);

      return reverseNestedAggregation.getDocCount();
    }

    return bucket.getDocCount();
  }

  private static Terms findTermsAggregation(Aggregation aggregation) {
    return (Terms) resolveCommonAggregations(aggregation);
  }

  private static Missing findMissingAggregation(Aggregation aggregation) {
    return (Missing) resolveCommonAggregations(aggregation);
  }

  private static Aggregation resolveCommonAggregations(Aggregation aggregation) {
    if (aggregation instanceof Global) {
      val agg = (Global) aggregation;

      return resolveCommonAggregations(getSubaggregation(agg.getAggregations()));
    } else if (aggregation instanceof Filter) {
      val agg = (Filter) aggregation;

      return resolveCommonAggregations(getSubaggregation(agg.getAggregations()));
    } else if (aggregation instanceof Nested) {
      val agg = (Nested) aggregation;

      return resolveCommonAggregations(getSubaggregation(agg.getAggregations()));
    } else if (aggregation instanceof Missing || aggregation instanceof Terms) {
      return aggregation;
    }

    throw new IllegalArgumentException(format("Encountered an unknown aggregation %s", aggregation));
  }

  private static Aggregation getSubaggregation(Aggregations aggregations) {
    return aggregations.asList().get(0);
  }

}
