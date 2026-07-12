package edu.ucsb.cs.scaffold.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.ConceptGraphYamlDTO;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  // Matches the practice_problems.url column width.
  public static final int MAX_URL_LENGTH = 512;

  private final ConceptRepository conceptRepository;
  private final ConceptEdgeRepository conceptEdgeRepository;
  private final PracticeProblemRepository practiceProblemRepository;
  private final CourseRepository courseRepository;
  private final UserStateV2Repository userStateV2Repository;
  private final MarkdownService markdownService;
  private final ConceptGraphService conceptGraphService;

  /**
   * The complete concept-graph content of the course as a YAML document (see docs/yaml-format.md).
   * Top-level concepts are numbered with consecutive external ids 1..n in database-id order; edges
   * refer to those external ids.
   */
  public String createYAML(long courseId) throws EntityNotFoundException, JsonProcessingException {
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
    return header + YAML_MAPPER.writeValueAsString(dto);
  }

  /**
   * Replaces the course's entire concept-graph content with the content of a YAML document in the
   * docs/yaml-format.md format, reporting the outcome as a JSON-shaped map.
   *
   * <p>The replacement is all-or-nothing: the document is fully parsed and validated first, and if
   * anything is wrong the course is left untouched and every problem found is reported under {@code
   * errors}. Only a valid document deletes the course's concepts, edges, practice problems, and
   * per-user scaffold state ({@code user_state_v2} rows, which refer to concepts by ids that no
   * longer exist after a replacement) before creating the new content.
   */
  @Transactional
  public Map<String, Object> replaceFromYAML(long courseId, InputStream yamlStream)
      throws EntityNotFoundException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    List<String> errors = new ArrayList<>();
    ConceptGraphYamlDTO dto = parseYaml(yamlStream, errors);
    if (dto != null) {
      validate(dto, errors);
    }
    if (!errors.isEmpty()) {
      return report(false, errors, 0, 0, 0, 0, 0);
    }

    int userStatesCleared = deleteCourseContentAndUserState(courseId);

    int conceptsCreated = 0;
    int subconceptsCreated = 0;
    int edgesCreated = 0;
    int practiceProblemsCreated = 0;

    Map<Long, Concept> savedByExternalId = new HashMap<>();
    for (ConceptGraphYamlDTO.ConceptNodeDTO node : dto.getConcepts()) {
      int level = node.getLevel() != null ? node.getLevel() : 1;
      Concept saved =
          conceptRepository.save(
              Concept.builder()
                  .course(course)
                  .label(markdownService.clean(node.getLabel()))
                  .description(markdownService.clean(node.getDescription()))
                  .example(markdownService.clean(node.getExample()))
                  .color(
                      node.getColor() != null
                          ? node.getColor()
                          : conceptGraphService.colorForLevel(level))
                  .level(level)
                  .x(node.getX() != null ? node.getX() : 0)
                  .y(node.getY() != null ? node.getY() : 0)
                  .build());
      savedByExternalId.put(node.getId(), saved);
      conceptsCreated++;
      practiceProblemsCreated += savePracticeProblems(course, saved, node.getPracticeProblems());

      List<ConceptGraphYamlDTO.SubconceptNodeDTO> subconcepts =
          node.getSubconcepts() != null ? node.getSubconcepts() : List.of();
      for (int i = 0; i < subconcepts.size(); i++) {
        ConceptGraphYamlDTO.SubconceptNodeDTO subNode = subconcepts.get(i);
        Concept savedSub =
            conceptRepository.save(
                Concept.builder()
                    .course(course)
                    .label(markdownService.clean(subNode.getLabel()))
                    .description(markdownService.clean(subNode.getDescription()))
                    .example(markdownService.clean(subNode.getExample()))
                    .parent(saved)
                    .sortOrder(i)
                    .build());
        subconceptsCreated++;
        practiceProblemsCreated +=
            savePracticeProblems(course, savedSub, subNode.getPracticeProblems());
      }
    }

    for (ConceptGraphYamlDTO.EdgeNodeDTO edgeNode :
        dto.getEdges() != null ? dto.getEdges() : List.<ConceptGraphYamlDTO.EdgeNodeDTO>of()) {
      conceptEdgeRepository.save(
          ConceptEdge.builder()
              .course(course)
              .source(savedByExternalId.get(edgeNode.getFrom()))
              .target(savedByExternalId.get(edgeNode.getTo()))
              .build());
      edgesCreated++;
    }

    return report(
        true,
        errors,
        conceptsCreated,
        subconceptsCreated,
        edgesCreated,
        practiceProblemsCreated,
        userStatesCleared);
  }

  private ConceptGraphYamlDTO parseYaml(InputStream yamlStream, List<String> errors) {
    // Read the stream ourselves before handing the text to Jackson: Jackson wraps stream
    // IOExceptions in JsonProcessingException, which would make an unreadable upload look
    // like a syntax error in the file.
    try {
      String yaml = new String(yamlStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      return YAML_MAPPER.readValue(yaml, ConceptGraphYamlDTO.class);
    } catch (JsonProcessingException e) {
      errors.add("could not parse YAML: " + e.getOriginalMessage());
    } catch (IOException e) {
      errors.add("could not read file: " + e.getMessage());
    }
    return null;
  }

  /**
   * Checks everything that could make the document unloadable or leave the course in a state the
   * rest of the app does not expect (the same invariants the single-concept endpoints enforce).
   * Every problem found is appended to {@code errors}; an empty list afterward means the document
   * is safe to load.
   */
  private void validate(ConceptGraphYamlDTO dto, List<String> errors) {
    if (dto.getFormat() == null) {
      errors.add("format is required (expected: format: 1)");
    } else if (dto.getFormat() != 1) {
      errors.add("unsupported format %d (expected: format: 1)".formatted(dto.getFormat()));
    }
    if (dto.getConcepts() == null) {
      errors.add("concepts is required (may be an empty list: concepts: [])");
    }

    Set<Long> externalIds = new HashSet<>();
    List<ConceptGraphYamlDTO.ConceptNodeDTO> concepts =
        dto.getConcepts() != null ? dto.getConcepts() : List.of();
    for (int i = 0; i < concepts.size(); i++) {
      ConceptGraphYamlDTO.ConceptNodeDTO node = concepts.get(i);
      String where = "concepts[%d]".formatted(i);
      if (node == null) {
        errors.add(where + " is empty");
        continue;
      }
      if (node.getId() == null) {
        errors.add(where + ": id is required");
      } else if (!externalIds.add(node.getId())) {
        errors.add(where + ": duplicate id " + node.getId());
      }
      validateLabel(where, node.getLabel(), errors, Concept.MAX_RENDERED_LABEL_LENGTH);
      validateUrls(where, node.getPracticeProblems(), errors);

      Set<String> subconceptLabels = new HashSet<>();
      List<ConceptGraphYamlDTO.SubconceptNodeDTO> subconcepts =
          node.getSubconcepts() != null ? node.getSubconcepts() : List.of();
      for (int j = 0; j < subconcepts.size(); j++) {
        ConceptGraphYamlDTO.SubconceptNodeDTO subNode = subconcepts.get(j);
        String subWhere = where + ".subconcepts[%d]".formatted(j);
        if (subNode == null) {
          errors.add(subWhere + " is empty");
          continue;
        }
        String cleanLabel =
            validateLabel(
                subWhere, subNode.getLabel(), errors, Concept.MAX_RENDERED_SUBCONCEPT_LABEL_LENGTH);
        if (cleanLabel != null && !subconceptLabels.add(cleanLabel)) {
          errors.add(subWhere + ": duplicate subconcept label " + cleanLabel);
        }
        validateUrls(subWhere, subNode.getPracticeProblems(), errors);
      }
    }

    // Edges are checked in order against the edges accepted so far, so the cycle message points
    // at the edge that closes the cycle.
    List<ConceptEdge> acceptedEdges = new ArrayList<>();
    Set<List<Long>> seenEndpoints = new HashSet<>();
    List<ConceptGraphYamlDTO.EdgeNodeDTO> edges =
        dto.getEdges() != null ? dto.getEdges() : List.of();
    for (int i = 0; i < edges.size(); i++) {
      ConceptGraphYamlDTO.EdgeNodeDTO edge = edges.get(i);
      String where = "edges[%d]".formatted(i);
      if (edge == null) {
        errors.add(where + " is empty");
        continue;
      }
      if (edge.getFrom() == null || edge.getTo() == null) {
        errors.add(where + ": from and to are required");
        continue;
      }
      boolean endpointsExist = true;
      if (!externalIds.contains(edge.getFrom())) {
        errors.add(where + ": no concept with id " + edge.getFrom());
        endpointsExist = false;
      }
      if (!externalIds.contains(edge.getTo())) {
        errors.add(where + ": no concept with id " + edge.getTo());
        endpointsExist = false;
      }
      if (!endpointsExist) {
        continue;
      }
      if (edge.getFrom().equals(edge.getTo())) {
        errors.add(where + ": an edge cannot connect a concept to itself");
        continue;
      }
      if (!seenEndpoints.add(List.of(edge.getFrom(), edge.getTo()))) {
        errors.add(
            where + ": duplicate edge from %d to %d".formatted(edge.getFrom(), edge.getTo()));
        continue;
      }
      if (conceptGraphService.wouldCreateCycle(acceptedEdges, edge.getFrom(), edge.getTo())) {
        errors.add(
            where
                + ": edge from %d to %d would create a cycle"
                    .formatted(edge.getFrom(), edge.getTo()));
        continue;
      }
      acceptedEdges.add(
          ConceptEdge.builder()
              .source(Concept.builder().id(edge.getFrom()).build())
              .target(Concept.builder().id(edge.getTo()).build())
              .build());
    }
  }

  /**
   * Validates a concept or subconcept label the same way the single-concept endpoints do, and
   * returns the cleaned label (used for duplicate detection), or null if it was invalid.
   */
  private String validateLabel(
      String where, String label, List<String> errors, int maxRenderedLength) {
    String cleanLabel = markdownService.clean(label);
    if (cleanLabel == null || cleanLabel.isEmpty()) {
      errors.add(where + ": label may not be empty");
      return null;
    }
    int renderedLength = markdownService.renderedLength(cleanLabel);
    if (renderedLength > maxRenderedLength) {
      errors.add(
          where
              + ": label renders to %d characters; the maximum is %d"
                  .formatted(renderedLength, maxRenderedLength));
      return null;
    }
    return cleanLabel;
  }

  private void validateUrls(String where, List<String> urls, List<String> errors) {
    if (urls == null) {
      return;
    }
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < urls.size(); i++) {
      String url = urls.get(i);
      String urlWhere = where + ".practiceProblems[%d]".formatted(i);
      if (url == null || url.isBlank()) {
        errors.add(urlWhere + ": url may not be empty");
        continue;
      }
      String cleanUrl = url.strip();
      if (cleanUrl.length() > MAX_URL_LENGTH) {
        errors.add(
            urlWhere
                + ": url is %d characters long; the maximum is %d"
                    .formatted(cleanUrl.length(), MAX_URL_LENGTH));
        continue;
      }
      if (!seen.add(cleanUrl)) {
        errors.add(urlWhere + ": duplicate practice problem url " + cleanUrl);
      }
    }
  }

  /**
   * Deletes the course's practice problems, edges, and concepts (children before parents, in an
   * order that never breaks a foreign key), plus every user's per-course scaffold state. Returns
   * how many user-state rows were cleared.
   */
  private int deleteCourseContentAndUserState(long courseId) {
    practiceProblemRepository.deleteAll(practiceProblemRepository.findByCourseId(courseId));
    conceptEdgeRepository.deleteAll(conceptEdgeRepository.findByCourseId(courseId));

    List<Concept> existingConcepts = conceptRepository.findByCourseId(courseId);
    conceptRepository.deleteAll(existingConcepts.stream().filter(Concept::isSubconcept).toList());
    conceptRepository.deleteAll(
        existingConcepts.stream().filter(concept -> !concept.isSubconcept()).toList());

    List<UserStateV2> userStates = userStateV2Repository.findByCourseId(courseId);
    userStateV2Repository.deleteAll(userStates);
    return userStates.size();
  }

  private int savePracticeProblems(Course course, Concept concept, List<String> urls) {
    if (urls == null) {
      return 0;
    }
    for (String url : urls) {
      practiceProblemRepository.save(
          PracticeProblem.builder().course(course).concept(concept).url(url.strip()).build());
    }
    return urls.size();
  }

  private Map<String, Object> report(
      boolean success,
      List<String> errors,
      int conceptsCreated,
      int subconceptsCreated,
      int edgesCreated,
      int practiceProblemsCreated,
      int userStatesCleared) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("success", success);
    report.put("errors", errors);
    report.put("conceptsCreated", conceptsCreated);
    report.put("subconceptsCreated", subconceptsCreated);
    report.put("edgesCreated", edgesCreated);
    report.put("practiceProblemsCreated", practiceProblemsCreated);
    report.put("userStatesCleared", userStatesCleared);
    return report;
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
