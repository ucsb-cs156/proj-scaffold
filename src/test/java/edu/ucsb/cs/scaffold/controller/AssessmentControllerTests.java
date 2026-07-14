package edu.ucsb.cs.scaffold.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.annotations.WithStaffCoursePermissions;
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
  public void getAssessments_maps_and_orders_by_pl_assessment_order_then_name_and_excludes_locked()
      throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // out of order on purpose: order 2, no order (falls back to name "hw01"), order 1, and a
    // locked one that must be excluded entirely regardless of its order.
    PlAssessment orderTwo =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw02")
            .plAssessmentOrder(2L)
            .plAssessmentTitle("Homework 2")
            .plAssessmentId(5001L)
            .plAssessmentSetAbbreviation("HW")
            .plAssessmentNumber("2")
            .plAssessmentSetColor("green1")
            .locked(false)
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
            .plAssessmentSetAbbreviation(null)
            .plAssessmentNumber(null)
            .plAssessmentSetColor(null)
            .locked(false)
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
            .plAssessmentSetAbbreviation("HW")
            .plAssessmentNumber("0")
            .plAssessmentSetColor("blue1")
            .locked(false)
            .build();
    PlAssessment lockedAssessment =
        PlAssessment.builder()
            .id(104L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw-locked")
            .plAssessmentOrder(0L)
            .plAssessmentTitle("Homework Locked")
            .plAssessmentId(5002L)
            .locked(true)
            .build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(10L, 20L))
        .thenReturn(List.of(orderTwo, noOrderFallsBackToName, orderOne, lockedAssessment));

    String expectedJson =
        """
        [
          {
            "id": "103", "pl_assessment_id": "5000", "name": "Homework 0",
            "pl_assessment_set_abbreviation": "HW", "pl_assessment_number": "0",
            "pl_assessment_set_color": "blue1"
          },
          {
            "id": "101", "pl_assessment_id": "5001", "name": "Homework 2",
            "pl_assessment_set_abbreviation": "HW", "pl_assessment_number": "2",
            "pl_assessment_set_color": "green1"
          },
          {
            "id": "102", "pl_assessment_id": null, "name": "hw01",
            "pl_assessment_set_abbreviation": null, "pl_assessment_number": null,
            "pl_assessment_set_color": null
          }
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

  // ---------- GET /api/assessments/all ----------

  @Test
  public void anonymous_user_cannot_get_all_assessments() throws Exception {
    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithStaffCoursePermissions
  public void staff_cannot_get_all_assessments() throws Exception {
    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getAllAssessments_returns_404_when_course_not_found() throws Exception {
    when(courseRepository.findById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getAllAssessments_returns_empty_list_when_pl_repo_id_is_null() throws Exception {
    // No PlRepo/PlInstance associated with the course yet is a normal, unconfigured state,
    // not an error -- the modal should just show nothing to manage.
    Course course = Course.builder().id(1L).plRepoId(null).plInstanceId(2L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void getAllAssessments_returns_empty_list_when_pl_instance_id_is_null() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(null).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void getAllAssessments_returns_both_locked_and_unlocked_assessments() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    PlAssessment unlocked =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw01")
            .plAssessmentOrder(1L)
            .plAssessmentTitle("Homework 1")
            .plAssessmentSetAbbreviation("HW")
            .plAssessmentNumber("1")
            .plAssessmentSetColor("blue1")
            .locked(false)
            .build();
    PlAssessment locked =
        PlAssessment.builder()
            .id(102L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw02")
            .plAssessmentOrder(2L)
            .plAssessmentTitle(null)
            .locked(true)
            .build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(10L, 20L))
        .thenReturn(List.of(unlocked, locked));

    String expectedJson =
        """
        [
          {
            "id": "101", "name": "Homework 1", "locked": false,
            "pl_assessment_set_abbreviation": "HW", "pl_assessment_number": "1",
            "pl_assessment_set_color": "blue1"
          },
          {
            "id": "102", "name": "hw02", "locked": true,
            "pl_assessment_set_abbreviation": null, "pl_assessment_number": null,
            "pl_assessment_set_color": null
          }
        ]
        """;

    mockMvc
        .perform(get("/api/assessments/all").param("courseId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson, true));
  }

  // ---------- PUT /api/assessments/lock ----------

  @Test
  public void anonymous_user_cannot_set_locked() throws Exception {
    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithStaffCoursePermissions
  public void staff_cannot_set_locked() throws Exception {
    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_returns_404_when_course_not_found() throws Exception {
    when(courseRepository.findById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "99")
                .param("assessmentId", "101")
                .param("locked", "true"))
        .andExpect(status().isNotFound());
    verify(plAssessmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_returns_404_when_assessment_not_found() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(plAssessmentRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "999")
                .param("locked", "true"))
        .andExpect(status().isNotFound());
    verify(plAssessmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_returns_400_when_assessment_belongs_to_a_different_course()
      throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    PlAssessment otherCoursesAssessment =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(999L)
            .plInstanceId(888L)
            .name("hw01")
            .locked(true)
            .build();
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(otherCoursesAssessment));

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "false"))
        .andExpect(status().isBadRequest());
    verify(plAssessmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_returns_400_when_only_pl_instance_id_differs() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    // Same plRepoId as the course, but a different plInstanceId: the first half of the OR
    // check (plRepoId mismatch) is false, so this exercises the second half.
    PlAssessment otherInstancesAssessment =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(888L)
            .name("hw01")
            .locked(true)
            .build();
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(otherInstancesAssessment));

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "false"))
        .andExpect(status().isBadRequest());
    verify(plAssessmentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_can_unlock_an_assessment() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    PlAssessment assessment =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw01")
            .plAssessmentTitle("Homework 1")
            .plAssessmentSetAbbreviation("HW")
            .plAssessmentNumber("1")
            .plAssessmentSetColor("blue1")
            .locked(true)
            .build();
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(assessment));
    when(plAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "false"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                    {
                      "id": "101", "name": "Homework 1", "locked": false,
                      "pl_assessment_set_abbreviation": "HW", "pl_assessment_number": "1",
                      "pl_assessment_set_color": "blue1"
                    }
                    """,
                    true));
  }

  @Test
  @WithInstructorCoursePermissions
  public void setLocked_can_lock_an_assessment() throws Exception {
    Course course = Course.builder().id(1L).plRepoId(10L).plInstanceId(20L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    PlAssessment assessment =
        PlAssessment.builder()
            .id(101L)
            .plRepoId(10L)
            .plInstanceId(20L)
            .name("hw01")
            .plAssessmentTitle("Homework 1")
            .locked(false)
            .build();
    when(plAssessmentRepository.findById(101L)).thenReturn(Optional.of(assessment));
    when(plAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    mockMvc
        .perform(
            put("/api/assessments/lock")
                .with(csrf())
                .param("courseId", "1")
                .param("assessmentId", "101")
                .param("locked", "true"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                    {
                      "id": "101", "name": "Homework 1", "locked": true,
                      "pl_assessment_set_abbreviation": null, "pl_assessment_number": null,
                      "pl_assessment_set_color": null
                    }
                    """,
                    true));
  }
}
