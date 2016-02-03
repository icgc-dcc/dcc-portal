/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.filter;

import static javax.ws.rs.core.Response.fromResponse;

import javax.ws.rs.core.Response;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.portal.model.Versions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VersionFilter implements ContainerResponseFilter {

  /**
   * Constants.
   */
  private static final String API_VERSION_HEADER = "X-ICGC-Api-Version";
  private static final String API_PORTAL_VERSION_HEADER = "X-ICGC-Portal-Version";
  private static final String API_PORTAL_COMMIT_ID_HEADER = "X-ICGC-Portal-CommitId";
  private static final String API_INDEX_COMMIT_ID_HEADER = "X-ICGC-Index-CommitId";
  private static final String API_INDEX_NAME_HEADER = "X-ICGC-Index-Name";

  /**
   * Dependencies.
   */
  @NonNull
  private final Versions versions;

  @Override
  public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
    val response = addVersionHeaders(containerResponse.getResponse());
    containerResponse.setResponse(response);

    return containerResponse;
  }

  private Response addVersionHeaders(Response response) {
    return fromResponse(response)
        .header(API_VERSION_HEADER, versions.getApi())
        .header(API_PORTAL_VERSION_HEADER, versions.getPortal())
        .header(API_PORTAL_COMMIT_ID_HEADER, versions.getPortalCommit())
        .header(API_INDEX_COMMIT_ID_HEADER, versions.getIndexCommit())
        .header(API_INDEX_NAME_HEADER, versions.getIndexName())
        .build();
  }

}
