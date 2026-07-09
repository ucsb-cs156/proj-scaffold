package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ConceptYamlServiceTests {

  private final ConceptRepository conceptRepository = mock(ConceptRepository.class);
  private final ConceptEdgeRepository conceptEdgeRepository = mock(ConceptEdgeRepository.class);
  private final PracticeProblemRepository practiceProblemRepository =
      mock(PracticeProblemRepository.class);
  private final CourseRepository courseRepository = mock(CourseRepository.class);
  private final UserStateV2Repository userStateV2Repository = mock(UserStateV2Repository.class);

  private final ConceptYamlService service =
      new ConceptYamlService(
          conceptRepository,
          conceptEdgeRepository,
          practiceProblemRepository,
          courseRepository,
          userStateV2Repository,
          new MarkdownService(),
          new ConceptGraphService());

  private final Course course = Course.builder().id(42L).courseName("CMPSC 8").build();

  private Concept topLevel(Long id, String label) {
    return Concept.builder().id(id).course(course).label(label).build();
  }

  private Concept subconcept(Long id, String label, Concept parent, Integer sortOrder) {
    return Concept.builder()
        .id(id)
        .course(course)
        .label(label)
        .parent(parent)
        .sortOrder(sortOrder)
        .build();
  }

  private InputStream yamlStream(String yaml) {
    return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
  }

  /** Makes each repository's save behave like JPA: concepts get ids 100, 101, 102, ... */
  private void stubSavesToAssignIds() {
    AtomicLong nextConceptId = new AtomicLong(100);
    when(conceptRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Concept concept = invocation.getArgument(0);
              concept.setId(nextConceptId.getAndIncrement());
              return concept;
            });
    when(conceptEdgeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(practiceProblemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Map<String, Object> successReport(
      int conceptsCreated,
      int subconceptsCreated,
      int edgesCreated,
      int practiceProblemsCreated,
      int userStatesCleared) {
    return Map.of(
        "success",
        true,
        "errors",
        List.of(),
        "conceptsCreated",
        conceptsCreated,
        "subconceptsCreated",
        subconceptsCreated,
        "edgesCreated",
        edgesCreated,
        "practiceProblemsCreated",
        practiceProblemsCreated,
        "userStatesCleared",
        userStatesCleared);
  }

  private Map<String, Object> failureReport(List<String> errors) {
    return Map.of(
        "success",
        false,
        "errors",
        errors,
        "conceptsCreated",
        0,
        "subconceptsCreated",
        0,
        "edgesCreated",
        0,
        "practiceProblemsCreated",
        0,
        "userStatesCleared",
        0);
  }

  /** Runs an import expected to fail validation and asserts nothing was written or deleted. */
  private Map<String, Object> replaceExpectingFailure(String yaml) throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    Map<String, Object> report = service.replaceFromYAML(42L, yamlStream(yaml));
    assertEquals(false, report.get("success"));
    verify(conceptRepository, never()).save(any());
    verify(conceptEdgeRepository, never()).save(any());
    verify(practiceProblemRepository, never()).save(any());
    verify(conceptRepository, never()).deleteAll(any());
    verify(conceptEdgeRepository, never()).deleteAll(any());
    verify(practiceProblemRepository, never()).deleteAll(any());
    verify(userStateV2Repository, never()).deleteAll(any());
    return report;
  }

  // ---------------------------------------------------------------- createYAML

  @Test
  public void createYAML_throws_when_course_not_found() {
    when(courseRepository.findById(7L)).thenReturn(Optional.empty());

    EntityNotFoundException thrown =
        assertThrows(EntityNotFoundException.class, () -> service.createYAML(7L));
    assertEquals("Course with id 7 not found", thrown.getMessage());
  }

  @Test
  public void createYAML_exports_an_empty_course() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of());
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(42L)).thenReturn(List.of());

    String expected =
        """
        # Concept graph for course 42 (CMPSC 8)
        # See docs/yaml-format.md for the format.
        format: 1
        concepts: []
        edges: []
        """;
    assertEquals(expected, service.createYAML(42L));
  }

  @Test
  public void createYAML_exports_concepts_subconcepts_practice_problems_and_edges()
      throws Exception {
    Concept recursion = topLevel(10L, "Recursion");
    recursion.setColor("#fe9a71");
    recursion.setLevel(2);
    recursion.setX(800);
    recursion.setY(300);
    recursion.setDescription("A function that *calls itself*.");
    recursion.setExample("```java\nint f(int n) { return n == 0 ? 1 : n * f(n - 1); }\n```\n");

    Concept loops = topLevel(20L, "Loops");
    loops.setColor("#c99ffe");
    loops.setLevel(1);
    loops.setX(-175);
    loops.setY(0);

    Concept io = topLevel(30L, "IO");

    // Display order is sortOrder then id, with null sortOrder last: recursive case (0),
    // base case (1), then the null-sortOrder row.
    Concept baseCase = subconcept(12L, "Base case", recursion, 1);
    Concept recursiveCase = subconcept(11L, "Recursive case", recursion, 0);
    recursiveCase.setDescription("The part that recurses.");
    Concept legacyOrder = subconcept(13L, "Call stack", recursion, null);

    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    // Deliberately shuffled: export must sort top-level concepts by id and subconcepts by
    // display order.
    when(conceptRepository.findByCourseId(42L))
        .thenReturn(List.of(baseCase, loops, legacyOrder, recursion, io, recursiveCase));

    // Deliberately out of id order: each concept's URLs must come out in id order.
    when(practiceProblemRepository.findByCourseId(42L))
        .thenReturn(
            List.of(
                PracticeProblem.builder()
                    .id(101L)
                    .course(course)
                    .concept(recursion)
                    .url("https://example.org/r2")
                    .build(),
                PracticeProblem.builder()
                    .id(100L)
                    .course(course)
                    .concept(recursion)
                    .url("https://example.org/r1")
                    .build(),
                PracticeProblem.builder()
                    .id(102L)
                    .course(course)
                    .concept(recursiveCase)
                    .url("https://example.org/rc1")
                    .build()));

    // Deliberately out of order: edges must be sorted by (from, to) external ids.
    when(conceptEdgeRepository.findByCourseId(42L))
        .thenReturn(
            List.of(
                ConceptEdge.builder().id(2L).course(course).source(loops).target(io).build(),
                ConceptEdge.builder()
                    .id(1L)
                    .course(course)
                    .source(recursion)
                    .target(loops)
                    .build()));

    String expected =
        """
        # Concept graph for course 42 (CMPSC 8)
        # See docs/yaml-format.md for the format.
        format: 1
        concepts:
          - id: 1
            label: Recursion
            color: "#fe9a71"
            level: 2
            x: 800
            "y": 300
            description: A function that *calls itself*.
            example: |
              ```java
              int f(int n) { return n == 0 ? 1 : n * f(n - 1); }
              ```
            practiceProblems:
              - https://example.org/r1
              - https://example.org/r2
            subconcepts:
              - label: Recursive case
                description: The part that recurses.
                practiceProblems:
                  - https://example.org/rc1
              - label: Base case
              - label: Call stack
          - id: 2
            label: Loops
            color: "#c99ffe"
            level: 1
            x: -175
            "y": 0
          - id: 3
            label: IO
        edges:
          - from: 1
            to: 2
          - from: 2
            to: 3
        """;
    assertEquals(expected, service.createYAML(42L));
  }

  // ------------------------------------------------------------ replaceFromYAML

  @Test
  public void replaceFromYAML_throws_when_course_not_found() {
    when(courseRepository.findById(7L)).thenReturn(Optional.empty());

    EntityNotFoundException thrown =
        assertThrows(
            EntityNotFoundException.class, () -> service.replaceFromYAML(7L, yamlStream("")));
    assertEquals("Course with id 7 not found", thrown.getMessage());
  }

  @Test
  public void replaceFromYAML_replaces_existing_content_and_clears_user_state() throws Exception {
    Concept oldTop = topLevel(1L, "Old concept");
    Concept oldSub = subconcept(2L, "Old subconcept", oldTop, 0);
    List<ConceptEdge> oldEdges =
        List.of(ConceptEdge.builder().id(3L).course(course).source(oldTop).target(oldTop).build());
    List<PracticeProblem> oldProblems =
        List.of(
            PracticeProblem.builder()
                .id(4L)
                .course(course)
                .concept(oldTop)
                .url("https://old.example.org")
                .build());
    List<UserStateV2> oldStates = List.of(new UserStateV2(), new UserStateV2());

    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of(oldTop, oldSub));
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(oldEdges);
    when(practiceProblemRepository.findByCourseId(42L)).thenReturn(oldProblems);
    when(userStateV2Repository.findByCourseId(42L)).thenReturn(oldStates);
    stubSavesToAssignIds();

    // The exact document createYAML produces, plus an unquoted y (accepted on upload even
    // though downloads quote it).
    String yaml =
        """
        # Concept graph for course 42 (CMPSC 8)
        # See docs/yaml-format.md for the format.
        format: 1
        concepts:
          - id: 1
            label: Recursion
            color: "#fe9a71"
            level: 2
            x: 800
            "y": 300
            description: A function that *calls itself*.
            example: |
              ```java
              int f(int n) { return n == 0 ? 1 : n * f(n - 1); }
              ```
            practiceProblems:
              - https://example.org/r1
              - https://example.org/r2
            subconcepts:
              - label: Recursive case
                description: The part that recurses.
                practiceProblems:
                  - https://example.org/rc1
              - label: Base case
              - label: Call stack
          - id: 2
            label: Loops
            color: "#c99ffe"
            level: 1
            x: -175
            y: 0
          - id: 3
            label: IO
        edges:
          - from: 1
            to: 2
          - from: 2
            to: 3
        """;

    Map<String, Object> report = service.replaceFromYAML(42L, yamlStream(yaml));
    assertEquals(successReport(3, 3, 2, 3, 2), report);

    // Old content is deleted, subconcepts before their parents.
    verify(practiceProblemRepository).deleteAll(oldProblems);
    verify(conceptEdgeRepository).deleteAll(oldEdges);
    verify(conceptRepository).deleteAll(List.of(oldSub));
    verify(conceptRepository).deleteAll(List.of(oldTop));
    verify(userStateV2Repository).deleteAll(oldStates);

    ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository, org.mockito.Mockito.times(6)).save(conceptCaptor.capture());
    List<Concept> saved = conceptCaptor.getAllValues();

    Concept recursion = saved.get(0);
    assertEquals("Recursion", recursion.getLabel());
    assertEquals("#fe9a71", recursion.getColor());
    assertEquals(2, recursion.getLevel());
    assertEquals(800, recursion.getX());
    assertEquals(300, recursion.getY());
    assertEquals("A function that *calls itself*.", recursion.getDescription());
    assertEquals(
        "```java\nint f(int n) { return n == 0 ? 1 : n * f(n - 1); }\n```", recursion.getExample());
    assertSame(course, recursion.getCourse());
    assertNull(recursion.getParent());
    assertNull(recursion.getSortOrder());

    Concept recursiveCase = saved.get(1);
    assertEquals("Recursive case", recursiveCase.getLabel());
    assertEquals("The part that recurses.", recursiveCase.getDescription());
    assertSame(recursion, recursiveCase.getParent());
    assertEquals(0, recursiveCase.getSortOrder());
    assertEquals("Base case", saved.get(2).getLabel());
    assertEquals(1, saved.get(2).getSortOrder());
    assertEquals("Call stack", saved.get(3).getLabel());
    assertEquals(2, saved.get(3).getSortOrder());

    Concept loops = saved.get(4);
    assertEquals("Loops", loops.getLabel());
    assertEquals(-175, loops.getX());
    assertEquals(0, loops.getY());
    Concept io = saved.get(5);
    assertEquals("IO", io.getLabel());

    ArgumentCaptor<PracticeProblem> problemCaptor = ArgumentCaptor.forClass(PracticeProblem.class);
    verify(practiceProblemRepository, org.mockito.Mockito.times(3)).save(problemCaptor.capture());
    List<PracticeProblem> savedProblems = problemCaptor.getAllValues();
    assertEquals("https://example.org/r1", savedProblems.get(0).getUrl());
    assertSame(recursion, savedProblems.get(0).getConcept());
    assertSame(course, savedProblems.get(0).getCourse());
    assertEquals("https://example.org/r2", savedProblems.get(1).getUrl());
    assertEquals("https://example.org/rc1", savedProblems.get(2).getUrl());
    assertSame(recursiveCase, savedProblems.get(2).getConcept());

    ArgumentCaptor<ConceptEdge> edgeCaptor = ArgumentCaptor.forClass(ConceptEdge.class);
    verify(conceptEdgeRepository, org.mockito.Mockito.times(2)).save(edgeCaptor.capture());
    List<ConceptEdge> savedEdges = edgeCaptor.getAllValues();
    assertSame(recursion, savedEdges.get(0).getSource());
    assertSame(loops, savedEdges.get(0).getTarget());
    assertSame(course, savedEdges.get(0).getCourse());
    assertSame(loops, savedEdges.get(1).getSource());
    assertSame(io, savedEdges.get(1).getTarget());
  }

  @Test
  public void replaceFromYAML_applies_defaults_and_sanitizes_markdown() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of());
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(42L)).thenReturn(List.of());
    when(userStateV2Repository.findByCourseId(42L)).thenReturn(List.of());
    stubSavesToAssignIds();

    String yaml =
        """
        format: 1
        concepts:
          - id: 7
            label: Solo <script>alert('x')</script>
            description: Fine <script>alert('x')</script>text
        """;

    Map<String, Object> report = service.replaceFromYAML(42L, yamlStream(yaml));
    assertEquals(successReport(1, 0, 0, 0, 0), report);

    ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(conceptCaptor.capture());
    Concept saved = conceptCaptor.getValue();
    // The <script> tags are stripped by MarkdownService.clean; commonmark treats the
    // element's inner text as ordinary text, so that part survives.
    assertEquals("Solo alert('x')", saved.getLabel());
    assertEquals("Fine alert('x')text", saved.getDescription());
    assertNull(saved.getExample());
    assertEquals("#c99ffe", saved.getColor());
    assertEquals(1, saved.getLevel());
    assertEquals(0, saved.getX());
    assertEquals(0, saved.getY());
  }

  @Test
  public void replaceFromYAML_with_defaulted_level_uses_that_levels_color() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of());
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(42L)).thenReturn(List.of());
    when(userStateV2Repository.findByCourseId(42L)).thenReturn(List.of());
    stubSavesToAssignIds();

    String yaml =
        """
        format: 1
        concepts:
          - id: 7
            label: Solo
            level: 3
        """;

    assertEquals(successReport(1, 0, 0, 0, 0), service.replaceFromYAML(42L, yamlStream(yaml)));

    ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
    verify(conceptRepository).save(conceptCaptor.capture());
    assertEquals("#93ebff", conceptCaptor.getValue().getColor());
    assertEquals(3, conceptCaptor.getValue().getLevel());
  }

  @Test
  public void replaceFromYAML_can_clear_a_course_with_an_empty_concept_list() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    when(conceptRepository.findByCourseId(42L)).thenReturn(List.of());
    when(conceptEdgeRepository.findByCourseId(42L)).thenReturn(List.of());
    when(practiceProblemRepository.findByCourseId(42L)).thenReturn(List.of());
    when(userStateV2Repository.findByCourseId(42L)).thenReturn(List.of());

    String yaml = """
        format: 1
        concepts: []
        """;

    assertEquals(successReport(0, 0, 0, 0, 0), service.replaceFromYAML(42L, yamlStream(yaml)));
    verify(conceptRepository, never()).save(any());
  }

  @Test
  public void replaceFromYAML_reports_missing_and_unsupported_format() throws Exception {
    Map<String, Object> report =
        replaceExpectingFailure("""
            concepts: []
            """);
    assertEquals(failureReport(List.of("format is required (expected: format: 1)")), report);

    report =
        replaceExpectingFailure("""
            format: 2
            concepts: []
            """);
    assertEquals(failureReport(List.of("unsupported format 2 (expected: format: 1)")), report);
  }

  @Test
  public void replaceFromYAML_reports_missing_concepts_list() throws Exception {
    Map<String, Object> report = replaceExpectingFailure("""
            format: 1
            """);
    assertEquals(
        failureReport(List.of("concepts is required (may be an empty list: concepts: [])")),
        report);
  }

  @Test
  public void replaceFromYAML_reports_unparseable_yaml() throws Exception {
    Map<String, Object> report = replaceExpectingFailure("format: [unclosed");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) report.get("errors");
    assertEquals(1, errors.size());
    assertTrue(
        errors.get(0).startsWith("could not parse YAML: "),
        "unexpected error message: " + errors.get(0));
  }

  @Test
  public void replaceFromYAML_reports_unknown_keys_as_probable_typos() throws Exception {
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              - id: 1
                labl: Oops
            """);
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) report.get("errors");
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).startsWith("could not parse YAML: "));
    assertTrue(errors.get(0).contains("labl"), "unexpected error message: " + errors.get(0));
  }

  @Test
  public void replaceFromYAML_reports_unreadable_stream() throws Exception {
    when(courseRepository.findById(42L)).thenReturn(Optional.of(course));
    InputStream broken =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("boom");
          }
        };

    Map<String, Object> report = service.replaceFromYAML(42L, broken);
    assertEquals(failureReport(List.of("could not read file: boom")), report);
    verify(conceptRepository, never()).save(any());
  }

  @Test
  public void replaceFromYAML_reports_concept_id_and_label_problems() throws Exception {
    String longLabel = "a".repeat(40);
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              -
              - label: No id
              - id: 3
                label: Fine
              - id: 3
                label: Duplicate id
              - id: 4
              - id: 5
                label: %s
              - id: 6
                label: ""
            """
                .formatted(longLabel));
    assertEquals(
        failureReport(
            List.of(
                "concepts[0] is empty",
                "concepts[1]: id is required",
                "concepts[3]: duplicate id 3",
                "concepts[4]: label may not be empty",
                "concepts[5]: label renders to 40 characters; the maximum is 32",
                "concepts[6]: label may not be empty")),
        report);
  }

  @Test
  public void replaceFromYAML_reports_subconcept_problems() throws Exception {
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              - id: 1
                label: Parent
                subconcepts:
                  -
                  - label: Twin
                  - label: Twin
                  - description: no label here
            """);
    assertEquals(
        failureReport(
            List.of(
                "concepts[0].subconcepts[0] is empty",
                "concepts[0].subconcepts[2]: duplicate subconcept label Twin",
                "concepts[0].subconcepts[3]: label may not be empty")),
        report);
  }

  @Test
  public void replaceFromYAML_reports_practice_problem_url_problems() throws Exception {
    String longUrl = "https://x.example/" + "a".repeat(500);
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              - id: 1
                label: Parent
                practiceProblems:
                  -
                  - "  "
                  - https://example.org/p1
                  - https://example.org/p1
                  - %s
                subconcepts:
                  - label: Child
                    practiceProblems:
                      - ""
            """
                .formatted(longUrl));
    assertEquals(
        failureReport(
            List.of(
                "concepts[0].practiceProblems[0]: url may not be empty",
                "concepts[0].practiceProblems[1]: url may not be empty",
                "concepts[0].practiceProblems[3]: duplicate practice problem url"
                    + " https://example.org/p1",
                "concepts[0].practiceProblems[4]: url is 518 characters long; the maximum is 512",
                "concepts[0].subconcepts[0].practiceProblems[0]: url may not be empty")),
        report);
  }

  @Test
  public void replaceFromYAML_reports_edge_problems() throws Exception {
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              - id: 1
                label: A
              - id: 2
                label: B
              - id: 3
                label: C
            edges:
              -
              - from: 1
              - to: 2
              - from: 9
                to: 1
              - from: 1
                to: 9
              - from: 9
                to: 8
              - from: 1
                to: 1
              - from: 1
                to: 2
              - from: 1
                to: 2
            """);
    assertEquals(
        failureReport(
            List.of(
                "edges[0] is empty",
                "edges[1]: from and to are required",
                "edges[2]: from and to are required",
                "edges[3]: no concept with id 9",
                "edges[4]: no concept with id 9",
                "edges[5]: no concept with id 9",
                "edges[5]: no concept with id 8",
                "edges[6]: an edge cannot connect a concept to itself",
                "edges[8]: duplicate edge from 1 to 2")),
        report);
  }

  @Test
  public void replaceFromYAML_rejects_edges_that_close_a_cycle() throws Exception {
    Map<String, Object> report =
        replaceExpectingFailure(
            """
            format: 1
            concepts:
              - id: 1
                label: A
              - id: 2
                label: B
              - id: 3
                label: C
            edges:
              - from: 1
                to: 2
              - from: 2
                to: 3
              - from: 3
                to: 1
            """);
    assertEquals(failureReport(List.of("edges[2]: edge from 3 to 1 would create a cycle")), report);
  }
}
