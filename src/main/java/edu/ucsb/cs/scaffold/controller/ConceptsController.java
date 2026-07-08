package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import edu.ucsb.cs.scaffold.services.MarkdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Concepts")
@RestController
@RequiredArgsConstructor
public class ConceptsController extends ApiController {

  public static final int MAX_RENDERED_CONCEPT_LABEL_LENGTH = 32;

  // Top-level concept names are slugs: lowercase letters, digits, and hyphens.
  public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9-]+$");

  private final ConceptRepository conceptRepository;
  private final PracticeProblemRepository practiceProblemRepository;
  private final ConceptEdgeRepository conceptEdgeRepository;
  private final CourseRepository courseRepository;
  private final MarkdownService markdownService;

  @Operation(summary = "Get description/example/practiceUrl content for every concept in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/content")
  public Map<String, ConceptContentDTO> getContent(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    Map<Long, String> urlByConceptId = firstUrlByConceptId(courseId);

    Map<String, ConceptContentDTO> result = new LinkedHashMap<>();
    for (Concept concept : conceptRepository.findByCourseId(courseId)) {
      Long parentId = concept.isSubconcept() ? concept.getParent().getId() : null;
      result.put(
          contentKey(concept),
          new ConceptContentDTO(
              concept.getId(),
              parentId,
              markdownService.toHtml(concept.getDescription()),
              markdownService.toHtml(concept.getExample()),
              urlByConceptId.get(concept.getId())));
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
            .filter(Concept::isSubconcept)
            .sorted(Comparator.comparing(Concept::getId))
            .collect(
                Collectors.groupingBy(
                    concept -> concept.getParent().getId(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    List<MajorConceptDTO> result = new ArrayList<>();
    concepts.stream()
        .filter(concept -> !concept.isSubconcept())
        .sorted(Comparator.comparing(Concept::getId))
        .forEach(
            major -> {
              List<SubconceptDTO> subconceptDtos =
                  subconceptsByParentId.getOrDefault(major.getId(), List.of()).stream()
                      .map(
                          sub ->
                              new SubconceptDTO(
                                  sub.getId(),
                                  sub.getParent().getId(),
                                  markdownService.toInlineHtml(sub.getLabel())))
                      .toList();
              result.add(
                  new MajorConceptDTO(
                      major.getId(),
                      major.getName(),
                      markdownService.toInlineHtml(major.getLabel()),
                      major.getColor(),
                      subconceptDtos));
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
      if (!concept.isSubconcept()) {
        result.put(concept.getName(), new PositionDTO(concept.getX(), concept.getY()));
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
      result.add(new EdgeDTO(edge.getSource().getName(), edge.getTarget().getName()));
    }
    return result;
  }

  @Operation(
      summary = "Create a new concept or subconcept",
      description =
          """
          With no parentConceptId, creates a top-level concept: name, x, and y are required.
          With a parentConceptId (which must be an existing top-level concept in the same
          course), creates a subconcept: name, x, y, and color must be omitted.
          label, description, and example are Markdown; they are sanitized and canonicalized
          before being stored.
          """)
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/api/concepts/post")
  public Concept postConcept(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "label") @RequestParam String label,
      @Parameter(name = "name") @RequestParam(required = false) String name,
      @Parameter(name = "parentConceptId") @RequestParam(required = false) Long parentConceptId,
      @Parameter(name = "description") @RequestParam(required = false) String description,
      @Parameter(name = "example") @RequestParam(required = false) String example,
      @Parameter(name = "color") @RequestParam(required = false) String color,
      @Parameter(name = "x") @RequestParam(required = false) Integer x,
      @Parameter(name = "y") @RequestParam(required = false) Integer y)
      throws EntityNotFoundException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    String cleanLabel = cleanAndValidateLabel(label);

    Concept concept =
        Concept.builder()
            .course(course)
            .label(cleanLabel)
            .description(markdownService.clean(description))
            .example(markdownService.clean(example))
            .build();

    if (parentConceptId != null) {
      Concept parent =
          conceptRepository
              .findById(parentConceptId)
              .orElseThrow(() -> new EntityNotFoundException(Concept.class, parentConceptId));
      if (!parent.getCourse().getId().equals(courseId)) {
        throw new IllegalArgumentException(
            "parentConceptId %d belongs to a different course".formatted(parentConceptId));
      }
      if (parent.isSubconcept()) {
        throw new IllegalArgumentException(
            "parentConceptId %d is a subconcept; concepts can only be nested one level deep"
                .formatted(parentConceptId));
      }
      if (name != null || color != null || x != null || y != null) {
        throw new IllegalArgumentException(
            "name, color, x, and y may not be specified for a subconcept");
      }
      rejectDuplicateLabelUnderParent(parent, cleanLabel);
      concept.setParent(parent);
    } else {
      validateNewTopLevelName(courseId, name);
      if (x == null || y == null) {
        throw new IllegalArgumentException("x and y are required for a top-level concept");
      }
      concept.setName(name);
      concept.setColor(color);
      concept.setX(x);
      concept.setY(y);
    }

    return conceptRepository.save(concept);
  }

  @Operation(summary = "Add a practice problem URL to a concept")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PostMapping("/api/concepts/practiceproblems/post")
  public PracticeProblem postPracticeProblem(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
      @Parameter(name = "url") @RequestParam String url)
      throws EntityNotFoundException {

    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));

    String cleanUrl = url.strip();
    if (cleanUrl.isEmpty()) {
      throw new IllegalArgumentException("url may not be empty");
    }
    Long courseId = concept.getCourse().getId();
    if (practiceProblemRepository
        .findByCourseIdAndConceptIdAndUrl(courseId, conceptId, cleanUrl)
        .isPresent()) {
      throw new IllegalArgumentException(
          "concept %d already has practice problem url %s".formatted(conceptId, cleanUrl));
    }

    PracticeProblem practiceProblem =
        PracticeProblem.builder()
            .course(concept.getCourse())
            .concept(concept)
            .url(cleanUrl)
            .build();
    return practiceProblemRepository.save(practiceProblem);
  }

  @Operation(
      summary = "Designate an existing concept as a subconcept of a top-level concept",
      description =
          """
          Sets the parent of concept conceptId to parentConceptId. The concept must have no
          subconcepts of its own and no prerequisite edges (delete those first). The parent
          must be a top-level concept in the same course. The concept keeps its x,y position
          (if any), its name is cleared, and it takes on the parent's color.
          """)
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PutMapping("/api/concepts/designate")
  public Concept designateSubconcept(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
      @Parameter(name = "parentConceptId") @RequestParam Long parentConceptId)
      throws EntityNotFoundException {

    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));
    Concept parent =
        conceptRepository
            .findById(parentConceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, parentConceptId));

    if (concept.getId().equals(parent.getId())) {
      throw new IllegalArgumentException("a concept cannot be its own parent");
    }
    if (!parent.getCourse().getId().equals(concept.getCourse().getId())) {
      throw new IllegalArgumentException(
          "parentConceptId %d belongs to a different course".formatted(parentConceptId));
    }
    if (parent.isSubconcept()) {
      throw new IllegalArgumentException(
          "parentConceptId %d is a subconcept; concepts can only be nested one level deep"
              .formatted(parentConceptId));
    }
    if (!conceptRepository.findByParentId(conceptId).isEmpty()) {
      throw new IllegalArgumentException(
          "concept %d has subconcepts of its own, so it cannot become a subconcept"
              .formatted(conceptId));
    }
    if (!conceptEdgeRepository.findBySourceIdOrTargetId(conceptId, conceptId).isEmpty()) {
      throw new IllegalArgumentException(
          "concept %d has prerequisite edges; delete them before designating it as a subconcept"
              .formatted(conceptId));
    }
    rejectDuplicateLabelUnderParent(parent, concept.getLabel());

    concept.setParent(parent);
    concept.setName(null);
    concept.setColor(parent.getColor());
    return conceptRepository.save(concept);
  }

  @Operation(
      summary = "Split a subconcept off into its own top-level concept",
      description =
          """
          Severs the concept's relationship with its parent, making it a top-level concept
          with the given name and x,y position. If the concept has no color, it inherits its
          former parent's color.
          """)
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PutMapping("/api/concepts/splitoff")
  public Concept splitOffSubconcept(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "x") @RequestParam Integer x,
      @Parameter(name = "y") @RequestParam Integer y)
      throws EntityNotFoundException {

    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));

    if (!concept.isSubconcept()) {
      throw new IllegalArgumentException(
          "concept %d is already a top-level concept".formatted(conceptId));
    }
    validateNewTopLevelName(concept.getCourse().getId(), name);

    if (concept.getColor() == null) {
      concept.setColor(concept.getParent().getColor());
    }
    concept.setParent(null);
    concept.setName(name);
    concept.setX(x);
    concept.setY(y);
    return conceptRepository.save(concept);
  }

  @Operation(summary = "Create a prerequisite edge between two top-level concepts")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #sourceConceptId)")
  @PostMapping("/api/concepts/edges/post")
  public ConceptEdge postConceptEdge(
      @Parameter(name = "sourceConceptId") @RequestParam Long sourceConceptId,
      @Parameter(name = "targetConceptId") @RequestParam Long targetConceptId)
      throws EntityNotFoundException {

    Concept source =
        conceptRepository
            .findById(sourceConceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, sourceConceptId));
    Concept target =
        conceptRepository
            .findById(targetConceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, targetConceptId));

    if (source.getId().equals(target.getId())) {
      throw new IllegalArgumentException("an edge cannot connect a concept to itself");
    }
    if (!source.getCourse().getId().equals(target.getCourse().getId())) {
      throw new IllegalArgumentException("both concepts must belong to the same course");
    }
    if (source.isSubconcept() || target.isSubconcept()) {
      throw new IllegalArgumentException("edges may only connect top-level concepts");
    }
    if (conceptEdgeRepository
        .findBySourceIdAndTargetId(sourceConceptId, targetConceptId)
        .isPresent()) {
      throw new IllegalArgumentException(
          "edge from concept %d to concept %d already exists"
              .formatted(sourceConceptId, targetConceptId));
    }

    ConceptEdge edge =
        ConceptEdge.builder().course(source.getCourse()).source(source).target(target).build();
    return conceptEdgeRepository.save(edge);
  }

  @Operation(summary = "Delete a prerequisite edge")
  @PreAuthorize("@CourseSecurity.hasConceptEdgeManagementPermissions(#root, #id)")
  @DeleteMapping("/api/concepts/edges/delete")
  public Object deleteConceptEdge(@Parameter(name = "id") @RequestParam Long id)
      throws EntityNotFoundException {
    ConceptEdge edge =
        conceptEdgeRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ConceptEdge.class, id));
    conceptEdgeRepository.delete(edge);
    return genericMessage("ConceptEdge with id %s deleted".formatted(id));
  }

  /**
   * The key under which a concept's content appears in the map returned by getContent. Top-level
   * concepts use their name; subconcepts have no name and use the derived key "parentName:label",
   * which is the same key the frontend builds for them.
   */
  static String contentKey(Concept concept) {
    if (concept.isSubconcept()) {
      return concept.getParent().getName() + ":" + concept.getLabel();
    }
    return concept.getName();
  }

  private String cleanAndValidateLabel(String label) {
    String cleanLabel = markdownService.clean(label);
    if (cleanLabel.isEmpty()) {
      throw new IllegalArgumentException("label may not be empty");
    }
    int renderedLength = markdownService.renderedLength(cleanLabel);
    if (renderedLength > MAX_RENDERED_CONCEPT_LABEL_LENGTH) {
      throw new IllegalArgumentException(
          "label renders to %d characters; the maximum is %d"
              .formatted(renderedLength, MAX_RENDERED_CONCEPT_LABEL_LENGTH));
    }
    return cleanLabel;
  }

  private void validateNewTopLevelName(Long courseId, String name) {
    if (name == null || !NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "name is required for a top-level concept and may contain only lowercase letters,"
              + " digits, and hyphens");
    }
    if (conceptRepository.findByCourseIdAndName(courseId, name).isPresent()) {
      throw new IllegalArgumentException(
          "a concept named %s already exists in course %d".formatted(name, courseId));
    }
  }

  private void rejectDuplicateLabelUnderParent(Concept parent, String label) {
    if (conceptRepository.findByParentIdAndLabel(parent.getId(), label).isPresent()) {
      throw new IllegalArgumentException(
          "concept %d already has a subconcept with label %s".formatted(parent.getId(), label));
    }
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

  public record ConceptContentDTO(
      Long id, Long parentId, String descriptionHtml, String exampleHtml, String practiceUrl) {}

  public record MajorConceptDTO(
      Long id, String name, String labelHtml, String color, List<SubconceptDTO> subconcepts) {}

  public record SubconceptDTO(Long id, Long parentId, String labelHtml) {}

  public record PositionDTO(Integer x, Integer y) {}

  public record EdgeDTO(String source, String target) {}
}
