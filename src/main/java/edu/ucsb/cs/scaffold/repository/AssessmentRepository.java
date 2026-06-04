package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findAllByOrderByNameAsc();
}
