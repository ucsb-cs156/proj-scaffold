package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.InsertStatus;
import edu.ucsb.cs.scaffold.enums.RosterStatus;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.RosterStudentDTO;
import edu.ucsb.cs.scaffold.model.UpsertResponse;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs.scaffold.utilities.CanonicalFormConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "RosterStudents")
@RequestMapping("/api/rosterstudents")
@RestController
@Slf4j
public class RosterStudentsController extends ApiController {

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  /**
   * This method creates a new RosterStudent. It is important to keep the code in this method
   * consistent with the code for adding multiple roster students from a CSV
   *
   * @return the created RosterStudent
   */
  @Operation(summary = "Create a new roster student")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/post")
  public ResponseEntity<UpsertResponse> postRosterStudent(
      @Parameter(name = "studentId") @RequestParam String studentId,
      @Parameter(name = "firstName") @RequestParam String firstName,
      @Parameter(name = "lastName") @RequestParam String lastName,
      @Parameter(name = "email") @RequestParam String email,
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "section") @RequestParam(required = false) String section)
      throws EntityNotFoundException {

    // Get Course or else throw an error

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    RosterStudent rosterStudent =
        RosterStudent.builder()
            .studentId(studentId)
            .firstName(firstName)
            .lastName(lastName)
            .email(email.strip())
            .section(section != null ? section : "")
            .build();

    UpsertResponse upsertResponse = upsertStudent(rosterStudent, course, RosterStatus.MANUAL);
    if (upsertResponse.getInsertStatus() == InsertStatus.REJECTED) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(upsertResponse);
    } else {
      rosterStudent = rosterStudentRepository.save(upsertResponse.rosterStudent());
      updateUserService.attachUserToRosterStudent(rosterStudent);
      return ResponseEntity.ok(upsertResponse);
    }
  }

  /**
   * This method returns a list of roster students for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all roster students for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course/{courseId}")
  public Iterable<RosterStudentDTO> rosterStudentForCourse(
      @Parameter(name = "courseId") @PathVariable Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Iterable<RosterStudent> rosterStudents =
        rosterStudentRepository.findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(courseId);
    Iterable<RosterStudentDTO> rosterStudentDTOs =
        () ->
            java.util.stream.StreamSupport.stream(rosterStudents.spliterator(), false)
                .map(RosterStudentDTO::new)
                .iterator();
    return rosterStudentDTOs;
  }

  public static UpsertResponse upsertStudent(
      RosterStudent student, Course course, RosterStatus rosterStatus) {
    String convertedEmail = CanonicalFormConverter.convertToValidEmail(student.getEmail()).strip();
    Optional<RosterStudent> existingStudent =
        course.getRosterStudents().stream()
            .filter(
                filteringStudent -> student.getStudentId().equals(filteringStudent.getStudentId()))
            .findFirst();
    Optional<RosterStudent> existingStudentByEmail =
        course.getRosterStudents().stream()
            .filter(filteringStudent -> convertedEmail.equals(filteringStudent.getEmail()))
            .findFirst();
    if (existingStudent.isPresent() && existingStudentByEmail.isPresent()) {
      if (existingStudent.get().getId().equals(existingStudentByEmail.get().getId())) {
        RosterStudent existingStudentObj = existingStudent.get();
        existingStudentObj.setRosterStatus(rosterStatus);
        existingStudentObj.setFirstName(student.getFirstName());
        existingStudentObj.setLastName(student.getLastName());
        existingStudentObj.setSection(student.getSection());
        return new UpsertResponse(InsertStatus.UPDATED, existingStudentObj);
      } else {
        return new UpsertResponse(InsertStatus.REJECTED, student);
      }
    } else if (existingStudent.isPresent() || existingStudentByEmail.isPresent()) {
      RosterStudent existingStudentObj =
          existingStudent.isPresent() ? existingStudent.get() : existingStudentByEmail.get();
      existingStudentObj.setRosterStatus(rosterStatus);
      existingStudentObj.setFirstName(student.getFirstName());
      existingStudentObj.setLastName(student.getLastName());
      existingStudentObj.setSection(student.getSection());
      existingStudentObj.setEmail(convertedEmail);
      existingStudentObj.setStudentId(student.getStudentId());
      return new UpsertResponse(InsertStatus.UPDATED, existingStudentObj);
    } else {
      student.setCourse(course);
      student.setEmail(convertedEmail);
      student.setRosterStatus(rosterStatus);
      return new UpsertResponse(InsertStatus.INSERTED, student);
    }
  }

  @Operation(summary = "Get Associated Roster Students with a User")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/associatedRosterStudents")
  public Iterable<RosterStudent> getAssociatedRosterStudents() {
    User currentUser = currentUserService.getUser();
    Iterable<RosterStudent> rosterStudents = rosterStudentRepository.findAllByUser((currentUser));
    return rosterStudents;
  }

  @Operation(summary = "Update a roster student")
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  @PutMapping("/update")
  public RosterStudent updateRosterStudent(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "firstName") @RequestParam(required = false) String firstName,
      @Parameter(name = "lastName") @RequestParam(required = false) String lastName,
      @Parameter(name = "studentId") @RequestParam(required = false) String studentId,
      @Parameter(name = "section") @RequestParam(required = false) String section)
      throws EntityNotFoundException {

    if (firstName == null
        || lastName == null
        || studentId == null
        || firstName.trim().isEmpty()
        || lastName.trim().isEmpty()
        || studentId.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required fields cannot be empty");
    }

    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));

    if (!rosterStudent.getStudentId().trim().equals(studentId.trim())) {
      Optional<RosterStudent> existingStudent =
          rosterStudentRepository.findByCourseIdAndStudentId(
              rosterStudent.getCourse().getId(), studentId.trim());
      if (existingStudent.isPresent()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Student ID already exists in this course");
      }
    }

    rosterStudent.setFirstName(firstName.trim());
    rosterStudent.setLastName(lastName.trim());
    rosterStudent.setStudentId(studentId.trim());

    if (section != null) {
      rosterStudent.setSection(section.trim());
    }

    return rosterStudentRepository.save(rosterStudent);
  }

  @Operation(
      summary = "Restore a roster student",
      description = "Restores a student who previously was dropped from the course")
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  @PutMapping("/restore")
  public RosterStudent restoreRosterStudent(@Parameter(name = "id") @RequestParam Long id)
      throws EntityNotFoundException {
    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));
    rosterStudent.setRosterStatus(RosterStatus.MANUAL);
    return rosterStudentRepository.save(rosterStudent);
  }

  @Operation(summary = "Delete a roster student")
  @PreAuthorize("@CourseSecurity.hasRosterStudentManagementPermissions(#root, #id)")
  @DeleteMapping("/delete")
  @Transactional
  public ResponseEntity<String> deleteRosterStudent(@Parameter(name = "id") @RequestParam Long id)
      throws EntityNotFoundException {
    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, id));

    rosterStudent.getCourse().getRosterStudents().remove(rosterStudent);
    rosterStudent.setCourse(null);
    rosterStudentRepository.delete(rosterStudent);

    return ResponseEntity.ok(
        "Successfully deleted roster student and removed him/her from the course list");
  }
}
