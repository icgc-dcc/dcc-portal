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

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.Joiners.DOT;

import org.icgc.dcc.portal.manifest.model.Manifest;
import org.icgc.dcc.portal.manifest.model.ManifestFormat;
import org.icgc.dcc.portal.model.Repository;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class Manifests {

  private final static String PREFIX = "manifest";

  public static String getFileName(@NonNull Manifest manifest) {
    val repo = getRepo(manifest);

    val timestamp = manifest.getTimestamp();
    if (manifest.getFormat() == ManifestFormat.TARBALL) {
      // Archive
      return PREFIX + "." + timestamp + ".tar.gz";
    } else if (repo != null) {
      // Single repo
      return getFileName(repo, timestamp);
    } else {
      // Concatenated manifest
      return PREFIX + ".concatenated." + timestamp + ".txt";
    }
  }

  public static String getFileName(Repository repo, long timestamp) {
    return DOT.join(PREFIX, repo.getRepoCode(), timestamp, getFileExtension(repo));
  }

  public static String getFileExtension(Repository repo) {
    return repo.isGNOS() ? "xml" : "txt";
  }

  private static Repository getRepo(Manifest manifest) {
    if (manifest.getRepos().size() == 1) {
      return Repository.get(manifest.getRepos().get(0));
    }

    return null;
  }

}
