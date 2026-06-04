package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.QuestionConcept;
import edu.ucsb.cs.scaffold.repository.QuestionConceptRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Questions")
@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionConceptRepository questionConceptRepository;

    @Operation(summary = "List concepts associated with a question")
    @GetMapping("/api/questions/{questionId}/concepts")
    public List<QuestionConcept> getQuestionConcepts(
            @Parameter(description = "UUID of the question") @PathVariable UUID questionId) {
        return questionConceptRepository.findByQuestionId(questionId);
    }
}
