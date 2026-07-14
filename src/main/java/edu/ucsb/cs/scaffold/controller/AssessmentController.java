package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Serves ConceptGraphPage only (via useBackend, not legacyClient.ts). Assessments/questions come
// from the PrairieLearn-synced pl_assessment / pl_assessment_question / pl_question tables,
// scoped to the course's associated PlRepo + PlInstance, instead of the old hand-seeded
// assessments/questions tables (which now serve only LegacyHomePage via
// LegacyAssessmentController at /api/legacy/assessments).
@Tag(name = "Assessments")
@RestController
@RequiredArgsConstructor
public class AssessmentController extends ApiController {

  private final CourseRepository courseRepository;
  private final PlAssessmentRepository plAssessmentRepository;
  private final PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  private final PlQuestionRepository plQuestionRepository;

  @Operation(
      summary =
          "List the unlocked assessments available for a course's associated PrairieLearn"
              + " repo/instance")
  @GetMapping("/api/assessments")
  public List<AssessmentDTO> getAssessments(
      @Parameter(description = "Id of the course") @RequestParam Long courseId) {
    Course course = courseRepository.findById(courseId).orElse(null);
    if (course == null || course.getPlRepoId() == null || course.getPlInstanceId() == null) {
      // No PrairieLearn repo/instance associated with this course yet (or the course doesn't
      // exist): the menu shows no assessments rather than erroring.
      return List.of();
    }

    return plAssessmentRepository
        .findByPlRepoIdAndPlInstanceId(course.getPlRepoId(), course.getPlInstanceId())
        .stream()
        .filter(a -> !a.isLocked())
        .sorted(
            Comparator.comparing(
                    PlAssessment::getPlAssessmentOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlAssessment::getName))
        .map(AssessmentController::toAssessmentDTO)
        .toList();
  }

  @Operation(
      summary =
          "List all assessments (locked and unlocked) for a course, for instructor management")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @GetMapping("/api/assessments/all")
  public List<AssessmentManagementDTO> getAllAssessments(
      @Parameter(description = "Id of the course") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    if (course.getPlRepoId() == null || course.getPlInstanceId() == null) {
      // No PrairieLearn repo/instance associated with this course yet: a normal,
      // not-yet-configured state, not an error. The modal shows no assessments to manage.
      return List.of();
    }

    return plAssessmentRepository
        .findByPlRepoIdAndPlInstanceId(course.getPlRepoId(), course.getPlInstanceId())
        .stream()
        .sorted(
            Comparator.comparing(
                    PlAssessment::getPlAssessmentOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlAssessment::getName))
        .map(AssessmentController::toAssessmentManagementDTO)
        .toList();
  }

  @Operation(summary = "Lock or unlock an assessment so it is hidden from or shown to students")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PutMapping("/api/assessments/lock")
  public AssessmentManagementDTO setLocked(
      @Parameter(description = "Id of the course") @RequestParam Long courseId,
      @Parameter(description = "Id of the PlAssessment") @RequestParam Long assessmentId,
      @Parameter(description = "true to lock (hide), false to unlock (show)") @RequestParam
          boolean locked) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    PlAssessment assessment =
        plAssessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new EntityNotFoundException(PlAssessment.class, assessmentId));

    if (!Objects.equals(assessment.getPlRepoId(), course.getPlRepoId())
        || !Objects.equals(assessment.getPlInstanceId(), course.getPlInstanceId())) {
      throw new IllegalArgumentException(
          "assessmentId %d belongs to a different course".formatted(assessmentId));
    }

    assessment.setLocked(locked);
    plAssessmentRepository.save(assessment);
    return toAssessmentManagementDTO(assessment);
  }

  @Operation(
      summary = "List questions for an assessment, in the order they appear in the assessment")
  @GetMapping("/api/assessments/{assessmentId}/questions")
  public List<QuestionDTO> getQuestions(
      @Parameter(description = "Id of the PlAssessment") @PathVariable Long assessmentId) {
    List<PlAssessmentQuestion> joinRows =
        plAssessmentQuestionRepository.findByPlAssessmentIdOrderByOrdinalAsc(assessmentId);
    if (joinRows.isEmpty()) {
      return List.of();
    }

    Map<Long, PlQuestion> questionsById =
        plQuestionRepository
            .findAllById(joinRows.stream().map(PlAssessmentQuestion::getPlQuestionId).toList())
            .stream()
            .collect(Collectors.toMap(PlQuestion::getId, q -> q));

    return joinRows.stream()
        .map(join -> questionsById.get(join.getPlQuestionId()))
        .filter(Objects::nonNull)
        .map(q -> toQuestionDTO(q, assessmentId))
        .toList();
  }

  private static AssessmentDTO toAssessmentDTO(PlAssessment a) {
    return new AssessmentDTO(
        String.valueOf(a.getId()),
        a.getPlAssessmentId() != null ? String.valueOf(a.getPlAssessmentId()) : null,
        assessmentLabel(a),
        a.getPlAssessmentSetAbbreviation(),
        a.getPlAssessmentNumber(),
        a.getPlAssessmentSetColor());
  }

  private static AssessmentManagementDTO toAssessmentManagementDTO(PlAssessment a) {
    return new AssessmentManagementDTO(
        String.valueOf(a.getId()),
        assessmentLabel(a),
        a.isLocked(),
        a.getPlAssessmentSetAbbreviation(),
        a.getPlAssessmentNumber(),
        a.getPlAssessmentSetColor());
  }

  private static String assessmentLabel(PlAssessment a) {
    return a.getPlAssessmentTitle() != null ? a.getPlAssessmentTitle() : a.getName();
  }

  private static QuestionDTO toQuestionDTO(PlQuestion q, Long assessmentId) {
    return new QuestionDTO(
        String.valueOf(q.getId()),
        String.valueOf(assessmentId),
        q.getUuid().toString(),
        q.getTitle());
  }

  public record AssessmentDTO(
      String id,
      @JsonProperty("pl_assessment_id") String plAssessmentId,
      String name,
      @JsonProperty("pl_assessment_set_abbreviation") String plAssessmentSetAbbreviation,
      @JsonProperty("pl_assessment_number") String plAssessmentNumber,
      @JsonProperty("pl_assessment_set_color") String plAssessmentSetColor) {}

  public record AssessmentManagementDTO(
      String id,
      String name,
      boolean locked,
      @JsonProperty("pl_assessment_set_abbreviation") String plAssessmentSetAbbreviation,
      @JsonProperty("pl_assessment_number") String plAssessmentNumber,
      @JsonProperty("pl_assessment_set_color") String plAssessmentSetColor) {}

  public record QuestionDTO(
      String id,
      @JsonProperty("assessment_id") String assessmentId,
      @JsonProperty("pl_question_uuid") String plQuestionUuid,
      String title) {}
}
