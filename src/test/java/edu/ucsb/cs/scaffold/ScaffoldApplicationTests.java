package edu.ucsb.cs.scaffold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.UserActivityRepository;
import edu.ucsb.cs.scaffold.repository.UserActivityV2Repository;
import edu.ucsb.cs.scaffold.repository.UserStateRepository;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ScaffoldApplicationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserStateRepository userStateRepository;

  @Autowired private UserActivityRepository userActivityRepository;

  @Autowired private UserStateV2Repository userStateV2Repository;

  @Autowired private UserActivityV2Repository userActivityV2Repository;

  @Autowired private CourseRepository courseRepository;

  @Test
  void healthCheckReturnsOk() throws Exception {
    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void getAssessmentsReturnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/assessments"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  void userStateCanBeUpsertedAndFetchedByUserId() throws Exception {
    userStateRepository.deleteAll();

    mockMvc
        .perform(
            post("/api/user-state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1234,
                                  "starred_ids": ["graph-a", "graph-b"],
                                  "detail_cards": [{"cardType":"hint","itemLabel":"Item 1"}],
                                  "mastered_subconcepts": ["sub-1"]
                                }
                                """))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state/1234"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.starred_ids[0]").value("graph-a"))
        .andExpect(jsonPath("$.starred_ids[1]").value("graph-b"))
        .andExpect(jsonPath("$.detail_cards[0].cardType").value("hint"))
        .andExpect(jsonPath("$.mastered_subconcepts[0]").value("sub-1"));
  }

  @Test
  void missingUserStateReturns404() throws Exception {
    userStateRepository.deleteAll();

    mockMvc.perform(get("/api/user-state/0")).andExpect(status().isNotFound());
  }

  @Test
  void userActivityCanBeLogged() throws Exception {
    userActivityRepository.deleteAll();

    mockMvc
        .perform(
            post("/api/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1234,
                                  "event_type": "login",
                                  "payload": {"source": "test"}
                                }
                                """))
        .andExpect(status().isNoContent());

    assertThat(userActivityRepository.count()).isEqualTo(1);
  }

  @Test
  void insertUserActivityWithNullUseridReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "event_type": "login"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void insertUserActivityWithNullEventTypeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void insertUserActivityWithBlankEventTypeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1,
                                  "event_type": "   "
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void insertUserActivityWithNullPayloadSucceeds() throws Exception {
    userActivityRepository.deleteAll();

    mockMvc
        .perform(
            post("/api/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1,
                                  "event_type": "login"
                                }
                                """))
        .andExpect(status().isNoContent());

    assertThat(userActivityRepository.count()).isEqualTo(1);
  }

  @Test
  void upsertUserStateWithNullUseridReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "starred_ids": []
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upsertUserStateWithNullFieldsUsesDefaults() throws Exception {
    userStateRepository.deleteAll();

    mockMvc
        .perform(
            post("/api/user-state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 42
                                }
                                """))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state/42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.starred_ids").isArray())
        .andExpect(jsonPath("$.mastered_subconcepts").isArray());
  }

  @Test
  void getSystemInfoReturnsOk() throws Exception {
    mockMvc
        .perform(get("/api/systemInfo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.oauthLogin").exists());
  }

  @Test
  void getQuestionsForAssessmentReturnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/assessments/{id}/questions", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void getConceptsForQuestionReturnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/questions/{id}/concepts", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void userStateV2CanBeUpsertedAndFetchedByUserIdAndCourseId() throws Exception {
    userStateV2Repository.deleteAll();
    Long courseId =
        courseRepository.save(Course.builder().courseName("Test Course").build()).getId();

    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1234,
                                  "courseId": %d,
                                  "starred_ids": ["graph-a", "graph-b"],
                                  "detail_cards": [{"cardType":"hint","itemLabel":"Item 1"}],
                                  "mastered_subconcepts": ["sub-1"]
                                }
                                """
                        .formatted(courseId)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state-v2").param("userid", "1234").param("courseId", "" + courseId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.starred_ids[0]").value("graph-a"))
        .andExpect(jsonPath("$.starred_ids[1]").value("graph-b"))
        .andExpect(jsonPath("$.detail_cards[0].cardType").value("hint"))
        .andExpect(jsonPath("$.mastered_subconcepts[0]").value("sub-1"));
  }

  @Test
  void userStateV2IsScopedPerCourseNotJustUserid() throws Exception {
    userStateV2Repository.deleteAll();
    Long courseA = courseRepository.save(Course.builder().courseName("Course A").build()).getId();
    Long courseB = courseRepository.save(Course.builder().courseName("Course B").build()).getId();

    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userid": 999, "courseId": %d, "starred_ids": ["course-a-concept"]}
                    """
                        .formatted(courseA)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userid": 999, "courseId": %d, "starred_ids": ["course-b-concept"]}
                    """
                        .formatted(courseB)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state-v2").param("userid", "999").param("courseId", "" + courseA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.starred_ids[0]").value("course-a-concept"));

    mockMvc
        .perform(get("/api/user-state-v2").param("userid", "999").param("courseId", "" + courseB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.starred_ids[0]").value("course-b-concept"));
  }

  @Test
  void missingUserStateV2Returns404() throws Exception {
    userStateV2Repository.deleteAll();

    mockMvc
        .perform(get("/api/user-state-v2").param("userid", "0").param("courseId", "0"))
        .andExpect(status().isNotFound());
  }

  @Test
  void upsertUserStateV2WithNullUseridReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "courseId": 1,
                                  "starred_ids": []
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upsertUserStateV2WithNullCourseIdReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1,
                                  "starred_ids": []
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upsertUserStateV2WithNullFieldsUsesDefaults() throws Exception {
    userStateV2Repository.deleteAll();
    Long courseId =
        courseRepository.save(Course.builder().courseName("Test Course").build()).getId();

    mockMvc
        .perform(
            post("/api/user-state-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 42,
                                  "courseId": %d
                                }
                                """
                        .formatted(courseId)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state-v2").param("userid", "42").param("courseId", "" + courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.starred_ids").isArray())
        .andExpect(jsonPath("$.mastered_subconcepts").isArray());
  }

  @Test
  void userActivityV2CanBeLogged() throws Exception {
    userActivityV2Repository.deleteAll();
    Long courseId =
        courseRepository.save(Course.builder().courseName("Test Course").build()).getId();

    mockMvc
        .perform(
            post("/api/user-activity-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1234,
                                  "courseId": %d,
                                  "event_type": "login",
                                  "payload": {"source": "test"}
                                }
                                """
                        .formatted(courseId)))
        .andExpect(status().isNoContent());

    assertThat(userActivityV2Repository.count()).isEqualTo(1);
  }

  @Test
  void insertUserActivityV2WithNullUseridReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-activity-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "courseId": 1,
                                  "event_type": "login"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void insertUserActivityV2WithNullCourseIdReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/user-activity-v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "userid": 1,
                                  "event_type": "login"
                                }
                                """))
        .andExpect(status().isBadRequest());
  }
}
