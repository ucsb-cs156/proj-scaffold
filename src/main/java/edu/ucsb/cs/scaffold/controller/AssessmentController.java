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

@Tag(name = "Assessments")
@RestController
@RequiredArgsConstructor
public class AssessmentController {

  @Autowired private final AssessmentRepository assessmentRepository;

  @Autowired private final QuestionRepository questionRepository;

  @Operation(summary = "List all assessments ordered by name")
  @GetMapping("/api/assessments")
  public List<Assessment> getAssessments() {
    return assessmentRepository.findAllByOrderByNameAsc();
  }

  @Operation(summary = "List questions for an assessment ordered by title")
  @GetMapping("/api/assessments/{assessmentId}/questions")
  public List<Question> getQuestions(
      @Parameter(description = "UUID of the assessment") @PathVariable UUID assessmentId) {
    return questionRepository.findByAssessmentIdOrderByTitleAsc(assessmentId);
  }
}
