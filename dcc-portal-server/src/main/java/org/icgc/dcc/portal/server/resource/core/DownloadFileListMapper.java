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
package org.icgc.dcc.portal.server.resource.core;

import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.util.Collection;
import java.util.Set;

import org.icgc.dcc.download.core.model.DownloadFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Utility to convert a download listing to a filtered / transformed view.
 */
@RequiredArgsConstructor
public class DownloadFileListMapper {

  /**
   * Configuration.
   */
  @NonNull
  private final Set<String> fields;
  private final boolean authorized;
  private final boolean flatten;

  public ArrayNode map(Collection<DownloadFile> files) {
    val value = convert(files);
    transform(value);

    return value;
  }

  private ArrayNode convert(Collection<DownloadFile> files) {
    return DEFAULT.convertValue(flatten ? flatten(files) : files, ArrayNode.class);
  }

  private Collection<DownloadFile> flatten(Collection<DownloadFile> files) {
    val result = Lists.<DownloadFile> newArrayList();
    for (val file : files) {
      flatten(result, file);
    }

    return result;
  }

  private void flatten(Collection<DownloadFile> result, DownloadFile file) {
    // Remove contents
    result.add(new DownloadFile(file.getName(), file.getType(), file.getDate(), file.getSize()));

    // Base case
    if (file.getContents() == null) return;

    for (val child : file.getContents()) {
      // Recurse
      flatten(result, child);
    }
  }

  private void transform(JsonNode node) {
    if (node.isArray()) {
      transformFiles((ArrayNode) node);
    } else if (node.isObject()) {
      transformFile((ObjectNode) node);
    }
  }

  private void transformFiles(ArrayNode files) {
    val childern = files.iterator();
    while (childern.hasNext()) {
      val child = childern.next();
      if (isFileIncluded(child)) {
        // Recurse
        transform(child);
      } else {
        childern.remove();
      }
    }
  }

  private void transformFile(ObjectNode file) {
    val fieldNames = file.fieldNames();
    while (fieldNames.hasNext()) {
      val fieldName = fieldNames.next();

      if (isContents(fieldName)) {
        // Recurse
        transform(file.get(fieldName));
      } else if (!isFieldIncluded(fieldName)) {
        fieldNames.remove();
      } else if (fieldName.equals("type")) {
        val type = file.get("type").textValue();
        file.put(fieldName, type.equals("FILE") ? "f" : "d");
      }
    }
  }

  private boolean isFileIncluded(JsonNode file) {
    val fileName = file.get("name").textValue();

    return authorized ? !fileName.contains("open") : !fileName.contains("controlled");
  }

  private boolean isFieldIncluded(String fieldName) {
    if (isContents(fieldName)) return true;

    return fields.isEmpty() || fields.contains(fieldName);
  }

  private static boolean isContents(String fieldName) {
    return fieldName.equals("contents");
  }

}
