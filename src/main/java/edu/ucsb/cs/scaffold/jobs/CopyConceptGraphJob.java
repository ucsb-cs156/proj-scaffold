package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.ConceptYamlService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;

/**
 * Copies one course's entire concept-graph content (concepts, subconcepts, prerequisite edges, and
 * practice problems) into another course, replacing ALL of the destination course's content and
 * clearing every user's saved per-course scaffold state for that course.
 *
 * <p>Implemented as an in-memory YAML export of the "from" course immediately followed by a YAML
 * import into the "to" course (see docs/yaml-format.md), so this job cannot drift from the
 * behavior of the download/upload endpoints in {@link
 * edu.ucsb.cs.scaffold.controller.ConceptsYamlController}: the same code performs both, and any
 * future bug fix in {@link ConceptYamlService} applies to this job automatically.
 *
 * <p>Before copying anything, the job verifies that both courses exist and that the launching user
 * has admin or instructor/staff level access to both; otherwise it logs an error and terminates
 * without touching either course.
 */
@Builder
public class CopyConceptGraphJob implements JobContextConsumer {

  private long userId;
  private Long fromCourseId;
  private Long toCourseId;
  private UserRepository userRepository;
  private CourseRepository courseRepository;
  private AdminRepository adminRepository;
  private ConceptYamlService conceptYamlService;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return toCourseId;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        "Copying concept graph from course %d to course %d".formatted(fromCourseId, toCourseId));

    Optional<Course> fromCourse = courseRepository.findById(fromCourseId);
    Optional<Course> toCourse = courseRepository.findById(toCourseId);
    List<String> missingCourses = new ArrayList<>();
    if (fromCourse.isEmpty()) {
      missingCourses.add("from course id %d".formatted(fromCourseId));
    }
    if (toCourse.isEmpty()) {
      missingCourses.add("to course id %d".formatted(toCourseId));
    }
    if (!missingCourses.isEmpty()) {
      throw new Exception(
          "Cannot copy concept graph: %s not found".formatted(String.join(" and ", missingCourses)));
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new Exception("Cannot copy concept graph: user %d not found".formatted(userId)));

    if (!hasManagePermissions(user, fromCourse.get())) {
      throw new Exception(
          "Cannot copy concept graph: user %s does not have admin or instructor/staff access to the from course (id %d)"
              .formatted(user.getEmail(), fromCourseId));
    }
    if (!hasManagePermissions(user, toCourse.get())) {
      throw new Exception(
          "Cannot copy concept graph: user %s does not have admin or instructor/staff access to the to course (id %d)"
              .formatted(user.getEmail(), toCourseId));
    }

    String yaml = conceptYamlService.createYAML(fromCourseId);
    Map<String, Object> report =
        conceptYamlService.replaceFromYAML(
            toCourseId, new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    if (!Boolean.TRUE.equals(report.get("success"))) {
      throw new Exception(
          "Failed to copy concept graph: %s".formatted(report.get("errors")));
    }

    ctx.log(
        "Copied concept graph: %s concepts, %s subconcepts, %s edges, %s practice problems created; %s user states cleared"
            .formatted(
                report.get("conceptsCreated"),
                report.get("subconceptsCreated"),
                report.get("edgesCreated"),
                report.get("practiceProblemsCreated"),
                report.get("userStatesCleared")));
  }

  private boolean hasManagePermissions(User user, Course course) {
    String email = user.getEmail();
    if (adminRepository.existsByEmail(email)) {
      return true;
    }
    if (course.getCourseStaff() != null
        && course.getCourseStaff().stream()
            .anyMatch(staff -> email.equals(staff.getEmail()))) {
      return true;
    }
    return email.equals(course.getInstructorEmail());
  }
}
