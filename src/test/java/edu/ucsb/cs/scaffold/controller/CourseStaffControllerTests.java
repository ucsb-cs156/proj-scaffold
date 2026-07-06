package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.*;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs.scaffold.services.jobs.JobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = CourseStaffController.class)
public class CourseStaffControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private JobService service;

  @Autowired private ObjectMapper objectMapper;

  Course course1 =
      Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

  CourseStaff cs1 =
      CourseStaff.builder()
          .firstName("Chris")
          .lastName("Gaucho")
          .email("cgaucho@example.org")
          .course(course1)
          .build();

  CourseStaff cs2 =
      CourseStaff.builder()
          .id(2L)
          .firstName("Lauren")
          .lastName("Del Playa")
          .email("ldelplaya@ucsb.edu")
          .course(course1)
          .build();

  /** Test the POST endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostCourseStaff() throws Exception {

    Course course2 =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@example.org")
            .course(course2)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course2));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs2);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseStaffRepository, times(1)).save(eq(cs2));

    verify(updateUserService).attachUserToCourseStaff(any(CourseStaff.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(cs2);
    assertEquals(expectedJson, responseString);
  }

  /** Test the POST endpoint email sanitization */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostCourseStaffEmailSanitized() throws Exception {

    Course course2 =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@example.org")
            .course(course2)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course2));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs2);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", " cgaucho@example.org ") // Expect the spaces to be stripped
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseStaffRepository, times(1)).save(eq(cs2));

    verify(updateUserService).attachUserToCourseStaff(any(CourseStaff.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(cs2);
    assertEquals(expectedJson, responseString);
  }

  /** Test the POST endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_post_course_staff_join_course_status() throws Exception {

    Course course2 =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("cgaucho@example.org")
            .course(course2)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course2));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(cs2);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseStaffRepository, times(1)).save(eq(cs2));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(cs2);
    assertEquals(expectedJson, responseString);
  }

  /**
   * Test that you cannot post a single course staff for a course that does not exist
   *
   * @throws Exception
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_AdminCannotPostCourseStaffForCourseThatDoesNotExist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/coursestaff/post")
                    .with(csrf())
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET endpoint */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCourseStaffByCourse() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(java.util.List.of(cs1, cs2));

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/coursestaff/course").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(cs1, cs2));
    assertEquals(expectedJson, responseString);
  }

  /** Test whether admin can get course staff for a non existing course */
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void admin_can_get_course_staff_for_a_non_existing_course() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/coursestaff/course").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(courseRepository, atLeastOnce()).findById(eq(1L));
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateCourseStaff_success() throws Exception {
    CourseStaff existingStaffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .email("old@ucsb.edu")
            .course(course1)
            .build();

    CourseStaff updatedStaffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("New")
            .lastName("NewName")
            .email("old@ucsb.edu")
            .course(course1)
            .build();

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(existingStaffMember));
    when(courseStaffRepository.save(any(CourseStaff.class))).thenReturn(updatedStaffMember);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/coursestaff")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<CourseStaff> captor = ArgumentCaptor.forClass(CourseStaff.class);
    verify(courseStaffRepository).save(captor.capture());
    CourseStaff saved = captor.getValue();
    assertEquals("New", saved.getFirstName());
    assertEquals("NewName", saved.getLastName());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedStaffMember);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateCourseStaff_course_not_found() throws Exception {

    when(courseRepository.findById(eq(42L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/coursestaff")
                    .with(csrf())
                    .param("courseId", "42")
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   "))
            .andExpect(status().isNotFound())
            .andReturn();
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_success() throws Exception {
    CourseStaff staffMember =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(course1)
            .build();

    List<CourseStaff> staffList = new ArrayList<>();
    staffList.add(staffMember);
    course1.setCourseStaff(staffList);

    when(courseStaffRepository.findById(eq(1L))).thenReturn(Optional.of(staffMember));
    when(courseRepository.save(any(Course.class))).thenReturn(course1);

    CourseStaff staffMemberUpdated =
        CourseStaff.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Staff")
            .email("teststaff@ucsb.edu")
            .course(null)
            .build();

    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseStaffRepository).findById(eq(1L));
    verify(courseStaffRepository).delete(eq(staffMemberUpdated));
    assertEquals(course1.getCourseStaff(), List.of());

    assertEquals("Successfully deleted staff member.", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteCourseStaff_notFound() throws Exception {
    when(courseStaffRepository.findById(eq(99L))).thenReturn(Optional.empty());
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "99")
                    .param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(courseStaffRepository).findById(eq(99L));
    verify(courseStaffRepository, never()).delete(any(CourseStaff.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "CourseStaff with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testDeleteCourseStaff_unauthorized() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                delete("/api/coursestaff/delete")
                    .with(csrf())
                    .param("id", "1")
                    .param("courseId", "7"))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseStaffRepository, never()).findById(any());
    verify(courseStaffRepository, never()).delete(any(CourseStaff.class));
  }
}
