package edu.ucsb.cs.scaffold.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
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
 * Verifies that the Swagger UI page loads correctly.
 *
 * <p>This is intentionally a light-weight smoke test: it only checks that the API definition
 * loaded successfully and that at least one known controller tag (RosterStudents) is rendered on
 * the page. This is meant to catch cases where the Swagger page fails to load entirely, without
 * requiring frequent updates as the API surface changes.
 *
 * <p>Prerequisites: the frontend must be built ({@code npm run build} inside {@code frontend/}) so
 * that {@code target/classes/public/index.html} exists. Run with:
 *
 * <pre>
 * INTEGRATION=true mvn test
 * </pre>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ResourceLock("port-8080")
@Import(IntegrationConfig.class)
public class SwaggerWebIT extends WebTestCase {

  @Test
  public void swagger_ui_page_loads_successfully() throws Exception {
    setupRegularUser();
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/swagger-ui/index.html"));

    assertThat(page.locator("body")).not().containsText("Failed to load API definition");

    assertThat(
            page.locator("span")
                .filter(new Locator.FilterOptions().setHasText("RosterStudents"))
                .first())
        .isVisible();
  }
}
