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
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLQuestionController.class)
public class PLQuestionControllerTests extends ControllerTestCase {

  @MockitoBean PlQuestionRepository plQuestionRepository;

  @MockitoBean PlRepoRepository plRepoRepository;

  private final PlRepo repo =
      PlRepo.builder().id(1L).repoName("PrairieLearn/pl-ucsb-cmpsc5b").build();

  private final UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plQuestion?plrepoId=1")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/plQuestion?plrepoId=1")).andExpect(status().is(403));
  }

  // PlQuestions are created, updated, and deleted by the SyncPlRepo job (issues #45/#47), not by
  // POST/DELETE; the endpoints were removed, so even an admin gets 405 Method Not Allowed.
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_endpoint_no_longer_exists() throws Exception {
    mockMvc
        .perform(
            post("/api/plQuestion?plRepoId=1&questionId=q1&uuid=%s&title=Title".formatted(uuid))
                .with(csrf()))
        .andExpect(status().is(405));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void delete_endpoint_no_longer_exists() throws Exception {
    mockMvc.perform(delete("/api/plQuestion?id=1").with(csrf())).andExpect(status().is(405));
  }

  // Functionality tests

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_all_questions_for_a_repo() throws Exception {
    when(plRepoRepository.findById(eq(1L))).thenReturn(Optional.of(repo));
    PlQuestion question1 =
        PlQuestion.builder().id(1L).plRepoId(1L).questionId("q1").uuid(uuid).title("T1").build();
    PlQuestion question2 =
        PlQuestion.builder().id(2L).plRepoId(1L).questionId("q2").uuid(uuid).title("T2").build();
    ArrayList<PlQuestion> expected = new ArrayList<>(Arrays.asList(question1, question2));
    when(plQuestionRepository.findByPlRepoId(eq(1L))).thenReturn(expected);

    MvcResult response =
        mockMvc.perform(get("/api/plQuestion?plrepoId=1")).andExpect(status().isOk()).andReturn();

    verify(plQuestionRepository, times(1)).findByPlRepoId(1L);
    String expectedJson = mapper.writeValueAsString(expected);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void get_all_questions_for_non_existant_repo_returns_404() throws Exception {
    when(plRepoRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/plQuestion?plrepoId=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("PlRepo with id 7 not found", json.get("message"));
  }
}
