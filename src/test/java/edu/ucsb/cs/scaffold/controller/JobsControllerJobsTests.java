package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.jobs.RotatePatKeysJob;
import edu.ucsb.cs.scaffold.jobs.UpdateAllJob;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.JobsRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs.scaffold.services.jobs.JobService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * This class tests the ability of the JobsController to launch jobs. By contrast,
 * JobsControllerDetailedTests tests the ability of the JobsController to get the status of jobs
 * that have already been launched.
 *
 * @see JobsController
 * @see JobsControllerDetailedTests
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

  @Autowired ObjectMapper objectMapper;

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_updateAll_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdBy(user)
            .createdAt(null)
            .updatedAt(null)
            .status("started")
            .build();

    when(jobService.runAsJob(any(UpdateAllJob.class))).thenReturn(jobStarted);

    // act
    mockMvc
        .perform(post("/api/jobs/launch/updateAll").with(csrf()))
        .andExpect(status().isOk())
        .andReturn();

    // assert

    verify(jobService, times(1)).runAsJob(any(UpdateAllJob.class));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_launch_rotatePatKeys_job() throws Exception {

    // arrange

    User user = currentUserService.getUser();

    Job jobStarted =
        Job.builder()
            .id(0L)
            .createdBy(user)
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
}
