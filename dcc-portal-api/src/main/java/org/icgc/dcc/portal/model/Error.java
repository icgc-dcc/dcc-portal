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

package org.icgc.dcc.portal.model;

import static org.apache.commons.httpclient.HttpStatus.getStatusText;

import java.io.IOException;
import java.io.Serializable;

import javax.ws.rs.core.Response.StatusType;

import lombok.Value;

@Value
public class Error implements Serializable {

  private final int code;
  private final String message;

  public Error(int code, String message) {
    this.code = code;
    this.message = formatMessage(this.code, message);
  }

  public Error(int code, IOException e) {
    this(code, e.getMessage());
  }

  public Error(StatusType code, IOException e) {
    this(code.getStatusCode(), e.getMessage());
  }

  public Error(StatusType code, String message) {
    this(code.getStatusCode(), message);
  }

  private static String formatMessage(int code, String message) {
    return getStatusText(code) + (message == null ? "." : ". " + message);
  }
}
