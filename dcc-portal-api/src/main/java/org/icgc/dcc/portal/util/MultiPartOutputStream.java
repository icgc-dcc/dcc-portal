//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.icgc.dcc.portal.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Handle a multipart MIME response.
 */
public class MultiPartOutputStream extends FilterOutputStream {

  /**
   * Constant.
   */
  private static final byte[] CRLF = { '\r', '\n' };
  private static final byte[] DASHDASH = { '-', '-' };

  /**
   * Configuration.
   */
  private final String boundary;
  private final byte[] boundaryBytes;

  /**
   * State
   */
  private boolean inPart = false;

  public MultiPartOutputStream(String boundary, OutputStream out) throws IOException {
    super(out);
    this.boundary = boundary;
    this.boundaryBytes = boundary.getBytes(ISO_8859_1);
    this.inPart = false;
  }

  public String getBoundary() {
    return boundary;
  }

  public OutputStream getOut() {
    return out;
  }

  /**
   * Start creation of the next Content.
   */
  public void startPart(String contentType)
      throws IOException {
    if (inPart) out.write(CRLF);
    inPart = true;
    out.write(DASHDASH);
    out.write(boundaryBytes);
    out.write(CRLF);
    if (contentType != null) out.write(("Content-Type: " + contentType).getBytes(ISO_8859_1));
    out.write(CRLF);
    out.write(CRLF);
  }

  /**
   * Start creation of the next Content.
   */
  public void startPart(String contentType, String[] headers)
      throws IOException {
    if (inPart) out.write(CRLF);
    inPart = true;
    out.write(DASHDASH);
    out.write(boundaryBytes);
    out.write(CRLF);
    if (contentType != null) out.write(("Content-Type: " + contentType).getBytes(ISO_8859_1));
    out.write(CRLF);
    for (int i = 0; headers != null && i < headers.length; i++) {
      out.write(headers[i].getBytes(ISO_8859_1));
      out.write(CRLF);
    }
    out.write(CRLF);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
  }

  /**
   * End the current part.
   */
  @Override
  public void close()
      throws IOException {
    if (inPart) out.write(CRLF);
    out.write(DASHDASH);
    out.write(boundaryBytes);
    out.write(DASHDASH);
    out.write(CRLF);
    inPart = false;
    super.close();
  }

}
