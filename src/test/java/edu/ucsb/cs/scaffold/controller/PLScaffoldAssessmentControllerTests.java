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
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.entity.PlScaffoldAssessment;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLScaffoldAssessmentController.class)
public class PLScaffoldAssessmentControllerTests extends ControllerTestCase {

  @MockitoBean PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @MockitoBean PlRepoRepository plRepoRepository;

  @MockitoBean PlInstanceRepository plInstanceRepository;

  @MockitoBean PlQuestionRepository plQuestionRepository;

  private final PlRepo repo =
      PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
  private final PlInstance instance =
      PlInstance.builder().id(2L).plRepoId(1L).shortName("Fall2025").build();
  private final PlQuestion question =
      PlQuestion.builder()
          .id(3L)
          .plRepoId(1L)
          .questionId("q1")
          .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
          .title("Title")
          .build();

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

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/pScaffoldAssessment?id=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/pScaffoldAssessment?id=1").with(csrf()))
        .andExpect(status().is(403));
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

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_assessment_for_non_existent_repo() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 1 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_assessment_for_non_existent_instance() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlInstance with id 2 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_assessment_for_non_existent_question() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));
    when(plQuestionRepository.findById(eq(3L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlQuestion with id 3 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_post_a_new_assessment() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));
    when(plQuestionRepository.findById(eq(3L))).thenReturn(Optional.of(question));
    when(plScaffoldAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndPlQuestionId(
            eq(1L), eq(2L), eq(3L)))
        .thenReturn(false);

    PlScaffoldAssessment assessment =
        PlScaffoldAssessment.builder()
            .plRepoId(1L)
            .plInstanceId(2L)
            .plQuestionId(3L)
            .status("Pending")
            .url(null)
            .build();
    PlScaffoldAssessment savedAssessment =
        PlScaffoldAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .plQuestionId(3L)
            .status("Pending")
            .url(null)
            .build();
    when(plScaffoldAssessmentRepository.save(eq(assessment))).thenReturn(savedAssessment);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plScaffoldAssessmentRepository, times(1)).save(assessment);
    String expectedJson = mapper.writeValueAsString(savedAssessment);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_duplicate_assessment() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    when(plInstanceRepository.findById(eq(2L))).thenReturn(Optional.of(instance));
    when(plQuestionRepository.findById(eq(3L))).thenReturn(Optional.of(question));
    when(plScaffoldAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndPlQuestionId(
            eq(1L), eq(2L), eq(3L)))
        .thenReturn(true);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/pScaffoldAssessment?plRepoId=1&plInstanceId=2&plQuestionId=3")
                    .with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "PlScaffoldAssessment already exists for plRepoId 1, plInstanceId 2, plQuestionId 3",
        json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_delete_an_assessment() throws Exception {
    PlScaffoldAssessment assessment =
        PlScaffoldAssessment.builder()
            .id(1L)
            .plRepoId(1L)
            .plInstanceId(2L)
            .plQuestionId(3L)
            .status("Pending")
            .url(null)
            .build();
    when(plScaffoldAssessmentRepository.findById(eq(1L))).thenReturn(Optional.of(assessment));

    MvcResult response =
        mockMvc
            .perform(delete("/api/pScaffoldAssessment?id=1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plScaffoldAssessmentRepository, times(1)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("PlScaffoldAssessment with id 1 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_non_existant_assessment_and_gets_right_error_message()
      throws Exception {
    when(plScaffoldAssessmentRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/pScaffoldAssessment?id=7").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlScaffoldAssessment with id 7 not found", json.get("message"));
  }
}
