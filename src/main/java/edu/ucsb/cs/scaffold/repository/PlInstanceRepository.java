package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlInstanceRepository extends JpaRepository<PlInstance, Long> {
  List<PlInstance> findByPlRepoId(Long plRepoId);

  Optional<PlInstance> findByPlRepoIdAndShortName(Long plRepoId, String name);

  boolean existsByPlRepoIdAndShortName(Long plRepoId, String name);

  void deleteByPlRepoId(Long plRepoId);
}
