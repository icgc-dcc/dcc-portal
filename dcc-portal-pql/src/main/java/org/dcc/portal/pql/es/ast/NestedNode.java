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
package org.dcc.portal.pql.es.ast;

import static lombok.AccessLevel.PRIVATE;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.icgc.dcc.common.core.model.Identifiable;

@Value
@EqualsAndHashCode(callSuper = true)
public class NestedNode extends ExpressionNode {

  private static final ScoreMode DEFAULT_SCORE_MODE = ScoreMode.AVG;

  @NonNull
  String path;

  @NonNull
  ScoreMode scoreMode;

  public NestedNode(String path, ExpressionNode... children) {
    this(path, DEFAULT_SCORE_MODE, children);
  }

  public NestedNode(@NonNull String path, @NonNull ScoreMode scoreMode, ExpressionNode... children) {
    super(children);
    this.path = path;
    this.scoreMode = scoreMode;
  }

  @Override
  public <T, A> T accept(@NonNull NodeVisitor<T, A> visitor, @NonNull Optional<A> context) {
    return visitor.visitNested(this, context);
  }

  @Override
  public boolean hasNestedParent() {
    return true;
  }

  @NoArgsConstructor(access = PRIVATE)
  public static enum ScoreMode implements Identifiable {
    AVG,
    TOTAL,
    SUM,
    MAX,
    NONE;

    @Override
    public String getId() {
      return name().toLowerCase();
    }
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
