package edu.ucsb.cs.scaffold.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AssessmentController.class)
@Import(edu.ucsb.cs.scaffold.testconfig.TestConfig.class)
@TestPropertySource(
    properties = "app.admin.emails=djensen@ucsb.edu,phtcon@ucsb.edu,acdamstedt@ucsb.edu")
public class AssessmentControllerTests extends ControllerTestCase {

  @MockitoBean CourseRepository courseRepository;
  @MockitoBean PlAssessmentRepository plAssessmentRepository;
  @MockitoBean PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  @MockitoBean PlQuestionRepository plQuestionRepository;

  @Test
  public void getAssessments_returns_empty_list_when_course_not_found() throws Exception {
    when(courseRepository.findById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/assessments").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void getAssessments_returns_empty_list_when_pl_repo_id_is_null() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(null).plInstanceId(2L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    mockMvc
        .perform(get("/api/assessments").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void getAssessments_returns_empty_list_when_pl_instance_id_is_null() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(2L).plInstanceId(null).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    mockMvc
        .perform(get("/api/assessments").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void getAssessments_maps_and_orders_by_pl_assessment_order_then_name() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // out of order on purpose: order 2, no order (falls back to name "hw01"), order 1
    PlAssessment orderTwo =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw02")
            .plAssessmentOrder(2L)
            .plAssessmentTitle("Homework 2")
            .plAssessmentId(5001L)
            .build();
    PlAssessment noOrderFallsBackToName =
        PlAssessment.builder()
            .id(102L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw01")
            .plAssessmentOrder(null)
            .plAssessmentTitle(null)
            .plAssessmentId(null)
            .build();
    PlAssessment orderOne =
        PlAssessment.builder()
            .id(103L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw00")
            .plAssessmentOrder(1L)
            .plAssessmentTitle("Homework 0")
            .plAssessmentId(5000L)
            .build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(10L, 20L))
        .thenReturn(List.of(orderTwo, noOrderFallsBackToName, orderOne));

    String expectedJson =
        """
        [
          { "id": "103", "pl_assessment_id": "5000", "name": "Homework 0" },
          { "id": "101", "pl_assessment_id": "5001", "name": "Homework 2" },
          { "id": "102", "pl_assessment_id": null, "name": "hw01" }
        ]
        """;

    mockMvc
        .perform(get("/api/assessments").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson, true));
  }

  @Test
  public void getQuestions_returns_empty_list_when_assessment_has_no_questions() throws Exception {
    when(plAssessmentQuestionRepository.findByPlAssessmentIdOrderByOrdinalAsc(999L))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/assessments/999/questions"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void getQuestions_returns_questions_in_ordinal_order_and_skips_orphaned_join_rows()
      throws Exception {
    PlAssessmentQuestion joinRowOne =
        PlAssessmentQuestion.builder()
            .id(1L)
            .plRepoId(10L)
            .plAssessmentId(101L)
            .plQuestionId(201L)
            .ordinal(0)
            .build();
    // References a PlQuestion that no longer exists; must be silently skipped.
    PlAssessmentQuestion orphanedJoinRow =
        PlAssessmentQuestion.builder()
            .id(2L)
            .plRepoId(10L)
            .plAssessmentId(101L)
            .plQuestionId(999L)
            .ordinal(1)
            .build();
    PlAssessmentQuestion joinRowTwo =
        PlAssessmentQuestion.builder()
            .id(3L)
            .plRepoId(10L)
            .plAssessmentId(101L)
            .plQuestionId(202L)
            .ordinal(2)
            .build();
    when(plAssessmentQuestionRepository.findByPlAssessmentIdOrderByOrdinalAsc(101L))
        .thenReturn(List.of(joinRowOne, orphanedJoinRow, joinRowTwo));

    UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    PlQuestion question1 =
        PlQuestion.builder()
            .id(201L)
            .plRepoId(10L)
            .questionId("q1")
            .uuid(uuid1)
            .title("Q1")
            .build();
    PlQuestion question2 =
        PlQuestion.builder()
            .id(202L)
            .plRepoId(10L)
            .questionId("q2")
            .uuid(uuid2)
            .title("Q2")
            .build();
    when(plQuestionRepository.findAllById(List.of(201L, 999L, 202L)))
        .thenReturn(List.of(question1, question2));

    String expectedJson =
        """
        [
          { "id": "201", "assessment_id": "101", "pl_question_uuid": "%s", "title": "Q1" },
          { "id": "202", "assessment_id": "101", "pl_question_uuid": "%s", "title": "Q2" }
        ]
        """
            .formatted(uuid1, uuid2);

    mockMvc
        .perform(get("/api/assessments/101/questions"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson, true));
  }
}
