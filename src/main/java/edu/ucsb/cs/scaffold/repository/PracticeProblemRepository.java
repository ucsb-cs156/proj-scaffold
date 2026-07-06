package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PracticeProblem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeProblemRepository extends JpaRepository<PracticeProblem, Long> {
  List<PracticeProblem> findByCourseId(Long courseId);

  List<PracticeProblem> findByCourseIdAndConceptId(Long courseId, Long conceptId);

  Optional<PracticeProblem> findByCourseIdAndConceptIdAndUrl(
      Long courseId, Long conceptId, String url);
}
