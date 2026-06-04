package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.UserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserStateRepository extends JpaRepository<UserState, UUID> {

    Optional<UserState> findByPin(String pin);
}
