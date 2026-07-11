package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLAssessmentController.class)
public class PLAssessmentControllerTests extends ControllerTestCase {

  @MockitoBean PlAssessmentRepository plAssessmentRepository;

  @MockitoBean PlRepoRepository plRepoRepository;

  @MockitoBean PlInstanceRepository plInstanceRepository;

  private final PlRepo repo =
      PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
  private final PlInstance instance =
      PlInstance.builder().id(2L).plRepoId(1L).name("Fall2025").build();

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

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post(
                "/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plAssessment?id=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plAssessment?id=1").with(csrf())).andExpect(status().is(403));
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

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_an_assessment_for_non_existent_repo() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 1 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_an_assessment_for_non_existent_instance() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlInstance with id 2 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_post_a_new_assessment() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));
    when(plAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndName(eq(1L), eq(2L), eq("HW1")))
        .thenReturn(false);
    PlAssessment assessment =
        PlAssessment.builder()
            .plRepoId(1L)
            .plInstanceId(2L)
            .name("HW1")
            .url("https://example.com/hw1")
            .build();
    PlAssessment savedAssessment =
        PlAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .name("HW1")
            .url("https://example.com/hw1")
            .build();
    when(plAssessmentRepository.save(eq(assessment))).thenReturn(savedAssessment);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plAssessmentRepository, times(1)).save(assessment);
    String expectedJson = mapper.writeValueAsString(savedAssessment);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_a_blank_assessment_name() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessment?plRepoId=1&plInstanceId=2&name= &url=https://example.com")
                    .with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("name is required", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_a_duplicate_assessment_name() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));
    when(plAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndName(eq(1L), eq(2L), eq("HW1")))
        .thenReturn(true);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessment?plRepoId=1&plInstanceId=2&name=HW1&url=https://example.com/hw1")
                    .with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "PlAssessment with name HW1 already exists for plRepoId 1, plInstanceId 2",
        json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_delete_an_assessment() throws Exception {
    PlAssessment assessment =
        PlAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .name("HW1")
            .url("https://example.com/hw1")
            .build();
    when(plAssessmentRepository.findById(eq(1L))).thenReturn(Optional.of(assessment));

    MvcResult response =
        mockMvc
            .perform(delete("/api/plAssessment?id=1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plAssessmentRepository, times(1)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("PlAssessment with id 1 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_non_existant_assessment_and_gets_right_error_message()
      throws Exception {
    when(plAssessmentRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/plAssessment?id=7").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlAssessment with id 7 not found", json.get("message"));
  }
}
