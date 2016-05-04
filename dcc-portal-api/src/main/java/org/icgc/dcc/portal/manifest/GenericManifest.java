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
package org.icgc.dcc.portal.manifest;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.supercsv.io.CsvListWriter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

public class GenericManifest {

  /**
   * Constants.
   */
  private static final String FILE_ENCODING = UTF_8.name();
  private static final Joiner COMMA_JOINER = COMMA.skipNulls();

  // This is cray cray...
  private static final BiFunction<Collection<Map<String, String>>, String, String> CONCAT_WITH_COMMA =
      (fileInfo, fieldName) -> COMMA_JOINER.join(transform(fileInfo, map -> map.get(fieldName)));

  /**
   * Constants - Fields
   */
  private static final String[] TSV_HEADERS =
      { "url", "file_name", "file_size", "md5_sum", "study" };
  private static final List<String> TSV_COLUMN_FIELD_NAMES = ImmutableList.of(
      Fields.FILE_NAME,
      Fields.FILE_SIZE,
      Fields.FILE_MD5SUM,
      Fields.STUDY);

  @SneakyThrows
  public static void write(OutputStream buffer, Multimap<String, Map<String, String>> downloadUrlGroups) {
    @Cleanup
    val tsv = createTsv(buffer);
    tsv.writeHeader(TSV_HEADERS);

    for (val url : downloadUrlGroups.keySet()) {
      val fileInfo = downloadUrlGroups.get(url);
      val row = createRow(url, fileInfo);

      tsv.write(row);
    }

    tsv.flush();
  }

  private static List<String> createRow(String url, Collection<Map<String, String>> fileInfo) {
    val otherColumns =
        Lists.transform(TSV_COLUMN_FIELD_NAMES, fieldName -> CONCAT_WITH_COMMA.apply(fileInfo, fieldName));
    return combineCollections(Stream.of(newArrayList(url), otherColumns));
  }

  @SneakyThrows
  private static CsvListWriter createTsv(OutputStream stream) {
    return new CsvListWriter(new OutputStreamWriter(stream, FILE_ENCODING), TAB_PREFERENCE);
  }

  private static <T> List<T> combineCollections(Stream<? extends Collection<T>> source) {
    return source.flatMap(Collection::stream)
        .collect(toImmutableList());
  }

}
