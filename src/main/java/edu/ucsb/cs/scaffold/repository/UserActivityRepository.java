package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {
}
