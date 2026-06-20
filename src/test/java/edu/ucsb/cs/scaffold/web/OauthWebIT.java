package edu.ucsb.cs.scaffold.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import edu.ucsb.cs.scaffold.WebTestCase;
import edu.ucsb.cs.scaffold.testconfig.IntegrationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ResourceLock("port-8080")
@Import(IntegrationConfig.class)
public class OauthWebIT extends WebTestCase {

  @Test
  public void regular_user_can_login_and_logout() throws Exception {
    setupUser(false);
    assertThat(page.getByText("Log Out")).isVisible();
    page.getByText("Log Out").click();
    assertThat(page.getByText("Log In")).isVisible();
    assertThat(page.getByText("Log Out")).not().isVisible();
  }

  @Test
  public void admin_user_can_login() throws Exception {
    setupUser(true);
    assertThat(page.getByText("Log Out")).isVisible();
  }
}
