package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.model.Assessment;
import edu.ucsb.cs.scaffold.model.Question;
import edu.ucsb.cs.scaffold.repository.AssessmentRepository;
import edu.ucsb.cs.scaffold.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;

    @GetMapping("/assessments")
    public List<Assessment> getAssessments() {
        return assessmentRepository.findAllByOrderByNameAsc();
    }

    @GetMapping("/assessments/{assessmentId}/questions")
    public List<Question> getQuestions(@PathVariable UUID assessmentId) {
        return questionRepository.findByAssessmentIdOrderByTitleAsc(assessmentId);
    }
}
