package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.controller.CoursesController.InstructorCourseView;
import edu.ucsb.cs.scaffold.controller.CoursesController.StaffCoursesDTO;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.CourseStaff;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.entity.RosterStudent;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.jobs.SyncCourseWithPlRepoJobFactory;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.CourseStaffRepository;
import edu.ucsb.cs.scaffold.repository.InstructorRepository;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.RosterStudentRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.services.CurrentUserService;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService.CourseInstanceInfo;
import edu.ucsb.cs156.jobs.repositories.JobsRepository;
import edu.ucsb.cs156.jobs.services.JobService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
public class CoursesControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private UserRepository userRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @MockitoBean private InstructorRepository instructorRepository;

  @MockitoBean private AdminRepository adminRepository;

  @MockitoBean private JobsRepository jobsRepository;

  @MockitoBean private PatCredentialRepository patCredentialRepository;

  @MockitoBean private PatEncryptionService patEncryptionService;

  @MockitoBean private PlRepoRepository plRepoRepository;

  @MockitoBean private GithubService githubService;

  @MockitoBean private PlInstanceRepository plInstanceRepository;

  @MockitoBean private PrairieLearnService prairieLearnService;

  @MockitoBean private SyncCourseWithPlRepoJobFactory syncCourseWithPlRepoJobFactory;

  @MockitoBean private JobService jobService;

  /** Test that ROLE_ADMIN can create a course */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostCourse_byAdmin() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("canvas-token")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(course);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/post")
                    .with(csrf())
                    .param("courseName", "CS156")
                    .param("term", "S25")
                    .param("school", "UCSB")
                    .param("canvasApiToken", "canvas-token")
                    .param("canvasCourseId", "12345"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).save(eq(course));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
    assertEquals(expectedJson, responseString);
  }

  /** Test that ROLE_INSTRUCTOR can create a course */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testPostCourse_byInstructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("canvas-token")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(course);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/post")
                    .with(csrf())
                    .param("courseName", "CS156")
                    .param("term", "S25")
                    .param("school", "UCSB")
                    .param("canvasApiToken", "canvas-token")
                    .param("canvasCourseId", "12345"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).save(eq(course));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET all endpoint for courses for admins */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testAllCourses_ROLE_ADMIN() throws Exception {

    // arrange
    Course course1 = Course.builder().courseName("CS156").term("S25").school(School.UCSB).build();

    InstructorCourseView courseView1 = new InstructorCourseView(course1);

    Course course2 = Course.builder().courseName("CS148").term("S25").school(School.UCSB).build();
    InstructorCourseView courseView2 = new InstructorCourseView(course2);

    when(courseRepository.findAll()).thenReturn(java.util.List.of(course1, course2));

    // act

    MvcResult response =
        mockMvc.perform(get("/api/courses/list/admins")).andExpect(status().isOk()).andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1, courseView2));
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET endpoint for courses for instructors */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testAllCourses_ROLE_INSTRUCTOR() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .build();

    InstructorCourseView courseView1 = new InstructorCourseView(course1);

    when(courseRepository.findByInstructorEmail(eq(user.getEmail())))
        .thenReturn(java.util.List.of(course1));

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/list/instructors"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1));
    verify(courseRepository, times(1)).findByInstructorEmail(eq(user.getEmail()));
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void testListCoursesForCurrentUser() throws Exception {
    String email = "student@example.com";

    OAuth2User principal = Mockito.mock(OAuth2User.class);
    when(principal.getAttribute("email")).thenReturn(email);

    Authentication auth =
        new OAuth2AuthenticationToken(
            principal,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            "test-client");

    User dbUser = User.builder().email(email).build();
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(dbUser));

    Course course =
        Course.builder().id(55L).courseName("Test Course").term("S25").school(School.UCSB).build();

    RosterStudent rs = new RosterStudent();
    rs.setId(123L);
    rs.setCourse(course);
    rs.setEmail(email);

    when(rosterStudentRepository.findAllByEmail(eq(email))).thenReturn(List.of(rs));

    MvcResult result =
        mockMvc
            .perform(get("/api/courses/list/students").with(authentication(auth)))
            .andExpect(status().isOk())
            .andReturn();

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("id", course.getId());
    expected.put("courseName", course.getCourseName());
    expected.put("term", course.getTerm());
    expected.put("school", course.getSchool().getDisplayName());
    expected.put("rosterStudentId", rs.getId());

    String expectedJson = mapper.writeValueAsString(List.of(expected));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testStudenIsStaffInCourse() throws Exception {
    // arrange
    User currentUser = User.builder().id(123L).email("user@example.org").build();

    Course course1 =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    Course course2 =
        Course.builder().id(2L).courseName("CS24").term("S25").school(School.UCSB).build();

    CourseStaff cs1 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("user@example.org")
            .course(course1)
            .user(currentUser)
            .id(37L)
            .build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("user@example.org")
            .course(course2)
            .user(currentUser)
            .id(42L)
            .build();

    StaffCoursesDTO staffCourse1 =
        new StaffCoursesDTO(
            course1.getId(),
            course1.getCourseName(),
            course1.getTerm(),
            course1.getSchool(),
            cs1.getId());

    StaffCoursesDTO staffCourse2 =
        new StaffCoursesDTO(
            course2.getId(),
            course2.getCourseName(),
            course2.getTerm(),
            course2.getSchool(),
            cs2.getId());

    when(courseStaffRepository.findAllByEmail("user@example.org")).thenReturn(List.of(cs1, cs2));

    when(courseRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(course1, course2));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/list/staff").param("studentId", "123"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of(staffCourse1, staffCourse2));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testListCoursesForCurrentUserUnified_nonAdmin() throws Exception {
    // arrange
    String email = "user@example.org";

    Course studentCourse =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();
    Course staffCourse =
        Course.builder().id(2L).courseName("CS24").term("S25").school(School.UCSB).build();
    Course instructorCourse =
        Course.builder()
            .id(3L)
            .courseName("CS148")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(email)
            .build();

    RosterStudent rs = new RosterStudent();
    rs.setId(10L);
    rs.setCourse(studentCourse);
    rs.setEmail(email);

    CourseStaff cs = CourseStaff.builder().id(20L).email(email).course(staffCourse).build();

    when(adminRepository.existsByEmail(email)).thenReturn(false);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of(rs));
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of(cs));
    when(courseRepository.findByInstructorEmail(email)).thenReturn(List.of(instructorCourse));
    when(courseRepository.findAllById(any()))
        .thenReturn(List.of(studentCourse, staffCourse, instructorCourse));

    CoursesController.CourseListDTO studentDto =
        new CoursesController.CourseListDTO(
            studentCourse.getId(),
            studentCourse.getCourseName(),
            studentCourse.getTerm(),
            studentCourse.getSchool(),
            studentCourse.getInstructorEmail(),
            true,
            false,
            false,
            false);

    CoursesController.CourseListDTO staffDto =
        new CoursesController.CourseListDTO(
            staffCourse.getId(),
            staffCourse.getCourseName(),
            staffCourse.getTerm(),
            staffCourse.getSchool(),
            staffCourse.getInstructorEmail(),
            false,
            true,
            false,
            false);

    CoursesController.CourseListDTO instructorDto =
        new CoursesController.CourseListDTO(
            instructorCourse.getId(),
            instructorCourse.getCourseName(),
            instructorCourse.getTerm(),
            instructorCourse.getSchool(),
            instructorCourse.getInstructorEmail(),
            false,
            false,
            true,
            false);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of(studentDto, staffDto, instructorDto));
    assertEquals(expectedJson, responseString);
    verify(courseRepository, never()).findAll();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testListCoursesForCurrentUserUnified_admin() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course1 =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();
    Course course2 =
        Course.builder().id(2L).courseName("CS24").term("S25").school(School.UCSB).build();

    when(adminRepository.existsByEmail(email)).thenReturn(true);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of());
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of());
    when(courseRepository.findByInstructorEmail(email)).thenReturn(List.of());
    when(courseRepository.findAll()).thenReturn(List.of(course1, course2));

    CoursesController.CourseListDTO dto1 =
        new CoursesController.CourseListDTO(
            course1.getId(),
            course1.getCourseName(),
            course1.getTerm(),
            course1.getSchool(),
            course1.getInstructorEmail(),
            false,
            false,
            false,
            true);

    CoursesController.CourseListDTO dto2 =
        new CoursesController.CourseListDTO(
            course2.getId(),
            course2.getCourseName(),
            course2.getTerm(),
            course2.getSchool(),
            course2.getInstructorEmail(),
            false,
            false,
            false,
            true);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of(dto1, dto2));
    assertEquals(expectedJson, responseString);
    verify(courseRepository, never()).findAllById(any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetCourseAccessInfo_withInstructorAccess() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(email)
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(adminRepository.existsByEmail(email)).thenReturn(false);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of());
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of());

    CoursesController.CourseListDTO expectedDto =
        new CoursesController.CourseListDTO(
            course.getId(),
            course.getCourseName(),
            course.getTerm(),
            course.getSchool(),
            course.getInstructorEmail(),
            false,
            false,
            true,
            false);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedDto);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetCourseAccessInfo_withStudentAccess() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    RosterStudent rs = new RosterStudent();
    rs.setId(10L);
    rs.setCourse(course);
    rs.setEmail(email);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(adminRepository.existsByEmail(email)).thenReturn(false);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of(rs));
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of());

    CoursesController.CourseListDTO expectedDto =
        new CoursesController.CourseListDTO(
            course.getId(),
            course.getCourseName(),
            course.getTerm(),
            course.getSchool(),
            course.getInstructorEmail(),
            true,
            false,
            false,
            false);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedDto);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetCourseAccessInfo_withStaffAccess() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course =
        Course.builder().id(1L).courseName("CS156").term("S25").school(School.UCSB).build();

    CourseStaff cs = CourseStaff.builder().id(20L).email(email).course(course).build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(adminRepository.existsByEmail(email)).thenReturn(false);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of());
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of(cs));

    CoursesController.CourseListDTO expectedDto =
        new CoursesController.CourseListDTO(
            course.getId(),
            course.getCourseName(),
            course.getTerm(),
            course.getSchool(),
            course.getInstructorEmail(),
            false,
            true,
            false,
            false);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedDto);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testGetCourseAccessInfo_withAdminAccessOnly() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("someone_else@example.org")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(adminRepository.existsByEmail(email)).thenReturn(true);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of());
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of());

    CoursesController.CourseListDTO expectedDto =
        new CoursesController.CourseListDTO(
            course.getId(),
            course.getCourseName(),
            course.getTerm(),
            course.getSchool(),
            course.getInstructorEmail(),
            false,
            false,
            false,
            true);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(expectedDto);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetCourseAccessInfo_noAccess() throws Exception {
    // arrange
    String email = "user@example.org";

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("someone_else@example.org")
            .build();

    Course otherCourse =
        Course.builder().id(2L).courseName("CS24").term("S25").school(School.UCSB).build();

    RosterStudent rsOtherCourse = new RosterStudent();
    rsOtherCourse.setId(99L);
    rsOtherCourse.setCourse(otherCourse);
    rsOtherCourse.setEmail(email);

    CourseStaff csOtherCourse =
        CourseStaff.builder().id(98L).email(email).course(otherCourse).build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(adminRepository.existsByEmail(email)).thenReturn(false);
    when(rosterStudentRepository.findAllByEmail(email)).thenReturn(List.of(rsOtherCourse));
    when(courseStaffRepository.findAllByEmail(email)).thenReturn(List.of(csOtherCourse));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isNotFound()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testGetCourseAccessInfo_courseDoesNotExist() throws Exception {
    // arrange
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/list/1")).andExpect(status().isNotFound()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCourseById() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .build();

    CoursesController.InstructorCourseView courseView =
        new CoursesController.InstructorCourseView(course);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(courseView);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCourseById_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isNotFound()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testGetCourseById_AdminCanGetCourseCreatedBySomeoneElse() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_returnsCorrectOutput() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .canvasApiToken("canvas-token-1234567890")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "********************890");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCanvasInfo_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForLessThanThreeCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .canvasApiToken("12")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "12");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForNoChars() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "",
            "canvasApiToken", "");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForThreeCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .canvasApiToken("123")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "123");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForFourCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(otherInstructorUser.getEmail())
            .canvasApiToken("1234")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "*234");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, rosterStudentRepository);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_success_returns_ok() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(Collections.emptyList())
            .courseStaff(Collections.emptyList())
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    verify(jobsRepository).deleteByScopeTypeAndScopeId(eq("course"), eq(1L));
    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).delete(eq(course));
    verifyNoMoreInteractions(courseRepository, rosterStudentRepository, jobsRepository);
    String expectedJson = mapper.writeValueAsString(Map.of("message", "Course with id 1 deleted"));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_students_throws_illegal_argument() throws Exception {
    RosterStudent student = RosterStudent.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(List.of(student))
            .courseStaff(Collections.emptyList())
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, rosterStudentRepository, jobsRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_staff_throws_illegal_argument() throws Exception {
    CourseStaff staff = CourseStaff.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(Collections.emptyList())
            .courseStaff(List.of(staff))
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, rosterStudentRepository, jobsRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_students_and_staff_throws_illegal_argument() throws Exception {
    RosterStudent student = RosterStudent.builder().id(1L).build();
    CourseStaff staff = CourseStaff.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(List.of(student))
            .courseStaff(List.of(staff))
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, rosterStudentRepository, jobsRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void delete_course_non_admin_returns_forbidden() throws Exception {
    mockMvc
        .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(courseRepository, rosterStudentRepository, jobsRepository);
  }

  /**
   * Test that when we try to update the instructor emaii, if the course does not exist, it returns
   * an appropriate error.
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  /** Test that ROLE_ADMIN can update instructor email */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_instructor() throws Exception {
    User admin = currentUserService.getCurrentUser().getUser();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /** Test that ROLE_ADMIN can update instructor email */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_admin() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(eq(updatedCourse));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /** Test that updating an instructor email sanitizes the address properly */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_sanitized() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", " new-instructor@example.com "))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(eq(updatedCourse));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /**
   * Test that updateInstructorEmail fails when email doesn't exist in instructor or admin tables
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_emailNotFound() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);

    // act & assert
    mockMvc
        .perform(
            put("/api/courses/updateInstructor")
                .with(csrf())
                .param("courseId", "1")
                .param("instructorEmail", "nonexistent@example.com"))
        .andExpect(status().isBadRequest());

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
    verify(adminRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
    verify(courseRepository, never()).save(any(Course.class));
  }

  /** Test that updateInstructorEmail requires ADMIN role */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testUpdateInstructorEmail_requiresAdmin() throws Exception {
    // act & assert
    mockMvc
        .perform(
            put("/api/courses/updateInstructor")
                .with(csrf())
                .param("courseId", "1")
                .param("instructorEmail", "new-instructor@example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateCourse_success_admin() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("OldName")
            .term("OldTerm")
            .school(School.UCSB)
            .instructorEmail("rando@example.com")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("NewName")
            .term("NewTerm")
            .school(School.OTHER)
            .instructorEmail("rando@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "NewName")
                    .param("term", "NewTerm")
                    .param("school", "OTHER")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository, times(1)).save(courseCaptor.capture());
    Course savedCourse = courseCaptor.getValue();
    assertEquals("NewName", savedCourse.getCourseName());
    assertEquals("NewTerm", savedCourse.getTerm());
    assertEquals(School.OTHER, savedCourse.getSchool());

    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void updateCourse_notFound() throws Exception {
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "2")
                    .param("courseName", "AnyName")
                    .param("term", "AnyTerm")
                    .param("school", "UCSB")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateCourse_forbidden_for_non_instructor() throws Exception {
    mockMvc
        .perform(
            put("/api/courses")
                .param("courseId", "1")
                .param("courseName", "NewName")
                .param("term", "NewTerm")
                .param("school", "UCSB")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_course_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Updated Course")
                    .param("term", "F25")
                    .param("school", "UCSB")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_course_success_returns_ok() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Updated Course")
            .term("F25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Updated Course")
                    .param("term", "F25")
                    .param("school", "UCSB")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void admin_can_update_course_created_by_someone_else() throws Exception {
    User adminUser = currentUserService.getCurrentUser().getUser();
    User instructorUser =
        User.builder().id(adminUser.getId() + 1L).email("instructor@example.com").build();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(instructorUser.getEmail())
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Admin Updated Course")
            .term("F25")
            .school(School.UCSB)
            .instructorEmail(instructorUser.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Admin Updated Course")
                    .param("term", "F25")
                    .param("school", "UCSB")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateCourseCanvasToken_success_admin() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("Name")
            .term("Term")
            .school(School.UCSB)
            .instructorEmail("rando@example.com")
            .canvasApiToken("oldToken")
            .canvasCourseId("oldCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Name")
            .term("Term")
            .school(School.UCSB)
            .instructorEmail("rando@example.com")
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseRepository, times(1)).save(any(Course.class));

    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void updateCourseCanvasToken_notFound() throws Exception {
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "2")
                    .param("canvasApiToken", "AnyToken")
                    .param("canvasCourseId", "AnyCourseId")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateCourseCanvasToken_forbidden_for_non_instructor() throws Exception {
    mockMvc
        .perform(
            put("/api/courses/updateCourseCanvasToken")
                .param("courseId", "1")
                .param("canvasApiToken", "newToken")
                .param("canvasCourseId", "newCourseId")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_success_returns_ok() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("originalToken")
            .canvasCourseId("originalCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void admin_can_updateCourseCanvasToken_created_by_someone_else() throws Exception {
    User adminUser = currentUserService.getCurrentUser().getUser();
    User instructorUser =
        User.builder().id(adminUser.getId() + 1L).email("instructor@example.com").build();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(instructorUser.getEmail())
            .canvasApiToken("originalToken")
            .canvasCourseId("originalCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(instructorUser.getEmail())
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  // Tests for InstructorCourseView constructor with null collections
  @Test
  public void testInstructorCourseView_withNullRosterStudents() throws Exception {
    /** Test that InstructorCourseView correctly counts students and staff */
    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("instructor@example.com")
            .rosterStudents(null) // explicitly null
            .courseStaff(List.of()) // empty list
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(1L, view.id());
    assertEquals("CS156", view.courseName());
    assertEquals("S25", view.term());
    assertEquals(School.UCSB, view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals(0, view.numStudents()); // should be 0 when null
    assertEquals(0, view.numStaff()); // should be 0 for empty list
  }

  @Test
  public void testInstructorCourseView_withNullCourseStaff() throws Exception {
    // arrange
    Course course =
        Course.builder()
            .id(2L)
            .courseName("CS148")
            .term("F25")
            .school(School.UCSB)
            .instructorEmail("instructor@example.com")
            .rosterStudents(List.of()) // empty list
            .courseStaff(null) // explicitly null
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(2L, view.id());
    assertEquals("CS148", view.courseName());
    assertEquals("F25", view.term());
    assertEquals(School.UCSB, view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals(0, view.numStudents()); // should be 0 for empty list
    assertEquals(0, view.numStaff()); // should be 0 when null
  }

  @Test
  public void testInstructorCourseView_withBothCollectionsNull() throws Exception {
    // arrange
    Course course =
        Course.builder()
            .id(3L)
            .courseName("CS24")
            .term("W25")
            .school(School.UCSB)
            .instructorEmail("instructor@example.com")
            .rosterStudents(null) // explicitly null
            .courseStaff(null) // explicitly null
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(3L, view.id());
    assertEquals("CS24", view.courseName());
    assertEquals("W25", view.term());
    assertEquals(School.UCSB, view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals(0, view.numStudents()); // should be 0 when null
    assertEquals(0, view.numStaff()); // should be 0 when null
  }

  @Test
  public void testInstructorCourseView_withNonNullCollections() throws Exception {
    // arrange
    RosterStudent student1 = RosterStudent.builder().id(1L).build();
    RosterStudent student2 = RosterStudent.builder().id(2L).build();
    CourseStaff staff1 = CourseStaff.builder().id(1L).build();
    CourseStaff staff2 = CourseStaff.builder().id(2L).build();
    CourseStaff staff3 = CourseStaff.builder().id(3L).build();

    Course course =
        Course.builder()
            .id(4L)
            .courseName("CS130A")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("instructor@example.com")
            .rosterStudents(List.of(student1, student2)) // 2 students
            .courseStaff(List.of(staff1, staff2, staff3)) // 3 staff
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals("CS130A", view.courseName());
    assertEquals("S25", view.term());
    assertEquals(School.UCSB, view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals(2, view.numStudents()); // should match list size
    assertEquals(3, view.numStaff()); // should match list size
  }

  @Test
  public void testInstructorCourseView_countsStudentsAndStaff() {
    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail("test@example.com")
            .build();

    // Test with null lists
    InstructorCourseView viewWithNulls = new InstructorCourseView(course);
    assertEquals(0, viewWithNulls.numStudents());
    assertEquals(0, viewWithNulls.numStaff());

    // Test with empty lists
    course.setRosterStudents(Collections.emptyList());
    course.setCourseStaff(Collections.emptyList());
    InstructorCourseView viewWithEmpty = new InstructorCourseView(course);
    assertEquals(0, viewWithEmpty.numStudents());

    // Test with populated lists
    RosterStudent student1 = RosterStudent.builder().id(1L).build();
    RosterStudent student2 = RosterStudent.builder().id(2L).build();
    CourseStaff staff1 = CourseStaff.builder().id(1L).build();

    course.setRosterStudents(List.of(student1, student2));
    course.setCourseStaff(List.of(staff1));
    InstructorCourseView viewWithData = new InstructorCourseView(course);
    assertEquals(2, viewWithData.numStudents());
    assertEquals(1, viewWithData.numStaff());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_same_value_does_not_change() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("sameToken")
            .canvasCourseId("sameCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

    mockMvc
        .perform(
            put("/api/courses/updateCourseCanvasToken")
                .param("courseId", "1")
                .param("canvasApiToken", "sameToken")
                .param("canvasCourseId", "sameCourseId")
                .with(csrf()))
        .andExpect(status().isOk());

    verify(courseRepository).save(eq(originalCourse));
    assertEquals("sameToken", originalCourse.getCanvasApiToken());
    assertEquals("sameCourseId", originalCourse.getCanvasCourseId());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_empty_string_no_change() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("existingToken")
            .canvasCourseId("existingCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

    mockMvc
        .perform(
            put("/api/courses/updateCourseCanvasToken")
                .param("courseId", "1")
                .param("canvasApiToken", "")
                .param("canvasCourseId", "")
                .with(csrf()))
        .andExpect(status().isOk());

    verify(courseRepository).save(originalCourse);
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_null_params_no_change() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school(School.UCSB)
            .instructorEmail(user.getEmail())
            .canvasApiToken("existingToken")
            .canvasCourseId("existingCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

    mockMvc
        .perform(put("/api/courses/updateCourseCanvasToken").param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    assertEquals("existingToken", originalCourse.getCanvasApiToken());
    assertEquals("existingCourseId", originalCourse.getCanvasCourseId());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseEmails_defaults_to_students_one_per_line() throws Exception {
    RosterStudent studentA = RosterStudent.builder().email("anna@ucsb.edu").build();
    RosterStudent studentB = RosterStudent.builder().email("zebra@ucsb.edu").build();

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(studentB, studentA));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/emails").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("anna@ucsb.edu\r\nzebra@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseEmails_staff_only_sorted_lexicographically() throws Exception {
    CourseStaff staffA = CourseStaff.builder().email("aardvark@ucsb.edu").build();
    CourseStaff staffB = CourseStaff.builder().email("zoe@ucsb.edu").build();

    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of(staffB, staffA));
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/emails").param("courseId", "1").param("type", "STAFF"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("aardvark@ucsb.edu\r\nzoe@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseEmails_students_explicit_type_comma_separated() throws Exception {
    RosterStudent studentA = RosterStudent.builder().email("alpha@ucsb.edu").build();
    RosterStudent studentB = RosterStudent.builder().email("zeta@ucsb.edu").build();
    CourseStaff staff = CourseStaff.builder().email("staff@ucsb.edu").build();

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(studentB, studentA));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of(staff));

    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/emails")
                    .param("courseId", "1")
                    .param("type", "STUDENTS")
                    .param("format", "COMMA_SEPARATED"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("alpha@ucsb.edu,zeta@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseEmails_students_blank_team_is_unfiltered() throws Exception {
    RosterStudent studentA = RosterStudent.builder().email("alpha@ucsb.edu").build();
    RosterStudent studentB = RosterStudent.builder().email("zeta@ucsb.edu").build();

    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(studentB, studentA));
    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of());

    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/emails")
                    .param("courseId", "1")
                    .param("team", "")
                    .param("format", "COMMA_SEPARATED"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("alpha@ucsb.edu,zeta@ucsb.edu", response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseEmails_all_comma_separated_staff_first() throws Exception {
    CourseStaff staffA = CourseStaff.builder().email("alpha-staff@ucsb.edu").build();
    CourseStaff staffB = CourseStaff.builder().email("zeta-staff@ucsb.edu").build();
    RosterStudent studentA = RosterStudent.builder().email("alpha-student@ucsb.edu").build();
    RosterStudent studentB = RosterStudent.builder().email("zeta-student@ucsb.edu").build();

    when(courseStaffRepository.findByCourseId(eq(1L))).thenReturn(List.of(staffB, staffA));
    when(rosterStudentRepository.findByCourseId(eq(1L))).thenReturn(List.of(studentB, studentA));

    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/emails")
                    .param("courseId", "1")
                    .param("type", "ALL")
                    .param("format", "COMMA_SEPARATED"))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        "alpha-staff@ucsb.edu,zeta-staff@ucsb.edu,alpha-student@ucsb.edu,zeta-student@ucsb.edu",
        response.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void getCourseEmails_forbidden_without_course_manage_permissions() throws Exception {
    mockMvc
        .perform(get("/api/courses/emails").param("courseId", "1"))
        .andExpect(status().isForbidden());
  }

  // ────────────────────────── updateGithubRepo ──────────────────────────

  private Course courseForRepoTests() {
    return Course.builder()
        .id(1L)
        .courseName("CS156")
        .term("S26")
        .school(School.UCSB)
        .instructorEmail("instructor@ucsb.edu")
        .build();
  }

  private void setupGithubPat() {
    PatCredential credential =
        PatCredential.builder()
            .id(5L)
            .userId(currentUserService.getCurrentUser().getUser().getId())
            .platform(PatPlatform.GITHUB)
            .ciphertext("CIPHER")
            .keyVersion(2)
            .lastFour("3f2a")
            .build();
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.of(credential));
    when(patEncryptionService.decrypt(eq("CIPHER"), eq(2))).thenReturn("ghp_token");
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateGithubRepo_forbidden_without_course_manage_permissions() throws Exception {
    mockMvc
        .perform(
            put("/api/courses/updateGithubRepo")
                .param("courseId", "1")
                .param("repoName", "ucsb-cs156/pl-demo")
                .with(csrf()))
        .andExpect(status().isForbidden());
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateGithubRepo_returns_404_when_course_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("Course with id 1 not found", json.get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_returns_403_when_user_has_no_github_pat() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseForRepoTests()));
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("must set up Github PAT first", json.get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_returns_403_when_the_pat_cannot_read_the_repo() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseForRepoTests()));
    setupGithubPat();
    when(githubService.hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token")))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("No access to repo via Github PAT token", json.get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_returns_403_when_the_pat_has_read_only_access() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseForRepoTests()));
    setupGithubPat();
    when(githubService.hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token"))).thenReturn(false);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("Read/write access to repo via Github PAT is required", json.get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_records_an_existing_pl_repo_id_on_the_course() throws Exception {
    Course course = courseForRepoTests();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    setupGithubPat();
    when(githubService.hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token"))).thenReturn(true);
    when(plRepoRepository.findByRepoName(eq("ucsb-cs156/pl-demo")))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(plRepoRepository.findById(eq(9L)))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(plRepoRepository, never()).save(any());
    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository, times(1)).save(captor.capture());
    assertEquals(Long.valueOf(9L), captor.getValue().getPlRepoId());
    Map<String, Object> json = responseToJson(response);
    assertEquals(9, json.get("plRepoId"));
    assertEquals("ucsb-cs156/pl-demo", json.get("plRepoName"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_creates_the_pl_repo_when_it_is_not_yet_in_the_table()
      throws Exception {
    Course course = courseForRepoTests();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    setupGithubPat();
    when(githubService.hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token"))).thenReturn(true);
    when(plRepoRepository.findByRepoName(eq("ucsb-cs156/pl-demo"))).thenReturn(Optional.empty());
    when(plRepoRepository.save(any(PlRepo.class)))
        .thenAnswer(
            inv -> {
              PlRepo saved = inv.getArgument(0);
              saved.setId(42L);
              return saved;
            });
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateGithubRepo")
                    .param("courseId", "1")
                    .param("repoName", "ucsb-cs156/pl-demo")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<PlRepo> repoCaptor = ArgumentCaptor.forClass(PlRepo.class);
    verify(plRepoRepository, times(1)).save(repoCaptor.capture());
    assertEquals("ucsb-cs156/pl-demo", repoCaptor.getValue().getRepoName());
    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository, times(1)).save(captor.capture());
    assertEquals(Long.valueOf(42L), captor.getValue().getPlRepoId());
    Map<String, Object> json = responseToJson(response);
    assertEquals(42, json.get("plRepoId"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateGithubRepo_strips_surrounding_whitespace_from_the_repo_name() throws Exception {
    Course course = courseForRepoTests();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    setupGithubPat();
    when(githubService.hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token"))).thenReturn(true);
    when(plRepoRepository.findByRepoName(eq("ucsb-cs156/pl-demo")))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc
        .perform(
            put("/api/courses/updateGithubRepo")
                .param("courseId", "1")
                .param("repoName", "  ucsb-cs156/pl-demo\n")
                .with(csrf()))
        .andExpect(status().isOk());

    verify(githubService, times(1)).hasWriteAccess(eq("ucsb-cs156/pl-demo"), eq("ghp_token"));
    verify(plRepoRepository, times(1)).findByRepoName(eq("ucsb-cs156/pl-demo"));
  }

  // ────────────────────────── updatePLInstance ──────────────────────────

  private static final String INFO_COURSE_INSTANCE_JSON =
      """
      { "uuid": "eb2647dc-9e3d-46cf-b365-b952dbe617e4", "longName": "Spring 2026" }
      """;

  private Course courseWithRepo() {
    Course course = courseForRepoTests();
    course.setPlRepoId(9L);
    return course;
  }

  private void setupBothPats() {
    setupGithubPat();
    PatCredential plCredential =
        PatCredential.builder()
            .id(6L)
            .userId(currentUserService.getCurrentUser().getUser().getId())
            .platform(PatPlatform.PRAIRIELEARN)
            .ciphertext("PL_CIPHER")
            .keyVersion(2)
            .lastFour("3395")
            .build();
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.of(plCredential));
    when(patEncryptionService.decrypt(eq("PL_CIPHER"), eq(2))).thenReturn("pl_token");
  }

  private void setupVerifiedInstance() {
    when(plRepoRepository.findById(eq(9L)))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(prairieLearnService.getCourseInstance(eq(213133L), eq("pl_token")))
        .thenReturn(new CourseInstanceInfo(213133L, "Spring 2026", "S26"));
    when(githubService.getFileContent(
            eq("ucsb-cs156/pl-demo"),
            eq("courseInstances/S26/infoCourseInstance.json"),
            eq("ghp_token")))
        .thenReturn(INFO_COURSE_INSTANCE_JSON);
  }

  private MvcResult performUpdatePLInstance(int expectedStatus) throws Exception {
    return mockMvc
        .perform(
            put("/api/courses/updatePLInstance")
                .param("courseId", "1")
                .param("instanceId", "213133")
                .with(csrf()))
        .andExpect(status().is(expectedStatus))
        .andReturn();
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updatePLInstance_forbidden_without_course_manage_permissions() throws Exception {
    performUpdatePLInstance(403);
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updatePLInstance_returns_404_when_course_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    MvcResult response = performUpdatePLInstance(404);
    assertEquals("Course with id 1 not found", responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_user_has_no_github_pat() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("must set up Github PAT first", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
    verify(jobService, never()).runAsJob(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_user_has_no_prairielearn_pat() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupGithubPat();
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("must set up PrairieLearn PAT first", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_course_has_no_pl_repo() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseForRepoTests()));
    setupBothPats();

    MvcResult response = performUpdatePLInstance(403);
    assertEquals(
        "must associate course with PlRepo first", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_404_when_the_pl_repo_row_is_missing() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    when(plRepoRepository.findById(eq(9L))).thenReturn(Optional.empty());

    MvcResult response = performUpdatePLInstance(404);
    assertEquals("PlRepo with id 9 not found", responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_prairielearn_rejects_the_instance_id()
      throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    when(plRepoRepository.findById(eq(9L)))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(prairieLearnService.getCourseInstance(eq(213133L), eq("pl_token")))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_prairielearn_returns_no_usable_body()
      throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    when(plRepoRepository.findById(eq(9L)))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(prairieLearnService.getCourseInstance(eq(213133L), eq("pl_token"))).thenReturn(null);

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_the_repo_has_no_matching_instance_directory()
      throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    setupVerifiedInstance();
    when(githubService.getFileContent(
            eq("ucsb-cs156/pl-demo"),
            eq("courseInstances/S26/infoCourseInstance.json"),
            eq("ghp_token")))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_the_long_names_do_not_match() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    setupVerifiedInstance();
    when(githubService.getFileContent(
            eq("ucsb-cs156/pl-demo"),
            eq("courseInstances/S26/infoCourseInstance.json"),
            eq("ghp_token")))
        .thenReturn("{ \"longName\": \"Fall 2024\" }");

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
    verify(courseRepository, never()).save(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_the_long_name_is_missing_from_the_repo_json()
      throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    setupVerifiedInstance();
    when(githubService.getFileContent(
            eq("ucsb-cs156/pl-demo"),
            eq("courseInstances/S26/infoCourseInstance.json"),
            eq("ghp_token")))
        .thenReturn("{ \"uuid\": \"eb2647dc\" }");

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_returns_403_when_the_repo_json_is_unparseable() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(courseWithRepo()));
    setupBothPats();
    setupVerifiedInstance();
    when(githubService.getFileContent(
            eq("ucsb-cs156/pl-demo"),
            eq("courseInstances/S26/infoCourseInstance.json"),
            eq("ghp_token")))
        .thenReturn("not json at all {{{");

    MvcResult response = performUpdatePLInstance(403);
    assertEquals("course instance id not found", responseToJson(response).get("message"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_creates_the_pl_instance_when_it_does_not_exist() throws Exception {
    Course course = courseWithRepo();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    setupBothPats();
    setupVerifiedInstance();
    when(plInstanceRepository.findByPlRepoIdAndShortName(eq(9L), eq("S26")))
        .thenReturn(Optional.empty());
    when(plInstanceRepository.save(any(PlInstance.class)))
        .thenAnswer(
            inv -> {
              PlInstance saved = inv.getArgument(0);
              saved.setId(77L);
              return saved;
            });
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response = performUpdatePLInstance(200);

    ArgumentCaptor<PlInstance> instanceCaptor = ArgumentCaptor.forClass(PlInstance.class);
    verify(plInstanceRepository, times(1)).save(instanceCaptor.capture());
    PlInstance saved = instanceCaptor.getValue();
    assertEquals(Long.valueOf(9L), saved.getPlRepoId());
    assertEquals("S26", saved.getShortName());
    assertEquals("Spring 2026", saved.getLongName());
    assertEquals(Long.valueOf(213133L), saved.getNumericId());

    ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository, times(1)).save(courseCaptor.capture());
    assertEquals(Long.valueOf(77L), courseCaptor.getValue().getPlInstanceId());
    assertEquals(77, responseToJson(response).get("plInstanceId"));

    // a successful association immediately launches the sync job for the course
    verify(syncCourseWithPlRepoJobFactory, times(1)).create(eq(1L), eq(courseCaptor.getValue()));
    verify(jobService, times(1)).runAsJob(any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updatePLInstance_updates_the_numeric_id_when_the_pl_instance_exists()
      throws Exception {
    Course course = courseWithRepo();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    setupBothPats();
    setupVerifiedInstance();
    PlInstance existing = PlInstance.builder().id(55L).plRepoId(9L).shortName("S26").build();
    when(plInstanceRepository.findByPlRepoIdAndShortName(eq(9L), eq("S26")))
        .thenReturn(Optional.of(existing));
    when(plInstanceRepository.save(any(PlInstance.class))).thenAnswer(inv -> inv.getArgument(0));
    when(plInstanceRepository.findById(eq(55L))).thenReturn(Optional.of(existing));
    when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

    MvcResult response = performUpdatePLInstance(200);

    ArgumentCaptor<PlInstance> instanceCaptor = ArgumentCaptor.forClass(PlInstance.class);
    verify(plInstanceRepository, times(1)).save(instanceCaptor.capture());
    PlInstance saved = instanceCaptor.getValue();
    assertEquals(Long.valueOf(55L), saved.getId()); // same row, updated in place
    assertEquals(Long.valueOf(213133L), saved.getNumericId());
    assertEquals("Spring 2026", saved.getLongName());
    Map<String, Object> json = responseToJson(response);
    assertEquals(55, json.get("plInstanceId"));
    assertEquals("S26", json.get("plInstanceShortName"));
    assertEquals(213133, json.get("plInstanceNumericId"));
  }

  // ────────────── PL association details on single-course endpoints ──────────────

  @Test
  @WithInstructorCoursePermissions
  public void getCourseById_resolves_pl_association_details() throws Exception {
    Course course = courseForRepoTests();
    course.setPlRepoId(9L);
    course.setPlInstanceId(55L);
    course.setRosterStudents(
        List.of(RosterStudent.builder().build(), RosterStudent.builder().build()));
    course.setCourseStaff(List.of(CourseStaff.builder().build()));
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(plRepoRepository.findById(eq(9L)))
        .thenReturn(Optional.of(PlRepo.builder().id(9L).repoName("ucsb-cs156/pl-demo").build()));
    when(plInstanceRepository.findById(eq(55L)))
        .thenReturn(
            Optional.of(
                PlInstance.builder()
                    .id(55L)
                    .plRepoId(9L)
                    .shortName("S26")
                    .numericId(213133L)
                    .build()));

    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("ucsb-cs156/pl-demo", json.get("plRepoName"));
    assertEquals("S26", json.get("plInstanceShortName"));
    assertEquals(213133, json.get("plInstanceNumericId"));
    assertEquals(2, json.get("numStudents"));
    assertEquals(1, json.get("numStaff"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void getCourseById_leaves_details_null_when_the_associated_rows_are_missing()
      throws Exception {
    Course course = courseForRepoTests();
    course.setPlRepoId(9L);
    course.setPlInstanceId(55L);
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(plRepoRepository.findById(eq(9L))).thenReturn(Optional.empty());
    when(plInstanceRepository.findById(eq(55L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(9, json.get("plRepoId"));
    assertNull(json.get("plRepoName"));
    assertNull(json.get("plInstanceShortName"));
    assertNull(json.get("plInstanceNumericId"));
    // courseForRepoTests() has null staff/student lists: the counts fall back to 0
    assertEquals(0, json.get("numStudents"));
    assertEquals(0, json.get("numStaff"));
  }
}
