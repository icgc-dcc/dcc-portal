package org.icgc.dcc.portal.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.icgc.dcc.portal.analysis.EnrichmentAnalyses.calculateGeneCountPValue;
import lombok.val;

import org.junit.Test;

public class EnrichmentAnalysesTest {

  @Test
  public void testCalculateGeneCountPValueDCC2851a() throws Exception {
    int q = 1; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 1; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(0.0006288517, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValueDCC2851b() throws Exception {
    int q = 4; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 28; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(6.137557e-10, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValueDCC2851c() throws Exception {
    int q = 4; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 33; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(1.225993e-09, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValueDCC2851d() throws Exception {
    int q = 2; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 5; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(3.161646e-06, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValueDCC2851e() throws Exception {
    int q = 2; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 2; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(3.164034e-07, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValueDCC2851f() throws Exception {
    int q = 5; // geneSetOverlapGeneCount
    int k = 5; // overlapGeneCount
    int m = 5; // geneSetGeneCount
    int n = 7951; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);
    System.out.println(pValue);

    assertThat(pValue).isCloseTo(3.330669e-16, offset(0.0001));
  }

  @Test
  public void testCalculateGeneCountPValue() throws Exception {
    int q = 110; // geneSetOverlapGeneCount
    int k = 180; // overlapGeneCount
    int m = 3652; // geneSetGeneCount
    int n = 21804; // universeGeneCount

    val pValue = calculateGeneCountPValue(q, k, m, n);

    assertThat(pValue).isCloseTo(2.220446e-16, offset(0.0001));
  }

}
