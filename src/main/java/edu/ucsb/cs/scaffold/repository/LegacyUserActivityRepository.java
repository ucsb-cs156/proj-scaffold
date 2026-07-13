package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.LegacyUserActivity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyUserActivityRepository extends JpaRepository<LegacyUserActivity, UUID> {}
