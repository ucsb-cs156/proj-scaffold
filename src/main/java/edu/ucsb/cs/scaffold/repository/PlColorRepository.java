package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlColorRepository extends JpaRepository<PlColor, String> {}
