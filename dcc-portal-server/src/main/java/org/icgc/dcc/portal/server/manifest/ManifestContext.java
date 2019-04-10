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

import java.io.OutputStream;

import org.icgc.dcc.portal.server.manifest.model.Manifest;
import org.icgc.dcc.portal.server.model.Query;

import lombok.Value;

@Value
public class ManifestContext {

  /**
   * Metadata.
   */
  Manifest manifest;

  OutputStream output;

  public Query getQuery() {
    return Query.builder().filters(manifest.getFilters()).build();
  }

  public boolean isActive(String repo) {

    /**
     * Why this terrible kludge you may ask? Why is it buried here?
     *
     * Well, once upon a time several metadata systems were built to track metadata about objects.
     * Then an indexer was built to collate these different systems together.
     * And all of this worked in harmony with a central ID service to maintain referential integrity across all of it.
     *
     * The developers looked upon this and saw that it was good.
     *
     * Then one day, a requirement came in, that required all of this to be thrown out so that a tiny number of objects
     * could be back-doored into our system. Rather than rebuild the world, here we are, adding kludges to fix issues
     * such as this one: https://github.com/icgc-dcc/dcc-repository/issues/39
     */
    if (repo.equals("song-pdc")) {
      repo = "pdc";
    }

    return manifest.getRepos().isEmpty() || manifest.getRepos().contains(repo);
  }

}
