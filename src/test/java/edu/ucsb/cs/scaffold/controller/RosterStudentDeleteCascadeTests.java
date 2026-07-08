package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Regression tests for the JPA cascade semantics that {@link
 * RosterStudentsController#deleteRosterStudent} depends on.
 *
 * <p>The controller's mocked MockMvc tests ({@link RosterStudentsControllerTests}) verify that the
 * repository's {@code delete} method is called, but mocks cannot detect a subtle Hibernate hazard:
 * {@code Course.rosterStudents} is mapped with {@code cascade = CascadeType.ALL}, so if a {@code
 * RosterStudent} is deleted while it is still present in its managed {@code Course}'s collection,
 * Hibernate's cascade silently <em>cancels</em> the delete at flush time (an {@code UPDATE} is
 * issued instead of a {@code DELETE}, and the row survives).
 *
 * <p>These tests run the delete flow against a real embedded database so that a future refactoring
 * of {@code deleteRosterStudent} (in particular, removal of the line that takes the student out of
 * {@code course.getRosterStudents()}) cannot silently break deletion.
 */
@DataJpaTest
public class RosterStudentDeleteCascadeTests {

  @Autowired RosterStudentRepository rosterStudentRepository;
  @Autowired CourseRepository courseRepository;
  @Autowired EntityManager em;

  /**
   * Persists a course containing one roster student, then clears the persistence context so that
   * subsequent lookups load fresh managed entities (as they would in a real request).
   *
   * @return the id of the persisted roster student
   */
  private Long setupCourseWithStudent() {
    Course course = Course.builder().courseName("CS156").term("F25").build();
    course = courseRepository.save(course);
    RosterStudent rosterStudent =
        RosterStudent.builder()
            .studentId("A1234567")
            .firstName("Test")
            .lastName("Student")
            .email("test@ucsb.edu")
            .course(course)
            .build();
    rosterStudent = rosterStudentRepository.save(rosterStudent);
    em.flush();
    em.clear();
    return rosterStudent.getId();
  }

  /**
   * Replicates the body of {@code RosterStudentsController.deleteRosterStudent} against a real
   * database and asserts that the row is actually removed. Removing the student from the course's
   * cascading collection before calling {@code delete} is what makes the delete take effect; this
   * test fails if that step is removed.
   */
  @Test
  public void delete_afterRemovalFromCourseCollection_removesRow() {
    Long id = setupCourseWithStudent();

    RosterStudent rosterStudent = rosterStudentRepository.findById(id).orElseThrow();
    rosterStudent.getCourse().getRosterStudents().remove(rosterStudent);
    rosterStudentRepository.delete(rosterStudent);
    em.flush();
    em.clear();

    assertTrue(
        rosterStudentRepository.findById(id).isEmpty(),
        "roster student row should be deleted from the database");
  }

  /**
   * Documents the hazard the controller code guards against: if the student is <em>not</em> removed
   * from the course's {@code cascade = CascadeType.ALL} collection first, Hibernate cancels the
   * delete at flush time and the row survives. If this test ever fails, Hibernate's cascade
   * semantics have changed and the delete flow in {@code RosterStudentsController} should be
   * re-examined.
   */
  @Test
  public void delete_whileStillInCourseCollection_isCancelledByCascade() {
    Long id = setupCourseWithStudent();

    RosterStudent rosterStudent = rosterStudentRepository.findById(id).orElseThrow();
    // intentionally NOT removed from rosterStudent.getCourse().getRosterStudents()
    rosterStudentRepository.delete(rosterStudent);
    em.flush();
    em.clear();

    assertTrue(
        rosterStudentRepository.findById(id).isPresent(),
        "delete is expected to be cancelled by the cascade from Course.rosterStudents");
  }
}
