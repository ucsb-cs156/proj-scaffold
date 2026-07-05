package edu.ucsb.cs.scaffold.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.model.Assessment;
import edu.ucsb.cs.scaffold.model.Question;
import edu.ucsb.cs.scaffold.repository.AssessmentRepository;
import edu.ucsb.cs.scaffold.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
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

  @MockitoBean AssessmentRepository assessmentRepository;

  @MockitoBean QuestionRepository questionRepository;

  @Test
  public void any_user_can_get_assessments() throws Exception {

    Assessment a1 = new Assessment();
    a1.setId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    a1.setName("Assessment 1");
    a1.setPlAssessmentId("pl-assessment-1");

    // mock the repository to return a list of assessments
    when(assessmentRepository.findAllByOrderByNameAsc())
        .thenReturn(
            new ArrayList<>(List.of(a1))); // return a list with one assessment for simplicity

    String expectedJson =
        """
        [
          {
            "id": "123e4567-e89b-12d3-a456-426614174000",
            "name": "Assessment 1",
            "plAssessmentId": "pl-assessment-1"
          }
        ]
        """;

    mockMvc
        .perform(get("/api/assessments"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson));
  }

  @Test
  public void any_user_can_get_questions_for_assessment() throws Exception {

    Question q1 = new Question();
    q1.setId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    q1.setPlQuestionUuid("123e4567-e89b-12d3-a456-426614174002");
    q1.setTitle("Question 1");
    q1.setAssessmentId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174003"));

    String expectedJson =
        """
        [
          {
            "id": "123e4567-e89b-12d3-a456-426614174001",
            "plQuestionUuid": "123e4567-e89b-12d3-a456-426614174002",
            "title": "Question 1",
            "assessmentId": "123e4567-e89b-12d3-a456-426614174003"
          }
        ]
        """;

    // mock the repository to return a list of questions
    when(questionRepository.findByAssessmentIdOrderByTitleAsc(any()))
        .thenReturn(new ArrayList<>(List.of(q1))); // return a list with one question for simplicity
    mockMvc
        .perform(get("/api/assessments/123e4567-e89b-12d3-a456-426614174000/questions"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson));
  }
}
