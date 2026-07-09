package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.Concept;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
  List<Concept> findByCourseId(Long courseId);

  List<Concept> findByParentId(Long parentId);

  Optional<Concept> findByParentIdAndLabel(Long parentId, String label);
}
