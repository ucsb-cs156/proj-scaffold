package edu.ucsb.cs.scaffold.web;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import edu.ucsb.cs.scaffold.WebTestCase;
import edu.ucsb.cs.scaffold.testconfig.IntegrationConfig;
import java.util.regex.Pattern;
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
public class HomePageWebIT extends WebTestCase {

  @Test
  public void logged_in_user_sees_legacy_home_page() throws Exception {
    setupRegularUser();
    // Navigate to the database-driven concept graph page for course 1, which the
    // application always seeds on startup.
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/LegacyHomePage"));
    assertThat(
            page.locator("div")
                .filter(
                    new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^ScaffoldSelect assessment…0 \\/ 26$")))
                .getByRole(AriaRole.IMG)
                .first())
        .isVisible();
  }

  @Test
  public void logged_in_regular_user_sees_home_page() throws Exception {

    setupRegularUser();
    // Navigate to the database-driven concept graph page for course 1, which the
    // application always seeds on startup.
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/"));
    // Verify that there is an h1 element with the text "Your Student Courses"
    assertThat(
            page.locator("h1")
                .filter(new Locator.FilterOptions().setHasText("Your Student Courses"))
                .first())
        .isVisible();
    assertThat(
            page.locator("h1")
                .filter(new Locator.FilterOptions().setHasText("Your Staff Courses"))
                .first())
        .isVisible();

    // Verify that there the text "Your Instructor Courses" is not visble on the
    // page in ANY kind of element
    assertThat(
            page.locator("*")
                .filter(new Locator.FilterOptions().setHasText("Your Instructor Courses"))
                .first())
        .not()
        .isVisible();
  }

  @Test
  public void logged_in_instructor_user_sees_home_page() throws Exception {

    setupInstructorUser();
    // Navigate to the database-driven concept graph page for course 1, which the
    // application always seeds on startup.
    page.navigate(page.url().replaceAll("(http://localhost:\\d+).*", "$1/"));
    // Verify that there is an h1 element with the text "Your Student Courses"
    assertThat(
            page.locator("h1")
                .filter(new Locator.FilterOptions().setHasText("Your Instructor Courses"))
                .first())
        .isVisible();
    assertThat(
            page.locator("h1")
                .filter(new Locator.FilterOptions().setHasText("Your Student Courses"))
                .first())
        .isVisible();
    assertThat(
            page.locator("h1")
                .filter(new Locator.FilterOptions().setHasText("Your Staff Courses"))
                .first())
        .isVisible();
  }
}
