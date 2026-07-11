package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatCredentialRepository extends JpaRepository<PatCredential, Long> {
  Optional<PatCredential> findByUserId(long userId);
}
