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

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class ManifestArchive implements AutoCloseable {

  /**
   * State.
   */
  private final TarArchiveOutputStream tar;

  public ManifestArchive(@NonNull OutputStream output) throws IOException {
    this.tar = createTar(output);
  }

  public void addManifest(@NonNull String fileName, @NonNull ByteArrayOutputStream fileContents) throws IOException {
    val tarEntry = new TarArchiveEntry(fileName);

    tarEntry.setSize(fileContents.size());
    tar.putArchiveEntry(tarEntry);

    fileContents.writeTo(tar);
    tar.closeArchiveEntry();
  }

  @Override
  @SneakyThrows
  public void close() {
    tar.close();
  }

  private static TarArchiveOutputStream createTar(OutputStream output) throws IOException {
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(output)));
    tar.setLongFileMode(LONGFILE_GNU);

    return tar;
  }

}