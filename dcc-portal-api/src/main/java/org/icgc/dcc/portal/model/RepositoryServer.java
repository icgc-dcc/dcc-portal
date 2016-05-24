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
package org.icgc.dcc.portal.model;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = PRIVATE)
public enum RepositoryServer {

  // @formatter:off
  EGA("ega",                                    "EGA - United Kingdom"),
  GDC("gdc",                                    "GDC - Chicago"),
  CGHUB("cghub",                                "CGHub - Santa Cruz"),  
  TCGA("tcga",                                  "TCGA DCC - Bethesda"),   
  PCAWG_BARCELONA("pcawg-barcelona",            "PCAWG - Barcelona"),  
  PCAWG_CGHUB("pcawg-cghub",                    "PCAWG - Santa Cruz"),    
  PCAWG_TOKYO("pcawg-tokyo",                    "PCAWG - Tokyo"),   
  PCAWG_SEOUL("pcawg-seoul",                    "PCAWG - Seoul"),        
  PCAWG_LONDON("pcawg-london",                  "PCAWG - London"),        
  PCAWG_HEIDELBERG("pcawg-heidelberg",          "PCAWG - Heidelberg"),    
  PCAWG_CHICAGO_ICGC("pcawg-chicago-icgc",      "PCAWG - Chicago (ICGC)"),
  PCAWG_CHICAGO_TCGA("pcawg-chicago-tcga",      "PCAWG - Chicago (TCGA)"),
  AWS_VIRGINIA("aws-virginia",                  "AWS - Virginia"),        
  COLLABORATORY("collaboratory",                "Collaboratory");         
  // @formatter:on

  @NonNull
  private final String repoCode;
  @NonNull
  private final String name;

  public boolean isS3() {
    return this == AWS_VIRGINIA
        || this == COLLABORATORY;
  }

  public boolean isGNOS() {
    return this == CGHUB
        || this == PCAWG_BARCELONA
        || this == PCAWG_CGHUB
        || this == PCAWG_TOKYO
        || this == PCAWG_SEOUL
        || this == PCAWG_LONDON
        || this == PCAWG_HEIDELBERG
        || this == PCAWG_CHICAGO_ICGC
        || this == PCAWG_CHICAGO_TCGA;
  }

  public boolean isGDC() {
    return this == GDC;
  }

  public boolean isEGA() {
    return this == EGA;
  }

  @JsonCreator
  public static RepositoryServer get(String repoCode) {
    return repoCode == null ? null : RepositoryServer.valueOf(repoCode.toUpperCase().replaceAll("-", "_"));
  }

  @JsonValue
  public String getRepoCode() {
    return repoCode;
  }

  @JsonValue
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return repoCode;
  }

}
