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
package org.icgc.dcc.portal.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Sets;
import com.yammer.dropwizard.jersey.params.AbstractParam;

/**
 * TODO
 */
@Slf4j
public class UuidListParam extends AbstractParam<Set<UUID>> {

  /**
   * @param input
   */
  public UuidListParam(String input) {

    super(input);
  }

  @Override
  protected Set<UUID> parse(String input) {

    val set = Sets.newHashSet(input.replaceAll("\"", "").split(","));
    val result = new HashSet<UUID>(set.size());
    for (val s : set) {

      try {

        result.add(UUID.fromString(s));

      } catch (Exception e) {
        // we ignore any error here and continue the conversion
        log.debug("Failed to convert {} to UUID", s, e.getMessage());
      }
    }
    return result;
  }
}
