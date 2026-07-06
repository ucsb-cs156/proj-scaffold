package edu.ucsb.cs.scaffold.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = ConceptsController.class)
public class ConceptsControllerTests extends ControllerTestCase {

  @MockitoBean private ConceptRepository conceptRepository;

  @MockitoBean private PracticeProblemRepository practiceProblemRepository;

  private List<Concept> buildSampleConcepts() {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();

    Concept recursion =
        Concept.builder()
            .id(1L)
            .course(course)
            .conceptId("recursion")
            .label("Recursion")
            .description("d1")
            .example("e1")
            .color("#fe9a71")
            .x(800)
            .y(300)
            .parent(null)
            .build();

    Concept baseCase =
        Concept.builder()
            .id(2L)
            .course(course)
            .conceptId("recursion:Base case")
            .label("Base case")
            .description("d2")
            .example("e2")
            .parent(recursion)
            .build();

    Concept stateChange =
        Concept.builder()
            .id(3L)
            .course(course)
            .conceptId("recursion:State change")
            .label("State change")
            .description("d3")
            .parent(recursion)
            .build();

    Concept loops =
        Concept.builder()
            .id(4L)
            .course(course)
            .conceptId("loops")
            .label("Loops")
            .description("d4")
            .example("e4")
            .color("#fe9a71")
            .x(490)
            .y(300)
            .parent(null)
            .build();

    return List.of(recursion, baseCase, stateChange, loops);
  }

  private List<PracticeProblem> buildSamplePracticeProblems(List<Concept> concepts) {
    Concept recursion = concepts.get(0);
    Concept baseCase = concepts.get(1);

    PracticeProblem pp1 =
        PracticeProblem.builder().id(10L).concept(recursion).url("url-recursion-1").build();
    PracticeProblem pp2 =
        PracticeProblem.builder().id(11L).concept(recursion).url("url-recursion-2").build();
    PracticeProblem pp3 =
        PracticeProblem.builder().id(12L).concept(baseCase).url("url-basecase").build();

    return List.of(pp1, pp2, pp3);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_concept_content() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);
    when(practiceProblemRepository.findByCourseId(42L))
        .thenReturn(buildSamplePracticeProblems(concepts));

    String expectedJson =
        """
        {
          "recursion": { "description": "d1", "example": "e1", "practiceUrl": "url-recursion-1" },
          "recursion:Base case": { "description": "d2", "example": "e2", "practiceUrl": "url-basecase" },
          "recursion:State change": { "description": "d3", "example": null, "practiceUrl": null },
          "loops": { "description": "d4", "example": "e4", "practiceUrl": null }
        }
        """;

    mockMvc
        .perform(get("/api/concepts/content").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_concept_graph() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    String expectedJson =
        """
        [
          {
            "name": "recursion",
            "label": "Recursion",
            "color": "#fe9a71",
            "subconcepts": ["Base case", "State change"]
          },
          {
            "name": "loops",
            "label": "Loops",
            "color": "#fe9a71",
            "subconcepts": []
          }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/graph").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_concept_positions() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    String expectedJson =
        """
        {
          "recursion": { "x": 800, "y": 300 },
          "loops": { "x": 490, "y": 300 }
        }
        """;

    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void endpoints_return_empty_results_for_a_course_with_no_concepts() throws Exception {
    when(conceptRepository.findByCourseId(99L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(99L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/concepts/content").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));

    mockMvc
        .perform(get("/api/concepts/graph").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));
  }

  @Test
  public void anonymous_user_cannot_get_concept_content() throws Exception {
    mockMvc
        .perform(get("/api/concepts/content").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void anonymous_user_cannot_get_concept_graph() throws Exception {
    mockMvc
        .perform(get("/api/concepts/graph").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void anonymous_user_cannot_get_concept_positions() throws Exception {
    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }
}
