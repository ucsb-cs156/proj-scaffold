package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.StudentUserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentUserIdRepository extends JpaRepository<StudentUserId, UUID> {

    List<StudentUserId> findByUserid(String userid);
}
