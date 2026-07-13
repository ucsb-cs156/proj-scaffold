package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.QuestionConcept;
import edu.ucsb.cs.scaffold.repository.QuestionConceptRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Frozen copy of QuestionController serving only LegacyHomePage.tsx (via legacyClient.ts),
// so the non-legacy endpoints can change shape without touching the legacy page. Reads the
// same tables; only the endpoint contract is frozen.
@Tag(name = "Legacy Questions")
@RestController
@RequiredArgsConstructor
public class LegacyQuestionController {

  private final QuestionConceptRepository questionConceptRepository;

  @Operation(summary = "List concepts associated with a question (legacy)")
  @GetMapping("/api/legacy/questions/{questionId}/concepts")
  public List<QuestionConcept> getQuestionConcepts(
      @Parameter(description = "UUID of the question") @PathVariable UUID questionId) {
    return questionConceptRepository.findByQuestionId(questionId);
  }
}
