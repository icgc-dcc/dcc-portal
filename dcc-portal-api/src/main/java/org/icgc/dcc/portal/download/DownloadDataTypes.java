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
package org.icgc.dcc.portal.download;

import static lombok.AccessLevel.PRIVATE;

import java.util.Set;

import lombok.NoArgsConstructor;

import org.icgc.dcc.common.core.model.DownloadDataType;

import com.google.common.collect.ImmutableSet;

@NoArgsConstructor(access = PRIVATE)
public final class DownloadDataTypes {

  public static final Set<DownloadDataType> PUBLIC_DATA_TYPES = ImmutableSet.<DownloadDataType> builder()
      .add(DownloadDataType.SSM_OPEN)
      .add(DownloadDataType.DONOR)
      .addAll(DownloadDataType.CLINICAL)
      .add(DownloadDataType.JCN)
      .add(DownloadDataType.METH_ARRAY)
      .add(DownloadDataType.METH_SEQ)
      .add(DownloadDataType.MIRNA_SEQ)
      .add(DownloadDataType.STSM)
      .add(DownloadDataType.PEXP)
      .add(DownloadDataType.EXP_ARRAY)
      .add(DownloadDataType.EXP_SEQ)
      .build();

  public static final Set<DownloadDataType> CONTROLLED_DATA_TYPES = ImmutableSet.<DownloadDataType> builder()
      .add(DownloadDataType.SSM_CONTROLLED)
      .add(DownloadDataType.SGV_CONTROLLED)
      .addAll(DownloadDataType.CLINICAL)
      .add(DownloadDataType.CNSM)
      .add(DownloadDataType.JCN)
      .add(DownloadDataType.METH_ARRAY)
      .add(DownloadDataType.METH_SEQ)
      .add(DownloadDataType.MIRNA_SEQ)
      .add(DownloadDataType.STSM)
      .add(DownloadDataType.PEXP)
      .add(DownloadDataType.EXP_ARRAY)
      .add(DownloadDataType.EXP_SEQ)
      .build();

}
