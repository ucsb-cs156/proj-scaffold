package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.jobs.CopyConceptGraphJob;
import edu.ucsb.cs.scaffold.jobs.RotatePatKeysJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJob;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJobFactory;
import edu.ucsb.cs.scaffold.jobs.UpdateAllJob;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.ConceptYamlService;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests for this app's JobsController: launching jobs, and the course-scoped jobs listing. The
 * generic endpoints (list all / logs / delete) belong to the lib-jobs library controller and are
 * tested in the library.
 *
 * @see JobsController
 */
@Slf4j
@WebMvcTest(controllers = JobsController.class)
public class JobsControllerJobsTests extends ControllerTestCase {

  @MockitoBean JobsRepository jobsRepository;

  @MockitoBean UserRepository userRepository;

  @MockitoBean UpdateUserService updateUserService;

  @MockitoBean JobService jobService;

  @MockitoBean RosterStudentRepository rosterStudentRepository;

  @MockitoBean CourseRepository courseRepository;

  @MockitoBean CourseStaffRepository courseStaffRepository;

  @MockitoBean PatEncryptionService patEncryptionService;

  @MockitoBean PatCredentialRepository patCredentialRepository;

  @MockitoBean PlRepoRepository plRepoRepository;

  @MockitoBean PlInstanceRepository plInstanceRepository;

  @MockitoBean PlQuestionRepository plQuestionRepository;

  @MockitoBean PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @MockitoBean PlAssessmentRepository plAssessmentRepository;

  @MockitoBean PlAssessmentQuestionRepository plAssessmentQuestionRepository;

  @MockitoBean GithubService githubService;

  @MockitoBean SyncCourseWithPlRepoJobFactory syncCourseWithPlRepoJobFactory;

  @MockitoBean AdminRepository adminRepository;

  @MockitoBean ConceptYamlService conceptYamlService;

  @Autowired ObjectMapper objectMapper;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_updateAll_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    when(jobService.runAsJob(any(UpdateAllJob.class))).thenReturn(jobStarted);

    // act
    MvcResult response =
        mockMvc
            .perform(post("/api/jobs/launch/updateAll").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(jobService, times(1)).runAsJob(any(UpdateAllJob.class));
    String expectedJson = objectMapper.writeValueAsString(jobStarted);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_rotatePatKeys_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    when(jobService.runAsJob(any(RotatePatKeysJob.class))).thenReturn(jobStarted);

    // act
    MvcResult response =
        mockMvc
            .perform(post("/api/jobs/launch/rotatePatKeys").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(jobService, times(1)).runAsJob(any(RotatePatKeysJob.class));
    String expectedJson = objectMapper.writeValueAsString(jobStarted);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructors_cannot_launch_rotatePatKeys_job() throws Exception {
    mockMvc
        .perform(post("/api/jobs/launch/rotatePatKeys").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_launch_rotatePatKeys_job() throws Exception {
    mockMvc.perform(post("/api/jobs/launch/rotatePatKeys")).andExpect(status().is(403));
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_with_course_permissions_can_launch_syncCourseWithPlRepo_job()
      throws Exception {

    // arrange

    User user = currentUserService.getUser();
    Course course = Course.builder().id(3L).courseName("CS156").build();
    when(courseRepository.findById(eq(3L))).thenReturn(Optional.of(course));

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    SyncCourseWithPlRepoJob job = SyncCourseWithPlRepoJob.builder().course(course).build();
    // the job runs with the launching user's id (MockCurrentUserServiceImpl returns id 1)
    when(syncCourseWithPlRepoJobFactory.create(eq(1L), eq(course))).thenReturn(job);
    when(jobService.runAsJob(eq(job))).thenReturn(jobStarted);

    // act
    MvcResult response =
        mockMvc
            .perform(post("/api/jobs/launch/syncCourseWithPlRepo?courseId=3").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(syncCourseWithPlRepoJobFactory, times(1)).create(eq(1L), eq(course));
    verify(jobService, times(1)).runAsJob(eq(job));

    String expectedJson = objectMapper.writeValueAsString(jobStarted);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_syncCourseWithPlRepo_job() throws Exception {
    Course course = Course.builder().id(3L).courseName("CS156").build();
    when(courseRepository.findById(eq(3L))).thenReturn(Optional.of(course));
    SyncCourseWithPlRepoJob job = SyncCourseWithPlRepoJob.builder().course(course).build();
    when(syncCourseWithPlRepoJobFactory.create(eq(1L), eq(course))).thenReturn(job);
    Job jobStarted = Job.builder().id(0L).status("started").build();
    when(jobService.runAsJob(eq(job))).thenReturn(jobStarted);

    mockMvc
        .perform(post("/api/jobs/launch/syncCourseWithPlRepo?courseId=3").with(csrf()))
        .andExpect(status().isOk());

    verify(jobService, times(1)).runAsJob(eq(job));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void launching_syncCourseWithPlRepo_for_a_missing_course_returns_404() throws Exception {
    when(courseRepository.findById(eq(3L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(post("/api/jobs/launch/syncCourseWithPlRepo?courseId=3").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("Course with id 3 not found", json.get("message"));
    verify(jobService, never()).runAsJob(any());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_launch_syncCourseWithPlRepo_job() throws Exception {
    mockMvc
        .perform(post("/api/jobs/launch/syncCourseWithPlRepo?courseId=3").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_launch_syncCourseWithPlRepo_job() throws Exception {
    mockMvc
        .perform(post("/api/jobs/launch/syncCourseWithPlRepo?courseId=3"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_jobs_by_course() throws Exception {

    // arrange
    Job job1 = Job.builder().log("job for course 5 - 1").build();
    Job job2 = Job.builder().log("job for course 5 - 2").build();
    List<Job> expectedJobs = List.of(job1, job2);

    when(jobsRepository.findByScopeTypeAndScopeIdOrderByIdDesc("course", 5L))
        .thenReturn(expectedJobs);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/jobs/course").param("courseId", "5"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(jobsRepository).findByScopeTypeAndScopeIdOrderByIdDesc("course", 5L);
    String expectedJson = objectMapper.writeValueAsString(expectedJobs);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_get_jobs_by_course() throws Exception {
    mockMvc.perform(get("/api/jobs/course").param("courseId", "5")).andExpect(status().is(403));
  }

  @WithInstructorCoursePermissions
  @Test
  public void instructor_with_course_permissions_can_launch_copyConceptGraph_job()
      throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdById(user.getId())
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    when(jobService.runAsJob(any(CopyConceptGraphJob.class))).thenReturn(jobStarted);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/jobs/launch/copyConceptGraph?fromCourseId=3&toCourseId=4").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(jobService, times(1)).runAsJob(any(CopyConceptGraphJob.class));
    String expectedJson = objectMapper.writeValueAsString(jobStarted);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_copyConceptGraph_job() throws Exception {
    Job jobStarted = Job.builder().id(0L).status("started").build();
    when(jobService.runAsJob(any(CopyConceptGraphJob.class))).thenReturn(jobStarted);

    mockMvc
        .perform(post("/api/jobs/launch/copyConceptGraph?fromCourseId=3&toCourseId=4").with(csrf()))
        .andExpect(status().isOk());

    verify(jobService, times(1)).runAsJob(any(CopyConceptGraphJob.class));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_launch_copyConceptGraph_job() throws Exception {
    mockMvc
        .perform(post("/api/jobs/launch/copyConceptGraph?fromCourseId=3&toCourseId=4").with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_launch_copyConceptGraph_job() throws Exception {
    mockMvc
        .perform(post("/api/jobs/launch/copyConceptGraph?fromCourseId=3&toCourseId=4"))
        .andExpect(status().is(403));
  }
}
