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
public class ConceptGraphWebIT extends WebTestCase {

  @Test
  public void logged_in_user_sees_concept_graph() throws Exception {

    setupAdminUser();
    // Navigate to the database-driven concept graph page for course 1, which the
    // application always seeds on startup.
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/course/1"));
    // The top bar renders once the graph data loads; the star counter shows the
    // seeded course's 26 concepts, and the settings link appears once the course
    // itself has been fetched.
    assertThat(page.getByTestId("ScaffoldTopBar")).isVisible();
    assertThat(page.getByTestId("ScaffoldTopBar").getByText("0 / 26")).isVisible();
    assertThat(page.getByTestId("ScaffoldTopBar-linkToSettings")).isVisible();
  }
}
