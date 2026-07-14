package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestionConcept;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionConceptRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PLAssessmentQuestionController.class)
public class PLAssessmentQuestionControllerTests extends ControllerTestCase {

  @MockitoBean private ConceptRepository conceptRepository;

  @MockitoBean private PlAssessmentRepository plAssessmentRepository;

  @MockitoBean private PlAssessmentQuestionRepository plAssessmentQuestionRepository;

  @MockitoBean private PlAssessmentQuestionConceptRepository plAssessmentQuestionConceptRepository;

  // ---------- GET /api/plAssessmentQuestion/{plAssessmentQuestionId}/concepts ----------

  @Test
  public void getConcepts_returns_empty_list_when_nothing_tagged() throws Exception {
    when(plAssessmentQuestionConceptRepository.findByPlAssessmentQuestionId(501L))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/plAssessmentQuestion/501/concepts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json("[]"));
  }

  @Test
  public void getConcepts_returns_tagged_concepts() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    PlAssessmentQuestionConcept tag =
        PlAssessmentQuestionConcept.builder()
            .id(9L)
            .plAssessmentQuestionId(501L)
            .concept(concept)
            .build();
    when(plAssessmentQuestionConceptRepository.findByPlAssessmentQuestionId(501L))
        .thenReturn(List.of(tag));

    String expectedJson =
        """
        [
          { "id": "9", "question_id": "501", "concept_id": "1", "subconcept_label": null }
        ]
        """;

    mockMvc
        .perform(get("/api/plAssessmentQuestion/501/concepts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson, true));
  }

  // ---------- POST /api/plAssessmentQuestion/addConcept ----------

  @Test
  public void anonymous_user_cannot_add_concept() throws Exception {
    mockMvc
        .perform(
            post("/api/plAssessmentQuestion/addConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_add_concept() throws Exception {
    mockMvc
        .perform(
            post("/api/plAssessmentQuestion/addConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_add_a_concept_tag_when_instances_match() throws Exception {
    Course course = Course.builder().id(42L).plInstanceId(7L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    PlAssessmentQuestion paq =
        PlAssessmentQuestion.builder()
            .id(501L)
            .plRepoId(10L)
            .plAssessmentId(101L)
            .plQuestionId(201L)
            .ordinal(0)
            .build();
    PlAssessment assessment =
        PlAssessment.builder().id(101L).plRepoId(10L).plInstanceId(7L).name("HW1").build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(plAssessmentQuestionRepository.findById(501L)).thenReturn(Optional.of(paq));
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(assessment));
    when(plAssessmentQuestionConceptRepository.save(any(PlAssessmentQuestionConcept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/plAssessmentQuestion/addConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isOk());

    ArgumentCaptor<PlAssessmentQuestionConcept> captor =
        ArgumentCaptor.forClass(PlAssessmentQuestionConcept.class);
    verify(plAssessmentQuestionConceptRepository).save(captor.capture());
    PlAssessmentQuestionConcept saved = captor.getValue();
    assertEquals(501L, saved.getPlAssessmentQuestionId());
    assertEquals(concept, saved.getConcept());
  }

  @Test
  @WithInstructorCoursePermissions
  public void add_concept_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessmentQuestion/addConcept")
                    .with(csrf())
                    .param("plAssessmentQuestionId", "501")
                    .param("conceptId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void add_concept_returns_404_when_pl_assessment_question_does_not_exist()
      throws Exception {
    Course course = Course.builder().id(42L).plInstanceId(7L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(plAssessmentQuestionRepository.findById(501L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessmentQuestion/addConcept")
                    .with(csrf())
                    .param("plAssessmentQuestionId", "501")
                    .param("conceptId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("PlAssessmentQuestion with id 501 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void add_concept_returns_422_when_pl_instance_does_not_match() throws Exception {
    Course course = Course.builder().id(42L).plInstanceId(7L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    PlAssessmentQuestion paq =
        PlAssessmentQuestion.builder()
            .id(501L)
            .plRepoId(10L)
            .plAssessmentId(101L)
            .plQuestionId(201L)
            .ordinal(0)
            .build();
    PlAssessment assessment =
        PlAssessment.builder().id(101L).plRepoId(10L).plInstanceId(99L).name("HW1").build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(plAssessmentQuestionRepository.findById(501L)).thenReturn(Optional.of(paq));
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(assessment));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/plAssessmentQuestion/addConcept")
                    .with(csrf())
                    .param("plAssessmentQuestionId", "501")
                    .param("conceptId", "1"))
            .andExpect(status().is(422))
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1's course PL instance does not match assessment 101's PL instance",
        json.get("message"));
    verify(plAssessmentQuestionConceptRepository, never())
        .save(any(PlAssessmentQuestionConcept.class));
  }

  // ---------- DELETE /api/plAssessmentQuestion/deleteConcept ----------

  @Test
  public void anonymous_user_cannot_delete_concept_tag() throws Exception {
    mockMvc
        .perform(
            delete("/api/plAssessmentQuestion/deleteConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_delete_concept_tag() throws Exception {
    mockMvc
        .perform(
            delete("/api/plAssessmentQuestion/deleteConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_delete_a_concept_tag() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    PlAssessmentQuestionConcept tag =
        PlAssessmentQuestionConcept.builder()
            .id(9L)
            .plAssessmentQuestionId(501L)
            .concept(concept)
            .build();
    when(plAssessmentQuestionConceptRepository.findByPlAssessmentQuestionIdAndConceptId(501L, 1L))
        .thenReturn(Optional.of(tag));

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/plAssessmentQuestion/deleteConcept")
                    .with(csrf())
                    .param("plAssessmentQuestionId", "501")
                    .param("conceptId", "1"))
            .andExpect(status().isOk())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept 1 untagged from PlAssessmentQuestion 501", json.get("message"));
    verify(plAssessmentQuestionConceptRepository).delete(tag);
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_concept_tag_returns_404_when_tag_does_not_exist() throws Exception {
    when(plAssessmentQuestionConceptRepository.findByPlAssessmentQuestionIdAndConceptId(501L, 1L))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(
            delete("/api/plAssessmentQuestion/deleteConcept")
                .with(csrf())
                .param("plAssessmentQuestionId", "501")
                .param("conceptId", "1"))
        .andExpect(status().isNotFound());
  }
}
