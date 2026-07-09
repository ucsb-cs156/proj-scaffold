package edu.ucsb.cs.scaffold.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.ConceptGraphYamlDTO;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Exports and imports a course's entire concept-graph content (concepts, subconcepts, prerequisite
 * edges, and practice problems) as a human-editable YAML document. See docs/yaml-format.md for the
 * format specification.
 */
@Service
@RequiredArgsConstructor
public class ConceptYamlService {

  // Emits multi-line Markdown as | block scalars and leaves simple strings unquoted, so the
  // export is pleasant to hand-edit. Reading is strict (FAIL_ON_UNKNOWN_PROPERTIES is on by
  // default outside Spring): an unknown key in an uploaded file is almost always a typo.
  private static final YAMLMapper YAML_MAPPER =
      YAMLMapper.builder()
          .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
          .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
          .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
          .build();

  // The order subconcepts are listed under their parent: the author's ordering, with the same
  // id tiebreaker as the /api/concepts/graph endpoint.
  private static final Comparator<Concept> SUBCONCEPT_DISPLAY_ORDER =
      Comparator.comparing(Concept::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Concept::getId);

  private final ConceptRepository conceptRepository;
  private final ConceptEdgeRepository conceptEdgeRepository;
  private final PracticeProblemRepository practiceProblemRepository;
  private final CourseRepository courseRepository;

  /**
   * The complete concept-graph content of the course as a YAML document (see docs/yaml-format.md).
   * Top-level concepts are numbered with consecutive external ids 1..n in database-id order; edges
   * refer to those external ids.
   */
  public String createYAML(long courseId) throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    List<Concept> concepts = conceptRepository.findByCourseId(courseId);

    List<Concept> topLevelConcepts =
        concepts.stream()
            .filter(concept -> !concept.isSubconcept())
            .sorted(Comparator.comparing(Concept::getId))
            .toList();

    Map<Long, List<Concept>> subconceptsByParentId =
        concepts.stream()
            .filter(Concept::isSubconcept)
            .sorted(SUBCONCEPT_DISPLAY_ORDER)
            .collect(
                Collectors.groupingBy(
                    concept -> concept.getParent().getId(), HashMap::new, Collectors.toList()));

    Map<Long, List<String>> urlsByConceptId = urlsByConceptId(courseId);

    Map<Long, Long> externalIdByInternalId = new HashMap<>();
    for (int i = 0; i < topLevelConcepts.size(); i++) {
      externalIdByInternalId.put(topLevelConcepts.get(i).getId(), (long) (i + 1));
    }

    List<ConceptGraphYamlDTO.ConceptNodeDTO> conceptNodes = new ArrayList<>();
    for (Concept concept : topLevelConcepts) {
      List<ConceptGraphYamlDTO.SubconceptNodeDTO> subconceptNodes =
          subconceptsByParentId.getOrDefault(concept.getId(), List.of()).stream()
              .map(
                  sub ->
                      ConceptGraphYamlDTO.SubconceptNodeDTO.builder()
                          .label(sub.getLabel())
                          .description(sub.getDescription())
                          .example(sub.getExample())
                          .practiceProblems(urlsByConceptId.get(sub.getId()))
                          .build())
              .toList();
      conceptNodes.add(
          ConceptGraphYamlDTO.ConceptNodeDTO.builder()
              .id(externalIdByInternalId.get(concept.getId()))
              .label(concept.getLabel())
              .color(concept.getColor())
              .level(concept.getLevel())
              .x(concept.getX())
              .y(concept.getY())
              .description(concept.getDescription())
              .example(concept.getExample())
              .practiceProblems(urlsByConceptId.get(concept.getId()))
              .subconcepts(subconceptNodes.isEmpty() ? null : subconceptNodes)
              .build());
    }

    List<ConceptGraphYamlDTO.EdgeNodeDTO> edgeNodes =
        conceptEdgeRepository.findByCourseId(courseId).stream()
            .map(
                edge ->
                    ConceptGraphYamlDTO.EdgeNodeDTO.builder()
                        .from(externalIdByInternalId.get(edge.getSource().getId()))
                        .to(externalIdByInternalId.get(edge.getTarget().getId()))
                        .build())
            .sorted(
                Comparator.comparing(ConceptGraphYamlDTO.EdgeNodeDTO::getFrom)
                    .thenComparing(ConceptGraphYamlDTO.EdgeNodeDTO::getTo))
            .toList();

    ConceptGraphYamlDTO dto =
        ConceptGraphYamlDTO.builder().format(1).concepts(conceptNodes).edges(edgeNodes).build();

    String header =
        "# Concept graph for course %d (%s)%n# See docs/yaml-format.md for the format.%n"
            .formatted(course.getId(), course.getCourseName());
    try {
      return header + YAML_MAPPER.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to serialize concept graph as YAML", e);
    }
  }

  /**
   * Every practice problem URL in the course, grouped by concept id, each concept's URLs in
   * database-id order (the order they were added). Concepts with no practice problems are absent,
   * so lookups feed straight into the NON_EMPTY-serialized DTO fields.
   */
  private Map<Long, List<String>> urlsByConceptId(long courseId) {
    Map<Long, List<String>> result = new HashMap<>();
    practiceProblemRepository.findByCourseId(courseId).stream()
        .sorted(Comparator.comparing(PracticeProblem::getId))
        .forEach(
            problem ->
                result
                    .computeIfAbsent(problem.getConcept().getId(), k -> new ArrayList<>())
                    .add(problem.getUrl()));
    return result;
  }
}
