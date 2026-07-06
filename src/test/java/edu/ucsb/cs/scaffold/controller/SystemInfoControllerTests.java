package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.model.SystemInfo;
import edu.ucsb.cs.scaffold.services.SystemInfoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = SystemInfoController.class)
public class SystemInfoControllerTests extends ControllerTestCase {

  @MockitoBean edu.ucsb.cs.scaffold.repository.UserRepository userRepository;

  @MockitoBean SystemInfoService mockSystemInfoService;

  @Test
  public void systemInfo__admin_logged_in() throws Exception {

    // arrange

    edu.ucsb.cs.scaffold.model.SystemInfo systemInfo =
        SystemInfo.builder()
            .showSwaggerUILink(true)
            .springH2ConsoleEnabled(true)
            .oauthLogin("/oauth2/authorization/google")
            .build();
    when(mockSystemInfoService.getSystemInfo()).thenReturn(systemInfo);
    String expectedJson = mapper.writeValueAsString(systemInfo);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/systemInfo")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void school_list_matches() throws Exception {
    MvcResult response =
        mockMvc.perform(get("/api/systemInfo/schools")).andExpect(status().isOk()).andReturn();
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of(School.values()));
    assertEquals(expectedJson, responseString);
  }
}
