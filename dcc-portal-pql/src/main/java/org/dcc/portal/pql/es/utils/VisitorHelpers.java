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
package org.dcc.portal.pql.es.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;

import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Maps;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class VisitorHelpers {

  /**
   * Checks if {@code optional} has a reference.
   * @throws IllegalArgumentException
   */
  public static <T> void checkOptional(@NonNull Optional<T> optional) {
    checkArgument(optional.isPresent(), "The optional does not contain any reference.");
  }

  /**
   * The methods visits children of {@code parent} with {@code visitor}. Each child returns an
   * {@code Optional<ExpressionNode>}. If the optional is not empty the child is replaced with the
   * {@link ExpressionNode} from the {@code Optional}.<br>
   * 
   * @param visitor applied to the {@code parent}
   * @param parent to be visited
   * @param context - query context
   */
  public static <T> Optional<ExpressionNode> visitChildren(@NonNull NodeVisitor<Optional<ExpressionNode>, T> visitor,
      @NonNull ExpressionNode parent, @NonNull Optional<T> context) {
    log.debug("[visitChildren] Processing node - \n{}", parent);
    val childrenToBeReplaced = Maps.<Integer, ExpressionNode> newHashMap();

    for (int i = 0; i < parent.childrenCount(); i++) {
      val child = parent.getChild(i);
      val childResult = child.accept(visitor, context);
      if (childResult.isPresent()) {
        childrenToBeReplaced.put(i, childResult.get());
      }
    }

    if (!childrenToBeReplaced.isEmpty()) {
      for (val entry : childrenToBeReplaced.entrySet()) {
        val index = entry.getKey();
        val value = entry.getValue();
        log.debug("[visitChildren] Replacing child \n{} \nwith \n{}", parent.getChild(index), value);
        parent.setChild(index, value);
      }
    }

    return Optional.empty();
  }

}
