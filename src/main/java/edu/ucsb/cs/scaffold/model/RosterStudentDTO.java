package edu.ucsb.cs.scaffold.model;

import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.enums.RosterStatus;

/**
 * This is a DTO class that represents a student in the roster. It is used to transfer data between
 * the server and the client.
 */
public record RosterStudentDTO(
    Long id,
    Long courseId,
    String studentId,
    String firstName,
    String lastName,
    String email,
    String section,
    long userId,
    RosterStatus rosterStatus) {

  public RosterStudentDTO(RosterStudent rosterStudent) {
    this(
        rosterStudent.getId(),
        rosterStudent.getCourse().getId(),
        rosterStudent.getStudentId(),
        rosterStudent.getFirstName(),
        rosterStudent.getLastName(),
        rosterStudent.getEmail(),
        rosterStudent.getSection(),
        rosterStudent.getUser() != null ? rosterStudent.getUser().getId() : 0,
        rosterStudent.getRosterStatus());
  }
}
