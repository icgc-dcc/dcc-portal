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

import lombok.NonNull;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Pagination {

  @ApiModelProperty(value = "Reqeusted total number of records to return", required = true)
  Integer count;
  @ApiModelProperty(value = "Actual total number of matching records", required = true)
  Long total;
  @ApiModelProperty(value = "Number of records to return in this batch", required = true)
  Integer size;
  @ApiModelProperty(value = "The index of the first record in this batch", required = true)
  Integer from;
  @ApiModelProperty(value = "Page number of this batch", required = true)
  Integer page;
  @ApiModelProperty(value = "Number of pages of this record set", required = true)
  Long pages;
  @ApiModelProperty(value = "Name of the column to sort this record set", required = true)
  String sort;
  @ApiModelProperty(value = "Sorting order (e.g. asc or desc)", required = true)
  String order;

  public static Pagination of(@NonNull Integer count, @NonNull Long total, @NonNull Query query) {
    return new Pagination(count, total, query);
  }

  public static Pagination of(int count, long total, int size, int from, @NonNull String sort, @NonNull String order) {
    return new Pagination(count, total, size, from, sort, order);
  }

  private Pagination(Integer count, Long total, Query query) {
    this(count, total, query.getSize(), query.getFrom(), query.getSort(), query.getOrder().toString());
  }

  private Pagination(int count, long total, int size, int from, String sort, String order) {
    this.count = count;
    this.total = total;
    this.size = size;
    this.from = from + 1;
    this.sort = sort;
    this.order = order.toLowerCase();

    this.page = size <= 1 ? from : (from / size) + 1;
    this.pages = size <= 1 ? total : (total + size - 1) / size;
  }

}
