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
package org.icgc.dcc.portal.server.manifest.writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.util.Joiners.WHITESPACE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Set;

import org.icgc.dcc.portal.server.manifest.model.ManifestFile;

import com.google.common.collect.Multimap;
import com.google.common.io.Resources;

import lombok.SneakyThrows;
import lombok.val;

public class PDCManifestWriter {

  /**
   * Constants.
   */
  private static final String MANIFEST_TEMPLATE = "/templates/manifest.pdc.sh.template";

  @SneakyThrows
  public static void write(OutputStream buffer, Multimap<String, ManifestFile> bundles) {
    val urls = bundles.keySet();

    val writer = new OutputStreamWriter(buffer, UTF_8);
    writer.write(createManifest(urls));
    writer.flush();
  }

  private static String createManifest(Set<String> urls) throws IOException {
    val manifest = new StringBuilder(readTemplate());
    for (val url : urls) {
      val endpointUrl = resolveEndpointURL(url);
      val remoteFile = formatS3URL(url);
      val localFile = ".";
      val command =
          formatCommand(
              "aws",
              "--profile", "pdc",
              "--endpoint-url", endpointUrl,
              "s3", "cp",
              remoteFile, localFile);

      manifest.append(command).append('\n');
    }

    return manifest.toString();
  }

  @SneakyThrows
  private static String resolveEndpointURL(String value) {
    val url = new URL(value);
    return url.getProtocol() + "://" + url.getHost();
  }

  private static String formatS3URL(String url) {
    return url.replace(resolveEndpointURL(url) + "/", "s3://");
  }

  private static String formatCommand(String... args) {
    return WHITESPACE.join(args);
  }

  private static String readTemplate() throws IOException {
    return Resources.toString(EGAManifestWriter.class.getResource(MANIFEST_TEMPLATE), UTF_8);
  }

}
