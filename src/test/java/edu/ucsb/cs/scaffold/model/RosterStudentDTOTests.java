package edu.ucsb.cs.scaffold.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.RosterStatus;
import org.junit.jupiter.api.Test;

/** This is a test class for the RosterStudentDTO class. */
public class RosterStudentDTOTests {

  @Test
  public void test_from() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    User user = User.builder().build();
    user.setId(2L);

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setId(3L);
    rosterStudent.setCourse(course);
    rosterStudent.setStudentId("U123456");
    rosterStudent.setFirstName("John");
    rosterStudent.setLastName("Doe");
    rosterStudent.setEmail("johndoe@example.com");
    rosterStudent.setSection("Section A");
    rosterStudent.setUser(user);
    rosterStudent.setRosterStatus(RosterStatus.ROSTER);

    // Act

    RosterStudentDTO dto = new RosterStudentDTO(rosterStudent);
    // Assert

    assertEquals(3L, dto.id());
    assertEquals(1L, dto.courseId());
    assertEquals("U123456", dto.studentId());
    assertEquals("John", dto.firstName());
    assertEquals("Doe", dto.lastName());
    assertEquals("johndoe@example.com", dto.email());
    assertEquals("Section A", dto.section());
    assertEquals(2L, dto.userId());
    assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
  }

  @Test
  public void test_from_when_user_is_null() {
    // Arrange
    Course course = new Course();
    course.setId(1L);

    RosterStudent rosterStudent = new RosterStudent();
    rosterStudent.setId(3L);
    rosterStudent.setCourse(course);
    rosterStudent.setStudentId("U123456");
    rosterStudent.setFirstName("John");
    rosterStudent.setLastName("Doe");
    rosterStudent.setEmail("johndoe@example.com");
    rosterStudent.setSection("Section B");
    rosterStudent.setUser(null);
    rosterStudent.setRosterStatus(RosterStatus.ROSTER);

    // Act
    RosterStudentDTO dto = new RosterStudentDTO(rosterStudent);

    // Assert
    assertEquals(3L, dto.id());
    assertEquals(1L, dto.courseId());
    assertEquals("U123456", dto.studentId());
    assertEquals("John", dto.firstName());
    assertEquals("Doe", dto.lastName());
    assertEquals("johndoe@example.com", dto.email());
    assertEquals("Section B", dto.section());
    assertEquals(0L, dto.userId());
    assertEquals(RosterStatus.ROSTER, dto.rosterStatus());
  }
}
