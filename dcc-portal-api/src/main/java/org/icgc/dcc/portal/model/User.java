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
package org.icgc.dcc.portal.model;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of a User that stores user information during a HTTP session information.
 */
@JsonPropertyOrder({ "id", "userName", "passwordDigest", "firstName", "lastName", "emailAddress", "openIDIdentifier", "sessionToken", "authorities"
})
@Data
@NoArgsConstructor
public class User implements Serializable {

  /**
   * <p>
   * Unique identifier for this entity
   * </p>
   */
  private String id;

  /**
   * An email address
   */
  @JsonProperty
  private String emailAddress = "";

  /**
   * An OpenID identifier that is held across sessions
   */
  @JsonProperty
  private String openIDIdentifier = "";

  /**
   * DACO access
   */
  @JsonProperty
  private Boolean daco = false;

  /**
   * DACO access
   */
  @JsonProperty
  private Boolean cloudAccess = false;

  /**
   * A shared secret between the cluster and the user's browser that is revoked when the session ends
   */
  @JsonProperty
  private UUID sessionToken;

  @JsonCreator
  public User(@JsonProperty("id") String id, @JsonProperty("sessionToken") UUID sessionToken) {
    this.id = id;
    this.sessionToken = sessionToken;
  }

}
