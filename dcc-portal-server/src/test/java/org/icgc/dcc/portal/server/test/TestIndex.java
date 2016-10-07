/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;

import org.icgc.dcc.portal.server.model.IndexType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Encapsulation of test configuration.
 */
@RequiredArgsConstructor(access = PRIVATE)
public enum TestIndex {

  RELEASE("test-icgc-release", "org/icgc/dcc/release/resources/mappings"),
  REPOSITORY("test-icgc-repository", "org/icgc/dcc/repository/resources/mappings");

  @Getter
  private final String name;
  private final String mappingsDir;

  @SneakyThrows
  public String getSettings() {
    val settingsFile = Resources.getResource(mappingsDir + "/" + "index.settings.json");
    val mapper = new ObjectMapper();
    val settingsSource = (ObjectNode) mapper.readTree(settingsFile);

    // Override production values that would introduce test timing delays / issues
    return settingsSource
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 0)
        .toString();
  }

  @SneakyThrows
  public String getMapping(IndexType indexType) {
    val mappingFileName = indexType.getId() + ".mapping.json";
    val mappingFile = Resources.getResource(mappingsDir + "/" + mappingFileName);
    return Resources.toString(mappingFile, UTF_8);
  }

}