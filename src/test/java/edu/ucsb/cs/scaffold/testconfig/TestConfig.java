package edu.ucsb.cs.scaffold.testconfig;

import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.GoogleSignInService;
import edu.ucsb.cs.scaffold.services.GrantedAuthoritiesService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public CurrentUserService currentUserService() {
    return new MockCurrentUserServiceImpl();
  }

  @Bean
  public GrantedAuthoritiesService grantedAuthoritiesService() {
    return new GrantedAuthoritiesService();
  }

  @Bean
  public GoogleSignInService googleSignInService() {
    return new MockGoogleSignInService();
  }
}
