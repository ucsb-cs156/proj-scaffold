package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptEdgeRepository extends JpaRepository<ConceptEdge, Long> {
  List<ConceptEdge> findByCourseId(Long courseId);

  Optional<ConceptEdge> findBySourceIdAndTargetId(Long sourceId, Long targetId);

  List<ConceptEdge> findBySourceIdOrTargetId(Long sourceId, Long targetId);
}
