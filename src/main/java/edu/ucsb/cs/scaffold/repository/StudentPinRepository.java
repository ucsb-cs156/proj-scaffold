package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.model.StudentPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentPinRepository extends JpaRepository<StudentPin, UUID> {

    List<StudentPin> findByPin(String pin);
}
