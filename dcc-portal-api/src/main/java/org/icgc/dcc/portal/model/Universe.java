/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.icgc.dcc.portal.model.GeneSetType.GO_TERM;
import static org.icgc.dcc.portal.model.GeneSetType.PATHWAY;
import static org.icgc.dcc.portal.util.Filters.goTermFilter;
import static org.icgc.dcc.portal.util.Filters.pathwayFilter;
import lombok.Getter;
import lombok.NonNull;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Getter
public enum Universe {

  REACTOME_PATHWAYS(null, PATHWAY, "pathway") {

    @Override
    public ObjectNode getFilter() {
      return pathwayFilter();
    }

  },

  GO_MOLECULAR_FUNCTION("GO:0003674", GO_TERM, "go_term.molecular_function") {

    @Override
    public ObjectNode getFilter() {
      return goTermFilter(getGeneSetId());
    }

  },

  GO_BIOLOGICAL_PROCESS("GO:0008150", GO_TERM, "go_term.biological_process") {

    @Override
    public ObjectNode getFilter() {
      return goTermFilter(getGeneSetId());
    }

  },

  GO_CELLULAR_COMPONENT("GO:0005575", GO_TERM, "go_term.cellular_component") {

    @Override
    public ObjectNode getFilter() {
      return goTermFilter(getGeneSetId());
    }

  };

  /**
   * Metadata.
   */
  private final String geneSetId;
  @NonNull
  private final GeneSetType geneSetType;
  @NonNull
  private final String geneSetFacetName;

  /**
   * @return a query filter the targets this {@code Universe} type.
   */
  public abstract ObjectNode getFilter();

  public boolean isGo() {
    return geneSetType == GeneSetType.GO_TERM;
  }

  // This seems to be needed for Lombok
  private Universe(String geneSetId, GeneSetType geneSetType, String geneSetFacetName) {
    this.geneSetId = geneSetId;
    this.geneSetType = geneSetType;
    this.geneSetFacetName = geneSetFacetName;
  }

}