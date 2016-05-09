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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.apache.commons.lang.time.DateFormatUtils.formatUTC;

import java.io.OutputStream;
import java.util.Date;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.icgc.dcc.portal.manifest.model.ManifestFile;

import com.google.common.collect.ListMultimap;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class GNOSManifestWriter {

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
  public static void write(OutputStream buffer, ListMultimap<String, ManifestFile> bundles,
      long timestamp) {
    int rowCount = 0;
    // If this is thread-safe, perhaps we can make this static???
    val factory = XMLOutputFactory.newInstance();
    @Cleanup
    val writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(buffer, FILE_ENCODING));

    startXmlDocument(writer, timestamp);

    for (val url : bundles.keySet()) {
      val bundle = bundles.get(url);

      if (isEmpty(bundle)) {
        continue;
      }

      val bundleId = bundle.get(0).getDataBundleId();

      writeXmlEntry(writer, bundleId, url, bundle, ++rowCount);
    }

    endXmlDocument(writer);
  }

  private static void writeXmlEntry(XMLStreamWriter writer, String id, String downloadUrl,
      Iterable<ManifestFile> bundle, final int rowCount) throws XMLStreamException {
    addDownloadUrlEntryToXml(writer, id, downloadUrl, rowCount);
    addFileInfoEntriesToXml(writer, bundle);
    closeDownloadUrlElement(writer);
  }

  private static void startXmlDocument(XMLStreamWriter writer, long timestamp) throws XMLStreamException {
    writer.writeStartDocument(FILE_ENCODING, "1.0");
    writer.writeStartElement(GNOS_ROOT);
    writer.writeAttribute("date", formatToUtc(new Date(timestamp)));
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

  private static void addFileInfoEntriesToXml(XMLStreamWriter writer, Iterable<ManifestFile> info)
      throws XMLStreamException {
    for (val file : info) {
      writer.writeStartElement(GNOS_FILE);
  
      addXmlElement(writer, GNOS_FILE_NAME, file.getName());
      addXmlElement(writer, GNOS_FILE_SIZE, file.getSize() + "");
  
      writer.writeStartElement(GNOS_CHECK_SUM);
      writer.writeAttribute("type", "md5");
      writer.writeCharacters(file.getMd5sum());
      writer.writeEndElement();
  
      writer.writeEndElement();
    }
  }

  private static void closeDownloadUrlElement(@NonNull XMLStreamWriter writer) throws XMLStreamException {
    // Close off GNOS_FILES ("files") element.
    writer.writeEndElement();
    // Close off GNOS_RECORD ("Result") element.
    writer.writeEndElement();
  }

  private static void addXmlElement(XMLStreamWriter writer, String elementName, String elementValue)
      throws XMLStreamException {
    writer.writeStartElement(elementName);
    writer.writeCharacters(elementValue);
    writer.writeEndElement();
  }

  private static String formatToUtc(@NonNull Date timestamp) {
    return formatUTC(timestamp, DATE_FORMAT_PATTERN);
  }

}