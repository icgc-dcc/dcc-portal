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
package org.dcc.portal.pql.query;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.meta.TypeModel;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrderContext;

import com.google.common.base.Splitter;

@NoArgsConstructor(access = PRIVATE)
public class ParseTreeVisitors {

  private static final Splitter FIELD_SPLITTER = Splitter.on(".");
  private static final String DOUBLE_QUOTE = "\"";
  private static final String SINGLE_QUOTE = "'";

  public static String cleanString(@NonNull String original) {
    checkState(
        original.startsWith(ParseTreeVisitors.SINGLE_QUOTE) && original.endsWith(ParseTreeVisitors.SINGLE_QUOTE) ||
            original.startsWith(ParseTreeVisitors.DOUBLE_QUOTE) && original.endsWith(ParseTreeVisitors.DOUBLE_QUOTE),
        "Incorrectly quoted string: %s", original);

    return original.substring(1, original.length() - 1);
  }

  public static Order getOrderAt(@NonNull OrderContext parent, int position) {
    for (val sign : parent.SIGN()) {
      if (sign.getSymbol().getCharPositionInLine() == position) {
        return Order.bySign(sign.getText());
      }
    }

    return null;
  }

  public static String getField(@NonNull String alias, @NonNull TypeModel typeModel) {
    return typeModel.getField(resolveAlias(alias, typeModel));
  }

  private static List<String> splitFields(String fullyQualifiedFieldName) {
    val fields = ParseTreeVisitors.FIELD_SPLITTER.splitToList(fullyQualifiedFieldName);

    return fields;
  }

  private static String resolveAlias(String alias, TypeModel typeModel) {
    if (TypeModel.SPECIAL_CASES_FIELDS.contains(alias)) {
      return alias;
    }

    val components = splitFields(alias);
    val noPrefixAndPrefixFromTypeModel = components.size() == 1 || !typeModel.prefix().equals(components.get(0));
    if (noPrefixAndPrefixFromTypeModel) {
      return alias;
    }

    return components.get(1);
  }

}
