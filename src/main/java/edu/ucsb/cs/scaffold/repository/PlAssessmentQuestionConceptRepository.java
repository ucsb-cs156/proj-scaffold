package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestionConcept;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlAssessmentQuestionConceptRepository
    extends JpaRepository<PlAssessmentQuestionConcept, Long> {
  List<PlAssessmentQuestionConcept> findByPlAssessmentQuestionId(Long plAssessmentQuestionId);

  Optional<PlAssessmentQuestionConcept> findByPlAssessmentQuestionIdAndConceptId(
      Long plAssessmentQuestionId, Long conceptId);
}
