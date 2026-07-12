package edu.ucsb.cs.scaffold.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.model.QuestionConcept;
import edu.ucsb.cs.scaffold.repository.QuestionConceptRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = LegacyQuestionController.class)
@Import(edu.ucsb.cs.scaffold.testconfig.TestConfig.class)
@TestPropertySource(
    properties = "app.admin.emails=djensen@ucsb.edu,phtcon@ucsb.edu,acdamstedt@ucsb.edu")
public class LegacyQuestionControllerTests extends ControllerTestCase {

  @MockitoBean QuestionConceptRepository questionConceptRepository;

  @Test
  public void any_user_can_get_concepts_for_question() throws Exception {

    QuestionConcept qc = new QuestionConcept();
    qc.setId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
    qc.setQuestionId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    qc.setConceptId("concept1");
    qc.setSubconceptLabel("subconceptLabel1");
    when(questionConceptRepository.findByQuestionId(any()))
        .thenReturn(new ArrayList<>(List.of(qc)));

    String expectedJson =
        """
        [
          {
            "id": "123e4567-e89b-12d3-a456-426614174001",
            "questionId": "123e4567-e89b-12d3-a456-426614174000",
            "conceptId": "concept1",
            "subconceptLabel": "subconceptLabel1"
          }
        ]
        """;

    mockMvc
        .perform(get("/api/legacy/questions/123e4567-e89b-12d3-a456-426614174000/concepts"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(content().json(expectedJson));
  }
}
