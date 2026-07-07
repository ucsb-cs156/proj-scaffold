package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Concepts")
@RestController
@RequiredArgsConstructor
public class ConceptsController {

  private final ConceptRepository conceptRepository;
  private final PracticeProblemRepository practiceProblemRepository;
  private final ConceptEdgeRepository conceptEdgeRepository;

  @Operation(summary = "Get description/example/practiceUrl content for every concept in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/content")
  public Map<String, ConceptContentDTO> getContent(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    Map<Long, String> urlByConceptId = firstUrlByConceptId(courseId);

    Map<String, ConceptContentDTO> result = new LinkedHashMap<>();
    for (Concept concept : conceptRepository.findByCourseId(courseId)) {
      result.put(
          concept.getConceptId(),
          new ConceptContentDTO(
              concept.getDescription(), concept.getExample(), urlByConceptId.get(concept.getId())));
    }
    return result;
  }

  @Operation(summary = "Get the major concepts and their subconcepts for a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/graph")
  public List<MajorConceptDTO> getGraph(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    List<Concept> concepts = conceptRepository.findByCourseId(courseId);

    Map<Long, List<Concept>> subconceptsByParentId =
        concepts.stream()
            .filter(concept -> concept.getParent() != null)
            .sorted(Comparator.comparing(Concept::getId))
            .collect(
                Collectors.groupingBy(
                    concept -> concept.getParent().getId(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    List<MajorConceptDTO> result = new ArrayList<>();
    concepts.stream()
        .filter(concept -> concept.getParent() == null)
        .sorted(Comparator.comparing(Concept::getId))
        .forEach(
            major -> {
              List<String> subconceptLabels =
                  subconceptsByParentId.getOrDefault(major.getId(), List.of()).stream()
                      .map(Concept::getLabel)
                      .toList();
              result.add(
                  new MajorConceptDTO(
                      major.getConceptId(), major.getLabel(), major.getColor(), subconceptLabels));
            });
    return result;
  }

  @Operation(summary = "Get the graph positions of the top-level concepts in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/positions")
  public Map<String, PositionDTO> getPositions(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    Map<String, PositionDTO> result = new LinkedHashMap<>();
    for (Concept concept : conceptRepository.findByCourseId(courseId)) {
      if (concept.getParent() == null) {
        result.put(concept.getConceptId(), new PositionDTO(concept.getX(), concept.getY()));
      }
    }
    return result;
  }

  @Operation(summary = "Get the prerequisite edges between concepts in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/edges")
  public List<EdgeDTO> getEdges(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    List<EdgeDTO> result = new ArrayList<>();
    for (ConceptEdge edge : conceptEdgeRepository.findByCourseId(courseId)) {
      result.add(new EdgeDTO(edge.getSource().getConceptId(), edge.getTarget().getConceptId()));
    }
    return result;
  }

  private Map<Long, String> firstUrlByConceptId(Long courseId) {
    Map<Long, String> result = new LinkedHashMap<>();
    List<PracticeProblem> problems =
        practiceProblemRepository.findByCourseId(courseId).stream()
            .sorted(Comparator.comparing(PracticeProblem::getId))
            .toList();
    for (PracticeProblem problem : problems) {
      result.putIfAbsent(problem.getConcept().getId(), problem.getUrl());
    }
    return result;
  }

  public record ConceptContentDTO(String description, String example, String practiceUrl) {}

  public record MajorConceptDTO(
      String name, String label, String color, List<String> subconcepts) {}

  public record PositionDTO(Integer x, Integer y) {}

  public record EdgeDTO(String source, String target) {}
}
