package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlAssessmentQuestionRepository extends JpaRepository<PlAssessmentQuestion, Long> {
  List<PlAssessmentQuestion> findByPlAssessmentIdOrderByOrdinalAsc(Long plAssessmentId);

  void deleteByPlAssessmentId(Long plAssessmentId);

  void deleteByPlQuestionId(Long plQuestionId);

  void deleteByPlRepoId(Long plRepoId);
}
