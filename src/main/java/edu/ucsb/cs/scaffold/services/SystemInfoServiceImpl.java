package edu.ucsb.cs.scaffold.services;

import edu.ucsb.cs.scaffold.model.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service("systemInfo")
@ConfigurationProperties
public class SystemInfoServiceImpl extends SystemInfoService {

  @Value("${spring.h2.console.enabled:false}")
  private boolean springH2ConsoleEnabled;

  @Value("${app.showSwaggerUILink:false}")
  private boolean showSwaggerUILink;

  @Value("${app.oauth.login:/oauth2/authorization/google}")
  private String oauthLogin;

  @Value("${app.sourceRepo:https://github.com/ucsb-cs156/proj-scaffold}")
  private String sourceRepo;

  @Override
  public SystemInfo getSystemInfo() {
    SystemInfo si =
        SystemInfo.builder()
            .springH2ConsoleEnabled(this.springH2ConsoleEnabled)
            .showSwaggerUILink(this.showSwaggerUILink)
            .oauthLogin(this.oauthLogin)
            .sourceRepo(this.sourceRepo)
            .build();
    log.info("getSystemInfo returns {}", si);
    return si;
  }
}
