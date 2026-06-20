package edu.ucsb.cs.scaffold.config;

import edu.ucsb.cs.scaffold.interceptors.RoleUpdateInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired private RoleUpdateInterceptor roleUpdateInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(roleUpdateInterceptor);
  }
}
