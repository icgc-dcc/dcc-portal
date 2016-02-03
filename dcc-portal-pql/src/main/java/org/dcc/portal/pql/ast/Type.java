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
package org.dcc.portal.pql.ast;

import com.google.common.collect.Ordering;

public enum Type {

  ROOT,
  AND,
  EQ,
  EXISTS,
  GE,
  GT,
  IN,
  LE,
  LT,
  MISSING,
  NE,
  NESTED,
  NOT,
  OR,
  COUNT,
  FACETS,
  LIMIT,
  SELECT,
  SORT;

  /**
   * This defines the order in which the top-level child nodes of a StatementNode are turned into a PQL string. This
   * reflects the ordering imposed by PQL syntax. A good example of this enforcement is all other nodes must appear
   * before 'sort' and 'limit'; another example being that 'sort' must appear before 'limit'. *WARNING* When a new enum
   * value is added, it should also be added to this list (in the appropriate position that reflects the PQL syntax).
   */
  public static final Ordering<Type> PARSE_ORDER = Ordering.<Type> explicit(
      ROOT,
      COUNT,
      SELECT,
      FACETS,
      AND,
      EQ,
      EXISTS,
      GE,
      GT,
      IN,
      LE,
      LT,
      MISSING,
      NE,
      NESTED,
      NOT,
      OR,
      SORT,
      LIMIT);
}
