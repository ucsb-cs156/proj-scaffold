package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlAssessmentSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlAssessmentSetRepository extends JpaRepository<PlAssessmentSet, Long> {
  List<PlAssessmentSet> findByPlInstanceId(Long plInstanceId);

  Optional<PlAssessmentSet> findByPlInstanceIdAndAbbreviation(
      Long plInstanceId, String abbreviation);

  void deleteByPlInstanceId(Long plInstanceId);
}
