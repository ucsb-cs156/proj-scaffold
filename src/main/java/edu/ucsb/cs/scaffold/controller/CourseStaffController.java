package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.CourseStaff;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseStaff")
@RequestMapping("/api/coursestaff")
@RestController
@Slf4j
public class CourseStaffController extends ApiController {

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CurrentUserService currentUserService;

  /**
   * This method creates a new CourseStaff.
   *
   * @return the created CourseStaff
   */
  @Operation(summary = "Add a staff member to a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("/post")
  public CourseStaff postCourseStaff(
      @Parameter(name = "firstName") @RequestParam String firstName,
      @Parameter(name = "lastName") @RequestParam String lastName,
      @Parameter(name = "email") @RequestParam String email,
      @Parameter(name = "courseId") @RequestParam Long courseId)
      throws EntityNotFoundException {

    // Get Course or else throw an error

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    CourseStaff courseStaff =
        CourseStaff.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email.strip())
            .course(course)
            .build();

    CourseStaff savedCourseStaff = courseStaffRepository.save(courseStaff);

    updateUserService.attachUserToCourseStaff(savedCourseStaff);

    return savedCourseStaff;
  }

  /**
   * This method returns a list of course staff for a given course.
   *
   * @return a list of all courses.
   */
  @Operation(summary = "List all course staff members for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course")
  public Iterable<CourseStaff> courseStaffForCourse(
      @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    Iterable<CourseStaff> courseStaffs = courseStaffRepository.findByCourseId(courseId);
    return courseStaffs;
  }

  @Operation(summary = "Update a staff member")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PutMapping("")
  public CourseStaff updateStaffMember(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "firstName") @RequestParam String firstName,
      @Parameter(name = "lastName") @RequestParam String lastName)
      throws EntityNotFoundException {

    CourseStaff staffMember =
        courseStaffRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CourseStaff.class, id));

    staffMember.setFirstName(firstName.trim());
    staffMember.setLastName(lastName.trim());
    return courseStaffRepository.save(staffMember);
  }

  @Operation(summary = "Delete a staff member")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @DeleteMapping("/delete")
  @Transactional
  public ResponseEntity<String> deleteStaffMember(
      @Parameter(name = "id") @RequestParam Long id,
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(
              name = "removeFromOrg",
              description = "Whether to remove course staff from GitHub organization")
          @RequestParam(defaultValue = "false")
          boolean removeFromOrg)
      throws EntityNotFoundException {
    CourseStaff staffMember =
        courseStaffRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CourseStaff.class, id));
    Course course = staffMember.getCourse();

    course.getCourseStaff().remove(staffMember);
    staffMember.setCourse(null);
    courseStaffRepository.delete(staffMember);

    return ResponseEntity.ok("Successfully deleted staff member.");
  }
}
