package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestionConcept;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.ConceptRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionConceptRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Tags Concepts onto a PlAssessmentQuestion (a PlQuestion's occurrence within one PlAssessment),
// so instructors can associate concepts with the questions students practice. Serves
// ConceptGraphPage's question-driven concept highlighting; replaces the QuestionController stub
// that this feature (issue #116) makes obsolete.
@Tag(name = "PlAssessmentQuestion")
@RequestMapping("/api/plAssessmentQuestion")
@RestController
@RequiredArgsConstructor
public class PLAssessmentQuestionController extends ApiController {

  private final ConceptRepository conceptRepository;
  private final PlAssessmentRepository plAssessmentRepository;
  private final PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  private final PlAssessmentQuestionConceptRepository plAssessmentQuestionConceptRepository;

  @Operation(summary = "List concepts tagged on a PlAssessmentQuestion")
  @GetMapping("/{plAssessmentQuestionId}/concepts")
  public List<TaggedConceptDTO> getConcepts(
      @Parameter(description = "Id of the PlAssessmentQuestion") @PathVariable
          Long plAssessmentQuestionId) {
    return plAssessmentQuestionConceptRepository
        .findByPlAssessmentQuestionId(plAssessmentQuestionId)
        .stream()
        .map(PLAssessmentQuestionController::toTaggedConceptDTO)
        .toList();
  }

  @Operation(
      summary =
          "Tag a Concept onto a PlAssessmentQuestion; the concept's course must be associated"
              + " with the same PrairieLearn instance as the assessment")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @PostMapping("/addConcept")
  public PlAssessmentQuestionConcept addConcept(
      @Parameter(description = "Id of the PlAssessmentQuestion") @RequestParam
          Long plAssessmentQuestionId,
      @Parameter(description = "Id of the Concept") @RequestParam Long conceptId)
      throws EntityNotFoundException {
    Concept concept =
        conceptRepository
            .findById(conceptId)
            .orElseThrow(() -> new EntityNotFoundException(Concept.class, conceptId));
    PlAssessmentQuestion plAssessmentQuestion =
        plAssessmentQuestionRepository
            .findById(plAssessmentQuestionId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        PlAssessmentQuestion.class, plAssessmentQuestionId));
    PlAssessment plAssessment =
        plAssessmentRepository
            .findById(plAssessmentQuestion.getPlAssessmentId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        PlAssessment.class, plAssessmentQuestion.getPlAssessmentId()));

    if (!Objects.equals(concept.getCourse().getPlInstanceId(), plAssessment.getPlInstanceId())) {
      throw new IllegalArgumentException(
          "concept %d's course PL instance does not match assessment %d's PL instance"
              .formatted(conceptId, plAssessment.getId()));
    }

    PlAssessmentQuestionConcept tag =
        PlAssessmentQuestionConcept.builder()
            .plAssessmentQuestionId(plAssessmentQuestionId)
            .concept(concept)
            .build();
    return plAssessmentQuestionConceptRepository.save(tag);
  }

  @Operation(summary = "Remove a Concept tag from a PlAssessmentQuestion")
  @PreAuthorize("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")
  @DeleteMapping("/deleteConcept")
  public Object deleteConcept(
      @Parameter(description = "Id of the PlAssessmentQuestion") @RequestParam
          Long plAssessmentQuestionId,
      @Parameter(description = "Id of the Concept") @RequestParam Long conceptId)
      throws EntityNotFoundException {
    PlAssessmentQuestionConcept tag =
        plAssessmentQuestionConceptRepository
            .findByPlAssessmentQuestionIdAndConceptId(plAssessmentQuestionId, conceptId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        PlAssessmentQuestionConcept.class,
                        "plAssessmentQuestionId=%d, conceptId=%d"
                            .formatted(plAssessmentQuestionId, conceptId)));
    plAssessmentQuestionConceptRepository.delete(tag);
    return genericMessage(
        "Concept %d untagged from PlAssessmentQuestion %d"
            .formatted(conceptId, plAssessmentQuestionId));
  }

  // Overrides ApiController's IllegalArgumentException -> 400 mapping for this controller only:
  // the mismatched-PL-instance check above is a 422 (Unprocessable Entity) per issue #116, since
  // the request is well-formed but semantically invalid, not malformed.
  @ExceptionHandler({IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public Object handleIllegalArgument(Throwable e) {
    return Map.of(
        "type", e.getClass().getSimpleName(),
        "message", e.getMessage());
  }

  private static TaggedConceptDTO toTaggedConceptDTO(PlAssessmentQuestionConcept tag) {
    return new TaggedConceptDTO(
        String.valueOf(tag.getId()),
        String.valueOf(tag.getPlAssessmentQuestionId()),
        String.valueOf(tag.getConcept().getId()),
        null);
  }

  public record TaggedConceptDTO(
      String id,
      @JsonProperty("question_id") String questionId,
      @JsonProperty("concept_id") String conceptId,
      @JsonProperty("subconcept_label") String subconceptLabel) {}
}
