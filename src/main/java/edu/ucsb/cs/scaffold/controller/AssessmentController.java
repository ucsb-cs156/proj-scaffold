package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class AssessmentController {

  private final CourseRepository courseRepository;
  private final PlAssessmentRepository plAssessmentRepository;
  private final PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  private final PlQuestionRepository plQuestionRepository;

  @Operation(
      summary =
          "List the assessments available for a course's associated PrairieLearn repo/instance")
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
        .sorted(
            Comparator.comparing(
                    PlAssessment::getPlAssessmentOrder,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PlAssessment::getName))
        .map(AssessmentController::toAssessmentDTO)
        .toList();
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
    String label = a.getPlAssessmentTitle() != null ? a.getPlAssessmentTitle() : a.getName();
    return new AssessmentDTO(
        String.valueOf(a.getId()),
        a.getPlAssessmentId() != null ? String.valueOf(a.getPlAssessmentId()) : null,
        label);
  }

  private static QuestionDTO toQuestionDTO(PlQuestion q, Long assessmentId) {
    return new QuestionDTO(
        String.valueOf(q.getId()),
        String.valueOf(assessmentId),
        q.getUuid().toString(),
        q.getTitle());
  }

  public record AssessmentDTO(
      String id, @JsonProperty("pl_assessment_id") String plAssessmentId, String name) {}

  public record QuestionDTO(
      String id,
      @JsonProperty("assessment_id") String assessmentId,
      @JsonProperty("pl_question_uuid") String plQuestionUuid,
      String title) {}
}
