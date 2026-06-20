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

/**
 * Verifies that after login, the user can see the main page with the concept graph.
 *
 * <p>Prerequisites: the frontend must be built ({@code npm run build} inside {@code frontend/})
 * so that {@code target/classes/public/index.html} exists. Run with:
 * <pre>INTEGRATION=true mvn test</pre>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ResourceLock("port-8080")
@Import(IntegrationConfig.class)
public class HomePageWebIT extends WebTestCase {

  @Test
  public void logged_in_user_sees_concept_graph() throws Exception {
    setupUser(false);

    // Navigate to home after login
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/"));

    // The concept graph SVG container should be present
    assertThat(page.locator("svg")).isVisible();
  }
}
