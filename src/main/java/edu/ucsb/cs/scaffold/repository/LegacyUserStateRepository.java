package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.LegacyUserState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyUserStateRepository extends JpaRepository<LegacyUserState, UUID> {

  Optional<LegacyUserState> findByUserid(Long userid);
}
