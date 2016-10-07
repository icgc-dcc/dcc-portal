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
package org.icgc.dcc.portal.server.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Index document type
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum IndexType {

  PROJECT("project"),
  DONOR("donor"),
  GENE("gene"),
  DRUG("drug"),
  MUTATION("mutation"),
  RELEASE("release"),
  PATHWAY("pathway"),
  GENE_SET("gene-set"),
  DONOR_CENTRIC("donor-centric"),
  GENE_CENTRIC("gene-centric"),
  MUTATION_CENTRIC("mutation-centric"),
  OCCURRENCE_CENTRIC("observation-centric"),
  REPOSITORY("repository"),
  FILE("file"),
  FILE_CENTRIC("file-centric"),

  DONOR_TEXT("donor-text"),
  GENE_TEXT("gene-text"),
  MUTATION_TEXT("mutation-text"),
  PATHWAY_TEXT("pathway-text"),
  GENESET_TEXT("gene-set-text"),
  DIAGRAM("diagram"),
  FILE_TEXT("file-text"),
  FILE_DONOR_TEXT("donor-text"),
  DRUG_TEXT("drug-text"),
  PROJECT_TEXT("project-text");

  @NonNull
  final String id;

}