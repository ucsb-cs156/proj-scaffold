package edu.ucsb.cs.scaffold;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import edu.ucsb.cs.scaffold.entity.Admin;
import edu.ucsb.cs.scaffold.entity.Instructor;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.InstructorRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.wiremock.WiremockServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.extension.jwt.JwtExtensionFactory;

@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class WebTestCase {

  @Autowired UserRepository userRepository;
  @Autowired AdminRepository adminRepository;
  @Autowired InstructorRepository instructorRepository;

  @LocalServerPort private int port;

  @Value("${app.playwright.headless:true}")
  private boolean runHeadless;

  private static WireMockServer wireMockServer;

  protected Browser browser;
  protected Page page;

  @BeforeAll
  public static void setupWireMock() {
    wireMockServer =
        new WireMockServer(
            options().port(8090).globalTemplating(true).extensions(new JwtExtensionFactory()));
    WiremockServiceImpl.setupOauthMocks(wireMockServer, false);
    wireMockServer.start();
  }

  @AfterAll
  public static void teardownWiremock() {
    wireMockServer.stop();
  }

  @AfterEach
  public void teardown() {
    browser.close();
  }

  public void setupRegularUser() {
    setupUser(false, false);
  }

  public void setupAdminUser() {
    setupUser(true, false);
  }

  public void setupInstructorUser() {
    setupUser(false, true);
  }

  @SuppressWarnings("null")
  private void setupUser(boolean isAdmin, boolean isInstructor) {
    WiremockServiceImpl.setupOauthMocks(wireMockServer, isAdmin);

    String email = isInstructor ? "instructor@ucsb.edu" : "cgaucho@ucsb.edu";
    email = isAdmin ? "admin@ucsb.edu" : email;

    User user =
        User.builder()
            .email(email)
            .familyName("Gaucho")
            .givenName("Chris")
            .fullName("Chris Gaucho")
            .googleSub("123456789")
            .pictureUrl("")
            .build();

    userRepository.save(user);
    if (isInstructor) {
      instructorRepository.save(Instructor.builder().email(email).build());
    }
    if (isAdmin) {
      adminRepository.save(Admin.builder().email(email).build());
    }

    browser =
        Playwright.create()
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(runHeadless));

    BrowserContext context = browser.newContext();
    page = context.newPage();

    String url = String.format("http://localhost:%d/oauth2/authorization/my-oauth-provider", port);
    page.navigate(url);

    page.locator("#username").fill(email);
    page.locator("#password").fill("password");
    page.locator("#submit").click();
  }
}
