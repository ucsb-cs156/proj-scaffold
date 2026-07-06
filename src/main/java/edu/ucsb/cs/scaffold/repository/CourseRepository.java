package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

  List<Course> findByInstructorEmail(String instructorEmail);
}
