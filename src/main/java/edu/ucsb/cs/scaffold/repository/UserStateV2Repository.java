package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.UserStateV2;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStateV2Repository extends JpaRepository<UserStateV2, UUID> {

  Optional<UserStateV2> findByUseridAndCourseId(Long userid, Long courseId);
}
