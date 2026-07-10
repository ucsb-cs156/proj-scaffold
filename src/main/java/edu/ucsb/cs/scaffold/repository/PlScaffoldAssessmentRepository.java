package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlScaffoldAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlScaffoldAssessmentRepository extends JpaRepository<PlScaffoldAssessment, Long> {
  Optional<PlScaffoldAssessment> findByPlQuestionIdAndPlInstanceId(
      Long plQuestionId, Long plInstanceId);

  boolean existsByPlRepoIdAndPlInstanceIdAndPlQuestionId(
      Long plRepoId, Long plInstanceId, Long plQuestionId);

  void deleteByPlRepoId(Long plRepoId);

  void deleteByPlInstanceId(Long plInstanceId);

  void deleteByPlQuestionId(Long plQuestionId);
}
