package org.icgc.dcc.portal.server.analysis;

import org.icgc.dcc.portal.server.analysis.KaplanMeier.DonorValue;
import org.junit.Test;

import java.util.ArrayList;
import lombok.val;

import static org.junit.Assert.*;


public class KaplanMeierTest {

  @Test
  public void testComputer() {
    val donors = new ArrayList<DonorValue>();

    donors.add(new DonorValue("DO1", "dead", 3, true));
    donors.add(new DonorValue("DO2", "dead", 3, true));
    donors.add(new DonorValue("DO3", "dead", 6, true));
    donors.add(new DonorValue("DO4", "dead", 8, true));
    donors.add(new DonorValue("DO5", "alive", 8, false));
    donors.add(new DonorValue("DO6", "dead", 9, true));
    donors.add(new DonorValue("DO7", "alive", 9, false));
    donors.add(new DonorValue("DO8", "alive", 9, false));
    donors.add(new DonorValue("DO9", "dead", 10, true));
    donors.add(new DonorValue("D10", "alive", 10, false));
    donors.add(new DonorValue("D11", "alive", 12, false));
    donors.add(new DonorValue("D12", "alive", 13, false));
    donors.add(new DonorValue("D13", "alive", 13, false));
    donors.add(new DonorValue("D14", "alive", 13, false));
    donors.add(new DonorValue("D15", "alive", 13, false));
    donors.add(new DonorValue("D16", "alive", 13, false));
    donors.add(new DonorValue("D17", "alive", 13, false));
    donors.add(new DonorValue("D18", "alive", 13, false));

    val intervals = KaplanMeier.compute(donors);
    // Not particularly useful as a test, but test is useful for debugging.
    assertTrue(intervals.size() == 6);
  }

}