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
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
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

@WebMvcTest(controllers = PLInstanceController.class)
public class PLInstanceControllerTests extends ControllerTestCase {

  @MockitoBean PlInstanceRepository plInstanceRepository;

  @MockitoBean PlRepoRepository plRepoRepository;

  @MockitoBean PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @MockitoBean PlAssessmentRepository plAssessmentRepository;

  private final PlRepo repo =
      PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plinstance?plrepoId=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plinstance?plrepoId=1")).andExpect(status().is(403));
  }

  // PlInstances are created by the SyncPlRepo job (issue #45), not by POST; the endpoint was
  // removed, so even an admin gets 405 Method Not Allowed.
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_endpoint_no_longer_exists() throws Exception {
    mockMvc
        .perform(post("/api/plinstance?plRepoId=1&name=Fall2025").with(csrf()))
        .andExpect(status().is(405));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plinstance?id=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_delete() throws Exception {
    mockMvc.perform(delete("/api/plinstance?id=1").with(csrf())).andExpect(status().is(403));
  }

  // Functionality tests

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_all_instances_for_a_repo() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    PlInstance instance1 = PlInstance.builder().id(1L).plRepoId(1L).shortName("Fall2025").build();
    PlInstance instance2 = PlInstance.builder().id(2L).plRepoId(1L).shortName("Winter2026").build();
    ArrayList<PlInstance> expected = new ArrayList<>(Arrays.asList(instance1, instance2));
    when(plInstanceRepository.findByPlRepoId(eq(1L))).thenReturn(expected);

    MvcResult response =
        mockMvc.perform(get("/api/plinstance?plrepoId=1")).andExpect(status().isOk()).andReturn();

    verify(plInstanceRepository, times(1)).findByPlRepoId(1L);
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void get_all_instances_for_non_existant_repo_returns_404() throws Exception {
    when(plRepoRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/plinstance?plrepoId=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_delete_an_instance_and_cascades_to_assessments() throws Exception {
    PlInstance instance = PlInstance.builder().id(1L).plRepoId(1L).shortName("Fall2025").build();
    when(plInstanceRepository.findById(eq(1L))).thenReturn(Optional.of(instance));

    MvcResult response =
        mockMvc
            .perform(delete("/api/plinstance?id=1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plScaffoldAssessmentRepository, times(1)).deleteByPlInstanceId(1L);
    verify(plAssessmentRepository, times(1)).deleteByPlInstanceId(1L);
    verify(plInstanceRepository, times(1)).delete(any());
    Map<String, Object> json = responseToJson(response);
    assertEquals("PlInstance with id 1 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_tries_to_delete_non_existant_instance_and_gets_right_error_message()
      throws Exception {
    when(plInstanceRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/plinstance?id=7").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlInstance with id 7 not found", json.get("message"));
  }
}
