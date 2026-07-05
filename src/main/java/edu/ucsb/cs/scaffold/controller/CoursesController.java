package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.CourseStaff;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.model.CurrentUser;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.InstructorRepository;
import edu.ucsb.cs.scaffold.repository.JobsRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@Tag(name = "Course")
@RequestMapping("/api/courses")
@RestController
@Slf4j
public class CoursesController extends ApiController {

  @Autowired private CourseRepository courseRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Autowired private InstructorRepository instructorRepository;

  @Autowired private AdminRepository adminRepository;

  @Autowired private JobsRepository jobsRepository;

  /**
   * This method creates a new Course.
   *
   * @param courseName the name of the course
   * @param term the term of the course
   * @param school the school of the course
   * @param canvasApiToken the Canvas API token (optional)
   * @param canvasCourseId the Canvas course ID (optional)
   */
  @Operation(summary = "Create a new course")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/post")
  public InstructorCourseView postCourse(
      @Parameter(name = "courseName") @RequestParam String courseName,
      @Parameter(name = "term") @RequestParam String term,
      @Parameter(name = "school") @RequestParam School school,
      @Parameter(name = "canvasApiToken") @RequestParam(required = false) String canvasApiToken,
      @Parameter(name = "canvasCourseId") @RequestParam(required = false) String canvasCourseId) {
    // get current date right now and set status to pending
    edu.ucsb.cs.scaffold.model.CurrentUser currentUser = getCurrentUser();
    Course course =
        Course.builder()
            .courseName(courseName)
            .term(term)
            .school(school)
            .instructorEmail(currentUser.getUser().getEmail().strip())
            .canvasApiToken(canvasApiToken)
            .canvasCourseId(canvasCourseId)
            .build();
    Course savedCourse = courseRepository.save(course);

    return new InstructorCourseView(savedCourse);
  }

  /** Projection of Course entity with fields that are relevant for instructors and admins */
  public static record InstructorCourseView(
      Long id,
      String courseName,
      String term,
      School school,
      String instructorEmail,
      int numStudents,
      int numStaff) {

    // Creates view from Course entity
    public InstructorCourseView(Course c) {
      this(
          c.getId(),
          c.getCourseName(),
          c.getTerm(),
          c.getSchool(),
          c.getInstructorEmail(),
          c.getRosterStudents() != null ? c.getRosterStudents().size() : 0,
          c.getCourseStaff() != null ? c.getCourseStaff().size() : 0);
    }
  }

  /**
   * This method returns a list of courses.
   *
   * @return a list of all courses for an instructor.
   */
  @Operation(summary = "List all courses for an instructor")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/allForInstructors")
  public Iterable<InstructorCourseView> allForInstructors() {
    CurrentUser currentUser = getCurrentUser();
    String instructorEmail = currentUser.getUser().getEmail();
    List<Course> courses = courseRepository.findByInstructorEmail(instructorEmail);

    List<InstructorCourseView> courseViews =
        courses.stream().map(InstructorCourseView::new).collect(Collectors.toList());
    return courseViews;
  }

  /**
   * This method returns a list of courses.
   *
   * @return a list of all courses for an admin.
   */
  @Operation(summary = "List all courses for an admin")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/allForAdmins")
  public Iterable<InstructorCourseView> allForAdmins() {
    List<Course> courses = courseRepository.findAll();

    List<InstructorCourseView> courseViews =
        courses.stream().map(InstructorCourseView::new).collect(Collectors.toList());
    return courseViews;
  }

  /**
   * This method returns single course by its id
   *
   * @return a course
   */
  @Operation(summary = "Get course by id")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #id)")
  @GetMapping("/{id}")
  public InstructorCourseView getCourseById(@Parameter(name = "id") @PathVariable Long id) {
    Course course =
        courseRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, id));
    // Convert to InstructorCourseView
    InstructorCourseView courseView = new InstructorCourseView(course);
    return courseView;
  }

  /**
   * This method returns the Canvas course ID and partially obscured Canvas token for a course by
   * its id. If the token is less than or equal to 3 characters long, it is returned in full.
   * Otherwise, all but the last three characters are replaced with asterisks. This is okay because
   * such short tokens are not generated by Canvas.
   *
   * @param courseId the id of the course
   * @return a map with courseId, canvasCourseId, and obscured canvasApiToken
   */
  @Operation(summary = "Get course Canvas course ID and Canvas token (partially obscured)")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("getCanvasInfo")
  public Map<String, String> getCourseCanvasInfo(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    String obscuredToken = null;

    if (course.getCanvasApiToken() != null) {
      String token = course.getCanvasApiToken();
      if (token.length() < 4) {
        obscuredToken = token;
      } else {
        String lastThree = token.substring(token.length() - 3);
        obscuredToken = "*".repeat(token.length() - 3) + lastThree;
      }
    }
    return Map.of(
        "courseId", course.getId().toString(),
        "canvasCourseId", course.getCanvasCourseId() != null ? course.getCanvasCourseId() : "",
        "canvasApiToken", obscuredToken != null ? obscuredToken : "");
  }

  public record RosterStudentCoursesDTO(
      Long id, String courseName, String term, String school, Long rosterStudentId) {}

  /**
   * This method returns a list of courses that the current user is enrolled.
   *
   * @return a list of courses in the DTO form along with the student status in the organization.
   */
  @Operation(summary = "List all courses for the current student, including their org status")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/list")
  public List<RosterStudentCoursesDTO> listCoursesForCurrentUser() {
    String email = getCurrentUser().getUser().getEmail();
    Iterable<RosterStudent> rosterStudentsIterable = rosterStudentRepository.findAllByEmail(email);
    List<RosterStudent> rosterStudents = new ArrayList<>();
    rosterStudentsIterable.forEach(rosterStudents::add);
    return rosterStudents.stream()
        .map(
            rs -> {
              Course course = rs.getCourse();
              RosterStudentCoursesDTO rsDto =
                  new RosterStudentCoursesDTO(
                      course.getId(),
                      course.getCourseName(),
                      course.getTerm(),
                      course.getSchool().getDisplayName(),
                      rs.getId());
              return rsDto;
            })
        .collect(Collectors.toList());
  }

  public record StaffCoursesDTO(
      Long id, String courseName, String term, School school, Long staffId) {}

  public enum EmailTypes {
    STUDENTS,
    STAFF,
    ALL
  }

  public enum EmailFormats {
    COMMA_SEPARATED,
    ONE_PER_LINE
  }

  /**
   * student see what courses they appear as staff in
   *
   * @param studentId the id of the student making request
   * @return a list of all courses student is staff in
   */
  @Operation(summary = "Student see what courses they appear as staff in")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/staffCourses")
  public List<StaffCoursesDTO> staffCourses() {
    CurrentUser currentUser = getCurrentUser();
    User user = currentUser.getUser();

    String email = user.getEmail();

    List<CourseStaff> staffMembers = courseStaffRepository.findAllByEmail(email);
    return staffMembers.stream()
        .map(
            s -> {
              Course course = s.getCourse();
              StaffCoursesDTO sDto =
                  new StaffCoursesDTO(
                      course.getId(),
                      course.getCourseName(),
                      course.getTerm(),
                      course.getSchool(),
                      s.getId());
              return sDto;
            })
        .collect(Collectors.toList());
  }

  @Operation(summary = "Update instructor email for a course (admin only)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PutMapping("/updateInstructor")
  public InstructorCourseView updateInstructorEmail(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "instructorEmail") @RequestParam String instructorEmail) {

    instructorEmail = instructorEmail.strip();

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    // Validate that the email exists in either instructor or admin table
    boolean isInstructor = instructorRepository.existsByEmail(instructorEmail);
    boolean isAdmin = adminRepository.existsByEmail(instructorEmail);

    if (!isInstructor && !isAdmin) {
      throw new IllegalArgumentException("Email must belong to either an instructor or admin");
    }

    course.setInstructorEmail(instructorEmail);
    Course savedCourse = courseRepository.save(course);

    return new InstructorCourseView(savedCourse);
  }

  @Operation(summary = "Get course emails")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/emails")
  public String getCourseEmails(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "type") @RequestParam(defaultValue = "STUDENTS") EmailTypes type,
      @Parameter(name = "team") @RequestParam(required = false) String team,
      @Parameter(name = "format") @RequestParam(defaultValue = "ONE_PER_LINE")
          EmailFormats format) {

    List<String> staffEmails =
        StreamSupport.stream(courseStaffRepository.findByCourseId(courseId).spliterator(), false)
            .map(CourseStaff::getEmail)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());

    List<String> studentEmails =
        StreamSupport.stream(rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .filter(student -> team == null || team.isBlank())
            .map(RosterStudent::getEmail)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());

    List<String> emails = studentEmails;
    if (type == EmailTypes.STAFF) {
      emails = staffEmails;
    } else if (type == EmailTypes.ALL) {
      emails = new ArrayList<>(staffEmails);
      emails.addAll(studentEmails);
    }

    String separator = format == EmailFormats.COMMA_SEPARATED ? "," : "\r\n";
    return String.join(separator, emails);
  }

  @Operation(summary = "Delete a course")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  @Transactional
  public Object deleteCourse(@RequestParam Long courseId)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    // Check if course has roster students or staff
    if (!course.getRosterStudents().isEmpty() || !course.getCourseStaff().isEmpty()) {
      throw new IllegalArgumentException("Cannot delete course with students or staff");
    }

    jobsRepository.deleteByCourse_Id(courseId);
    courseRepository.delete(course);
    return genericMessage("Course with id %s deleted".formatted(course.getId()));
  }

  /**
   * This method updates an existing course.
   *
   * @param courseId the id of the course to update
   * @param courseName the new name of the course
   * @param term the new term of the course
   * @param school the new school of the course
   * @return the updated course
   */
  @Operation(summary = "Update an existing course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("")
  public InstructorCourseView updateCourse(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "courseName") @RequestParam String courseName,
      @Parameter(name = "term") @RequestParam String term,
      @Parameter(name = "school") @RequestParam School school) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    course.setCourseName(courseName);
    course.setTerm(term);
    course.setSchool(school);

    Course savedCourse = courseRepository.save(course);

    return new InstructorCourseView(savedCourse);
  }

  /**
   * This method updates an existing course.
   *
   * @param courseId the id of the course to update
   * @param courseName the new name of the course
   * @param term the new term of the course
   * @param school the new school of the course
   * @param canvasApiToken the new Canvas API token for the course
   * @param canvasCourseId the new Canvas course ID
   * @return the updated course
   */
  @Operation(summary = "Update an existing course with Canvas token and course ID")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/updateCourseCanvasToken")
  public InstructorCourseView updateCourseWithCanvasToken(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "canvasApiToken") @RequestParam(required = false) String canvasApiToken,
      @Parameter(name = "canvasCourseId") @RequestParam(required = false) String canvasCourseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    if (canvasApiToken != null
        && !canvasApiToken.isEmpty()
        && !canvasApiToken.equals(course.getCanvasApiToken())) {
      course.setCanvasApiToken(canvasApiToken);
    }

    if (canvasCourseId != null
        && !canvasCourseId.isEmpty()
        && !canvasCourseId.equals(course.getCanvasCourseId())) {
      course.setCanvasCourseId(canvasCourseId);
    }

    Course savedCourse = courseRepository.save(course);

    return new InstructorCourseView(savedCourse);
  }
}
