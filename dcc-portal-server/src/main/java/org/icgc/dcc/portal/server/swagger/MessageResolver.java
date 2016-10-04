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
package org.icgc.dcc.portal.server.swagger;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.Properties;
import java.util.stream.Stream;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.PropertyPlaceholderHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageResolver {

  /**
   * Constants.
   */
  private static final String PLACEHOLDER_KEY_PREFIX = "${";
  private static final String PLACEHOLDER_KEY_SUFFIX = "}";

  private static final String EXPRESSION_KEY_PREFIX = "#{";
  private static final String EXPRESSION_KEY_SUFFIX = "}";

  private static final String MESSAGE_FILE = "/swagger.properties";

  /**
   * Configuration.
   */
  private final Properties messages = resolveMessages();

  public String resolve(String description) {
    val text = normalizeText(description);
    if (isMessageKey(text)) {
      try {
        val key = parseMessageKey(text);
        return messages.getProperty(key);
      } catch (Exception e) {
        log.error("Error resolving message for '" + text + "': ", e);
      }
    }

    return description;
  }

  private static String normalizeText(String text) {
    return nullToEmpty(text).trim();
  }

  private static boolean isMessageKey(String text) {
    return text.startsWith(PLACEHOLDER_KEY_PREFIX)
        && text.endsWith(PLACEHOLDER_KEY_SUFFIX);
  }

  private static String parseMessageKey(String text) {
    return text.substring(
        PLACEHOLDER_KEY_PREFIX.length(),
        text.length() - PLACEHOLDER_KEY_SUFFIX.length()).trim();
  }

  @SneakyThrows
  private static Properties resolveMessages() {
    val file = readMessages();

    Properties messages = resolvePlaceholders(file);
    messages = resolveExpressions(messages);

    return messages;
  }

  private static Properties resolvePlaceholders(Properties file) {
    val replacer = new PropertyPlaceholderHelper(PLACEHOLDER_KEY_PREFIX, PLACEHOLDER_KEY_SUFFIX);

    val messages = new Properties(file);
    for (val entry : file.entrySet()) {
      val key = entry.getKey();
      val value = (String) entry.getValue();

      String before;
      String after = value;
      do {
        before = after;
        after = replacer.replacePlaceholders(before, (name) -> messages.getProperty(name));
      } while (!after.equals(before));

      messages.put(key, after);
    }
    return messages;
  }

  private static Properties resolveExpressions(Properties file) {
    val replacer = new PropertyPlaceholderHelper(EXPRESSION_KEY_PREFIX, EXPRESSION_KEY_SUFFIX);
    val parser = new SpelExpressionParser();
    val context = new StandardEvaluationContext();

    class Methods {

      @SneakyThrows
      @SuppressFBWarnings
      public String values(String className) {
        Class<?> type = Class.forName(className);
        Object[] values = (Object[]) type.getMethod("values").invoke(null);
        return Stream.of(values).map(value -> value.toString()).collect(joining(","));
      }

    }
    context.setRootObject(new Methods());

    val messages = new Properties(file);
    for (val entry : file.entrySet()) {
      val key = entry.getKey();
      val value = (String) entry.getValue();

      val after = replacer.replacePlaceholders(value, (expression) -> {
        Expression compiled = parser.parseExpression(expression);
        return compiled.getValue(context).toString();
      });

      messages.put(key, after);
    }
    return messages;
  }

  private static Properties readMessages() throws IOException {
    val file = new Properties();

    try (val stream = MessageResolver.class.getResourceAsStream(MESSAGE_FILE)) {
      file.load(stream);
    }

    return file;
  }

}