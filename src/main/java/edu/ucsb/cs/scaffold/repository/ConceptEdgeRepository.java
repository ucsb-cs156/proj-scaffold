package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptEdgeRepository extends JpaRepository<ConceptEdge, Long> {
  List<ConceptEdge> findByCourseId(Long courseId);
}
