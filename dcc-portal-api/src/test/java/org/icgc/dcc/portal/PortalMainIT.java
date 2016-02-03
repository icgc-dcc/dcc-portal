package org.icgc.dcc.portal;

import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Ignore
public class PortalMainIT {

  private final Environment env = Environment.PROD;
  private WebDriver driver;

  @RequiredArgsConstructor(access = PRIVATE)
  @Getter
  enum Environment {
    LOCAL("http://localhost:8080"), DEV(""), PROD("http://portal.dcc.icgc.org");

    private final String baseUrl;

  }

  @Before
  @SneakyThrows
  public void setUp() {
    // Start Chrome driver
    System.setProperty("webdriver.chrome.driver", "/Applications/Google Chrome.app/Contents/MacOS/chromedriver");
    driver = new ChromeDriver();

    if (env == Environment.LOCAL) {
      // Start portal server
      System.setProperty("dw.elastic.indexName", "dcc-release-load-prod-06e-32-22");
      PortalMain main = new PortalMain();
      main.run(new String[] { "server", "src/main/conf/settings.yml" });
    }
  }

  @After
  public void tearDown() {
    driver.quit();
  }

  @Test
  public void testHomePage() {
    driver.get(env.getBaseUrl());
    assertThat(driver.getTitle()).isEqualTo("Welcome :: ICGC Data Portal");

    driver.findElement(By.xpath("//*[@id=\"home-page\"]/div[2]/header/nav[1]/ul/li[2]/a")).click();

    WebElement bloodFacet =
        driver
            .findElement(By
                .xpath(
                    "//*[@id=\"projects-page\"]/div[2]/div[1]/aside/div/ul/li[1]/ul/li[2]/ul[2]/li[1]/ul/li[1]/span[2]/span"));

    System.out.println(bloodFacet.getText());
  }

}
