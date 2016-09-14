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
package org.icgc.dcc.portal.server.pql.convert;

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.List;
import java.util.Map.Entry;

import lombok.NonNull;

import org.icgc.dcc.portal.server.pql.convert.model.JqlField;
import org.icgc.dcc.portal.server.pql.convert.model.Operation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * <h2>Background</h2> <br>
 * The gene facet filter(on the portal) has couple special cases related to pathways and compounds. Both of the search
 * criteria(pathway and compounds) the following set's of fields:
 * <ul>
 * <li> {@code has*}-fields. E.g. {@code hasCompound, hasPathway}
 * <li> {@code *Id}-fields. E.g. {@code compoundId, pathwayId*}
 * </ul>
 * 
 * There special conversion rules if a filter 'family' has both types of filters:
 * <ul>
 * <li>If both fields of the same family are present, they must be enclosed in an {@code or} clause. E.g.
 * 
 * <pre>
 * {"gene":{"coumpoundId":{"is":["ZINC01"]},"hasCompound":true}}
 * </pre>
 * 
 * converted to
 * 
 * <pre>
 * or(in('compoundId', 'ZINC01'), exists(compoundId))
 * </pre>
 * 
 * <li>In all the other combinations the fields should not be enclosed with {@code or}.
 * </ul>
 * 
 * <h2>Motivation</h2><br>
 * This class provides abstraction to separate the field families into different buckets and simplify their processing.
 */
// TODO: Choose a better class name
public class GeneFacetFilters {

  private static final Operation HAS_OPERATION = Operation.HAS;
  // Doesn't have to be 'IS', rather something different from 'HAS'
  private static final Operation ID_OPERATION = Operation.IS;

  private final Table<String, Operation, JqlField> fields = HashBasedTable.create();

  public void addIdField(@NonNull String familyName, @NonNull JqlField field) {
    fields.put(familyName, ID_OPERATION, field);
  }

  public void addHasField(@NonNull String familyName, @NonNull JqlField field) {
    fields.put(familyName, HAS_OPERATION, field);
  }

  public boolean hasFields() {
    return !fields.isEmpty();
  }

  public List<List<JqlField>> getFieldsByFamily() {
    return fields.rowMap().entrySet().stream()
        .sorted((left, right) -> left.getKey().compareTo(right.getKey()))
        .map(Entry::getValue)
        .map(values -> newArrayList(values.values()).stream()
            .sorted((left, right) -> left.getOperation().compareTo(right.getOperation()))
            .collect(toImmutableList()))
        .collect(toImmutableList());
  }

}
