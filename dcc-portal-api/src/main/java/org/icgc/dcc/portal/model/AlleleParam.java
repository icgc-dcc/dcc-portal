/**
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Pattern;

import lombok.NonNull;

import org.icgc.dcc.portal.util.AlleleParser;

import com.yammer.dropwizard.jersey.params.AbstractParam;

public class AlleleParam extends AbstractParam<AlleleMutation> {

  private static final Pattern ALLELE_SUB_REGEX = Pattern.compile("^[ACTG]+");
  private static final Pattern ALLELE_INS_REGEX = Pattern.compile("^([ACTG])>\\1[ACTG]+");
  private static final Pattern ALLELE_ALT_INS_REGEX = Pattern.compile("^->[ACTG]+");
  private static final Pattern ALLELE_DEL_REGEX = Pattern.compile("^[ACTG]+>-");
  private static final Pattern ALLELE_ALT_DEL_REGEX = Pattern.compile("^([ACTG])[ACTG]+>\\1");

  public AlleleParam(String input) {
    super(input);
  }

  @Override
  @NonNull
  protected AlleleMutation parse(String input) throws Exception {
    String allele = input.trim();

    checkArgument(isValidAllele(allele), "'allele' parameter is not valid for input '%s': Must be [ACTG]+ or an indel",
        input);

    return AlleleParser.parseAllele(allele);
  }

  private Boolean isValidAllele(String allele) {
    return ALLELE_SUB_REGEX.matcher(allele).matches() ||
        ALLELE_INS_REGEX.matcher(allele).matches() ||
        ALLELE_ALT_INS_REGEX.matcher(allele).matches() ||
        ALLELE_DEL_REGEX.matcher(allele).matches() ||
        ALLELE_ALT_DEL_REGEX.matcher(allele).matches();
  }

}
