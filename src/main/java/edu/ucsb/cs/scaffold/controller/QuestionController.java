package edu.ucsb.cs.scaffold.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Serves ConceptGraphPage only (via useBackend, not legacyClient.ts). Concept highlighting for a
// selected question is dark until PL questions have their own concept tagging: the old
// question_concepts table is keyed by the legacy questions.id UUID, which has no relationship to
// PlQuestion ids, and must not be bridged between the two. LegacyQuestionController (frozen, at
// /api/legacy/questions/{id}/concepts) still serves the legacy table for LegacyHomePage. No DTO
// is declared here since nothing is ever constructed; add one when real tagging lands.
@Tag(name = "Questions")
@RestController
public class QuestionController {

  @Operation(
      summary =
          "List concepts tagged on a question (currently always empty; PL question concept"
              + " tagging doesn't exist yet)")
  @GetMapping("/api/questions/{questionId}/concepts")
  public List<Object> getQuestionConcepts(
      @Parameter(description = "Id of the PlQuestion") @PathVariable Long questionId) {
    return List.of();
  }
}
