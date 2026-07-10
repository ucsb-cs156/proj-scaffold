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
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLRepoController.class)
public class PLRepoControllerTests extends ControllerTestCase {

  @MockitoBean PlRepoRepository plRepoRepository;

  @MockitoBean PlInstanceRepository plInstanceRepository;

  @MockitoBean PlQuestionRepository plQuestionRepository;

  @MockitoBean PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @MockitoBean PlAssessmentRepository plAssessmentRepository;

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plrepo")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plrepo")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void logged_in_instructor_can_get_all() throws Exception {
    mockMvc.perform(get("/api/plrepo")).andExpect(status().is(200));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/plrepo?repoName=PrairieLearn/pl-ucsb-cmpsc5b"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/plrepo?repoName=PrairieLearn/pl-ucsb-cmpsc5b").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plrepo?id=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plrepo?id=1").with(csrf())).andExpect(status().is(403));
  }

  // Functionality tests

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_all_repos() throws Exception {
    PlRepo repo1 = PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
    PlRepo repo2 = PlRepo.builder().id(2L).repoName("PrairieLearn/pl-ucsb-cmpsc8").build();
    ArrayList<PlRepo> expected = new ArrayList<>(Arrays.asList(repo1, repo2));
    when(plRepoRepository.findAll()).thenReturn(expected);

    MvcResult response = mockMvc.perform(get("/api/plrepo")).andExpect(status().isOk()).andReturn();

    verify(plRepoRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_post_a_new_repo() throws Exception {
    PlRepo repo = PlRepo.builder().repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
    PlRepo savedRepo = PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
    when(plRepoRepository.existsByRepoName(eq("PrairieLearn/pl-ucsb-cmpsc5b"))).thenReturn(false);
    when(plRepoRepository.save(eq(repo))).thenReturn(savedRepo);

    MvcResult response =
        mockMvc
            .perform(post("/api/plrepo?repoName=PrairieLearn/pl-ucsb-cmpsc5b").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plRepoRepository, times(1)).save(repo);
    String expectedJson = mapper.writeValueAsString(savedRepo);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_a_duplicate_repo_name() throws Exception {
    when(plRepoRepository.existsByRepoName(eq("PrairieLearn/pl-ucsb-cmpsc5b"))).thenReturn(true);

    MvcResult response =
        mockMvc
            .perform(post("/api/plrepo?repoName=PrairieLearn/pl-ucsb-cmpsc5b").with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "PlRepo with repoName PrairieLearn/pl-ucsb-cmpsc5b already exists", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_a_blank_repo_name() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/plrepo?repoName= ").with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("repoName is required", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_cannot_post_an_invalid_repo_name() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/plrepo?repoName=https://github.com/foo/bar").with(csrf()))
            .andExpect(status().is(400))
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "repoName must be in the format owner/repo, e.g. PrairieLearn/pl-ucsb-cmpsc5b",
        json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_delete_a_repo_and_cascades_to_instances_and_questions() throws Exception {
    PlRepo repo = PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));

    MvcResult response =
        mockMvc
            .perform(delete("/api/plrepo?id=1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plScaffoldAssessmentRepository, times(1)).deleteByPlRepoId(1L);
    verify(plAssessmentRepository, times(1)).deleteByPlRepoId(1L);
    verify(plInstanceRepository, times(1)).deleteByPlRepoId(1L);
    verify(plQuestionRepository, times(1)).deleteByPlRepoId(1L);
    verify(plRepoRepository, times(1)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 1 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_non_existant_repo_and_gets_right_error_message()
      throws Exception {
    when(plRepoRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/plrepo?id=7").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 7 not found", json.get("message"));
  }
}
