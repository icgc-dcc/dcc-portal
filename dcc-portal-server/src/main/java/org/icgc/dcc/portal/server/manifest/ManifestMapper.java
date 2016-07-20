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
package org.icgc.dcc.portal.server.manifest;

import static com.google.common.collect.Maps.uniqueIndex;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dcc.portal.pql.meta.FileTypeModel.Fields;
import org.dcc.portal.pql.meta.IndexModel;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.server.manifest.model.ManifestFile;
import org.icgc.dcc.portal.server.model.File;
import org.icgc.dcc.portal.server.model.File.Donor;
import org.icgc.dcc.portal.server.model.File.FileCopy;
import org.icgc.dcc.portal.server.model.Repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.val;

/**
 * Mapping logic from hits to manifest file copy entries.
 */
public class ManifestMapper {

  /**
   * Dependencies.
   */
  private final Map<String, Repository> repositories;

  public ManifestMapper(@NonNull List<Repository> repositories) {
    this.repositories = uniqueIndex(repositories, Repository::getCode);
  }

  public Stream<ManifestFile> map(SearchResponse searchResult) {
    return stream(searchResult.getHits()).flatMap(hit -> mapHit(hit));
  }

  /**
   * Merge the fields with flattened file copies fields.
   */
  private Stream<ManifestFile> mapHit(SearchHit hit) {
    val file = File.parse(hit.sourceAsString());

    // Collect common data
    val fileFields = mapFile(hit, file);

    // Map and combine with common data
    return file.getFileCopies().stream()
        .map(fileCopy -> mapFileCopy(fileCopy))
        .map(manifestFile -> mapManifestFile(manifestFile, fileFields));
  }

  private ManifestFile mapFileCopy(FileCopy fileCopy) {
    val indexFile = fileCopy.getIndexFile();
    val indexObjectId = (null == indexFile) ? "" : defaultString(indexFile.getObjectId());
    val repo = repositories.get(fileCopy.getRepoCode());
  
    val repoFileId = repo.isGNOS() ? fileCopy.getRepoDataBundleId() : fileCopy.getRepoFileId();
  
    return new ManifestFile()
        .setName(defaultString(fileCopy.getFileName()))
        .setFormat(defaultString(fileCopy.getFileFormat()))
        .setMd5sum(defaultString(fileCopy.getFileMd5sum()))
        .setSize(fileCopy.getFileSize())
        .setIndexObjectId(indexObjectId)
        .setRepoFileId(defaultString(repoFileId))
        .setRepoCode(defaultString(fileCopy.getRepoCode()))
        .setRepoType(defaultString(fileCopy.getRepoType()))
        .setRepoBaseUrl(defaultString(fileCopy.getRepoBaseUrl()))
        .setRepoDataPath(defaultString(fileCopy.getRepoDataPath()));
  }

  private static ManifestFile mapManifestFile(ManifestFile manifestFile, Map<String, String> fileFields) {
    return manifestFile
        .setId(fileFields.get(Fields.FILE_ID))
        .setObjectId(fileFields.get(Fields.FILE_UUID))
        .setStudy(fileFields.get(Fields.STUDY))
        .setDataBundleId(fileFields.get(Fields.DATA_BUNDLE_ID))
        .setDonorId(fileFields.get(Fields.DONOR_ID))
        .setProjectCode(fileFields.get(Fields.PROJECT_CODE));
  }

  private static Map<String, String> mapFile(SearchHit hit, File file) {
    val fileFields = Maps.<String, String> newHashMap();

    for (val fieldName : ImmutableList.of(
        Fields.FILE_UUID,
        Fields.FILE_ID,
        Fields.STUDY,
        Fields.DATA_BUNDLE_ID)) {
      fileFields.put(fieldName, mapValue(hit, fieldName));
    }

    val donors = file.getDonors() == null ? Collections.<Donor> emptyList() : file.getDonors();
    val donorCount = donors.size();
    val projectCount = donors.stream().map(Donor::getProjectCode).distinct().count();

    fileFields.put(Fields.DONOR_ID,
        donorCount == 0 ? "" : donorCount == 1 ? donors.get(0).getDonorId() : donorCount + " donors");
    fileFields.put(Fields.PROJECT_CODE,
        projectCount == 0 ? "" : donorCount == 1 ? donors.get(0).getProjectCode() : projectCount + " projects");

    return fileFields;
  }

  private static String mapValue(SearchHit hit, String fieldName) {
    val rawFieldName = IndexModel.getFileTypeModel().getField(fieldName);
    val resultField = hit.getFields().get(rawFieldName);

    return null == resultField ? "" : defaultString(getString(resultField.getValues()));
  }

}