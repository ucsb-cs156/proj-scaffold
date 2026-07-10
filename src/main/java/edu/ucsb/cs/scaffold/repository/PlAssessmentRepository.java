package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlAssessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlAssessmentRepository extends JpaRepository<PlAssessment, Long> {
  List<PlAssessment> findByPlRepoIdAndPlInstanceId(Long plRepoId, Long plInstanceId);

  boolean existsByPlRepoIdAndPlInstanceIdAndName(Long plRepoId, Long plInstanceId, String name);

  void deleteByPlRepoId(Long plRepoId);

  void deleteByPlInstanceId(Long plInstanceId);
}
