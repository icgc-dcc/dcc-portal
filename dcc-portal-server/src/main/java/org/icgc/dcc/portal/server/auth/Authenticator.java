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
package org.icgc.dcc.portal.server.auth;

import com.google.common.base.Optional;

/**
 * An interface for classes which authenticate user-provided credentials and return principal objects.
 *
 * @param <C> the type of credentials the authenticator can authenticate
 * @param <P> the type of principals the authenticator returns
 */
public interface Authenticator<C, P> {

  /**
   * Given a set of user-provided credentials, return an optional principal.
   *
   * <p>
   * If the credentials are valid and map to a principal, returns an {@code Optional.of(p)}.
   * </p>
   *
   * <p>
   * If the credentials are invalid, returns an {@code Optional.absent()}.
   * </p>
   *
   * @param credentials a set of user-provided credentials
   * @return either an authenticated principal or an absent optional
   * @throws AuthenticationException if the credentials cannot be authenticated due to an underlying error
   */
  Optional<P> authenticate(C credentials) throws AuthenticationException;
}
