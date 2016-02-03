/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static com.google.common.base.Charsets.UTF_8;
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.net.URLDecoder;

import lombok.SneakyThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.ApiModel;
import com.yammer.dropwizard.jersey.params.AbstractParam;

@ApiModel(value = FiltersParam.EXTRA_DETAIL_FOR_DONOR_COUNT_FILTER_PARAM)
public class FiltersParam extends AbstractParam<ObjectNode> {

  public static final String EXTRA_DETAIL_FOR_DONOR_COUNT_FILTER_PARAM =
      "When counting donors, a 'state' attribute can be specified to filter the types of " +
          "donors to be included. There are two types of donors, 'live' and 'pending'. " +
          "If the 'state' is not specified, 'live' is the default. " +
          "To count both types, use '*'. Here are some examples: " +
          "{\"donor\":{\"studies\":{\"is\":[\"PCAWG\"]},\"state\":{\"is\":[\"*\"]}}}, " +
          "{\"donor\":{\"studies\":{\"is\":[\"PCAWG\"]},\"state\":{\"is\":[\"live\"]}}}, " +
          "{\"donor\":{\"studies\":{\"is\":[\"PCAWG\"]},\"state\":{\"is\":[\"pending\"]}}}.";

  public FiltersParam(String input) {
    super(input);
  }

  @Override
  protected ObjectNode parse(String input) throws Exception {
    return parseFilters(input);
  }

  @SneakyThrows
  public static ObjectNode parseFilters(String text) {
    // String wrappedFilters = wrap(text);
    String json = URLDecoder.decode(text, UTF_8.name());

    return (ObjectNode) MAPPER.readTree(json);
  }

}
