package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.QuestionConcept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionConceptRepository extends JpaRepository<QuestionConcept, UUID> {

    List<QuestionConcept> findByQuestionId(UUID questionId);
}
