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
package org.icgc.dcc.portal.server.analysis;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.icgc.dcc.portal.server.analysis.SurvivalAnalyzer.Interval;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SurvivalLogRankTest {

  /**
   * Derived from example provided in http://www.mas.ncl.ac.uk/~njnsm/medfac/docs/surv.pdf
   */
  @Test
  public void runTest() {
    val i11 = new Interval(0, 6);
    i11.setDied(3);
    i11.setDonors(asList(dAt(6), dAt(6), dAt(6), cAt(6)));
    val i12 = new Interval(6, 7);
    i12.setDied(1);
    i12.setDonors(asList(dAt(7)));
    val i13 = new Interval(7, 10);
    i13.setDied(1);
    i13.setDonors(asList(dAt(10), cAt(9), cAt(10)));
    val i14 = new Interval(10, 13);
    i14.setDied(1);
    i14.setDonors(asList(dAt(13), cAt(11)));
    val i15 = new Interval(13, 16);
    i15.setDied(1);
    i15.setDonors(asList(dAt(16)));
    val i16 = new Interval(16, 22);
    i16.setDied(1);
    i16.setDonors(asList(dAt(22), cAt(17), cAt(19), cAt(20)));
    val i17 = new Interval(22, 23);
    i17.setDied(1);
    i17.setDonors(asList(dAt(23)));
    val i18 = new Interval(23, 35);
    i18.setDied(0);
    i18.setDonors(asList(cAt(25), cAt(32), cAt(32), cAt(34), cAt(35)));
    val list1 = asList(i11, i12, i13, i14, i15, i16, i17, i18);

    val i21 = new Interval(0, 1);
    i21.setDied(2);
    i21.setDonors(asList(dAt(1), dAt(1)));
    val i22 = new Interval(1, 2);
    i22.setDied(2);
    i22.setDonors(asList(dAt(2), dAt(2)));
    val i23 = new Interval(2, 3);
    i23.setDied(1);
    i23.setDonors(asList(dAt(3)));
    val i24 = new Interval(3, 4);
    i24.setDied(2);
    i24.setDonors(asList(dAt(4), dAt(4)));
    val i25 = new Interval(4, 5);
    i25.setDied(2);
    i25.setDonors(asList(dAt(5), dAt(5)));
    val i26 = new Interval(5, 8);
    i26.setDied(4);
    i26.setDonors(asList(dAt(8), dAt(8), dAt(8), dAt(8)));
    val i27 = new Interval(8, 11);
    i27.setDied(2);
    i27.setDonors(asList(dAt(11), dAt(11)));
    val i28 = new Interval(11, 12);
    i28.setDied(2);
    i28.setDonors(asList(dAt(12), dAt(12)));
    val i29 = new Interval(12, 15);
    i29.setDied(1);
    i29.setDonors(asList(dAt(15)));
    val i210 = new Interval(15, 17);
    i210.setDied(1);
    i210.setDonors(asList(dAt(17)));
    val i211 = new Interval(17, 22);
    i211.setDied(1);
    i211.setDonors(asList(dAt(22)));
    val i212 = new Interval(22, 23);
    i212.setDied(1);
    i212.setDonors(asList(dAt(23)));
    val list2 = asList(i21, i22, i23, i24, i25, i26, i27, i28, i29, i210, i211, i212);

    val results = asList(list1, list2);
    val logRankTest = new SurvivalLogRank(results);
    val stats = logRankTest.runLogRankTest();

    log.debug("ChiSquared: {}", stats.getChiSquared());
    assertThat(stats.getChiSquared()).isCloseTo(15.23, Offset.offset(0.01));
    assertThat(stats.getPValue()).isLessThan(0.001);
  }

  private SurvivalAnalyzer.DonorValue cAt(int time) {
    return new SurvivalAnalyzer.DonorValue("1", "alive", time);
  }

  private SurvivalAnalyzer.DonorValue dAt(int time) {
    return new SurvivalAnalyzer.DonorValue("1", "deceased", time);
  }

}
