package edu.ucsb.cs.scaffold.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(controllers = QuestionController.class)
@Import(edu.ucsb.cs.scaffold.testconfig.TestConfig.class)
@TestPropertySource(
    properties = "app.admin.emails=djensen@ucsb.edu,phtcon@ucsb.edu,acdamstedt@ucsb.edu")
public class QuestionControllerTests extends ControllerTestCase {

  @Test
  public void getQuestionConcepts_always_returns_empty_list() throws Exception {
    // Concept highlighting for PL-backed questions is dark until PL question concept tagging
    // exists; see the comment on QuestionController.
    mockMvc
        .perform(get("/api/questions/201/concepts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json("[]"));
  }
}
