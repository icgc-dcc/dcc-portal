/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.util;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;

@NoArgsConstructor(access = PRIVATE)
public final class FormatUtils {

  /**
   * Formats a object's string representation with leading {@code 'es'} prefix for easy console grepping. This is to
   * complement the same style used in Jersey's {@code LoggingFilter}.
   */
  public static String formatRequest(ActionRequestBuilder<?, ?, ?, ?> builder) {
    String text =
        builder == null ? "null" : (builder instanceof GetRequestBuilder ? builder.request().toString() : builder
            .toString());
    String prefix = "es > ";

    return prefix + text.replaceAll("\n", "\n" + prefix);
  }

}
