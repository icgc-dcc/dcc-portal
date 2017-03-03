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
package org.icgc.dcc.portal.server.util;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.text.WordUtils;
import org.icgc.dcc.common.core.util.stream.Streams;

import java.util.function.Function;
import java.util.stream.Stream;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class Strings {

  /**
   * The empty String <code>""</code>.
   * @since 2.0
   */
  public static final String EMPTY = "";

  /**
   * <p>
   * Returns either the passed in String, or if the String is <code>null</code>, an empty String ("").
   * </p>
   *
   * <pre>
   * Strings.defaultString(null)  = ""
   * Strings.defaultString("")    = ""
   * Strings.defaultString("bat") = "bat"
   * </pre>
   *
   * @see ObjectUtils#toString(Object)
   * @see String#valueOf(Object)
   * @param str the String to check, may be null
   * @return the passed in String, or the empty String if it was <code>null</code>
   */
  public static String defaultString(String str) {
    return str == null ? EMPTY : str;
  }

  /**
   * <p>
   * Returns either the passed in String, or if the String is <code>null</code>, the value of <code>defaultStr</code>.
   * </p>
   *
   * <pre>
   * Strings.defaultString(null, "NULL")  = "NULL"
   * Strings.defaultString("", "NULL")    = ""
   * Strings.defaultString("bat", "NULL") = "bat"
   * </pre>
   *
   * @see ObjectUtils#toString(Object,String)
   * @see String#valueOf(Object)
   * @param str the String to check, may be null
   * @param defaultStr the default String to return if the input is <code>null</code>, may be null
   * @return the passed in String, or the default if it was <code>null</code>
   */
  public static String defaultString(String str, String defaultStr) {
    return str == null ? defaultStr : str;
  }

  /**
   * <p>
   * Capitalizes a String changing the first letter to title case as per {@link Character#toTitleCase(char)}. No other
   * letters are changed.
   * </p>
   *
   * <p>
   * For a word based algorithm, see {@link WordUtils#capitalize(String)}. A <code>null</code> input String returns
   * <code>null</code>.
   * </p>
   *
   * <pre>
   * Strings.capitalize(null)  = null
   * Strings.capitalize("")    = ""
   * Strings.capitalize("cat") = "Cat"
   * Strings.capitalize("cAt") = "CAt"
   * </pre>
   *
   * @param str the String to capitalize, may be null
   * @return the capitalized String, <code>null</code> if null String input
   * @see WordUtils#capitalize(String)
   * @see #uncapitalize(String)
   * @since 2.0
   */
  public static String capitalize(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return str;
    }
    return new StringBuilder(strLen)
        .append(Character.toTitleCase(str.charAt(0)))
        .append(str.substring(1))
        .toString();
  }

  /**
   * Converts input iterable to a String[] based on the defined mapping
   * @param objects iterable to be converted to String[]
   * @param mapping that converts each element of the iterable to a string
   * @param <T> generic type of elements in iterable
   * @return array of string representation of objects
   */
  public static <T> String[] toStringArray(final Iterable<T> objects, Function<T, ? extends String> mapping){
    return Streams.stream(objects)
        .map(mapping)
        .toArray(String[]::new);
  }

  /**
   * Converts input array to a String[] based on the defined mapping
   * @param array to be converted to String[]
   * @param mapping that converts each element of the array to a string
   * @param <T> generic type of elements in array
   * @return String[] of string representation of array
   */
  public static <T> String[] toStringArray(final T[] array, Function<T, ? extends String> mapping){
    return Stream.of(array)
        .map(mapping)
        .toArray(String[]::new);
  }

  /**
   * Converts input iterable to a String[], by calling Object::toString method on the object
   * @param objects iterable to be converted to String[]
   * @param <T> generic type of elements in iterable
   * @return array of string representation of objects
   */
  public static <T> String[] toStringArray(final Iterable<T> objects){
    return toStringArray(objects, Object::toString);
  }

  /**
   * Converts input array to a String[], by calling Object::toString method on the object
   * @param array to be converted to String[]
   * @param <T> generic type of elements in array
   * @return array of string representation of objects
   */
  public static <T> String[] toStringArray(final T[] array){
    return toStringArray(array, Object::toString);
  }

}
