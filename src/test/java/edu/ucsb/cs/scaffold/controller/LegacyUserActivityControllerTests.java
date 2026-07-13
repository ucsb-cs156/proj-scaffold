package edu.ucsb.cs.scaffold.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.model.LegacyUserActivity;
import edu.ucsb.cs.scaffold.repository.LegacyUserActivityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = LegacyUserActivityController.class)
@Import(edu.ucsb.cs.scaffold.testconfig.TestConfig.class)
@TestPropertySource(
    properties = "app.admin.emails=djensen@ucsb.edu,phtcon@ucsb.edu,acdamstedt@ucsb.edu")
public class LegacyUserActivityControllerTests extends ControllerTestCase {

  @MockitoBean LegacyUserActivityRepository userActivityRepository;

  @Test
  public void post_with_null_userid_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/api/legacy/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"event_type": "click"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void post_with_null_event_type_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/api/legacy/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userid": 1}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void post_with_blank_event_type_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/api/legacy/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userid": 1, "event_type": "   "}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void post_with_null_payload_saves_empty_json_object() throws Exception {
    mockMvc
        .perform(
            post("/api/legacy/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userid": 1, "event_type": "click"}
                    """))
        .andExpect(status().isNoContent());

    ArgumentCaptor<LegacyUserActivity> captor = ArgumentCaptor.forClass(LegacyUserActivity.class);
    verify(userActivityRepository).save(captor.capture());
    assertThat(captor.getValue().getPayload()).isEqualTo("{}");
  }

  @Test
  public void post_with_payload_saves_serialized_json_string() throws Exception {
    mockMvc
        .perform(
            post("/api/legacy/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userid": 1, "event_type": "click", "payload": {"key": "value"}}
                    """))
        .andExpect(status().isNoContent());

    ArgumentCaptor<LegacyUserActivity> captor = ArgumentCaptor.forClass(LegacyUserActivity.class);
    verify(userActivityRepository).save(captor.capture());
    assertThat(captor.getValue().getPayload()).isEqualTo("{\"key\":\"value\"}");
  }
}
