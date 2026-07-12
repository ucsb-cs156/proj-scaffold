package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.CreateConceptDTO;
import edu.ucsb.cs.scaffold.model.CreateSubconceptDTO;
import edu.ucsb.cs.scaffold.model.UpdateConceptDTO;
import edu.ucsb.cs.scaffold.model.UpdateSubconceptDTO;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.ConceptEdgeRepository;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PracticeProblemRepository;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import edu.ucsb.cs.scaffold.services.ConceptGraphService;
import edu.ucsb.cs.scaffold.services.MarkdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Concepts")
@RestController
@RequiredArgsConstructor
public class ConceptsController extends ApiController {

  public static final int MAX_RENDERED_CONCEPT_LABEL_LENGTH = Concept.MAX_RENDERED_LABEL_LENGTH;

  // Applied to every new top-level concept; users cannot assign a color at creation time for
  // now. Top-level concepts are assumed throughout the frontend (node styling, drag-out
  // detail edges) to always have a real color; a null/blank color silently breaks that
  // styling (e.g. an SVG edge with no stroke color renders with stroke: none and is
  // invisible). Matches the "Level 1" swatch in the frontend's concept-graph legend.
  public static final String DEFAULT_TOP_LEVEL_COLOR = "#c99ffe";

  // The display order of subconcepts within a parent. sortOrder is author-controlled
  // (see reorderSubconcepts); the id tiebreaker makes the order deterministic even for
  // rows with equal or missing sortOrder (pre-backfill data, concurrent-create ties).
  private static final Comparator<Concept> SUBCONCEPT_DISPLAY_ORDER =
      Comparator.comparing(Concept::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Concept::getId);

  private static final Comparator<Concept> COURSE_CONCEPT_TABLE_ORDER =
      Comparator.comparing(Concept::getLevel, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Concept::getX, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Concept::getId);

  private final ConceptRepository conceptRepository;
  private final PracticeProblemRepository practiceProblemRepository;
  private final ConceptEdgeRepository conceptEdgeRepository;
  private final CourseRepository courseRepository;
  private final UserStateV2Repository userStateV2Repository;
  private final MarkdownService markdownService;
  private final ConceptGraphService conceptGraphService;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Get description/example/practiceUrl content for every concept in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/content")
  public Map<Long, ConceptContentDTO> getContent(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    Map<Long, String> urlByConceptId = firstUrlByConceptId(courseId);

    Map<Long, ConceptContentDTO> result = new LinkedHashMap<>();
    for (Concept concept : conceptRepository.findByCourseId(courseId)) {
      Long parentId = concept.isSubconcept() ? concept.getParent().getId() : null;
      result.put(
          concept.getId(),
          new ConceptContentDTO(
              concept.getId(),
              parentId,
              markdownService.toHtml(concept.getDescription()),
              markdownService.toHtml(concept.getExample()),
              urlByConceptId.get(concept.getId())));
    }
    return result;
  }

  @Operation(summary = "Get the top-level concepts for a course in table order")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/course")
  public List<CourseConceptDTO> getCourseConcepts(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    return conceptRepository.findByCourseId(courseId).stream()
        .filter(concept -> !concept.isSubconcept())
        .sorted(COURSE_CONCEPT_TABLE_ORDER)
        .map(
            concept ->
                new CourseConceptDTO(
                    concept.getId(),
                    concept.getLabel(),
                    concept.getDescription(),
                    concept.getExample(),
                    concept.getLevel(),
                    concept.getX(),
                    concept.getY()))
        .toList();
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
            .sorted(SUBCONCEPT_DISPLAY_ORDER)
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
                      markdownService.toInlineHtml(major.getLabel()),
                      major.getColor(),
                      subconceptDtos));
            });
    return result;
  }

  @Operation(summary = "Get the graph positions of the top-level concepts in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/positions")
  public Map<Long, PositionDTO> getPositions(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    Map<Long, PositionDTO> result = new LinkedHashMap<>();
    for (Concept concept : conceptRepository.findByCourseId(courseId)) {
      if (!concept.isSubconcept()) {
        result.put(concept.getId(), new PositionDTO(concept.getX(), concept.getY()));
      }
    }
    return result;
  }

  @Operation(summary = "Get all top-level concepts for a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/top-level")
  public List<TopLevelConceptDTO> getTopLevelConcepts(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    return conceptRepository.findByCourseId(courseId).stream()
        .filter(concept -> !concept.isSubconcept())
        .sorted(Comparator.comparing(Concept::getId))
        .map(
            concept ->
                new TopLevelConceptDTO(
                    concept.getId(),
                    concept.getLabel(),
                    concept.getLevel(),
                    concept.getX(),
                    concept.getY()))
        .toList();
  }

  @Operation(summary = "Get all subconcepts for a course in table-row format")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/subconcepts")
  public List<SubConceptTableRowDTO> getSubconcepts(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    return conceptRepository.findByCourseId(courseId).stream()
        .filter(Concept::isSubconcept)
        .sorted(
            Comparator.comparing(
                    (Concept c) -> c.getParent().getLevel(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                    c -> c.getParent().getX(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                    Concept::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Concept::getId))
        .map(
            concept ->
                new SubConceptTableRowDTO(
                    concept.getId(),
                    concept.getLabel(),
                    concept.getDescription(),
                    concept.getExample(),
                    concept.getParent().getId(),
                    concept.getParent().getLabel(),
                    concept.getParent().getLevel(),
                    concept.getParent().getX(),
                    concept.getSortOrder()))
        .toList();
  }

  @Operation(summary = "Get the prerequisite edges between concepts in a course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/api/concepts/edges")
  public List<EdgeDTO> getEdges(
      @Parameter(description = "id of the course") @RequestParam Long courseId) {
    List<EdgeDTO> result = new ArrayList<>();
    for (ConceptEdge edge : conceptEdgeRepository.findByCourseId(courseId)) {
      result.add(
          new EdgeDTO(
              edge.getId(), edge.getSource().getId(), edge.getTarget().getId(), edge.getColor()));
    }
    return result;
  }

  @Operation(
      summary = "Create a new top-level concept",
      description =
          """
          Accepts a YAML (or JSON) request body. label, x, and y are required; color and
          level are not user-settable (every new top-level concept starts at level 1 with
          the default color, until a scaffold reset recomputes them). label, description,
          and example are Markdown; they are sanitized and canonicalized before being
          stored. YAML block scalars (|) make multi-line Markdown easy to enter through
          Swagger.
          """)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/yaml",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      implementation = CreateConceptDTO.class),
              examples =
                  @io.swagger.v3.oas.annotations.media.ExampleObject(
                      value =
                          """
                          courseId: 1
                          label: Arrays
                          description: |
                            An *array* is a fixed-size collection of elements.
                          example: |
                            ```java
                            int[] arr = new int[5];
                            ```
                          x: 0
                          y: 0
                          """)))
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #dto.courseId)")
  @PostMapping(
      value = "/api/concept",
      consumes = {"application/yaml", "application/x-yaml", "application/json"})
  public Concept postConcept(@RequestBody CreateConceptDTO dto) throws EntityNotFoundException {
    if (dto.getCourseId() == null) {
      throw new IllegalArgumentException("courseId is required");
    }
    Course course =
        courseRepository
            .findById(dto.getCourseId())
            .orElseThrow(() -> new EntityNotFoundException(Course.class, dto.getCourseId()));

    String cleanLabel = cleanAndValidateLabel(dto.getLabel());
    if (dto.getX() == null || dto.getY() == null) {
      throw new IllegalArgumentException("x and y are required for a top-level concept");
    }

    Concept concept =
        Concept.builder()
            .course(course)
            .label(cleanLabel)
            .description(markdownService.clean(dto.getDescription()))
            .example(markdownService.clean(dto.getExample()))
            .color(DEFAULT_TOP_LEVEL_COLOR)
            .level(1)
            .x(dto.getX())
            .y(dto.getY())
            .build();
    return conceptRepository.save(concept);
  }

  @Operation(
      summary = "Create a new subconcept of a top-level concept",
      description =
          """
          Accepts a YAML (or JSON) request body. The parent must be an existing top-level
          concept in the same course, and the label must be unique among the parent's
          subconcepts. Subconcepts have no name, position, or color of their own. label,
          description, and example are Markdown; they are sanitized and canonicalized before
          being stored.
          """)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/yaml",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      implementation = CreateSubconceptDTO.class),
              examples =
                  @io.swagger.v3.oas.annotations.media.ExampleObject(
                      value =
                          """
                          courseId: 1
                          parentConceptId: 42
                          label: Accessing a value
                          description: |
                            Use square brackets with an index.
                          example: |
                            ```java
                            int first = arr[0];
                            ```
                          """)))
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #dto.courseId)")
  @PostMapping(
      value = "/api/concept/subconcept",
      consumes = {"application/yaml", "application/x-yaml", "application/json"})
  public Concept postSubconcept(@RequestBody CreateSubconceptDTO dto)
      throws EntityNotFoundException {
    if (dto.getCourseId() == null) {
      throw new IllegalArgumentException("courseId is required");
    }
    if (dto.getParentConceptId() == null) {
      throw new IllegalArgumentException("parentConceptId is required");
    }
    Course course =
        courseRepository
            .findById(dto.getCourseId())
            .orElseThrow(() -> new EntityNotFoundException(Course.class, dto.getCourseId()));

    String cleanLabel = cleanAndValidateLabel(dto.getLabel());

    Concept parent =
        conceptRepository
            .findById(dto.getParentConceptId())
            .orElseThrow(
                () -> new EntityNotFoundException(Concept.class, dto.getParentConceptId()));
    if (!parent.getCourse().getId().equals(dto.getCourseId())) {
      throw new IllegalArgumentException(
          "parentConceptId %d belongs to a different course".formatted(dto.getParentConceptId()));
    }
    if (parent.isSubconcept()) {
      throw new IllegalArgumentException(
          "parentConceptId %d is a subconcept; concepts can only be nested one level deep"
              .formatted(dto.getParentConceptId()));
    }
    rejectDuplicateLabelUnderParent(parent, cleanLabel);

    Concept concept =
        Concept.builder()
            .course(course)
            .label(cleanLabel)
            .description(markdownService.clean(dto.getDescription()))
            .example(markdownService.clean(dto.getExample()))
            .parent(parent)
            .sortOrder(nextSortOrder(parent.getId()))
            .build();
    return conceptRepository.save(concept);
  }

  @Operation(summary = "Update the label, description, and example of a top-level concept")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PutMapping("/api/concept/put")
  public Concept putConcept(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
      @RequestBody UpdateConceptDTO dto)
      throws EntityNotFoundException {

    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));

    if (concept.isSubconcept()) {
      throw new IllegalArgumentException(
          "concept %d is a subconcept; use PUT /api/concept/subconcept/put to update it"
              .formatted(conceptId));
    }

    concept.setLabel(cleanAndValidateLabel(dto.getLabel()));
    concept.setDescription(markdownService.clean(dto.getDescription()));
    concept.setExample(markdownService.clean(dto.getExample()));
    return conceptRepository.save(concept);
  }

  @Operation(summary = "Update the label, description, and example of a subconcept")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PutMapping("/api/concept/subconcept/put")
  public Concept putSubconcept(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
      @RequestBody UpdateSubconceptDTO dto)
      throws EntityNotFoundException {

    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));

    if (!concept.isSubconcept()) {
      throw new IllegalArgumentException(
          "concept %d is not a subconcept; use PUT /api/concept/put to update it"
              .formatted(conceptId));
    }

    String cleanLabel = cleanAndValidateLabel(dto.getLabel());
    rejectDuplicateLabelUnderParent(concept.getParent(), cleanLabel, concept.getId());

    concept.setLabel(cleanLabel);
    concept.setDescription(markdownService.clean(dto.getDescription()));
    concept.setExample(markdownService.clean(dto.getExample()));
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
          (if any) and takes on the parent's color.
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
    concept.setColor(parent.getColor());
    concept.setSortOrder(nextSortOrder(parent.getId()));
    return conceptRepository.save(concept);
  }

  @Operation(
      summary = "Split a subconcept off into its own top-level concept",
      description =
          """
          Severs the concept's relationship with its parent, making it a top-level concept
          at the given x,y position. If the concept has no color, it inherits its former
          parent's color.
          """)
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PutMapping("/api/concepts/splitoff")
  public Concept splitOffSubconcept(
      @Parameter(name = "conceptId") @RequestParam Long conceptId,
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

    if (concept.getColor() == null) {
      concept.setColor(concept.getParent().getColor());
    }
    concept.setParent(null);
    concept.setSortOrder(null);
    concept.setX(x);
    concept.setY(y);
    return conceptRepository.save(concept);
  }

  @Operation(
      summary = "Reorder the subconcepts of a top-level concept",
      description =
          """
          Takes the complete ordered list of the parent's subconcept ids and rewrites every
          subconcept's sort position to its index in the list. Sending the whole permutation
          (rather than individual move operations) makes the update atomic and idempotent:
          concurrent reorders resolve to one complete, coherent ordering (last writer wins),
          and any sort-position ties left by concurrent subconcept creation are cleaned up as
          a side effect. The list must contain each current subconcept of the parent exactly
          once. Returns the subconcepts in their new order.
          """)
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #parentConceptId)")
  @PutMapping("/api/concepts/subconcepts/reorder")
  public List<SubconceptDTO> reorderSubconcepts(
      @Parameter(name = "parentConceptId") @RequestParam Long parentConceptId,
      @RequestBody List<Long> orderedSubconceptIds)
      throws EntityNotFoundException {

    Concept parent =
        conceptRepository
            .findById(parentConceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, parentConceptId));
    if (parent.isSubconcept()) {
      throw new IllegalArgumentException(
          "concept %d is a subconcept; only top-level concepts have subconcepts to reorder"
              .formatted(parentConceptId));
    }

    List<Concept> subconcepts = conceptRepository.findByParentId(parentConceptId);
    Map<Long, Concept> subconceptById = new HashMap<>();
    for (Concept subconcept : subconcepts) {
      subconceptById.put(subconcept.getId(), subconcept);
    }
    if (new HashSet<>(orderedSubconceptIds).size() != orderedSubconceptIds.size()
        || orderedSubconceptIds.size() != subconcepts.size()
        || !subconceptById.keySet().containsAll(orderedSubconceptIds)) {
      throw new IllegalArgumentException(
          "orderedSubconceptIds must contain the id of each subconcept of concept %d exactly once"
              .formatted(parentConceptId));
    }

    for (int i = 0; i < orderedSubconceptIds.size(); i++) {
      subconceptById.get(orderedSubconceptIds.get(i)).setSortOrder(i);
    }
    conceptRepository.saveAll(subconcepts);

    return orderedSubconceptIds.stream()
        .map(subconceptById::get)
        .map(
            sub ->
                new SubconceptDTO(
                    sub.getId(),
                    sub.getParent().getId(),
                    markdownService.toInlineHtml(sub.getLabel())))
        .toList();
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
    if (conceptGraphService.wouldCreateCycle(
        conceptEdgeRepository.findByCourseId(source.getCourse().getId()),
        sourceConceptId,
        targetConceptId)) {
      throw new IllegalArgumentException(
          "edge from concept %d to concept %d would create a cycle"
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

  @Operation(
      summary = "Delete a concept; deleting a top-level concept also deletes its subconcepts")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @DeleteMapping("/api/concept/delete")
  public Object deleteConcept(@Parameter(name = "conceptId") @RequestParam Long conceptId)
      throws EntityNotFoundException {
    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));

    Course course = concept.getCourse();
    if (concept.isSubconcept()) {
      deleteConceptArtifacts(course.getId(), List.of(conceptId));
      conceptRepository.delete(concept);
      return genericMessage("Concept with id %s deleted".formatted(conceptId));
    }

    List<Concept> subconcepts = conceptRepository.findByParentId(conceptId);
    List<Long> conceptIdsToDelete = new ArrayList<>();
    conceptIdsToDelete.add(conceptId);
    conceptIdsToDelete.addAll(subconcepts.stream().map(Concept::getId).toList());
    deleteConceptArtifacts(course.getId(), conceptIdsToDelete);
    conceptRepository.deleteAll(subconcepts);
    conceptRepository.delete(concept);
    return genericMessage("Concept with id %s deleted".formatted(conceptId));
  }

  @Operation(
      summary = "Recompute a course's concept-graph structure",
      description =
          """
          Runs cycle detection, transitive reduction, longest-path leveling, and layout over
          a course's top-level concepts and prerequisite edges, persisting the results:
          edges found to be part of a cycle are colored red and excluded from the rest of
          the analysis (their endpoints keep whatever level/position they already had);
          edges made redundant by the graph's transitive structure are deleted; every other
          top-level concept is assigned a longest-path level (roots are level 1), a color
          from that level, and an x,y position (each level arranged left to right, sorted by
          prior x then id, centered at x=0, stacked above the previous level).
          """)
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/api/course/scaffold/reset")
  public ScaffoldResetResponseDTO resetCourseScaffold(
      @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    List<Concept> topLevelConcepts =
        conceptRepository.findByCourseId(courseId).stream()
            .filter(concept -> !concept.isSubconcept())
            .toList();
    List<ConceptEdge> edges = conceptEdgeRepository.findByCourseId(courseId);

    Map<Long, Integer> priorXByConceptId =
        priorXByConceptId(topLevelConcepts, callerPrivatePositions(courseId));
    ConceptGraphService.ResetResult result =
        conceptGraphService.reset(topLevelConcepts, edges, priorXByConceptId);

    for (ConceptEdge edge : edges) {
      edge.setColor(
          result.cycleEdgeIds().contains(edge.getId())
              ? ConceptGraphService.CYCLE_EDGE_COLOR
              : null);
    }
    conceptEdgeRepository.saveAll(edges);

    List<ConceptEdge> removedEdges =
        edges.stream().filter(edge -> result.removedEdgeIds().contains(edge.getId())).toList();
    conceptEdgeRepository.deleteAll(removedEdges);

    for (Concept concept : topLevelConcepts) {
      int level = result.levelByConceptId().getOrDefault(concept.getId(), 1);
      ConceptGraphService.Position position = result.positionByConceptId().get(concept.getId());
      concept.setLevel(level);
      concept.setColor(conceptGraphService.colorForLevel(level));
      concept.setX(position.x());
      concept.setY(position.y());
    }
    conceptRepository.saveAll(topLevelConcepts);
    clearPrivateTopLevelPositions(courseId);

    List<CycleEdgeDTO> cycleEdgeDtos =
        edges.stream()
            .filter(edge -> result.cycleEdgeIds().contains(edge.getId()))
            .map(
                edge ->
                    new CycleEdgeDTO(
                        edge.getId(), edge.getSource().getId(), edge.getTarget().getId()))
            .toList();
    List<RemovedEdgeDTO> removedEdgeDtos =
        removedEdges.stream()
            .map(
                edge ->
                    new RemovedEdgeDTO(
                        edge.getId(), edge.getSource().getId(), edge.getTarget().getId()))
            .toList();
    List<LevelAssignmentDTO> levelDtos =
        topLevelConcepts.stream()
            .sorted(Comparator.comparing(Concept::getId))
            .map(
                concept ->
                    new LevelAssignmentDTO(
                        concept.getId(),
                        concept.getLabel(),
                        concept.getLevel(),
                        concept.getColor(),
                        concept.getX(),
                        concept.getY()))
            .toList();

    ScaffoldResetReportDTO report =
        new ScaffoldResetReportDTO(cycleEdgeDtos, removedEdgeDtos, levelDtos);
    return new ScaffoldResetResponseDTO(report, getGraph(courseId), getEdges(courseId));
  }

  private String cleanAndValidateLabel(String label) {
    String cleanLabel = markdownService.clean(label);
    if (cleanLabel == null || cleanLabel.isEmpty()) {
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

  /**
   * The sort position for a subconcept newly added to the given parent: one past the highest
   * existing position, so new subconcepts always append at the end of the author's ordering.
   * Subconcepts predating the sort_order column may have null positions; they are ignored here
   * (they display last, after positioned rows, until the author reorders).
   */
  private int nextSortOrder(Long parentId) {
    return conceptRepository.findByParentId(parentId).stream()
            .map(Concept::getSortOrder)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(-1)
        + 1;
  }

  private void rejectDuplicateLabelUnderParent(Concept parent, String label) {
    rejectDuplicateLabelUnderParent(parent, label, null);
  }

  private void rejectDuplicateLabelUnderParent(
      Concept parent, String label, Long excludeConceptId) {
    Concept duplicate =
        conceptRepository.findByParentIdAndLabel(parent.getId(), label).orElse(null);
    if (duplicate != null && !Objects.equals(duplicate.getId(), excludeConceptId)) {
      throw new IllegalArgumentException(
          "concept %d already has a subconcept with label %s".formatted(parent.getId(), label));
    }
  }

  private void deleteConceptArtifacts(Long courseId, List<Long> conceptIds) {
    Map<Long, ConceptEdge> distinctEdges = new LinkedHashMap<>();
    for (Long id : conceptIds) {
      practiceProblemRepository.deleteAll(
          practiceProblemRepository.findByCourseIdAndConceptId(courseId, id));
      for (ConceptEdge edge : conceptEdgeRepository.findBySourceIdOrTargetId(id, id)) {
        distinctEdges.put(edge.getId(), edge);
      }
    }
    conceptEdgeRepository.deleteAll(distinctEdges.values());
  }

  /**
   * The requesting user's own private, unsaved top-level position overrides for the course (see
   * {@link UserStateV2#getTopLevelPositions()}), keyed by the concept's numeric id as a string.
   * Empty if the user has never dragged a top-level node or has no saved state for the course.
   */
  private Map<String, StoredPosition> callerPrivatePositions(Long courseId) {
    Long userId = getCurrentUser().getUser().getId();
    return userStateV2Repository
        .findByUseridAndCourseId(userId, courseId)
        .map(state -> parseTopLevelPositions(state.getTopLevelPositions()))
        .orElseGet(Map::of);
  }

  private Map<String, StoredPosition> parseTopLevelPositions(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, StoredPosition>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to parse stored top-level position overrides", e);
    }
  }

  /**
   * The x value {@link ConceptGraphService#reset} should sort each concept by: the caller's private
   * override for that concept if they have dragged it, otherwise the concept's own persisted x.
   */
  private Map<Long, Integer> priorXByConceptId(
      List<Concept> topLevelConcepts, Map<String, StoredPosition> privatePositions) {
    Map<Long, Integer> result = new HashMap<>();
    for (Concept concept : topLevelConcepts) {
      StoredPosition override = privatePositions.get(String.valueOf(concept.getId()));
      Integer x = override != null && override.x() != null ? override.x() : concept.getX();
      result.put(concept.getId(), x);
    }
    return result;
  }

  /**
   * Clears every user's private top-level position overrides for the course. Called after a
   * successful reset, since a structural change (a concept moving to a different level) can make a
   * stale private override render in the wrong row.
   */
  private void clearPrivateTopLevelPositions(Long courseId) {
    List<UserStateV2> states = userStateV2Repository.findByCourseId(courseId);
    for (UserStateV2 state : states) {
      state.setTopLevelPositions("{}");
    }
    userStateV2Repository.saveAll(states);
  }

  private record StoredPosition(Integer x, Integer y) {}

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

  public record CourseConceptDTO(
      Long id,
      String label,
      String description,
      String example,
      Integer level,
      Integer x,
      Integer y) {}

  public record MajorConceptDTO(
      Long id, String labelHtml, String color, List<SubconceptDTO> subconcepts) {}

  public record SubconceptDTO(Long id, Long parentId, String labelHtml) {}

  public record TopLevelConceptDTO(Long id, String label, Integer level, Integer x, Integer y) {}

  public record SubConceptTableRowDTO(
      Long id,
      String label,
      String description,
      String example,
      Long parentId,
      String parentLabel,
      Integer parentLevel,
      Integer parentX,
      Integer sortOrder) {}

  public record PositionDTO(Integer x, Integer y) {}

  public record EdgeDTO(Long id, Long sourceId, Long targetId, String color) {}

  public record CycleEdgeDTO(Long edgeId, Long sourceId, Long targetId) {}

  public record RemovedEdgeDTO(Long edgeId, Long sourceId, Long targetId) {}

  public record LevelAssignmentDTO(
      Long id, String label, Integer level, String color, Integer x, Integer y) {}

  public record ScaffoldResetReportDTO(
      List<CycleEdgeDTO> cycleEdges,
      List<RemovedEdgeDTO> removedEdges,
      List<LevelAssignmentDTO> levels) {}

  public record ScaffoldResetResponseDTO(
      ScaffoldResetReportDTO report, List<MajorConceptDTO> graph, List<EdgeDTO> edges) {}
}
