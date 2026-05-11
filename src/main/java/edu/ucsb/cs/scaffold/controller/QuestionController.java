package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.QuestionConcept;
import edu.ucsb.cs.scaffold.repository.QuestionConceptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionConceptRepository questionConceptRepository;

    @GetMapping("/questions/{questionId}/concepts")
    public List<QuestionConcept> getQuestionConcepts(@PathVariable UUID questionId) {
        return questionConceptRepository.findByQuestionId(questionId);
    }
}
