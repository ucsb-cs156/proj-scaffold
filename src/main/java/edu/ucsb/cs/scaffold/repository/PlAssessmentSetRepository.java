package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlAssessmentSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlAssessmentSetRepository extends JpaRepository<PlAssessmentSet, Long> {
  List<PlAssessmentSet> findByPlRepoId(Long plRepoId);

  Optional<PlAssessmentSet> findByPlRepoIdAndAbbreviation(Long plRepoId, String abbreviation);

  void deleteByPlRepoId(Long plRepoId);
}
