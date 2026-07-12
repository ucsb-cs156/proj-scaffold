package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.Assessment;
import edu.ucsb.cs.scaffold.model.Question;
import edu.ucsb.cs.scaffold.repository.AssessmentRepository;
import edu.ucsb.cs.scaffold.repository.QuestionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Frozen copy of AssessmentController serving only LegacyHomePage.tsx (via legacyClient.ts),
// so the non-legacy endpoints can change shape without touching the legacy page. Reads the
// same tables; only the endpoint contract is frozen.
@Tag(name = "Legacy Assessments")
@RestController
@RequiredArgsConstructor
public class LegacyAssessmentController {

  @Autowired private final AssessmentRepository assessmentRepository;

  @Autowired private final QuestionRepository questionRepository;

  @Operation(summary = "List all assessments ordered by name (legacy)")
  @GetMapping("/api/legacy/assessments")
  public List<Assessment> getAssessments() {
    return assessmentRepository.findAllByOrderByNameAsc();
  }

  @Operation(summary = "List questions for an assessment ordered by title (legacy)")
  @GetMapping("/api/legacy/assessments/{assessmentId}/questions")
  public List<Question> getQuestions(
      @Parameter(description = "UUID of the assessment") @PathVariable UUID assessmentId) {
    return questionRepository.findByAssessmentIdOrderByTitleAsc(assessmentId);
  }
}
