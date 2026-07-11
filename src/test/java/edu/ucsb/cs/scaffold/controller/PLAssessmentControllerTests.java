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
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLAssessmentController.class)
public class PLAssessmentControllerTests extends ControllerTestCase {

  @MockitoBean PlAssessmentRepository plAssessmentRepository;

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plAssessment?plRepoId=1&plInstance=2")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plAssessment?plRepoId=1&plInstance=2")).andExpect(status().is(403));
  }

  // PlAssessments are populated by sync jobs, not by POST/DELETE; the endpoints were removed, so
  // even an admin gets 405 Method Not Allowed.
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_endpoint_no_longer_exists() throws Exception {
    mockMvc
        .perform(
            post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                .with(csrf()))
        .andExpect(status().is(405));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void delete_endpoint_no_longer_exists() throws Exception {
    mockMvc.perform(delete("/api/plAssessment?id=1").with(csrf())).andExpect(status().is(405));
  }

  // Functionality tests

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_all_assessments_for_a_repo_and_instance() throws Exception {
    PlAssessment assessment1 =
        PlAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .name("HW1")
            .url("https://example.com/hw1")
            .build();
    PlAssessment assessment2 =
        PlAssessment.builder()
            .id(2L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .name("HW2")
            .url("https://example.com/hw2")
            .build();
    ArrayList<PlAssessment> expected = new ArrayList<>(Arrays.asList(assessment1, assessment2));
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(eq(1L), eq(2L))).thenReturn(expected);

    MvcResult response =
        mockMvc
            .perform(get("/api/plAssessment?plRepoId=1&plInstance=2"))
            .andExpect(status().isOk())
            .andReturn();

    verify(plAssessmentRepository, times(1)).findByPlRepoIdAndPlInstanceId(1L, 2L);
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }
}
