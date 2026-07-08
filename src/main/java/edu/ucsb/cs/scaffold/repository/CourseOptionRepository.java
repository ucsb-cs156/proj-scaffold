package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.CourseOption;
import edu.ucsb.cs.scaffold.entity.CourseOptionKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOptionRepository extends JpaRepository<CourseOption, CourseOptionKey> {
  List<CourseOption> findByCourseId(Long courseId);

  Optional<CourseOption> findByCourseIdAndOption(Long courseId, String option);
}
