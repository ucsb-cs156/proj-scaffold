package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlRepo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlRepoRepository extends JpaRepository<PlRepo, Long> {
  Optional<PlRepo> findByRepoName(String repoName);

  boolean existsByRepoName(String repoName);
}
