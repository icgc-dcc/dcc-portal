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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.apache.commons.lang.time.DateFormatUtils.formatUTC;

import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;

import com.google.common.collect.ListMultimap;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class GNOSManifest {

  /**
   * Constants.
   */
  private static final String GNOS_ROOT = "ResultSet";
  private static final String GNOS_RECORD = "Result";
  private static final String GNOS_RECORD_ID = "analysis_id";
  private static final String GNOS_RECORD_URI = "analysis_data_uri";
  private static final String GNOS_FILES = "files";
  private static final String GNOS_FILE = "file";
  private static final String GNOS_FILE_NAME = "filename";
  private static final String GNOS_FILE_SIZE = "filesize";
  private static final String GNOS_CHECK_SUM = "checksum";

  private static final String DATE_FORMAT_PATTERN = ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
  private static final String FILE_ENCODING = UTF_8.name();

  @SneakyThrows
  public static void write(OutputStream buffer, ListMultimap<String, Map<String, String>> downloadUrlGroups,
      Date timestamp) {
    int rowCount = 0;
    // If this is thread-safe, perhaps we can make this static???
    val factory = XMLOutputFactory.newInstance();
    @Cleanup
    val writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(buffer, FILE_ENCODING));

    startXmlDocument(writer, timestamp);

    for (val url : downloadUrlGroups.keySet()) {
      val rowInfo = downloadUrlGroups.get(url);

      if (isEmpty(rowInfo)) {
        continue;
      }

      // TODO: is this still true that same url has the same data_bundle_id??
      val repoId = rowInfo.get(0).get(Fields.DATA_BUNDLE_ID);

      writeXmlEntry(writer, repoId, url, rowInfo, ++rowCount);
    }

    endXmlDocument(writer);
  }

  private static void startXmlDocument(XMLStreamWriter writer, Date timestamp) throws XMLStreamException {
    writer.writeStartDocument(FILE_ENCODING, "1.0");
    writer.writeStartElement(GNOS_ROOT);
    writer.writeAttribute("date", formatToUtc(timestamp));
  }

  private static void endXmlDocument(XMLStreamWriter writer) throws XMLStreamException {
    // Closes the root element - GNOS_ROOT
    writer.writeEndElement();
    writer.writeEndDocument();
  }

  private static void addDownloadUrlEntryToXml(XMLStreamWriter writer, String id, String downloadUrl, int rowCount)
      throws XMLStreamException {
    writer.writeStartElement(GNOS_RECORD);
    writer.writeAttribute("id", String.valueOf(rowCount));

    addXmlElement(writer, GNOS_RECORD_ID, id);
    addXmlElement(writer, GNOS_RECORD_URI, downloadUrl);

    writer.writeStartElement(GNOS_FILES);
  }

  private static void closeDownloadUrlElement(@NonNull XMLStreamWriter writer) throws XMLStreamException {
    // Close off GNOS_FILES ("files") element.
    writer.writeEndElement();
    // Close off GNOS_RECORD ("Result") element.
    writer.writeEndElement();
  }

  private static void addFileInfoEntriesToXml(XMLStreamWriter writer, Iterable<Map<String, String>> info)
      throws XMLStreamException {
    for (val fileInfo : info) {
      writer.writeStartElement(GNOS_FILE);

      addXmlElement(writer, GNOS_FILE_NAME, fileInfo.get(Fields.FILE_NAME));
      addXmlElement(writer, GNOS_FILE_SIZE, fileInfo.get(Fields.FILE_SIZE));

      writer.writeStartElement(GNOS_CHECK_SUM);
      writer.writeAttribute("type", "md5");
      writer.writeCharacters(fileInfo.get(Fields.FILE_MD5SUM));
      writer.writeEndElement();

      writer.writeEndElement();
    }
  }

  private static void addXmlElement(XMLStreamWriter writer, String elementName, String elementValue)
      throws XMLStreamException {
    writer.writeStartElement(elementName);
    writer.writeCharacters(elementValue);
    writer.writeEndElement();
  }

  private static void writeXmlEntry(XMLStreamWriter writer, String id, String downloadUrl,
      Iterable<Map<String, String>> fileInfo, final int rowCount) throws XMLStreamException {
    addDownloadUrlEntryToXml(writer, id, downloadUrl, rowCount);
    addFileInfoEntriesToXml(writer, fileInfo);
    closeDownloadUrlElement(writer);
  }

  private static String formatToUtc(@NonNull Date timestamp) {
    return formatUTC(timestamp, DATE_FORMAT_PATTERN);
  }

}