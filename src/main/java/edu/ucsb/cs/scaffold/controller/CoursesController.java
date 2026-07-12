package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.CourseStaff;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.errors.ForbiddenException;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJobFactory;
import edu.ucsb.cs.scaffold.model.CurrentUser;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.InstructorRepository;
import edu.ucsb.cs.scaffold.repository.JobsRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService;
import edu.ucsb.cs.scaffold.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.web.client.HttpClientErrorException;

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

  @Autowired private PatCredentialRepository patCredentialRepository;

  @Autowired private PatEncryptionService patEncryptionService;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private GithubService githubService;

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PrairieLearnService prairieLearnService;

  @Autowired private SyncCourseWithPlRepoJobFactory syncCourseWithPlRepoJobFactory;

  @Autowired private JobService jobService;

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
      int numStaff,
      Long plRepoId,
      Long plInstanceId,
      // Human-readable details of the PL associations; resolved only by the
      // single-course endpoints (getCourseById and the two update endpoints),
      // null in course lists.
      String plRepoName,
      String plInstanceShortName,
      Long plInstanceNumericId) {

    // Creates view from Course entity
    public InstructorCourseView(Course c) {
      this(
          c.getId(),
          c.getCourseName(),
          c.getTerm(),
          c.getSchool(),
          c.getInstructorEmail(),
          c.getRosterStudents() != null ? c.getRosterStudents().size() : 0,
          c.getCourseStaff() != null ? c.getCourseStaff().size() : 0,
          c.getPlRepoId(),
          c.getPlInstanceId(),
          null,
          null,
          null);
    }
  }

  /**
   * Builds an InstructorCourseView with the PL association details (repo name, instance short name,
   * instance numeric id) resolved from their tables — used by the single-course endpoints so the
   * frontend can show what is currently associated. List endpoints use the plain constructor and
   * leave these null.
   */
  private InstructorCourseView viewWithPlDetails(Course c) {
    String plRepoName =
        c.getPlRepoId() == null
            ? null
            : plRepoRepository.findById(c.getPlRepoId()).map(PlRepo::getRepoName).orElse(null);
    PlInstance plInstance =
        c.getPlInstanceId() == null
            ? null
            : plInstanceRepository.findById(c.getPlInstanceId()).orElse(null);
    return new InstructorCourseView(
        c.getId(),
        c.getCourseName(),
        c.getTerm(),
        c.getSchool(),
        c.getInstructorEmail(),
        c.getRosterStudents() != null ? c.getRosterStudents().size() : 0,
        c.getCourseStaff() != null ? c.getCourseStaff().size() : 0,
        c.getPlRepoId(),
        c.getPlInstanceId(),
        plRepoName,
        plInstance == null ? null : plInstance.getShortName(),
        plInstance == null ? null : plInstance.getNumericId());
  }

  /**
   * This method returns a list of courses.
   *
   * @return a list of all courses for an instructor.
   */
  @Operation(summary = "List all courses for an instructor")
  @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/list/instructors")
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
  @GetMapping("/list/admins")
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
    return viewWithPlDetails(course);
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
  @GetMapping("/list/students")
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
  @GetMapping("/list/staff")
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

  /** DTO representing a course along with the ways in which the current user has access to it. */
  public record CourseListDTO(
      Long id,
      String courseName,
      String term,
      School school,
      String instructorEmail,
      boolean studentAccess,
      boolean staffAccess,
      boolean instructorAccess,
      boolean adminAccess) {}

  /**
   * This method returns a unified list of courses that the current user has access to, whether as a
   * student, staff member, instructor, or admin.
   *
   * @return a list of courses along with the access flags for the current user.
   */
  @Operation(summary = "List all courses the current user has access to")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/list")
  public List<CourseListDTO> listCoursesForCurrentUserUnified() {
    String email = getCurrentUser().getUser().getEmail();

    boolean isAdmin = adminRepository.existsByEmail(email);

    Set<Long> studentCourseIds =
        rosterStudentRepository.findAllByEmail(email).stream()
            .map(rs -> rs.getCourse().getId())
            .collect(Collectors.toSet());

    Set<Long> staffCourseIds =
        courseStaffRepository.findAllByEmail(email).stream()
            .map(cs -> cs.getCourse().getId())
            .collect(Collectors.toSet());

    Set<Long> instructorCourseIds =
        courseRepository.findByInstructorEmail(email).stream()
            .map(Course::getId)
            .collect(Collectors.toSet());

    List<Course> courses;
    if (isAdmin) {
      courses = courseRepository.findAll();
    } else {
      Set<Long> accessibleCourseIds = new HashSet<>();
      accessibleCourseIds.addAll(studentCourseIds);
      accessibleCourseIds.addAll(staffCourseIds);
      accessibleCourseIds.addAll(instructorCourseIds);
      courses = courseRepository.findAllById(accessibleCourseIds);
    }

    return courses.stream()
        .map(
            c ->
                new CourseListDTO(
                    c.getId(),
                    c.getCourseName(),
                    c.getTerm(),
                    c.getSchool(),
                    c.getInstructorEmail(),
                    studentCourseIds.contains(c.getId()),
                    staffCourseIds.contains(c.getId()),
                    instructorCourseIds.contains(c.getId()),
                    isAdmin))
        .collect(Collectors.toList());
  }

  /**
   * This method returns the unified access info for a single course, if the current user has access
   * to it. If the user does not have access, or the course does not exist, a 404 is returned.
   *
   * @param courseId the id of the course
   * @return the course along with the access flags for the current user.
   */
  @Operation(summary = "Get unified course access info for a single course")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/list/{courseId}")
  public CourseListDTO getCourseAccessInfo(
      @Parameter(name = "courseId") @PathVariable Long courseId) {
    String email = getCurrentUser().getUser().getEmail();

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    boolean isAdmin = adminRepository.existsByEmail(email);

    boolean studentAccess =
        rosterStudentRepository.findAllByEmail(email).stream()
            .anyMatch(rs -> rs.getCourse().getId().equals(courseId));

    boolean staffAccess =
        courseStaffRepository.findAllByEmail(email).stream()
            .anyMatch(cs -> cs.getCourse().getId().equals(courseId));

    boolean instructorAccess = email.equals(course.getInstructorEmail());

    if (!isAdmin && !studentAccess && !staffAccess && !instructorAccess) {
      throw new EntityNotFoundException(Course.class, courseId);
    }

    return new CourseListDTO(
        course.getId(),
        course.getCourseName(),
        course.getTerm(),
        course.getSchool(),
        course.getInstructorEmail(),
        studentAccess,
        staffAccess,
        instructorAccess,
        isAdmin);
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
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
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

  /**
   * Associates a GitHub repo (PlRepo) with a course, after verifying that the current user's stored
   * GitHub PAT has read/write access to the repo. The check is a single GET /repos/{owner}/{repo}
   * call: a successful response proves read access and its permissions block reports push (write)
   * access, so nothing is written to the repo.
   *
   * @param courseId the id of the course
   * @param repoName the repo in owner/repo form (i.e. the part after https://github.com/)
   * @return the updated course
   */
  @Operation(summary = "Associate a GitHub repo (PlRepo) with a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/updateGithubRepo")
  public InstructorCourseView updateGithubRepo(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "repoName", description = "GitHub repo in owner/repo form") @RequestParam
          String repoName) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    long userId = getCurrentUser().getUser().getId();
    PatCredential credential =
        patCredentialRepository
            .findByUserIdAndPlatform(userId, PatPlatform.GITHUB)
            .orElseThrow(() -> new ForbiddenException("must set up Github PAT first"));
    String token =
        patEncryptionService.decrypt(credential.getCiphertext(), credential.getKeyVersion());

    String trimmedRepoName = repoName.strip();
    boolean canWrite;
    try {
      canWrite = githubService.hasWriteAccess(trimmedRepoName, token);
    } catch (HttpClientErrorException e) {
      throw new ForbiddenException("No access to repo via Github PAT token");
    }
    if (!canWrite) {
      throw new ForbiddenException("Read/write access to repo via Github PAT is required");
    }

    PlRepo plRepo =
        plRepoRepository
            .findByRepoName(trimmedRepoName)
            .orElseGet(
                () -> plRepoRepository.save(PlRepo.builder().repoName(trimmedRepoName).build()));
    course.setPlRepoId(plRepo.getId());
    return viewWithPlDetails(courseRepository.save(course));
  }

  /**
   * Associates a PrairieLearn course instance (PlInstance) with a course. The numeric instance id
   * is verified in two steps: it is fetched from the PrairieLearn API using the caller's
   * PrairieLearn PAT, and the course's GitHub repo must contain a matching
   * courseInstances/{shortName}/infoCourseInstance.json whose longName agrees. Only the
   * PrairieLearn API can supply the numeric id, and only the repo check proves the instance belongs
   * to this course's repo.
   *
   * @param courseId the id of the course
   * @param instanceId PrairieLearn's numeric course instance id
   * @return the updated course
   */
  @Operation(summary = "Associate a PrairieLearn course instance (PlInstance) with a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/updatePLInstance")
  public InstructorCourseView updatePLInstance(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "instanceId", description = "numeric PrairieLearn course instance id")
          @RequestParam
          Long instanceId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    long userId = getCurrentUser().getUser().getId();
    PatCredential githubCredential =
        patCredentialRepository
            .findByUserIdAndPlatform(userId, PatPlatform.GITHUB)
            .orElseThrow(() -> new ForbiddenException("must set up Github PAT first"));
    PatCredential plCredential =
        patCredentialRepository
            .findByUserIdAndPlatform(userId, PatPlatform.PRAIRIELEARN)
            .orElseThrow(() -> new ForbiddenException("must set up PrairieLearn PAT first"));
    if (course.getPlRepoId() == null) {
      throw new ForbiddenException("must associate course with PlRepo first");
    }
    PlRepo plRepo =
        plRepoRepository
            .findById(course.getPlRepoId())
            .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, course.getPlRepoId()));

    String plToken =
        patEncryptionService.decrypt(plCredential.getCiphertext(), plCredential.getKeyVersion());
    PrairieLearnService.CourseInstanceInfo info;
    try {
      info = prairieLearnService.getCourseInstance(instanceId, plToken);
    } catch (HttpClientErrorException e) {
      throw new ForbiddenException("course instance id not found");
    }
    if (info == null) {
      throw new ForbiddenException("course instance id not found");
    }

    String githubToken =
        patEncryptionService.decrypt(
            githubCredential.getCiphertext(), githubCredential.getKeyVersion());
    if (!repoConfirmsInstance(plRepo, info, githubToken)) {
      throw new ForbiddenException("course instance id not found");
    }

    PlInstance plInstance =
        plInstanceRepository
            .findByPlRepoIdAndShortName(plRepo.getId(), info.shortName())
            .orElseGet(
                () ->
                    PlInstance.builder()
                        .plRepoId(plRepo.getId())
                        .shortName(info.shortName())
                        .build());
    plInstance.setLongName(info.longName());
    plInstance.setNumericId(info.courseInstanceId());
    PlInstance savedInstance = plInstanceRepository.save(plInstance);

    course.setPlInstanceId(savedInstance.getId());
    Course savedCourse = courseRepository.save(course);

    // A successful association immediately kicks off a sync of the course's questions and
    // assessments (issue #69), so the instructor doesn't have to launch it by hand.
    jobService.runAsJob(syncCourseWithPlRepoJobFactory.create(userId, savedCourse));

    return viewWithPlDetails(savedCourse);
  }

  /**
   * True when the course's repo has courseInstances/{shortName}/infoCourseInstance.json and its
   * longName matches the one PrairieLearn reported for the instance.
   */
  private boolean repoConfirmsInstance(
      PlRepo plRepo, PrairieLearnService.CourseInstanceInfo info, String githubToken) {
    String infoJson;
    try {
      infoJson =
          githubService.getFileContent(
              plRepo.getRepoName(),
              "courseInstances/" + info.shortName() + "/infoCourseInstance.json",
              githubToken);
    } catch (HttpClientErrorException e) {
      return false;
    }
    try {
      JsonNode longName = new ObjectMapper().readTree(infoJson).path("longName");
      return !longName.isMissingNode() && longName.asText().equals(info.longName());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return false;
    }
  }
}
