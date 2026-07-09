package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ConceptYamlServiceTests {

  private final ConceptRepository conceptRepository = mock(ConceptRepository.class);
  private final ConceptEdgeRepository conceptEdgeRepository = mock(ConceptEdgeRepository.class);
  private final PracticeProblemRepository practiceProblemRepository =
      mock(PracticeProblemRepository.class);
  private final CourseRepository courseRepository = mock(CourseRepository.class);

  private final ConceptYamlService service =
      new ConceptYamlService(
          conceptRepository, conceptEdgeRepository, practiceProblemRepository, courseRepository);

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
}
