package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.jobs.ReadPLColorsJob;
import edu.ucsb.cs.scaffold.jobs.RotatePatKeysJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJobFactory;
import edu.ucsb.cs.scaffold.jobs.UpdateAllJob;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlColorRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Launch endpoints for this app's concrete jobs, plus the course-scoped jobs listing (which uses
 * this app's CourseSecurity rule). The generic admin endpoints (list all / paginated / logs /
 * delete) come from the lib-jobs library's own controller.
 */
@Tag(name = "Jobs")
@RequestMapping("/api/jobs")
@RestController
@Slf4j
public class JobsController extends ApiController {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private JobService jobService;

  @Autowired private UpdateUserService updateUserService;

  @Autowired private CourseRepository courseRepository;
  @Autowired private PatEncryptionService patEncryptionService;
  @Autowired private PatCredentialRepository patCredentialRepository;
  @Autowired private SyncCourseWithPlRepoJobFactory syncCourseWithPlRepoJobFactory;
  @Autowired private GithubService githubService;
  @Autowired private PlColorRepository plColorRepository;

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

  @Operation(
      summary =
          "Launch ReadPLColors job (read PrairieLearn's badge colors from GitHub and update the"
              + " pl_color table, using the launching admin's GitHub PAT)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/launch/readPLColors")
  public Job launchReadPLColorsJob() {

    ReadPLColorsJob job =
        ReadPLColorsJob.builder()
            .userId(getCurrentUser().getUser().getId())
            .patCredentialRepository(patCredentialRepository)
            .patEncryptionService(patEncryptionService)
            .githubService(githubService)
            .plColorRepository(plColorRepository)
            .build();
    return jobService.runAsJob(job);
  }

  @Operation(summary = "List jobs by courseId")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/course")
  public Iterable<Job> jobsByCourse(@Parameter(name = "courseId") @RequestParam Long courseId) {
    return jobsRepository.findByScopeTypeAndScopeIdOrderByIdDesc("course", courseId);
  }
}
