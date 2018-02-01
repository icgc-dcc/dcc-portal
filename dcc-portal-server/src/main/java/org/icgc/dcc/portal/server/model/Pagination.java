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

package org.icgc.dcc.portal.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.val;
import org.dcc.portal.pql.ast.StatementNode;
import org.elasticsearch.search.SearchHits;

@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Pagination {
  @JsonIgnore
  PaginationRequest request;
  @ApiModelProperty(value = "Reqeusted total number of records to return", required = true)
  Integer count;
  @ApiModelProperty(value = "Actual total number of matching records", required = true)
  Long total;
  @ApiModelProperty(value = "Page number of this batch", required = true)
  Integer page;
  @ApiModelProperty(value = "Number of pages of this record set", required = true)
  Long pages;


  public static Pagination of(@NonNull Integer count, @NonNull Long total, @NonNull Query query) {
    return new Pagination(count, total, PaginationRequest.of(query));
  }

  public static Pagination of(@NonNull Integer count, @NonNull Long total, PaginationRequest request) {
    return new Pagination(count, total, request);
  }


  private Pagination(int count, long total, PaginationRequest request) {
    this.request=request;
    this.count = count;
    this.total = total;

    this.page = request.size <= 1 ? request.from : (request.from / request.size) + 1;
    this.pages = request.size <= 1 ? total : (total + request.size - 1) / request.size;
  }

  public Integer getSize() {
    return request.size;
  }
  public Integer getFrom() {
    return request.from;
  }
  public String getSort() {
    return request.sort;
  }
  public String getOrder() {
    return request.order;

  }

}
