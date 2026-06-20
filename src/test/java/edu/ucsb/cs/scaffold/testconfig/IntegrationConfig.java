package edu.ucsb.cs.scaffold.testconfig;

import edu.ucsb.cs.scaffold.config.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(SecurityConfig.class)
public class IntegrationConfig {}
