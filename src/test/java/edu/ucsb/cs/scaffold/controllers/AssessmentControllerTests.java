package edu.ucsb.cs.scaffold.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.controller.AssessmentController;
import edu.ucsb.cs.scaffold.repository.AssessmentRepository;
import edu.ucsb.cs.scaffold.repository.QuestionRepository;
import java.util.ArrayList;
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

    // mock the repository to return a list of assessments
    when(assessmentRepository.findAllByOrderByNameAsc())
        .thenReturn(new ArrayList<>()); // return an empty list for simplicity

    mockMvc
        .perform(get("/api/assessments"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  public void any_user_can_get_questions_for_assessment() throws Exception {

    // mock the repository to return a list of questions
    when(questionRepository.findByAssessmentIdOrderByTitleAsc(any()))
        .thenReturn(new ArrayList<>()); // return an empty list for simplicity
    mockMvc
        .perform(get("/api/assessments/123e4567-e89b-12d3-a456-426614174000/questions"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }
}
