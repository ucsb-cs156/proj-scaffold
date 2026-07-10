package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.PlQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlQuestionRepository extends JpaRepository<PlQuestion, Long> {
  List<PlQuestion> findByPlRepoId(Long plRepoId);

  Optional<PlQuestion> findByPlRepoIdAndQuestionId(Long plRepoId, String questionId);

  boolean existsByPlRepoIdAndQuestionId(Long plRepoId, String questionId);

  void deleteByPlRepoId(Long plRepoId);
}
