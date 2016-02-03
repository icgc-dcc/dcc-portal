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

package org.icgc.dcc.portal.service;

import static org.icgc.dcc.portal.repository.TermsLookupRepository.TermLookupType.DONOR_IDS;

import java.util.Set;
import java.util.UUID;

import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.BaseEntitySet.Type;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySet.SubType;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Service allowing for the saving of user provided Donor Lists. (sets)
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class UserDonorListService {

  /**
   * Dependencies.
   */
  @NonNull
  private final EntityListRepository repository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final PortalProperties properties;

  @Getter(lazy = true)
  private final int dataVersion = loadDataVersion();

  public UUID save(@NonNull Set<String> donorIds) {
    val id = UUID.randomUUID();
    val version = getDataVersion();

    val newEntitySet =
        EntitySet.createForStatusFinished(id, "Uploaded donor set", "", Type.DONOR, donorIds.size(), version);
    newEntitySet.setSubtype(SubType.UPLOAD);

    termsLookupRepository.createTermsLookup(DONOR_IDS, id, donorIds);
    repository.save(newEntitySet, version);

    return id;
  }

  private int loadDataVersion() {
    return properties.getRelease().getDataVersion();
  }

}