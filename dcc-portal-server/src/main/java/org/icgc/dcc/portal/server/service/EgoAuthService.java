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
package org.icgc.dcc.portal.server.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.common.client.api.cud.User;
import org.icgc.dcc.portal.server.model.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates with the ICGC authenticator. The service is used to retrieve user information authenticate by the
 * Google API.
 */
@Slf4j
@Service
public class EgoAuthService {

  @Autowired
  private Settings settings;

  @SuppressWarnings("unchecked")
  public Optional<User> getUserInfo(String jwtToken) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("token", jwtToken);

    val entity = new HttpEntity<String>(headers);
    val resp = restTemplate.exchange(settings.getEgoUrl() + "/oauth/token/verify", HttpMethod.GET, entity, String.class);
    if (resp.getBody().equals("true")) {
      try {
        DecodedJWT jwt = JWT.decode(jwtToken);
        val m = (HashMap<String,String>) jwt.getClaims().get("context").asMap().get("user");
        val user = new User(m.get("email"), m.get("email"), m.get("firstName"), m.get("lastName"));
        return Optional.of(user);
      } catch (JWTDecodeException exception){
        //Invalid token
        return Optional.empty();
      }
    }

    return Optional.empty();
  }

  // This function doesn't valid jwtToken, make sure it is valid
  @SuppressWarnings("unchecked")
  public boolean hasDacoAccess(String jwtToken) {
    DecodedJWT jwt = JWT.decode(jwtToken);
    val scope = (List<String>) jwt.getClaims().get("context").asMap().get("scope");
    return scope.contains("portal.READ");
  }

  @SuppressWarnings("unchecked")
  public Boolean hasCloudAccess(String jwtToken) {
    DecodedJWT jwt = JWT.decode(jwtToken);
    val scope = (List<String>) jwt.getClaims().get("context").asMap().get("scope");
    return scope.contains("aws.READ") && scope.contains("collab.READ");
  }
}

