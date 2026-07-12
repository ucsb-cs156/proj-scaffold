package edu.ucsb.cs.scaffold.controller;

import static org.springframework.data.domain.Sort.by;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.jobs.RotatePatKeysJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJobFactory;
import edu.ucsb.cs.scaffold.jobs.UpdateAllJob;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.JobsRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs.scaffold.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Jobs")
@RequestMapping("/api/jobs")
@RestController
@Slf4j
public class JobsController extends ApiController {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private JobService jobService;

  @Autowired private UpdateUserService updateUserService;

  @Autowired ObjectMapper mapper;
  @Autowired private RosterStudentRepository rosterStudentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseStaffRepository courseStaffRepository;
  @Autowired private PatEncryptionService patEncryptionService;
  @Autowired private PatCredentialRepository patCredentialRepository;
  @Autowired private PlRepoRepository plRepoRepository;
  @Autowired private PlInstanceRepository plInstanceRepository;
  @Autowired private PlQuestionRepository plQuestionRepository;
  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;
  @Autowired private PlAssessmentRepository plAssessmentRepository;
  @Autowired private PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  @Autowired private GithubService githubService;
  @Autowired private SyncCourseWithPlRepoJobFactory syncCourseWithPlRepoJobFactory;

  @Operation(summary = "List all jobs")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/all")
  public Iterable<Job> allJobs() {
    Iterable<Job> jobs = jobsRepository.findAll(by(Sort.Direction.DESC, "createdAt"));
    return jobs;
  }

  @Operation(summary = "Delete all job records")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("/all")
  public Map<String, String> deleteAllJobs() {
    jobsRepository.deleteAll();
    return Map.of("message", "All jobs deleted");
  }

  @Operation(summary = "Get a specific Job Log by ID if it is in the database")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("")
  public Job getJobLogById(
      @Parameter(name = "id", description = "ID of the job") @RequestParam Long id)
      throws JsonProcessingException {

    Job job =
        jobsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Job.class, id));

    return job;
  }

  @Operation(summary = "Delete specific job record")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  public Map<String, String> deleteAllJobs(@Parameter(name = "id") @RequestParam Long id) {
    if (!jobsRepository.existsById(id)) {
      return Map.of("message", String.format("Job with id %d not found", id));
    }
    jobsRepository.deleteById(id);
    return Map.of("message", String.format("Job with id %d deleted", id));
  }

  @Operation(summary = "Get long job logs")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/logs/{id}")
  public String getJobLogs(@Parameter(name = "id", description = "Job ID") @PathVariable Long id) {

    return jobService.getJobLogs(id);
  }

  @Operation(summary = "Launch UpdateAll job")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/updateAll")
  public Job launchUpdateAllJob() {

    UpdateAllJob job = UpdateAllJob.builder().updateUserService(updateUserService).build();
    return jobService.runAsJob(job);
  }

  @Operation(
      summary =
          "Launch RotatePatKeys job (re-encrypt stored PATs under the current PAT_ENCRYPTION_KEY)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/rotatePatKeys")
  public Job launchRotatePatKeysJob() {

    RotatePatKeysJob job =
        RotatePatKeysJob.builder()
            .patEncryptionService(patEncryptionService)
            .patCredentialRepository(patCredentialRepository)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(
      summary =
          "Launch SyncCourseWithPlRepo job (sync the course's questions and assessments from"
              + " GitHub and PrairieLearn, using the launching user's PATs)")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/launch/syncCourseWithPlRepo")
  public Job launchSyncCourseWithPlRepoJob(
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    SyncCourseWithPlRepoJob job =
        syncCourseWithPlRepoJobFactory.create(getCurrentUser().getUser().getId(), course);
    return jobService.runAsJob(job);
  }

  @Operation(summary = "List jobs by courseId")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course")
  public Iterable<Job> jobsByCourse(@Parameter(name = "courseId") @RequestParam Long courseId) {

    Iterable<Job> jobs =
        jobsRepository.findByCourse_Id(courseId, by(Sort.Direction.DESC, "createdAt"));

    return jobs;
  }
}
