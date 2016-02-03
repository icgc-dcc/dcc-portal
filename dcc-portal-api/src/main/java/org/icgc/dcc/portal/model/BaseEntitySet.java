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

import java.util.Date;

import org.icgc.dcc.portal.repository.TermsLookupRepository.TermLookupType;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class representing an entity set.
 */
@Slf4j
@Data
@ApiModel(value = "BaseEntitySet")
public abstract class BaseEntitySet {

  @ApiModelProperty(value = "Name of Entity Set", required = true)
  private final String name;

  @ApiModelProperty(value = "Description of Entity Set")
  private final String description;

  @ApiModelProperty(value = "Type of Entity Set", required = true)
  private final Type type;

  private final long timestamp = new Date().getTime();

  @RequiredArgsConstructor
  @Getter
  public enum Type {

    DONOR("donor"),
    GENE("gene"),
    MUTATION("mutation"),
    FILE("file");

    @NonNull
    private final String name;

    /**
     * Returns the TermsLookupType value based on the Type value of this object.
     * @return TermsLookupType value for use in Elasticsearch
     */
    public TermLookupType toLookupType() {
      if (this == DONOR) {
        return TermLookupType.DONOR_IDS;
      } else if (this == GENE) {
        return TermLookupType.GENE_IDS;
      } else if (this == MUTATION) {
        return TermLookupType.MUTATION_IDS;
      } else if (this == FILE) {
        return TermLookupType.FILE_IDS;
      }

      log.error("No mapping for enum value '{}' of BaseEntityList.Type.", this);
      throw new IllegalStateException("No mapping for enum value: " + this);
    }

    // This should probably be declared elsewhere.
    private final static String INDEX_TYPE_NAME_SUFFIX = "-centric";

    public String getIndexTypeName() {
      return this.getName() + INDEX_TYPE_NAME_SUFFIX;
    }
  }
}
