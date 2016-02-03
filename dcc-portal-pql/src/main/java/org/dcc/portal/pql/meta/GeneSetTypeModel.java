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
package org.dcc.portal.pql.meta;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.dcc.portal.pql.meta.Type.GENE_SET;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfObjects;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.BooleanFieldModel.bool;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;

import lombok.val;

import org.dcc.portal.pql.meta.field.FieldModel;

import com.google.common.collect.ImmutableList;

public class GeneSetTypeModel extends TypeModel {

  // Including real fields, not aliases. Because after the AST is built by PqlParseTreeVisitor includes are resolved to
  // the real fields
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of(
      "pathway.hierarchy",
      "go_term.altIds",
      "go_term.synonyms",
      "go_term.inferredTree");

  private static final List<String> PUBLIC_FIELDS = ImmutableList.of(
      "id",
      "name",
      "type",
      "source",
      "description",
      "geneCount",
      "diagrammed",
      "hierarchy",
      "ontology",
      "altIds",
      "synonyms",
      "inferredTree");

  public GeneSetTypeModel() {
    super(defineFields(), emptyMap(), PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return GENE_SET;
  }

  @Override
  public List<String> getFacets() {
    return emptyList();
  }

  @Override
  public String prefix() {
    throw new RuntimeException("Find correct prefix for gene-set type model");
  }

  private static List<FieldModel> defineFields() {
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(string("id", "id"));
    fields.add(string("name", "name"));
    fields.add(string("type", "type"));
    fields.add(string("source", "source"));
    fields.add(string("description", "description"));

    fields.add(object("_summary",
        long_("_gene_count", "geneCount")));

    fields.add(object("pathway",
        bool("diagrammed", "diagrammed"),
        arrayOfObjects("hierarchy", "hierarchy", object(
            string("id")))));

    fields.add(object("go_term",
        string("ontology", "ontology"),
        arrayOfStrings("alt_ids", "altIds"),
        arrayOfStrings("synonyms", "synonyms"),
        arrayOfObjects("inferred_tree", "inferredTree", object(
            string("id")))));

    return fields.build();
  }

}
