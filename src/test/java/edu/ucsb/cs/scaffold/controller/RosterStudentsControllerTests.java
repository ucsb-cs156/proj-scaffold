package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.InsertStatus;
import edu.ucsb.cs.scaffold.enums.RosterStatus;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.model.RosterStudentDTO;
import edu.ucsb.cs.scaffold.model.UpsertResponse;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs156.jobs.services.JobService;
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
@WebMvcTest(controllers = {RosterStudentsController.class})
public class RosterStudentsControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UpdateUserService updateUserService;

  @MockitoBean private JobService service;

  @Autowired private ObjectMapper objectMapper;

  Course course1 =
      Course.builder()
          .id(1L)
          .courseName("CS156")
          .rosterStudents(List.of())
          .term("S25")
          .school(School.UCSB)
          .build();

  Course course2 =
      Course.builder()
          .id(2L)
          .courseName("CS156")
          .rosterStudents(List.of())
          .term("S25")
          .school(School.UCSB)
          .build();

  RosterStudent rs1 =
      RosterStudent.builder()
          .firstName("Chris")
          .lastName("Gaucho")
          .studentId("A123456")
          .email("cgaucho@example.org")
          .course(course1)
          .rosterStatus(RosterStatus.MANUAL)
          .build();

  RosterStudent rs2 =
      RosterStudent.builder()
          .id(2L)
          .firstName("Lauren")
          .lastName("Del Playa")
          .studentId("A987654")
          .email("ldelplaya@ucsb.edu")
          .course(course1)
          .rosterStatus(RosterStatus.ROSTER)
          .build();

  /** Test the POST endpoint */
  @Test
  @WithInstructorCoursePermissions
  public void testPostRosterStudent() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(rs1);
    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));
    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(eq(rs1));
    verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rs1));

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.INSERTED, upsertResponse.insertStatus());
  }

  /** Test the POST endpoint to make sure emails are sanitized when posting */
  @Test
  @WithInstructorCoursePermissions
  public void testPostRosterStudent_emailSanitized() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(rs1);
    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));
    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", " cgaucho@example.org ")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(eq(rs1));
    verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rs1));

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.INSERTED, upsertResponse.insertStatus());
  }

  /** Test that the POST endpoint converts @umail.ucsb.edu to @ucsb.edu */
  @Test
  @WithInstructorCoursePermissions
  public void testPostRosterStudent_withUmail() throws Exception {

    RosterStudent rsUmail =
        RosterStudent.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .studentId("A123456")
            .email("cgaucho@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.save(eq(rsUmail))).thenReturn(rsUmail);
    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));
    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@umail.ucsb.edu")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(eq(rsUmail));
    verify(updateUserService, times(1)).attachUserToRosterStudent(eq(rsUmail));

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.INSERTED, upsertResponse.insertStatus());
  }

  /** Test the POST endpoint when installation ID is null. */
  @Test
  @WithInstructorCoursePermissions
  public void testPostRosterStudentWithNoInstallationId() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));

    ArgumentCaptor<RosterStudent> rosterStudentCaptor =
        ArgumentCaptor.forClass(RosterStudent.class);

    when(rosterStudentRepository.save(any(RosterStudent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(rosterStudentCaptor.capture());

    RosterStudent rosterStudentSaved = rosterStudentCaptor.getValue();

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.INSERTED, upsertResponse.insertStatus());
  }

  /**
   * Test that you cannot post a single roster student for a course that does not exist
   *
   * @throws Exception
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void test_InstructorCannotPostRosterStudentForCourseThatDoesNotExist() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
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

  /** Test the POST endpoint */
  @Test
  @WithInstructorCoursePermissions
  public void test_post_fails_on_matching() throws Exception {

    RosterStudent rosterStudent1 =
        RosterStudent.builder().id(1L).studentId("A123456").course(course1).build();
    RosterStudent rosterStudent2 =
        RosterStudent.builder().id(2L).email("cgaucho@example.org").course(course1).build();
    course1.setRosterStudents(List.of(rosterStudent1, rosterStudent2));
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "Chris")
                    .param("lastName", "Gaucho")
                    .param("email", "cgaucho@example.org")
                    .param("courseId", "1"))
            .andExpect(status().isConflict())
            .andReturn();
  }

  /** Test the GET endpoint */
  @Test
  @WithInstructorCoursePermissions
  public void testRosterStudentsByCourse() throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.findByCourseIdOrderByFirstNameAscLastNameAscIgnoreCase(eq(1L)))
        .thenReturn(java.util.List.of(rs1, rs2));
    List<RosterStudentDTO> expectedRosterStudents =
        java.util.List.of(new RosterStudentDTO(rs1), new RosterStudentDTO(rs2));

    // act

    MvcResult response =
        mockMvc.perform(get("/api/rosterstudents/course/1")).andExpect(status().isOk()).andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedRosterStudents);
    assertEquals(expectedJson, responseString);
  }

  /** Test whether instructor can get roster students for a non existing course */
  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void getting_roster_students_for_a_non_existing_course_returns_appropriate_error()
      throws Exception {

    // arrange

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/rosterstudents/course/1"))
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
  @WithMockUser(roles = {"USER"})
  public void testGetAssociatedRosterStudents() throws Exception {
    // Arrange
    User currentUser = currentUserService.getUser();

    RosterStudent rs1WithUser =
        RosterStudent.builder()
            .id(1L)
            .firstName("Chris")
            .lastName("Gaucho")
            .studentId("A123456")
            .email("cgaucho@example.org")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .user(currentUser)
            .build();

    RosterStudent rs2WithUser =
        RosterStudent.builder()
            .id(2L)
            .firstName("Lauren")
            .lastName("Del Playa")
            .studentId("A987654")
            .email("ldelplaya@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .user(currentUser)
            .build();

    List<RosterStudent> expectedRosterStudents = List.of(rs1WithUser, rs2WithUser);

    when(rosterStudentRepository.findAllByUser(eq(currentUser))).thenReturn(expectedRosterStudents);

    // Act
    MvcResult response =
        mockMvc
            .perform(get("/api/rosterstudents/associatedRosterStudents").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(rosterStudentRepository, times(1)).findAllByUser(eq(currentUser));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedRosterStudents);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetAssociatedRosterStudents_noStudentsFound() throws Exception {
    // Arrange
    User currentUser = currentUserService.getUser();

    when(rosterStudentRepository.findAllByUser(eq(currentUser))).thenReturn(List.of());

    // Act
    MvcResult response =
        mockMvc
            .perform(get("/api/rosterstudents/associatedRosterStudents").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(rosterStudentRepository, times(1)).findAllByUser(eq(currentUser));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of());
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_emptyFields() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "   ")
                    .param("lastName", "   ")
                    .param("studentId", "   "))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateRosterStudent_success() throws Exception {
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent updatedStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("NewName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   ")
                    .param("studentId", "   A123456   "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(rosterStudentRepository).save(captor.capture());
    RosterStudent saved = captor.getValue();
    assertEquals("New", saved.getFirstName());
    assertEquals("NewName", saved.getLastName());
    assertEquals("A123456", saved.getStudentId());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedStudent);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_duplicateStudentId() throws Exception {
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent otherStudent =
        RosterStudent.builder()
            .id(2L)
            .firstName("Other")
            .lastName("Student")
            .studentId("A789012")
            .email("other@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
    when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A789012")))
        .thenReturn(Optional.of(otherStudent));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "New")
                    .param("lastName", "NewName")
                    .param("studentId", "A789012"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository).findById(eq(1L));
    verify(rosterStudentRepository).findByCourseIdAndStudentId(eq(1L), eq("A789012"));
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Student ID already exists in this course", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_notFound() throws Exception {
    when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "99")
                    .param("firstName", "New")
                    .param("lastName", "Name")
                    .param("studentId", "A123456"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(rosterStudentRepository).findById(eq(99L));
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "RosterStudent with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testUpdateRosterStudent_unauthorized() throws Exception {
    mockMvc
        .perform(
            put("/api/rosterstudents/update")
                .with(csrf())
                .param("id", "1")
                .param("firstName", "New")
                .param("lastName", "Name")
                .param("studentId", "A123456"))
        .andExpect(status().isForbidden());

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_newStudentIdNotExists() throws Exception {
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent updatedStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("NewName")
            .studentId("A999999")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
    when(rosterStudentRepository.findByCourseIdAndStudentId(eq(1L), eq("A999999")))
        .thenReturn(Optional.empty());
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "   New   ")
                    .param("lastName", "   NewName   ")
                    .param("studentId", "   A999999   "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(rosterStudentRepository).save(captor.capture());
    RosterStudent saved = captor.getValue();
    assertEquals("New", saved.getFirstName());
    assertEquals("NewName", saved.getLastName());
    assertEquals("A999999", saved.getStudentId());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedStudent);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_sameStudentIdWithWhitespace() throws Exception {
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Old")
            .lastName("OldName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    RosterStudent updatedStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("NewName")
            .studentId("A123456")
            .email("old@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existingStudent));
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(updatedStudent);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "  New  ")
                    .param("lastName", "  NewName  ")
                    .param("studentId", "  A123456  "))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<RosterStudent> captor = ArgumentCaptor.forClass(RosterStudent.class);
    verify(rosterStudentRepository).save(captor.capture());
    RosterStudent saved = captor.getValue();
    assertEquals("New", saved.getFirstName());
    assertEquals("NewName", saved.getLastName());
    assertEquals("A123456", saved.getStudentId());

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(updatedStudent);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_nullFields() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("lastName", "Doe")
                    .param("studentId", "A123456"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_nullFirstName() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("lastName", "Doe")
                    .param("studentId", "A123456"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_nullLastName() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "John")
                    .param("studentId", "A123456"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_nullStudentId() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "John")
                    .param("lastName", "Doe"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_emptyFirstName() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "")
                    .param("lastName", "Doe")
                    .param("studentId", "A123456"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_emptyLastName() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "John")
                    .param("lastName", "")
                    .param("studentId", "A123456"))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_emptyStudentId() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                put("/api/rosterstudents/update")
                    .with(csrf())
                    .param("id", "1")
                    .param("firstName", "John")
                    .param("lastName", "Doe")
                    .param("studentId", ""))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getErrorMessage();
    assertEquals("Required fields cannot be empty", responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteRosterStudent_success() throws Exception {
    RosterStudent rosterStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Student")
            .studentId("A123456")
            .email("test@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    List<RosterStudent> students = new ArrayList<>();
    students.add(rosterStudent);
    course1.setRosterStudents(students);

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));

    MvcResult response =
        mockMvc
            .perform(delete("/api/rosterstudents/delete").with(csrf()).param("id", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository).findById(eq(1L));
    verify(rosterStudentRepository).delete(eq(rosterStudent));
    assertEquals(course1.getRosterStudents(), List.of());
    // Since the student doesn't have a GitHub login, removeOrganizationMember should not be called

    assertEquals(
        "Successfully deleted roster student and removed him/her from the course list",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteRosterStudent_withGithubLogin_noOrgName_success() throws Exception {

    RosterStudent rosterStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Test")
            .lastName("Student")
            .studentId("A123456")
            .email("test@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();

    List<RosterStudent> students = new ArrayList<>();
    students.add(rosterStudent);
    course1.setRosterStudents(students);

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(rosterStudent));

    MvcResult response =
        mockMvc
            .perform(delete("/api/rosterstudents/delete").with(csrf()).param("id", "1"))
            .andExpect(status().isOk())
            .andReturn();

    verify(rosterStudentRepository).findById(eq(1L));
    verify(rosterStudentRepository).delete(eq(rosterStudent));
    assertEquals(course1.getRosterStudents(), List.of());

    assertEquals(
        "Successfully deleted roster student and removed him/her from the course list",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testDeleteRosterStudent_notFound() throws Exception {
    when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/rosterstudents/delete").with(csrf()).param("id", "99"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(rosterStudentRepository).findById(eq(99L));
    verify(rosterStudentRepository, never()).delete(any(RosterStudent.class));
    verify(courseRepository, never()).save(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "RosterStudent with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testDeleteRosterStudent_unauthorized() throws Exception {
    mockMvc
        .perform(delete("/api/rosterstudents/delete").with(csrf()).param("id", "1"))
        .andExpect(status().isForbidden());

    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).delete(any(RosterStudent.class));
    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpsertStudentWithDuplicateEmail() throws Exception {
    // Arrange
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Existing")
            .lastName("Student")
            .studentId("A123456")
            .email("cgaucho@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    RosterStudent newStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("Student")
            .studentId("A123457")
            .email("cgaucho@umail.ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .build();
    RosterStudent expectedSaved =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("Student")
            .studentId("A123457")
            .email("cgaucho@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .build();

    course1.setRosterStudents(List.of(existingStudent));

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.save(eq(expectedSaved))).thenReturn(expectedSaved);
    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123457")
                    .param("firstName", "New")
                    .param("lastName", "Student")
                    .param("email", "cgaucho@umail.ucsb.edu")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.UPDATED, upsertResponse.insertStatus());
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(eq(expectedSaved));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpsertStudentUpdatingTheEmail() throws Exception {
    // Arrange
    RosterStudent existingStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Existing")
            .lastName("Student")
            .studentId("A123456")
            .email("oldemail@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.ROSTER)
            .build();
    RosterStudent expectedSaved =
        RosterStudent.builder()
            .id(1L)
            .firstName("New")
            .lastName("But Same Student")
            .studentId("A123456")
            .email("newemail@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .build();

    course1.setRosterStudents(List.of(existingStudent));
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));
    when(rosterStudentRepository.save(eq(expectedSaved))).thenReturn(expectedSaved);
    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/rosterstudents/post")
                    .with(csrf())
                    .param("studentId", "A123456")
                    .param("firstName", "New")
                    .param("lastName", "But Same Student")
                    .param("email", "newemail@umail.ucsb.edu")
                    .param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    UpsertResponse upsertResponse = mapper.readValue(responseString, UpsertResponse.class);
    assertEquals(InsertStatus.UPDATED, upsertResponse.insertStatus());
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(rosterStudentRepository, times(1)).save(eq(expectedSaved));
  }

  @Test
  @WithInstructorCoursePermissions
  public void testRestoreRosterStudent_success() throws Exception {
    // Arrange
    RosterStudent droppedStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Dropped")
            .lastName("Student")
            .studentId("A123456")
            .email("dropped@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.DROPPED)
            .build();

    RosterStudent restoredStudent =
        RosterStudent.builder()
            .id(1L)
            .firstName("Dropped")
            .lastName("Student")
            .studentId("A123456")
            .email("dropped@ucsb.edu")
            .course(course1)
            .rosterStatus(RosterStatus.MANUAL)
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(droppedStudent));
    when(rosterStudentRepository.save(any(RosterStudent.class))).thenReturn(restoredStudent);

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/rosterstudents/restore").with(csrf()).param("id", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(rosterStudentRepository).findById(eq(1L));
    verify(rosterStudentRepository).save(eq(restoredStudent));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(restoredStudent);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testRestoreRosterStudent_notFound() throws Exception {
    // Arrange
    when(rosterStudentRepository.findById(eq(99L))).thenReturn(Optional.empty());

    // Act
    MvcResult response =
        mockMvc
            .perform(put("/api/rosterstudents/restore").with(csrf()).param("id", "99"))
            .andExpect(status().isNotFound())
            .andReturn();

    // Assert
    verify(rosterStudentRepository, times(1)).findById(eq(99L));
    verify(rosterStudentRepository, never()).save(any(RosterStudent.class));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "RosterStudent with id 99 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testRestoreRosterStudent_unauthorized() throws Exception {
    // Act
    mockMvc
        .perform(put("/api/rosterstudents/restore").with(csrf()).param("id", "1"))
        .andExpect(status().isForbidden());

    // Assert
    verify(rosterStudentRepository, never()).findById(any());
    verify(rosterStudentRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_updatesSectionWhenProvided() throws Exception {
    // Arrange
    RosterStudent existing =
        RosterStudent.builder()
            .id(1L)
            .firstName("Chris")
            .lastName("Gaucho")
            .studentId("A123456")
            .email("cgaucho@example.org")
            .course(course1)
            .section("0101")
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existing));

    ArgumentCaptor<RosterStudent> rosterStudentCaptor =
        ArgumentCaptor.forClass(RosterStudent.class);

    when(rosterStudentRepository.save(any(RosterStudent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    mockMvc
        .perform(
            put("/api/rosterstudents/update")
                .with(csrf())
                .param("id", "1")
                .param("firstName", "ChrisNew")
                .param("lastName", "GauchoNew")
                .param("studentId", "A123456")
                .param("section", "0202"))
        .andExpect(status().isOk());

    verify(rosterStudentRepository).save(rosterStudentCaptor.capture());
    RosterStudent saved = rosterStudentCaptor.getValue();

    // Assert
    assertEquals("0202", saved.getSection());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testUpdateRosterStudent_doesNotChangeSectionWhenNotProvided() throws Exception {
    // Arrange
    RosterStudent existing =
        RosterStudent.builder()
            .id(1L)
            .firstName("Chris")
            .lastName("Gaucho")
            .studentId("A123456")
            .email("cgaucho@example.org")
            .course(course1)
            .section("0101")
            .build();

    when(rosterStudentRepository.findById(eq(1L))).thenReturn(Optional.of(existing));

    ArgumentCaptor<RosterStudent> rosterStudentCaptor =
        ArgumentCaptor.forClass(RosterStudent.class);

    when(rosterStudentRepository.save(any(RosterStudent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    mockMvc
        .perform(
            put("/api/rosterstudents/update")
                .with(csrf())
                .param("id", "1")
                .param("firstName", "ChrisNew")
                .param("lastName", "GauchoNew")
                .param("studentId", "A123456"))
        .andExpect(status().isOk());

    verify(rosterStudentRepository).save(rosterStudentCaptor.capture());
    RosterStudent saved = rosterStudentCaptor.getValue();

    // Assert
    assertEquals("0101", saved.getSection());
  }

  @Test
  @WithInstructorCoursePermissions
  public void testPostRosterStudent_withSection() throws Exception {
    // Arrange
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course1));

    ArgumentCaptor<RosterStudent> rosterStudentCaptor =
        ArgumentCaptor.forClass(RosterStudent.class);

    when(rosterStudentRepository.save(any(RosterStudent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    doNothing().when(updateUserService).attachUserToRosterStudent(any(RosterStudent.class));

    // Act
    mockMvc
        .perform(
            post("/api/rosterstudents/post")
                .with(csrf())
                .param("studentId", "A123456")
                .param("firstName", "Chris")
                .param("lastName", "Gaucho")
                .param("email", "cgaucho@example.org")
                .param("courseId", "1")
                .param("section", "0101"))
        .andExpect(status().isOk());

    verify(rosterStudentRepository).save(rosterStudentCaptor.capture());
    RosterStudent saved = rosterStudentCaptor.getValue();

    // Assert
    assertEquals("0101", saved.getSection());
  }
}
