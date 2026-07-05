package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.Section;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {
  List<Section> findByCourseId(Long courseId);
}
