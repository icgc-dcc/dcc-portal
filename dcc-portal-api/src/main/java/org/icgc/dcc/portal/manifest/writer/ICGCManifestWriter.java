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
package org.icgc.dcc.portal.manifest.writer;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.icgc.dcc.portal.manifest.model.ManifestFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import lombok.SneakyThrows;
import lombok.val;

public class ICGCManifestWriter extends TsvManifestWriter {

  /**
   * Constants - Fields
   */
  private static final String[] TSV_HEADERS =
      { "repo_code", "file_id", "object_id", "file_format", "file_name", "file_size", "md5_sum", "index_object_id", "donor_id/donor_count", "project_id/project_count", "study" };

  @SneakyThrows
  public static void write(OutputStream buffer, Multimap<String, ManifestFile> bundles) {
    val tsv = createTsv(buffer);
    tsv.writeHeader(TSV_HEADERS);

    for (val url : bundles.keySet()) {
      val bundle = bundles.get(url);
      val row = createRow(bundle);

      tsv.write(row);
    }

    tsv.flush();
  }

  private static List<String> createRow(Collection<ManifestFile> bundle) {
    val row = Lists.<String> newArrayList();
    row.add(join(bundle, ManifestFile::getRepoCode));
    row.add(join(bundle, ManifestFile::getId));
    row.add(join(bundle, ManifestFile::getObjectId));
    row.add(join(bundle, ManifestFile::getFormat));
    row.add(join(bundle, ManifestFile::getName));
    row.add(join(bundle, ManifestFile::getSize));
    row.add(join(bundle, ManifestFile::getMd5sum));
    row.add(join(bundle, ManifestFile::getIndexObjectId));
    row.add(join(bundle, ManifestFile::getDonorId));
    row.add(join(bundle, ManifestFile::getProjectCode));
    row.add(join(bundle, ManifestFile::getStudy));

    return row;
  }

}
