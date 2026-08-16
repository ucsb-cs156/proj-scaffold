package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.CourseStaff;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.ConceptYamlService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CopyConceptGraphJobTests {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final CourseRepository courseRepository = mock(CourseRepository.class);
  private final AdminRepository adminRepository = mock(AdminRepository.class);
  private final ConceptYamlService conceptYamlService = mock(ConceptYamlService.class);

  private CopyConceptGraphJob.CopyConceptGraphJobBuilder jobBuilder() {
    return CopyConceptGraphJob.builder()
        .userId(1L)
        .fromCourseId(3L)
        .toCourseId(4L)
        .userRepository(userRepository)
        .courseRepository(courseRepository)
        .adminRepository(adminRepository)
        .conceptYamlService(conceptYamlService);
  }

  private Map<String, Object> successReport() {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("success", true);
    report.put("errors", List.of());
    report.put("conceptsCreated", 2);
    report.put("subconceptsCreated", 1);
    report.put("edgesCreated", 1);
    report.put("practiceProblemsCreated", 0);
    report.put("userStatesCleared", 5);
    return report;
  }

  @Test
  public void scope_type_and_id_are_course_and_toCourseId() {
    CopyConceptGraphJob job = jobBuilder().build();
    assertEquals("course", job.getScopeType());
    assertEquals(4L, job.getScopeId());
  }

  @Test
  public void admin_can_copy_concept_graph_between_courses_they_do_not_teach() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    Course fromCourse = Course.builder().id(3L).courseName("CS156-from").build();
    Course toCourse = Course.builder().id(4L).courseName("CS156-to").build();
    User admin = User.builder().id(1L).email("admin@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(adminRepository.existsByEmail("admin@example.org")).thenReturn(true);
    when(conceptYamlService.createYAML(3L)).thenReturn("concepts: []");
    when(conceptYamlService.replaceFromYAML(eq(4L), any(InputStream.class)))
        .thenReturn(successReport());

    jobBuilder().build().accept(ctx);

    verify(conceptYamlService).createYAML(3L);
    verify(conceptYamlService).replaceFromYAML(eq(4L), any(InputStream.class));
    String log = jobStarted.getLog();
    assertEquals(
        true,
        log.contains("Copying concept graph from course 3 to course 4")
            && log.contains("Copied concept graph"));
  }

  @Test
  public void instructor_of_both_courses_can_copy_concept_graph() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    CourseStaff unrelatedStaffMember =
        CourseStaff.builder().email("other-staff@example.org").build();
    Course fromCourse =
        Course.builder()
            .id(3L)
            .courseName("CS156-from")
            .instructorEmail("prof@example.org")
            .courseStaff(List.of(unrelatedStaffMember))
            .build();
    Course toCourse =
        Course.builder()
            .id(4L)
            .courseName("CS156-to")
            .instructorEmail("prof@example.org")
            .courseStaff(List.of(unrelatedStaffMember))
            .build();
    User instructor = User.builder().id(1L).email("prof@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(instructor));
    when(adminRepository.existsByEmail("prof@example.org")).thenReturn(false);
    when(conceptYamlService.createYAML(3L)).thenReturn("concepts: []");
    when(conceptYamlService.replaceFromYAML(eq(4L), any(InputStream.class)))
        .thenReturn(successReport());

    jobBuilder().build().accept(ctx);

    verify(conceptYamlService).replaceFromYAML(eq(4L), any(InputStream.class));
  }

  @Test
  public void staff_of_both_courses_can_copy_concept_graph() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    CourseStaff staffMember = CourseStaff.builder().email("staff@example.org").build();
    Course fromCourse =
        Course.builder().id(3L).courseName("CS156-from").courseStaff(List.of(staffMember)).build();
    Course toCourse =
        Course.builder().id(4L).courseName("CS156-to").courseStaff(List.of(staffMember)).build();
    User staff = User.builder().id(1L).email("staff@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(staff));
    when(adminRepository.existsByEmail("staff@example.org")).thenReturn(false);
    when(conceptYamlService.createYAML(3L)).thenReturn("concepts: []");
    when(conceptYamlService.replaceFromYAML(eq(4L), any(InputStream.class)))
        .thenReturn(successReport());

    jobBuilder().build().accept(ctx);

    verify(conceptYamlService).replaceFromYAML(eq(4L), any(InputStream.class));
  }

  @Test
  public void terminates_when_from_course_is_missing() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    when(courseRepository.findById(3L)).thenReturn(Optional.empty());
    when(courseRepository.findById(4L)).thenReturn(Optional.of(Course.builder().id(4L).build()));

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals("Cannot copy concept graph: from course id 3 not found", thrown.getMessage());
    verify(conceptYamlService, never()).createYAML(anyLong());
  }

  @Test
  public void terminates_when_to_course_is_missing() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    when(courseRepository.findById(3L)).thenReturn(Optional.of(Course.builder().id(3L).build()));
    when(courseRepository.findById(4L)).thenReturn(Optional.empty());

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals("Cannot copy concept graph: to course id 4 not found", thrown.getMessage());
    verify(conceptYamlService, never()).createYAML(anyLong());
  }

  @Test
  public void terminates_when_both_courses_are_missing() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    when(courseRepository.findById(3L)).thenReturn(Optional.empty());
    when(courseRepository.findById(4L)).thenReturn(Optional.empty());

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals(
        "Cannot copy concept graph: from course id 3 and to course id 4 not found",
        thrown.getMessage());
  }

  @Test
  public void terminates_when_user_is_missing() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    when(courseRepository.findById(3L)).thenReturn(Optional.of(Course.builder().id(3L).build()));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(Course.builder().id(4L).build()));
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals("Cannot copy concept graph: user 1 not found", thrown.getMessage());
  }

  @Test
  public void terminates_when_user_lacks_permission_on_from_course() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    Course fromCourse = Course.builder().id(3L).instructorEmail("someone-else@example.org").build();
    Course toCourse = Course.builder().id(4L).instructorEmail("user@example.org").build();
    User user = User.builder().id(1L).email("user@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(adminRepository.existsByEmail("user@example.org")).thenReturn(false);

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals(
        "Cannot copy concept graph: user user@example.org does not have admin or"
            + " instructor/staff access to the from course (id 3)",
        thrown.getMessage());
    verify(conceptYamlService, never()).createYAML(anyLong());
  }

  @Test
  public void terminates_when_user_lacks_permission_on_to_course() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    Course fromCourse = Course.builder().id(3L).instructorEmail("user@example.org").build();
    Course toCourse = Course.builder().id(4L).instructorEmail("someone-else@example.org").build();
    User user = User.builder().id(1L).email("user@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(adminRepository.existsByEmail("user@example.org")).thenReturn(false);

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals(
        "Cannot copy concept graph: user user@example.org does not have admin or"
            + " instructor/staff access to the to course (id 4)",
        thrown.getMessage());
    verify(conceptYamlService, never()).createYAML(anyLong());
  }

  @Test
  public void terminates_when_user_is_not_a_matching_staff_member_on_from_course()
      throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    CourseStaff unrelatedStaffMember =
        CourseStaff.builder().email("other-staff@example.org").build();
    Course fromCourse =
        Course.builder()
            .id(3L)
            .instructorEmail("someone-else@example.org")
            .courseStaff(List.of(unrelatedStaffMember))
            .build();
    Course toCourse = Course.builder().id(4L).instructorEmail("user@example.org").build();
    User user = User.builder().id(1L).email("user@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(adminRepository.existsByEmail("user@example.org")).thenReturn(false);

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals(
        "Cannot copy concept graph: user user@example.org does not have admin or"
            + " instructor/staff access to the from course (id 3)",
        thrown.getMessage());
    verify(conceptYamlService, never()).createYAML(anyLong());
  }

  @Test
  public void terminates_when_yaml_import_reports_failure() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    Course fromCourse = Course.builder().id(3L).build();
    Course toCourse = Course.builder().id(4L).build();
    User admin = User.builder().id(1L).email("admin@example.org").build();

    when(courseRepository.findById(3L)).thenReturn(Optional.of(fromCourse));
    when(courseRepository.findById(4L)).thenReturn(Optional.of(toCourse));
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(adminRepository.existsByEmail("admin@example.org")).thenReturn(true);
    when(conceptYamlService.createYAML(3L)).thenReturn("concepts: []");

    Map<String, Object> failureReport = new LinkedHashMap<>();
    failureReport.put("success", false);
    failureReport.put("errors", List.of("bad concept"));
    when(conceptYamlService.replaceFromYAML(eq(4L), any(InputStream.class)))
        .thenReturn(failureReport);

    CopyConceptGraphJob job = jobBuilder().build();
    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals("Failed to copy concept graph: [bad concept]", thrown.getMessage());
  }
}
