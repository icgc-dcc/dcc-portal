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
package org.dcc.portal.pql.ast.builder;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = PRIVATE)
public class FilterBuilders {

  public static EqBuilder eq(@NonNull String field, @NonNull Object value) {
    return new EqBuilder(field, value);
  }

  public static NeBuilder ne(@NonNull String field, @NonNull Object value) {
    return new NeBuilder(field, value);
  }

  public static AndBuilder and(@NonNull FilterBuilder... filters) {
    return new AndBuilder(filters);
  }

  public static ExistsBuilder exists(@NonNull String field) {
    return new ExistsBuilder(field);
  }

  public static GeBuilder ge(@NonNull String field, @NonNull Object value) {
    return new GeBuilder(field, value);
  }

  public static GtBuilder gt(@NonNull String field, @NonNull Object value) {
    return new GtBuilder(field, value);
  }

  public static LeBuilder le(@NonNull String field, @NonNull Object value) {
    return new LeBuilder(field, value);
  }

  public static LtBuilder lt(@NonNull String field, @NonNull Object value) {
    return new LtBuilder(field, value);
  }

  public static InBuilder in(@NonNull String field, @NonNull Object... value) {
    return new InBuilder(field, value);
  }

  public static MissingBuilder missing(@NonNull String field) {
    return new MissingBuilder(field);
  }

  public static NestedBuilder nested(@NonNull String path, @NonNull FilterBuilder... filterBuilders) {
    return new NestedBuilder(path, filterBuilders);
  }

  public static NotBuilder not(@NonNull FilterBuilder filterBuilder) {
    return new NotBuilder(filterBuilder);
  }

  public static OrBuilder or(@NonNull FilterBuilder... filters) {
    return new OrBuilder(filters);
  }

}
