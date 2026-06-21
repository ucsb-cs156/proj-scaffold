package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.Instructor;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorRepository extends CrudRepository<Instructor, String> {
  Optional<Instructor> findByEmail(String email);

  boolean existsByEmail(String email);
}
