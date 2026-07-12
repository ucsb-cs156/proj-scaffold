package edu.ucsb.cs.scaffold.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import edu.ucsb.cs.scaffold.services.ConceptGraphService;
import edu.ucsb.cs.scaffold.services.MarkdownService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ConceptsController.class)
@Import({MarkdownService.class, ConceptGraphService.class})
public class ConceptsControllerTests extends ControllerTestCase {

  @MockitoBean private ConceptRepository conceptRepository;

  @MockitoBean private PracticeProblemRepository practiceProblemRepository;

  @MockitoBean private ConceptEdgeRepository conceptEdgeRepository;

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private UserStateV2Repository userStateV2Repository;

  private List<Concept> buildSampleConcepts() {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();

    Concept recursion =
        Concept.builder()
            .id(1L)
            .course(course)
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
            .label("Base case")
            .description("d2")
            .example("e2")
            .parent(recursion)
            .build();

    Concept stateChange =
        Concept.builder()
            .id(3L)
            .course(course)
            .label("State change")
            .description("d3")
            .parent(recursion)
            .build();

    Concept loops =
        Concept.builder()
            .id(4L)
            .course(course)
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

  private List<ConceptEdge> buildSampleEdges(List<Concept> concepts) {
    Course course = concepts.get(0).getCourse();
    Concept recursion = concepts.get(0);
    Concept loops = concepts.get(3);

    ConceptEdge edge =
        ConceptEdge.builder().id(20L).course(course).source(loops).target(recursion).build();

    return List.of(edge);
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
          "1": {
            "id": 1, "parentId": null,
            "descriptionHtml": "<p>d1</p>", "exampleHtml": "<p>e1</p>",
            "practiceUrl": "url-recursion-1"
          },
          "2": {
            "id": 2, "parentId": 1,
            "descriptionHtml": "<p>d2</p>", "exampleHtml": "<p>e2</p>",
            "practiceUrl": "url-basecase"
          },
          "3": {
            "id": 3, "parentId": 1,
            "descriptionHtml": "<p>d3</p>", "exampleHtml": null,
            "practiceUrl": null
          },
          "4": {
            "id": 4, "parentId": null,
            "descriptionHtml": "<p>d4</p>", "exampleHtml": "<p>e4</p>",
            "practiceUrl": null
          }
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
            "id": 1,
            "labelHtml": "Recursion",
            "color": "#fe9a71",
            "subconcepts": [
              { "id": 2, "parentId": 1, "labelHtml": "Base case" },
              { "id": 3, "parentId": 1, "labelHtml": "State change" }
            ]
          },
          {
            "id": 4,
            "labelHtml": "Loops",
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
  public void logged_in_user_can_get_course_concepts_sorted_by_level_then_x() throws Exception {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();
    Concept levelTwo =
        Concept.builder()
            .id(1L)
            .course(course)
            .label("Recursion")
            .description("Calls itself")
            .example("factorial")
            .level(2)
            .x(800)
            .y(300)
            .build();
    Concept subconcept =
        Concept.builder().id(2L).course(course).label("Base case").parent(levelTwo).build();
    Concept earlyLevelOne =
        Concept.builder()
            .id(3L)
            .course(course)
            .label("Variables")
            .description("Stores values")
            .example("int x = 3;")
            .level(1)
            .x(125)
            .y(250)
            .build();
    Concept laterLevelOne =
        Concept.builder()
            .id(4L)
            .course(course)
            .label("Loops")
            .description("Repeats work")
            .example("for (...)")
            .level(1)
            .x(490)
            .y(300)
            .build();
    when(conceptRepository.findByCourseId(42L))
        .thenReturn(List.of(levelTwo, subconcept, laterLevelOne, earlyLevelOne));

    String expectedJson =
        """
        [
          { "id": 3, "label": "Variables", "description": "Stores values", "example": "int x = 3;", "level": 1, "x": 125, "y": 250 },
          { "id": 4, "label": "Loops", "description": "Repeats work", "example": "for (...)", "level": 1, "x": 490, "y": 300 },
          { "id": 1, "label": "Recursion", "description": "Calls itself", "example": "factorial", "level": 2, "x": 800, "y": 300 }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/course").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_concept_positions() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    String expectedJson =
        """
        {
          "1": { "x": 800, "y": 300 },
          "4": { "x": 490, "y": 300 }
        }
        """;

    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_concept_edges() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(buildSampleEdges(concepts));

    String expectedJson =
        """
        [
          { "id": 20, "sourceId": 4, "targetId": 1, "color": null }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/edges").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_sees_an_edges_cycle_color_when_set() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    List<ConceptEdge> edges = buildSampleEdges(concepts);
    edges.get(0).setColor("#FF0000");
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(edges);

    String expectedJson =
        """
        [
          { "id": 20, "sourceId": 4, "targetId": 1, "color": "#FF0000" }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/edges").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void endpoints_return_empty_results_for_a_course_with_no_concepts() throws Exception {
    when(conceptRepository.findByCourseId(99L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(99L)).thenReturn(List.of());
    when(conceptEdgeRepository.findByCourseId(99L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/concepts/content").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));

    mockMvc
        .perform(get("/api/concepts/graph").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    mockMvc
        .perform(get("/api/concepts/course").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("{}"));

    mockMvc
        .perform(get("/api/concepts/edges").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
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
  public void anonymous_user_cannot_get_course_concepts() throws Exception {
    mockMvc
        .perform(get("/api/concepts/course").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void anonymous_user_cannot_get_concept_edges() throws Exception {
    mockMvc
        .perform(get("/api/concepts/edges").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void anonymous_user_cannot_get_concept_positions() throws Exception {
    mockMvc
        .perform(get("/api/concepts/positions").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  // ---------- POST /api/concept ----------

  private static final String YAML = "application/yaml";

  @Test
  public void anonymous_user_cannot_post_concept() throws Exception {
    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content("courseId: 42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_post_concept() throws Exception {
    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content("courseId: 42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_post_a_top_level_concept() throws Exception {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        label: "**Recursion**"
        description: |-
          before

          <script>alert('x')</script>

          after
        example: |-
          ```python
          if a < b:
              print('x')
          ```
        x: 800
        y: 300
        """;

    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("**Recursion**"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals(course, saved.getCourse());
    assertEquals("**Recursion**", saved.getLabel());
    assertEquals("before\n\nafter", saved.getDescription());
    assertEquals("```python\nif a < b:\n    print('x')\n```", saved.getExample());
    assertEquals(ConceptsController.DEFAULT_TOP_LEVEL_COLOR, saved.getColor());
    assertEquals(1, saved.getLevel());
    assertEquals(800, saved.getX());
    assertEquals(300, saved.getY());
    assertNull(saved.getParent());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_post_a_top_level_concept_with_a_json_body() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/concept")
                .with(csrf())
                .contentType("application/json")
                .content("{\"courseId\": 42, \"label\": \"Loops\", \"x\": 1, \"y\": 2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Loops"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_post_a_top_level_concept_without_optional_markdown() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        label: Loops
        x: 1
        y: 2
        """;

    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk());

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals(ConceptsController.DEFAULT_TOP_LEVEL_COLOR, saved.getColor());
    assertNull(saved.getDescription());
    assertNull(saved.getExample());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_ignores_unknown_fields_such_as_color() throws Exception {
    // color is not part of CreateConceptDTO (it is never client-settable) and
    // parentConceptId belongs to the subconcept endpoint; both are silently ignored,
    // matching Spring Boot's default JSON leniency.
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        name: loops
        label: Loops
        color: "#ff0000"
        parentConceptId: 99
        x: 1
        y: 2
        """;

    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk());

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    assertEquals(ConceptsController.DEFAULT_TOP_LEVEL_COLOR, captor.getValue().getColor());
    assertNull(captor.getValue().getParent());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(7L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 7\nlabel: Loops"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Course with id 7 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_requires_a_courseId() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/concept").with(csrf()).contentType(YAML).content("label: Loops"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("courseId is required", json.get("message"));
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_requires_a_label() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(post("/api/concept").with(csrf()).contentType(YAML).content("courseId: 42"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("label may not be empty", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_treats_html_looking_label_content_as_literal_text() throws Exception {
    // Labels have no legitimate use for embedded raw HTML (unlike descriptions/examples), so
    // rather than sanitizing (and potentially emptying) HTML-looking label content, it is kept
    // as literal, escaped Markdown text. This also means CS notation such as "List<Integer>"
    // survives intact instead of being silently stripped by an HTML sanitizer.
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        label: "<script>alert('x')</script>"
        x: 1
        y: 2
        """;

    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("\\<script\\>alert('x')\\</script\\>"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_accepts_a_label_whose_rendered_length_is_exactly_the_maximum()
      throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body = "courseId: 42\nlabel: \"" + "x".repeat(32) + "\"\nx: 1\ny: 2";
    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_rejects_a_label_whose_rendered_length_is_too_long() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));

    // 33 rendered characters, even though the Markdown source is longer still.
    String body = "courseId: 42\nlabel: \"**" + "x".repeat(33) + "**\"\nx: 1\ny: 2";
    MvcResult response =
        mockMvc
            .perform(post("/api/concept").with(csrf()).contentType(YAML).content(body))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("label renders to 33 characters; the maximum is 32", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_requires_x() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nlabel: Loops\ny: 2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("x and y are required for a top-level concept", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_requires_y() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));

    mockMvc
        .perform(
            post("/api/concept")
                .with(csrf())
                .contentType(YAML)
                .content("courseId: 42\nlabel: Loops\nx: 1"))
        .andExpect(status().isBadRequest());
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_concept_rejects_a_malformed_yaml_body() throws Exception {
    mockMvc
        .perform(post("/api/concept").with(csrf()).contentType(YAML).content("label: [unclosed"))
        .andExpect(status().isBadRequest());
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  // ---------- POST /api/concept/subconcept ----------

  @Test
  public void anonymous_user_cannot_post_subconcept() throws Exception {
    mockMvc
        .perform(
            post("/api/concept/subconcept").with(csrf()).contentType(YAML).content("courseId: 42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_post_subconcept() throws Exception {
    mockMvc
        .perform(
            post("/api/concept/subconcept").with(csrf()).contentType(YAML).content("courseId: 42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_post_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentIdAndLabel(1L, "Base case")).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        parentConceptId: 1
        label: Base case
        description: |-
          The condition that stops the recursion.
        """;

    mockMvc
        .perform(post("/api/concept/subconcept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Base case"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals(parent, saved.getParent());
    assertEquals("Base case", saved.getLabel());
    assertEquals("The condition that stops the recursion.", saved.getDescription());
    assertNull(saved.getX());
    assertNull(saved.getY());
    assertNull(saved.getColor());
    assertNull(saved.getLevel());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(7L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 7\nparentConceptId: 1\nlabel: Base case"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Course with id 7 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_returns_404_when_parent_does_not_exist() throws Exception {
    Course course = Course.builder().id(42L).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(99L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nparentConceptId: 99\nlabel: Base case"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 99 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_requires_a_courseId() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("parentConceptId: 1\nlabel: Base case"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("courseId is required", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_requires_a_parentConceptId() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nlabel: Base case"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("parentConceptId is required", json.get("message"));
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_rejects_a_parent_from_a_different_course() throws Exception {
    Course course = Course.builder().id(42L).build();
    Course otherCourse = Course.builder().id(43L).build();
    Concept parent = Concept.builder().id(1L).course(otherCourse).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nparentConceptId: 1\nlabel: Base case"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("parentConceptId 1 belongs to a different course", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_rejects_a_parent_that_is_itself_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept grandparent = Concept.builder().id(1L).course(course).build();
    Concept parent = Concept.builder().id(2L).course(course).parent(grandparent).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(parent));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nparentConceptId: 2\nlabel: Base case"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "parentConceptId 2 is a subconcept; concepts can only be nested one level deep",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_rejects_a_duplicate_label_under_the_same_parent() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    Concept existing = Concept.builder().id(2L).course(course).parent(parent).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentIdAndLabel(1L, "Base case"))
        .thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType(YAML)
                    .content("courseId: 42\nparentConceptId: 1\nlabel: Base case"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("concept 1 already has a subconcept with label Base case", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_ignores_unknown_fields_such_as_name_and_position() throws Exception {
    // name, x, and y are not part of CreateSubconceptDTO; a body that includes them is
    // accepted and they are ignored (subconcepts never get a name or position).
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentIdAndLabel(1L, "Base case")).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String body =
        """
        courseId: 42
        parentConceptId: 1
        label: Base case
        name: base-case
        x: 5
        y: 6
        """;

    mockMvc
        .perform(post("/api/concept/subconcept").with(csrf()).contentType(YAML).content(body))
        .andExpect(status().isOk());

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    assertNull(captor.getValue().getX());
    assertNull(captor.getValue().getY());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_accepts_a_label_containing_markdown() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    String label = "Using `if __name__ == \"__main__\"`";
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentIdAndLabel(1L, label)).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> body =
        Map.of(
            "courseId", 42,
            "parentConceptId", 1,
            "label", label);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concept/subconcept")
                    .with(csrf())
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(label, json.get("label"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    assertEquals(label, captor.getValue().getLabel());
  }

  // ---------- PUT /api/concept/put ----------

  @Test
  public void anonymous_user_cannot_put_concept() throws Exception {
    mockMvc
        .perform(
            put("/api/concept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_put_concept() throws Exception {
    mockMvc
        .perform(
            put("/api/concept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_update_a_top_level_concept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept =
        Concept.builder()
            .id(1L)
            .course(course)
            .label("Old label")
            .description("Old description")
            .example("Old example")
            .build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content(
                    """
                    {"label":"Recursion","description":"Functions that call themselves.","example":"factorial(n - 1)"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Recursion"))
        .andExpect(jsonPath("$.description").value("Functions that call themselves."))
        .andExpect(jsonPath("$.example").value("factorial(n - 1)"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals("Recursion", saved.getLabel());
    assertEquals("Functions that call themselves.", saved.getDescription());
    assertEquals("factorial(n - 1)", saved.getExample());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_concept_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"Recursion\"}"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_concept_rejects_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(2L).course(course).build();
    Concept subconcept = Concept.builder().id(1L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(subconcept));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"Base case\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1 is a subconcept; use PUT /api/concept/subconcept/put to update it",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_concept_rejects_an_empty_label() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("label may not be empty", json.get("message"));
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  // ---------- PUT /api/concept/subconcept/put ----------

  @Test
  public void anonymous_user_cannot_put_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concept/subconcept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_put_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concept/subconcept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_update_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(2L).course(course).build();
    Concept concept =
        Concept.builder()
            .id(1L)
            .course(course)
            .parent(parent)
            .label("Old label")
            .description("Old description")
            .example("Old example")
            .build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(conceptRepository.findByParentIdAndLabel(2L, "Base case")).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concept/subconcept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content(
                    """
                    {"label":"Base case","description":"The condition that stops recursion.","example":"n == 0"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Base case"))
        .andExpect(jsonPath("$.description").value("The condition that stops recursion."))
        .andExpect(jsonPath("$.example").value("n == 0"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals("Base case", saved.getLabel());
    assertEquals("The condition that stops recursion.", saved.getDescription());
    assertEquals("n == 0", saved.getExample());
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_subconcept_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/subconcept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"Base case\"}"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_subconcept_rejects_a_top_level_concept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/subconcept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"Recursion\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1 is not a subconcept; use PUT /api/concept/put to update it",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_subconcept_rejects_an_empty_label() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(2L).course(course).build();
    Concept concept = Concept.builder().id(1L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/subconcept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("label may not be empty", json.get("message"));
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_subconcept_rejects_a_duplicate_label_under_the_same_parent() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(2L).course(course).build();
    Concept concept = Concept.builder().id(1L).course(course).parent(parent).build();
    Concept sibling = Concept.builder().id(3L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(conceptRepository.findByParentIdAndLabel(2L, "Base case"))
        .thenReturn(Optional.of(sibling));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concept/subconcept/put")
                    .with(csrf())
                    .contentType("application/json")
                    .param("conceptId", "1")
                    .content("{\"label\":\"Base case\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("concept 2 already has a subconcept with label Base case", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void put_subconcept_allows_reusing_its_own_existing_label() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(2L).course(course).build();
    Concept concept =
        Concept.builder().id(1L).course(course).parent(parent).label("Base case").build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(conceptRepository.findByParentIdAndLabel(2L, "Base case"))
        .thenReturn(Optional.of(concept));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concept/subconcept/put")
                .with(csrf())
                .contentType("application/json")
                .param("conceptId", "1")
                .content(
                    """
                    {"label":"Base case","description":"Updated description","example":"return 1;"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Base case"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.example").value("return 1;"));
  }

  // ---------- POST /api/concepts/practiceproblems/post ----------

  @Test
  public void anonymous_user_cannot_post_practice_problem() throws Exception {
    mockMvc
        .perform(
            post("/api/concepts/practiceproblems/post")
                .with(csrf())
                .param("conceptId", "1")
                .param("url", "https://example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_post_practice_problem() throws Exception {
    mockMvc
        .perform(
            post("/api/concepts/practiceproblems/post")
                .with(csrf())
                .param("conceptId", "1")
                .param("url", "https://example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_add_a_practice_problem_url() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(practiceProblemRepository.findByCourseIdAndConceptIdAndUrl(
            42L, 1L, "https://example.com/p1"))
        .thenReturn(Optional.empty());
    when(practiceProblemRepository.save(any(PracticeProblem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/concepts/practiceproblems/post")
                .with(csrf())
                .param("conceptId", "1")
                .param("url", "  https://example.com/p1  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://example.com/p1"));

    ArgumentCaptor<PracticeProblem> captor = ArgumentCaptor.forClass(PracticeProblem.class);
    verify(practiceProblemRepository).save(captor.capture());
    PracticeProblem saved = captor.getValue();
    assertEquals(course, saved.getCourse());
    assertEquals(concept, saved.getConcept());
    assertEquals("https://example.com/p1", saved.getUrl());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_practice_problem_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/practiceproblems/post")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("url", "https://example.com"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_practice_problem_rejects_a_blank_url() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/practiceproblems/post")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("url", "   "))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("url may not be empty", json.get("message"));
    verify(practiceProblemRepository, never()).save(any(PracticeProblem.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_practice_problem_rejects_a_duplicate_url() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(1L).course(course).build();
    PracticeProblem existing =
        PracticeProblem.builder()
            .id(10L)
            .course(course)
            .concept(concept)
            .url("https://example.com/p1")
            .build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(practiceProblemRepository.findByCourseIdAndConceptIdAndUrl(
            42L, 1L, "https://example.com/p1"))
        .thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/practiceproblems/post")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("url", "https://example.com/p1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1 already has practice problem url https://example.com/p1", json.get("message"));
  }

  // ---------- PUT /api/concepts/designate ----------

  @Test
  public void anonymous_user_cannot_designate_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/designate")
                .with(csrf())
                .param("conceptId", "1")
                .param("parentConceptId", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_designate_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/designate")
                .with(csrf())
                .param("conceptId", "1")
                .param("parentConceptId", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_designate_a_concept_as_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 =
        Concept.builder()
            .id(1L)
            .course(course)
            .label("Recursion")
            .color("#111111")
            .x(800)
            .y(300)
            .build();
    Concept c2 = Concept.builder().id(2L).course(course).color("#222222").build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());
    when(conceptEdgeRepository.findBySourceIdOrTargetId(1L, 1L)).thenReturn(List.of());
    when(conceptRepository.findByParentIdAndLabel(2L, "Recursion")).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concepts/designate")
                .with(csrf())
                .param("conceptId", "1")
                .param("parentConceptId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Recursion"))
        .andExpect(jsonPath("$.color").value("#222222"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertEquals(c2, saved.getParent());
    assertEquals("#222222", saved.getColor());
    assertEquals(800, saved.getX());
    assertEquals(300, saved.getY());
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_returns_404_when_parent_does_not_exist() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 2 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_making_a_concept_its_own_parent() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("a concept cannot be its own parent", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_a_parent_from_a_different_course() throws Exception {
    Course course = Course.builder().id(42L).build();
    Course otherCourse = Course.builder().id(43L).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    Concept c2 = Concept.builder().id(2L).course(otherCourse).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("parentConceptId 2 belongs to a different course", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_a_parent_that_is_itself_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept top = Concept.builder().id(3L).course(course).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    Concept c2 = Concept.builder().id(2L).course(course).parent(top).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "parentConceptId 2 is a subconcept; concepts can only be nested one level deep",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_a_concept_that_has_subconcepts() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    Concept c2 = Concept.builder().id(2L).course(course).build();
    Concept child = Concept.builder().id(3L).course(course).parent(c1).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(child));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1 has subconcepts of its own, so it cannot become a subconcept",
        json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_a_concept_that_has_prerequisite_edges() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).build();
    Concept c2 = Concept.builder().id(2L).course(course).build();
    ConceptEdge edge = ConceptEdge.builder().id(20L).course(course).source(c1).target(c2).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());
    when(conceptEdgeRepository.findBySourceIdOrTargetId(1L, 1L)).thenReturn(List.of(edge));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 1 has prerequisite edges; delete them before designating it as a subconcept",
        json.get("message"));
    verify(conceptRepository, never()).save(any(Concept.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_rejects_a_duplicate_label_under_the_new_parent() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).label("Recursion").build();
    Concept c2 = Concept.builder().id(2L).course(course).build();
    Concept existing = Concept.builder().id(3L).course(course).parent(c2).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(c2));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());
    when(conceptEdgeRepository.findBySourceIdOrTargetId(1L, 1L)).thenReturn(List.of());
    when(conceptRepository.findByParentIdAndLabel(2L, "Recursion"))
        .thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/designate")
                    .with(csrf())
                    .param("conceptId", "1")
                    .param("parentConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("concept 2 already has a subconcept with label Recursion", json.get("message"));
  }

  // ---------- PUT /api/concepts/splitoff ----------

  @Test
  public void anonymous_user_cannot_split_off_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/splitoff")
                .with(csrf())
                .param("conceptId", "1")
                .param("x", "1")
                .param("y", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_split_off_subconcept() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/splitoff")
                .with(csrf())
                .param("conceptId", "1")
                .param("x", "1")
                .param("y", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_split_off_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).color("#111111").build();
    Concept sub = Concept.builder().id(2L).course(course).parent(parent).label("Base case").build();
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(sub));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concepts/splitoff")
                .with(csrf())
                .param("conceptId", "2")
                .param("x", "500")
                .param("y", "600"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Base case"));

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    Concept saved = captor.getValue();
    assertNull(saved.getParent());
    assertEquals(500, saved.getX());
    assertEquals(600, saved.getY());
    // No color of its own, so it inherits its former parent's color.
    assertEquals("#111111", saved.getColor());
  }

  @Test
  @WithInstructorCoursePermissions
  public void split_off_keeps_the_subconcepts_own_color_when_it_has_one() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).color("#111111").build();
    Concept sub =
        Concept.builder()
            .id(2L)
            .course(course)
            .parent(parent)
            .label("Base case")
            .color("#333333")
            .build();
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(sub));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concepts/splitoff")
                .with(csrf())
                .param("conceptId", "2")
                .param("x", "500")
                .param("y", "600"))
        .andExpect(status().isOk());

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    assertEquals("#333333", captor.getValue().getColor());
  }

  @Test
  @WithInstructorCoursePermissions
  public void split_off_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(2L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/splitoff")
                    .with(csrf())
                    .param("conceptId", "2")
                    .param("x", "1")
                    .param("y", "2"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 2 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void split_off_rejects_a_concept_that_is_already_top_level() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept concept = Concept.builder().id(2L).course(course).build();
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(concept));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/splitoff")
                    .with(csrf())
                    .param("conceptId", "2")
                    .param("x", "1")
                    .param("y", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("concept 2 is already a top-level concept", json.get("message"));
  }

  // ---------- POST /api/concepts/edges/post ----------

  @Test
  public void anonymous_user_cannot_post_concept_edge() throws Exception {
    mockMvc
        .perform(
            post("/api/concepts/edges/post")
                .with(csrf())
                .param("sourceConceptId", "1")
                .param("targetConceptId", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_post_concept_edge() throws Exception {
    mockMvc
        .perform(
            post("/api/concepts/edges/post")
                .with(csrf())
                .param("sourceConceptId", "1")
                .param("targetConceptId", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_post_a_concept_edge() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    Concept target = Concept.builder().id(2L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(target));
    when(conceptEdgeRepository.findBySourceIdAndTargetId(1L, 2L)).thenReturn(Optional.empty());
    when(conceptEdgeRepository.save(any(ConceptEdge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/concepts/edges/post")
                .with(csrf())
                .param("sourceConceptId", "1")
                .param("targetConceptId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source.id").value(1))
        .andExpect(jsonPath("$.target.id").value(2));

    ArgumentCaptor<ConceptEdge> captor = ArgumentCaptor.forClass(ConceptEdge.class);
    verify(conceptEdgeRepository).save(captor.capture());
    ConceptEdge saved = captor.getValue();
    assertEquals(course, saved.getCourse());
    assertEquals(source, saved.getSource());
    assertEquals(target, saved.getTarget());
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_an_edge_that_would_create_a_cycle() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept loops = Concept.builder().id(1L).course(course).build();
    Concept recursion = Concept.builder().id(2L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(loops));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(recursion));
    when(conceptEdgeRepository.findBySourceIdAndTargetId(1L, 2L)).thenReturn(Optional.empty());
    // recursion -> loops already exists, so loops -> recursion would close a cycle.
    ConceptEdge existing =
        ConceptEdge.builder().id(20L).course(course).source(recursion).target(loops).build();
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("edge from concept 1 to concept 2 would create a cycle", json.get("message"));
    verify(conceptEdgeRepository, never()).save(any(ConceptEdge.class));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_returns_404_when_source_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_returns_404_when_target_does_not_exist() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 2 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_a_self_edge() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("an edge cannot connect a concept to itself", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_concepts_from_different_courses() throws Exception {
    Course course = Course.builder().id(42L).build();
    Course otherCourse = Course.builder().id(43L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    Concept target = Concept.builder().id(2L).course(otherCourse).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(target));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("both concepts must belong to the same course", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_a_subconcept_source() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept top = Concept.builder().id(3L).course(course).build();
    Concept source = Concept.builder().id(1L).course(course).parent(top).build();
    Concept target = Concept.builder().id(2L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(target));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("edges may only connect top-level concepts", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_a_subconcept_target() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept top = Concept.builder().id(3L).course(course).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    Concept target = Concept.builder().id(2L).course(course).parent(top).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(target));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("edges may only connect top-level concepts", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_edge_rejects_a_duplicate_edge() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    Concept target = Concept.builder().id(2L).course(course).build();
    ConceptEdge existing =
        ConceptEdge.builder().id(20L).course(course).source(source).target(target).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(source));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(target));
    when(conceptEdgeRepository.findBySourceIdAndTargetId(1L, 2L)).thenReturn(Optional.of(existing));

    MvcResult response =
        mockMvc
            .perform(
                post("/api/concepts/edges/post")
                    .with(csrf())
                    .param("sourceConceptId", "1")
                    .param("targetConceptId", "2"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("edge from concept 1 to concept 2 already exists", json.get("message"));
    verify(conceptEdgeRepository, never()).save(any(ConceptEdge.class));
  }

  // ---------- DELETE /api/concept/delete ----------

  @Test
  public void anonymous_user_cannot_delete_concept() throws Exception {
    mockMvc
        .perform(delete("/api/concept/delete").with(csrf()).param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_delete_concept() throws Exception {
    mockMvc
        .perform(delete("/api/concept/delete").with(csrf()).param("conceptId", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_delete_a_top_level_concept_and_its_subconcepts() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    List<PracticeProblem> practiceProblems = buildSamplePracticeProblems(concepts);
    Course course = concepts.get(0).getCourse();
    Concept concept = concepts.get(0);
    Concept baseCase = concepts.get(1);
    Concept stateChange = concepts.get(2);
    Concept loops = concepts.get(3);
    ConceptEdge edge1 =
        ConceptEdge.builder().id(20L).course(course).source(loops).target(concept).build();
    ConceptEdge edge2 =
        ConceptEdge.builder().id(21L).course(course).source(baseCase).target(loops).build();
    ConceptEdge edge3 =
        ConceptEdge.builder().id(22L).course(course).source(loops).target(stateChange).build();
    List<PracticeProblem> recursionProblems = practiceProblems.subList(0, 2);
    List<PracticeProblem> baseCaseProblems = practiceProblems.subList(2, 3);
    List<PracticeProblem> stateChangeProblems = List.of();

    when(conceptRepository.findById(1L)).thenReturn(Optional.of(concept));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(baseCase, stateChange));
    when(practiceProblemRepository.findByCourseIdAndConceptId(42L, 1L))
        .thenReturn(recursionProblems);
    when(practiceProblemRepository.findByCourseIdAndConceptId(42L, 2L))
        .thenReturn(baseCaseProblems);
    when(practiceProblemRepository.findByCourseIdAndConceptId(42L, 3L))
        .thenReturn(stateChangeProblems);
    when(conceptEdgeRepository.findBySourceIdOrTargetId(1L, 1L)).thenReturn(List.of(edge1));
    when(conceptEdgeRepository.findBySourceIdOrTargetId(2L, 2L)).thenReturn(List.of(edge2));
    when(conceptEdgeRepository.findBySourceIdOrTargetId(3L, 3L)).thenReturn(List.of(edge3, edge1));

    mockMvc
        .perform(delete("/api/concept/delete").with(csrf()).param("conceptId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Concept with id 1 deleted"));

    verify(practiceProblemRepository).deleteAll(recursionProblems);
    verify(practiceProblemRepository).deleteAll(baseCaseProblems);
    verify(practiceProblemRepository).deleteAll(stateChangeProblems);

    ArgumentCaptor<Iterable<ConceptEdge>> deletedEdgesCaptor =
        ArgumentCaptor.forClass(Iterable.class);
    verify(conceptEdgeRepository).deleteAll(deletedEdgesCaptor.capture());
    List<Long> deletedEdgeIds = new ArrayList<>();
    for (ConceptEdge edge : deletedEdgesCaptor.getValue()) {
      deletedEdgeIds.add(edge.getId());
    }
    assertEquals(List.of(20L, 21L, 22L), deletedEdgeIds);

    verify(conceptRepository).deleteAll(List.of(baseCase, stateChange));
    verify(conceptRepository).delete(concept);
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_delete_a_subconcept_directly() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    Concept subconcept = concepts.get(1);
    List<PracticeProblem> problems = buildSamplePracticeProblems(concepts).subList(2, 3);
    ConceptEdge edge =
        ConceptEdge.builder()
            .id(20L)
            .course(subconcept.getCourse())
            .source(subconcept)
            .target(concepts.get(3))
            .build();

    when(conceptRepository.findById(2L)).thenReturn(Optional.of(subconcept));
    when(practiceProblemRepository.findByCourseIdAndConceptId(42L, 2L)).thenReturn(problems);
    when(conceptEdgeRepository.findBySourceIdOrTargetId(2L, 2L)).thenReturn(List.of(edge));

    mockMvc
        .perform(delete("/api/concept/delete").with(csrf()).param("conceptId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Concept with id 2 deleted"));

    verify(practiceProblemRepository).deleteAll(problems);
    ArgumentCaptor<Iterable<ConceptEdge>> deletedEdgesCaptor =
        ArgumentCaptor.forClass(Iterable.class);
    verify(conceptEdgeRepository).deleteAll(deletedEdgesCaptor.capture());
    List<Long> deletedEdgeIds = new ArrayList<>();
    for (ConceptEdge deletedEdge : deletedEdgesCaptor.getValue()) {
      deletedEdgeIds.add(deletedEdge.getId());
    }
    assertEquals(List.of(20L), deletedEdgeIds);
    verify(conceptRepository).delete(subconcept);
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_concept_returns_404_when_concept_does_not_exist() throws Exception {
    when(conceptRepository.findById(1L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/concept/delete").with(csrf()).param("conceptId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 1 not found", json.get("message"));
  }

  // ---------- DELETE /api/concepts/edges/delete ----------

  @Test
  public void anonymous_user_cannot_delete_concept_edge() throws Exception {
    mockMvc
        .perform(delete("/api/concepts/edges/delete").with(csrf()).param("id", "20"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_delete_concept_edge() throws Exception {
    mockMvc
        .perform(delete("/api/concepts/edges/delete").with(csrf()).param("id", "20"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_delete_a_concept_edge() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept source = Concept.builder().id(1L).course(course).build();
    Concept target = Concept.builder().id(2L).course(course).build();
    ConceptEdge edge =
        ConceptEdge.builder().id(20L).course(course).source(source).target(target).build();
    when(conceptEdgeRepository.findById(20L)).thenReturn(Optional.of(edge));

    MvcResult response =
        mockMvc
            .perform(delete("/api/concepts/edges/delete").with(csrf()).param("id", "20"))
            .andExpect(status().isOk())
            .andReturn();

    verify(conceptEdgeRepository).delete(edge);
    Map<String, Object> json = responseToJson(response);
    assertEquals("ConceptEdge with id 20 deleted", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void delete_edge_returns_404_when_edge_does_not_exist() throws Exception {
    when(conceptEdgeRepository.findById(20L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/concepts/edges/delete").with(csrf()).param("id", "20"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("ConceptEdge with id 20 not found", json.get("message"));
  }

  // ---------- POST /api/course/scaffold/reset ----------

  @Test
  public void anonymous_user_cannot_reset_course_scaffold() throws Exception {
    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_reset_course_scaffold() throws Exception {
    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_course_scaffold_returns_404_when_course_does_not_exist() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Course with id 42 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_ranks_and_lays_out_a_simple_chain() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(999).y(999).build();
    Concept b = Concept.builder().id(2L).course(course).label("B").x(999).y(999).build();
    ConceptEdge edge = ConceptEdge.builder().id(10L).course(course).source(a).target(b).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, b));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of(edge));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.report.cycleEdges").isEmpty())
        .andExpect(jsonPath("$.report.removedEdges").isEmpty())
        .andExpect(jsonPath("$.report.levels[0].id").value(1))
        .andExpect(jsonPath("$.report.levels[0].label").value("A"))
        .andExpect(jsonPath("$.report.levels[0].level").value(1))
        .andExpect(jsonPath("$.report.levels[0].color").value("#c99ffe"))
        .andExpect(jsonPath("$.report.levels[0].x").value(0))
        .andExpect(jsonPath("$.report.levels[0].y").value(0))
        .andExpect(jsonPath("$.report.levels[1].label").value("B"))
        .andExpect(jsonPath("$.report.levels[1].level").value(2))
        .andExpect(jsonPath("$.report.levels[1].color").value("#feaef2"))
        .andExpect(jsonPath("$.report.levels[1].x").value(0))
        .andExpect(jsonPath("$.report.levels[1].y").value(-300))
        .andExpect(jsonPath("$.graph[0].color").value("#c99ffe"))
        .andExpect(jsonPath("$.graph[1].color").value("#feaef2"))
        .andExpect(jsonPath("$.edges[0].color").doesNotExist());

    assertEquals(1, a.getLevel());
    assertEquals(2, b.getLevel());
    assertNull(edge.getColor());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_sorts_by_the_callers_private_position_override_instead_of_the_saved_x()
      throws Exception {
    Course course = Course.builder().id(42L).build();
    // Saved x would order b before a; the caller's private drag reverses that.
    Concept a = Concept.builder().id(1L).course(course).label("A").x(500).y(0).build();
    Concept b = Concept.builder().id(2L).course(course).label("B").x(0).y(0).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, b));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());

    UserStateV2 callerState = new UserStateV2();
    callerState.setTopLevelPositions("{\"1\": {\"x\": -500, \"y\": 0}}");
    when(userStateV2Repository.findByUseridAndCourseId(1L, 42L))
        .thenReturn(Optional.of(callerState));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk());

    // With a's private x (-500) sorted before b's saved x (0), a lands on the left.
    assertEquals(-175, a.getX());
    assertEquals(175, b.getX());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_falls_back_to_saved_x_when_the_override_has_no_x_value() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(500).y(0).build();
    Concept b = Concept.builder().id(2L).course(course).label("B").x(0).y(0).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, b));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());

    // The override entry for "a" exists but has no x (only y is set), so its saved x (500)
    // is still used for sorting, keeping it to the right of b.
    UserStateV2 callerState = new UserStateV2();
    callerState.setTopLevelPositions("{\"1\": {\"y\": 50}}");
    when(userStateV2Repository.findByUseridAndCourseId(1L, 42L))
        .thenReturn(Optional.of(callerState));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk());

    assertEquals(175, a.getX());
    assertEquals(-175, b.getX());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_throws_when_the_callers_stored_positions_are_malformed() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(0).y(0).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());

    UserStateV2 callerState = new UserStateV2();
    callerState.setTopLevelPositions("not valid json");
    when(userStateV2Repository.findByUseridAndCourseId(1L, 42L))
        .thenReturn(Optional.of(callerState));

    jakarta.servlet.ServletException thrown =
        assertThrows(
            jakarta.servlet.ServletException.class,
            () ->
                mockMvc.perform(
                    post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42")));
    assertEquals(IllegalStateException.class, thrown.getCause().getClass());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_excludes_subconcepts_from_the_top_level_analysis() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(0).y(0).build();
    Concept sub = Concept.builder().id(2L).course(course).parent(a).label("Sub").build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, sub));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.report.levels", hasSize(1)))
        .andExpect(jsonPath("$.report.levels[0].label").value("A"));

    assertNull(sub.getLevel());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_clears_every_users_private_top_level_position_overrides() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(0).y(0).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());

    UserStateV2 instructorState = new UserStateV2();
    instructorState.setTopLevelPositions("{\"1\": {\"x\": 999, \"y\": 0}}");
    UserStateV2 studentState = new UserStateV2();
    studentState.setTopLevelPositions("{\"1\": {\"x\": -999, \"y\": 0}}");
    when(userStateV2Repository.findByUseridAndCourseId(1L, 42L))
        .thenReturn(Optional.of(instructorState));
    when(userStateV2Repository.findByCourseId(42L))
        .thenReturn(List.of(instructorState, studentState));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk());

    ArgumentCaptor<List<UserStateV2>> captor = ArgumentCaptor.forClass(List.class);
    verify(userStateV2Repository).saveAll(captor.capture());
    assertEquals(List.of(instructorState, studentState), captor.getValue());
    assertEquals("{}", instructorState.getTopLevelPositions());
    assertEquals("{}", studentState.getTopLevelPositions());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_colors_a_two_node_cycle_red_and_falls_both_concepts_back_to_level_one()
      throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(0).y(0).build();
    Concept b = Concept.builder().id(2L).course(course).label("B").x(0).y(0).build();
    ConceptEdge ab = ConceptEdge.builder().id(10L).course(course).source(a).target(b).build();
    ConceptEdge ba = ConceptEdge.builder().id(11L).course(course).source(b).target(a).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, b));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of(ab, ba));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.report.cycleEdges", hasSize(2)))
        .andExpect(jsonPath("$.report.cycleEdges[0].edgeId").value(10))
        .andExpect(jsonPath("$.report.cycleEdges[0].sourceId").value(1))
        .andExpect(jsonPath("$.report.cycleEdges[0].targetId").value(2))
        .andExpect(jsonPath("$.report.cycleEdges[1].edgeId").value(11))
        .andExpect(jsonPath("$.report.cycleEdges[1].sourceId").value(2))
        .andExpect(jsonPath("$.report.cycleEdges[1].targetId").value(1))
        .andExpect(jsonPath("$.report.removedEdges").isEmpty());

    assertEquals(1, a.getLevel());
    assertEquals(1, b.getLevel());
    assertEquals(ConceptGraphService.CYCLE_EDGE_COLOR, ab.getColor());
    assertEquals(ConceptGraphService.CYCLE_EDGE_COLOR, ba.getColor());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reset_deletes_an_edge_made_redundant_by_a_longer_path() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept a = Concept.builder().id(1L).course(course).label("A").x(0).y(0).build();
    Concept b = Concept.builder().id(2L).course(course).label("B").x(0).y(0).build();
    Concept c = Concept.builder().id(3L).course(course).label("C").x(0).y(0).build();
    ConceptEdge shortcut = ConceptEdge.builder().id(10L).course(course).source(a).target(c).build();
    ConceptEdge ab = ConceptEdge.builder().id(11L).course(course).source(a).target(b).build();
    ConceptEdge bc = ConceptEdge.builder().id(12L).course(course).source(b).target(c).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(a, b, c));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of(shortcut, ab, bc));

    mockMvc
        .perform(post("/api/course/scaffold/reset").with(csrf()).param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.report.removedEdges", hasSize(1)))
        .andExpect(jsonPath("$.report.removedEdges[0].edgeId").value(10))
        .andExpect(jsonPath("$.report.removedEdges[0].sourceId").value(1))
        .andExpect(jsonPath("$.report.removedEdges[0].targetId").value(3))
        .andExpect(jsonPath("$.report.levels[2].label").value("C"))
        .andExpect(jsonPath("$.report.levels[2].level").value(3));

    ArgumentCaptor<List<ConceptEdge>> captor = ArgumentCaptor.forClass(List.class);
    verify(conceptEdgeRepository).deleteAll(captor.capture());
    assertEquals(List.of(shortcut), captor.getValue());
  }

  // ---------- subconcept ordering (sortOrder) ----------

  @Test
  @WithMockUser(roles = {"USER"})
  public void graph_orders_subconcepts_by_sortOrder_then_id_with_nulls_last() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent =
        Concept.builder().id(1L).course(course).label("Recursion").color("#fe9a71").build();
    // Deliberately inverted: ids ascend 2,3,5,6 but the author's order is 3,6,2 with the
    // never-positioned 5 (null sortOrder) last. 3 and 6 tie on sortOrder 0 so id breaks the
    // tie; 6 is listed before 3 here so a stable sort without the id tiebreaker gets it wrong.
    Concept subA =
        Concept.builder().id(2L).course(course).label("A").parent(parent).sortOrder(2).build();
    Concept subB =
        Concept.builder().id(3L).course(course).label("B").parent(parent).sortOrder(0).build();
    Concept subC = Concept.builder().id(5L).course(course).label("C").parent(parent).build();
    Concept subD =
        Concept.builder().id(6L).course(course).label("D").parent(parent).sortOrder(0).build();
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(parent, subD, subC, subB, subA));

    String expectedJson =
        """
        [
          {
            "id": 1,
            "labelHtml": "Recursion",
            "color": "#fe9a71",
            "subconcepts": [
              { "id": 3, "parentId": 1, "labelHtml": "B" },
              { "id": 6, "parentId": 1, "labelHtml": "D" },
              { "id": 2, "parentId": 1, "labelHtml": "A" },
              { "id": 5, "parentId": 1, "labelHtml": "C" }
            ]
          }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/graph").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_assigns_sortOrder_0_when_the_parent_has_no_subconcepts()
      throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/concept/subconcept")
                .with(csrf())
                .contentType(YAML)
                .content("courseId: 42\nparentConceptId: 1\nlabel: Base case"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sortOrder").value(0));
  }

  @Test
  @WithInstructorCoursePermissions
  public void post_subconcept_appends_after_the_highest_existing_sortOrder() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    // Sibling sortOrders 5 and 0, plus a pre-backfill sibling with none: the next
    // position is max+1 = 6, ignoring the null.
    Concept sibA = Concept.builder().id(2L).course(course).parent(parent).sortOrder(5).build();
    Concept sibB = Concept.builder().id(3L).course(course).parent(parent).sortOrder(0).build();
    Concept sibC = Concept.builder().id(4L).course(course).parent(parent).build();
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(sibA, sibB, sibC));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/concept/subconcept")
                .with(csrf())
                .contentType(YAML)
                .content("courseId: 42\nparentConceptId: 1\nlabel: Base case"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sortOrder").value(6));
  }

  @Test
  @WithInstructorCoursePermissions
  public void designate_appends_the_concept_after_the_new_parents_subconcepts() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept c1 = Concept.builder().id(1L).course(course).label("Recursion").build();
    Concept parent = Concept.builder().id(2L).course(course).color("#222222").build();
    Concept existingSub =
        Concept.builder().id(3L).course(course).parent(parent).sortOrder(1).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(c1));
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());
    when(conceptRepository.findByParentId(2L)).thenReturn(List.of(existingSub));
    when(conceptEdgeRepository.findBySourceIdOrTargetId(1L, 1L)).thenReturn(List.of());
    when(conceptRepository.findByParentIdAndLabel(2L, "Recursion")).thenReturn(Optional.empty());
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concepts/designate")
                .with(csrf())
                .param("conceptId", "1")
                .param("parentConceptId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sortOrder").value(2));
  }

  @Test
  @WithInstructorCoursePermissions
  public void split_off_clears_the_subconcepts_sortOrder() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).color("#111111").build();
    Concept sub =
        Concept.builder()
            .id(2L)
            .course(course)
            .parent(parent)
            .label("Base case")
            .sortOrder(3)
            .build();
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(sub));
    when(conceptRepository.save(any(Concept.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            put("/api/concepts/splitoff")
                .with(csrf())
                .param("conceptId", "2")
                .param("x", "500")
                .param("y", "600"))
        .andExpect(status().isOk());

    ArgumentCaptor<Concept> captor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(captor.capture());
    assertNull(captor.getValue().getSortOrder());
  }

  // ---------- PUT /api/concepts/subconcepts/reorder ----------

  @Test
  public void anonymous_user_cannot_reorder_subconcepts() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/subconcepts/reorder")
                .with(csrf())
                .param("parentConceptId", "1")
                .contentType("application/json")
                .content("[2, 3]"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void user_without_course_permissions_cannot_reorder_subconcepts() throws Exception {
    mockMvc
        .perform(
            put("/api/concepts/subconcepts/reorder")
                .with(csrf())
                .param("parentConceptId", "1")
                .contentType("application/json")
                .content("[2, 3]"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void instructor_can_reorder_subconcepts() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    Concept subA =
        Concept.builder().id(2L).course(course).parent(parent).label("A").sortOrder(0).build();
    Concept subB =
        Concept.builder().id(3L).course(course).parent(parent).label("B").sortOrder(1).build();
    Concept subC =
        Concept.builder().id(5L).course(course).parent(parent).label("C").sortOrder(2).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(subA, subB, subC));

    String expectedJson =
        """
        [
          { "id": 5, "parentId": 1, "labelHtml": "C" },
          { "id": 2, "parentId": 1, "labelHtml": "A" },
          { "id": 3, "parentId": 1, "labelHtml": "B" }
        ]
        """;

    mockMvc
        .perform(
            put("/api/concepts/subconcepts/reorder")
                .with(csrf())
                .param("parentConceptId", "1")
                .contentType("application/json")
                .content("[5, 2, 3]"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));

    ArgumentCaptor<List<Concept>> captor = ArgumentCaptor.forClass(List.class);
    verify(conceptRepository).saveAll(captor.capture());
    assertEquals(
        1,
        captor.getValue().stream()
            .filter(c -> c.getId() == 2L)
            .findFirst()
            .orElseThrow()
            .getSortOrder());
    assertEquals(
        2,
        captor.getValue().stream()
            .filter(c -> c.getId() == 3L)
            .findFirst()
            .orElseThrow()
            .getSortOrder());
    assertEquals(
        0,
        captor.getValue().stream()
            .filter(c -> c.getId() == 5L)
            .findFirst()
            .orElseThrow()
            .getSortOrder());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_accepts_an_empty_list_for_a_parent_with_no_subconcepts() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of());

    mockMvc
        .perform(
            put("/api/concepts/subconcepts/reorder")
                .with(csrf())
                .param("parentConceptId", "1")
                .contentType("application/json")
                .content("[]"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]", true));
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_returns_404_when_the_parent_does_not_exist() throws Exception {
    when(conceptRepository.findById(99L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/subconcepts/reorder")
                    .with(csrf())
                    .param("parentConceptId", "99")
                    .contentType("application/json")
                    .content("[2, 3]"))
            .andExpect(status().isNotFound())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals("Concept with id 99 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_rejects_a_parent_that_is_itself_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept grandparent = Concept.builder().id(1L).course(course).build();
    Concept parent = Concept.builder().id(2L).course(course).parent(grandparent).build();
    when(conceptRepository.findById(2L)).thenReturn(Optional.of(parent));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/subconcepts/reorder")
                    .with(csrf())
                    .param("parentConceptId", "2")
                    .contentType("application/json")
                    .content("[3]"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "concept 2 is a subconcept; only top-level concepts have subconcepts to reorder",
        json.get("message"));
    verify(conceptRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_rejects_a_list_with_duplicate_ids() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    Concept subA = Concept.builder().id(2L).course(course).parent(parent).build();
    Concept subB = Concept.builder().id(3L).course(course).parent(parent).build();
    Concept subC = Concept.builder().id(5L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(subA, subB, subC));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/subconcepts/reorder")
                    .with(csrf())
                    .param("parentConceptId", "1")
                    .contentType("application/json")
                    .content("[2, 2, 3]"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "orderedSubconceptIds must contain the id of each subconcept of concept 1 exactly once",
        json.get("message"));
    verify(conceptRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_rejects_a_list_that_is_missing_a_subconcept() throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    Concept subA = Concept.builder().id(2L).course(course).parent(parent).build();
    Concept subB = Concept.builder().id(3L).course(course).parent(parent).build();
    Concept subC = Concept.builder().id(5L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(subA, subB, subC));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/subconcepts/reorder")
                    .with(csrf())
                    .param("parentConceptId", "1")
                    .contentType("application/json")
                    .content("[2, 3]"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "orderedSubconceptIds must contain the id of each subconcept of concept 1 exactly once",
        json.get("message"));
    verify(conceptRepository, never()).saveAll(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void reorder_rejects_a_list_containing_an_id_that_is_not_a_subconcept_of_the_parent()
      throws Exception {
    Course course = Course.builder().id(42L).build();
    Concept parent = Concept.builder().id(1L).course(course).build();
    Concept subA = Concept.builder().id(2L).course(course).parent(parent).build();
    Concept subB = Concept.builder().id(3L).course(course).parent(parent).build();
    Concept subC = Concept.builder().id(5L).course(course).parent(parent).build();
    when(conceptRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(conceptRepository.findByParentId(1L)).thenReturn(List.of(subA, subB, subC));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/concepts/subconcepts/reorder")
                    .with(csrf())
                    .param("parentConceptId", "1")
                    .contentType("application/json")
                    .content("[2, 3, 99]"))
            .andExpect(status().isBadRequest())
            .andReturn();
    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "orderedSubconceptIds must contain the id of each subconcept of concept 1 exactly once",
        json.get("message"));
    verify(conceptRepository, never()).saveAll(any());
  }

  // ---------- GET /api/concepts/top-level ----------

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_top_level_concepts() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    String expectedJson =
        """
        [
          { "id": 1, "label": "Recursion", "level": null, "x": 800, "y": 300 },
          { "id": 4, "label": "Loops", "level": null, "x": 490, "y": 300 }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/top-level").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void top_level_concepts_sorted_by_id() throws Exception {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();
    Concept b = Concept.builder().id(10L).course(course).label("B").level(2).x(200).y(100).build();
    Concept a = Concept.builder().id(5L).course(course).label("A").level(1).x(100).y(100).build();
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(b, a));

    mockMvc
        .perform(get("/api/concepts/top-level").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(5))
        .andExpect(jsonPath("$[1].id").value(10));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void top_level_concepts_excludes_subconcepts() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    mockMvc
        .perform(get("/api/concepts/top-level").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void top_level_concepts_empty_for_course_with_no_concepts() throws Exception {
    when(conceptRepository.findByCourseId(99L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/concepts/top-level").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void anonymous_user_cannot_get_top_level_concepts() throws Exception {
    mockMvc
        .perform(get("/api/concepts/top-level").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }

  // ---------- GET /api/concepts/subconcepts ----------

  @Test
  @WithMockUser(roles = {"USER"})
  public void logged_in_user_can_get_subconcepts() throws Exception {
    List<Concept> concepts = buildSampleConcepts();
    when(conceptRepository.findByCourseId(42L)).thenReturn(concepts);

    // concepts[0] = Recursion (top-level, x=800), concepts[1] = BaseCase (sub of Recursion),
    // concepts[2] = StateChange (sub of Recursion), concepts[3] = Loops (top-level)
    String expectedJson =
        """
        [
          {
            "id": 2,
            "label": "Base case",
            "description": "d2",
            "example": "e2",
            "parentId": 1,
            "parentLabel": "Recursion",
            "parentLevel": null,
            "parentX": 800,
            "sortOrder": null
          },
          {
            "id": 3,
            "label": "State change",
            "description": "d3",
            "example": null,
            "parentId": 1,
            "parentLabel": "Recursion",
            "parentLevel": null,
            "parentX": 800,
            "sortOrder": null
          }
        ]
        """;

    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void subconcepts_sorted_by_parent_level_then_x_then_sort_order() throws Exception {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();

    Concept parentA =
        Concept.builder().id(1L).course(course).label("A").level(1).x(200).y(100).build();
    Concept parentB =
        Concept.builder().id(2L).course(course).label("B").level(1).x(100).y(100).build();
    Concept parentC =
        Concept.builder().id(3L).course(course).label("C").level(2).x(300).y(200).build();

    Concept sub1 =
        Concept.builder().id(10L).course(course).label("Sub1").parent(parentA).sortOrder(2).build();
    Concept sub2 =
        Concept.builder().id(11L).course(course).label("Sub2").parent(parentA).sortOrder(1).build();
    Concept sub3 =
        Concept.builder().id(12L).course(course).label("Sub3").parent(parentB).sortOrder(1).build();
    Concept sub4 =
        Concept.builder().id(13L).course(course).label("Sub4").parent(parentC).sortOrder(1).build();

    when(conceptRepository.findByCourseId(42L))
        .thenReturn(List.of(parentA, parentB, parentC, sub1, sub2, sub3, sub4));

    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "42"))
        .andExpect(status().isOk())
        // level 1, x=100 (parentB) before level 1, x=200 (parentA)
        .andExpect(jsonPath("$[0].id").value(12))
        // sortOrder 1 before sortOrder 2 within same parent
        .andExpect(jsonPath("$[1].id").value(11))
        .andExpect(jsonPath("$[2].id").value(10))
        // level 2 (parentC) comes last
        .andExpect(jsonPath("$[3].id").value(13));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void subconcepts_level_takes_priority_over_x_in_sort() throws Exception {
    // Designed to kill the mutation "replace getParent().getLevel() return with 0".
    // parentLowX is at level=2 with x=50 (low x).
    // parentHighX is at level=1 with x=300 (high x).
    // Correct sort: level-1 child first (regardless of x).
    // If level is mutated to always return 0, then x decides: level-2 child (x=50) comes first.
    Course course = Course.builder().id(50L).courseName("CS Test").build();
    Concept parentLevel2LowX =
        Concept.builder().id(2L).course(course).label("Level2LowX").level(2).x(50).y(100).build();
    Concept parentLevel1HighX =
        Concept.builder().id(1L).course(course).label("Level1HighX").level(1).x(300).y(100).build();
    Concept subOfLevel2 =
        Concept.builder()
            .id(5L)
            .course(course)
            .label("SubLevel2")
            .parent(parentLevel2LowX)
            .sortOrder(1)
            .build();
    Concept subOfLevel1 =
        Concept.builder()
            .id(10L)
            .course(course)
            .label("SubLevel1")
            .parent(parentLevel1HighX)
            .sortOrder(1)
            .build();

    when(conceptRepository.findByCourseId(50L))
        .thenReturn(List.of(parentLevel2LowX, parentLevel1HighX, subOfLevel2, subOfLevel1));

    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "50"))
        .andExpect(status().isOk())
        // subOfLevel1 (parent level=1) must come before subOfLevel2 (parent level=2)
        // even though parent level=2 has lower x (50 < 300)
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[1].id").value(5));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void subconcepts_empty_for_course_with_no_concepts() throws Exception {
    when(conceptRepository.findByCourseId(99L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "99"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void subconcepts_empty_for_course_with_only_top_level_concepts() throws Exception {
    Course course = Course.builder().id(42L).courseName("CMPSC 8").build();
    Concept topLevel =
        Concept.builder().id(1L).course(course).label("Recursion").x(800).y(300).build();
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(topLevel));

    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "42"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  public void anonymous_user_cannot_get_subconcepts() throws Exception {
    mockMvc
        .perform(get("/api/concepts/subconcepts").param("courseId", "42"))
        .andExpect(status().isForbidden());
  }
}
