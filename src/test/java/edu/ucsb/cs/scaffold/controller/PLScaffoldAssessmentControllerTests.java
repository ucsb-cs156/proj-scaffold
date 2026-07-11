package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.entity.PlScaffoldAssessment;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLScaffoldAssessmentController.class)
public class PLScaffoldAssessmentControllerTests extends ControllerTestCase {

  @MockitoBean PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get() throws Exception {
    mockMvc
        .perform(get("/api/pScaffoldAssessment?plQuestion=3&plInstance=2"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get() throws Exception {
    mockMvc
        .perform(get("/api/pScaffoldAssessment?plQuestion=3&plInstance=2"))
        .andExpect(status().is(403));
  }

  // PlScaffoldAssessments are populated by sync jobs, not by POST/DELETE; the endpoints were
  // removed, so even an admin gets 405 Method Not Allowed.
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_endpoint_no_longer_exists() throws Exception {
    mockMvc
        .perform(
            post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3").with(csrf()))
        .andExpect(status().is(405));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void delete_endpoint_no_longer_exists() throws Exception {
    mockMvc
        .perform(delete("/api/pScaffoldAssessment?id=1").with(csrf()))
        .andExpect(status().is(405));
  }

  // Functionality tests

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_an_assessment() throws Exception {
    PlScaffoldAssessment assessment =
        PlScaffoldAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .plQuestionId(3L)
            .status("Pending")
            .url(null)
            .build();
    when(plScaffoldAssessmentRepository.findByPlQuestionIdAndPlInstanceId(eq(3L), eq(2L)))
        .thenReturn(Optional.of(assessment));

    MvcResult response =
        mockMvc
            .perform(get("/api/pScaffoldAssessment?plQuestion=3&plInstance=2"))
            .andExpect(status().isOk())
            .andReturn();

    verify(plScaffoldAssessmentRepository, times(1)).findByPlQuestionIdAndPlInstanceId(3L, 2L);
    String expectedJson = mapper.writeValueAsString(assessment);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void get_non_existant_assessment_returns_404() throws Exception {
    when(plScaffoldAssessmentRepository.findByPlQuestionIdAndPlInstanceId(eq(3L), eq(2L)))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/pScaffoldAssessment?plQuestion=3&plInstance=2"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "PlScaffoldAssessment with id plQuestion=3, plInstance=2 not found", json.get("message"));
  }
}
