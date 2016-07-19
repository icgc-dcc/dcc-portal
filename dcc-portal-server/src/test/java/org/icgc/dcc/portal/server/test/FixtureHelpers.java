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
package org.icgc.dcc.portal.server.test;

import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * A set of helper method for fixture files.
 */
public class FixtureHelpers {

  private FixtureHelpers() {
    /* singleton */ }

  /**
   * Reads the given fixture file from {@code src/test/resources} and returns its contents as a UTF-8 string.
   *
   * @param filename the filename of the fixture file
   * @return the contents of {@code src/test/resources/{filename}}
   * @throws IOException if {@code filename} doesn't exist or can't be opened
   */
  public static String fixture(String filename) throws IOException {
    return fixture(filename, Charsets.UTF_8);
  }

  /**
   * Reads the given fixture file from {@code src/test/resources} and returns its contents as a string.
   *
   * @param filename the filename of the fixture file
   * @param charset the character set of {@code filename}
   * @return the contents of {@code src/test/resources/{filename}}
   * @throws IOException if {@code filename} doesn't exist or can't be opened
   */
  private static String fixture(String filename, Charset charset) throws IOException {
    return Resources.toString(Resources.getResource(filename), charset).trim();
  }
}
