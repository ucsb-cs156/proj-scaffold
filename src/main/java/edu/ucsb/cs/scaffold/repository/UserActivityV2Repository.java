package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.UserActivityV2;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityV2Repository extends JpaRepository<UserActivityV2, UUID> {}
