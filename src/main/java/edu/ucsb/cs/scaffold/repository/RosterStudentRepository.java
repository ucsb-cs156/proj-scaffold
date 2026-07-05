package edu.ucsb.cs.scaffold.repository;

import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.RosterStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RosterStudentRepository extends JpaRepository<RosterStudent, Long> {
  List<RosterStudent> findAllByEmail(String email);

  public Iterable<RosterStudent> findByCourseId(Long courseId);

  @Query(
      """
          SELECT r
          FROM RosterStudent r
          WHERE r.course.id = :courseId
          ORDER BY LOWER(r.firstName), LOWER(r.lastName)
      """)
  Iterable<RosterStudent> findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(Long courseId);

  @Query(
      """
          SELECT r
          FROM RosterStudent r
          WHERE r.course.id = :courseId
            AND r.rosterStatus in :statuses
          ORDER BY LOWER(r.firstName), LOWER(r.lastName)
      """)
  List<RosterStudent> findByCourseIdAndRosterStatusInOrderByFirstNameAscLastNameAscIgnoreCase(
      Long courseId, List<RosterStatus> statuses);

  public Optional<RosterStudent> findByCourseIdAndStudentId(Long courseId, String studentId);

  public Optional<RosterStudent> findByCourseIdAndEmail(Long courseId, String email);

  Iterable<RosterStudent> findAllByUser(User user);
}
