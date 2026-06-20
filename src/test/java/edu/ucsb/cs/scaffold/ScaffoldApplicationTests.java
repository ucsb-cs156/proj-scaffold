package edu.ucsb.cs.scaffold;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs.scaffold.repository.UserActivityRepository;
import edu.ucsb.cs.scaffold.repository.UserStateRepository;
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

        mockMvc.perform(post("/api/user-state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userid": 1234,
                                  "starred_ids": ["graph-a", "graph-b"],
                                  "detail_cards": [{"cardType":"hint","itemLabel":"Item 1"}],
                                  "mastered_subconcepts": ["sub-1"]
                                }
                                """))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/user-state/1"))
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

        mockMvc.perform(get("/api/user-state/0"))
                .andExpect(status().isNotFound());
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

    org.assertj.core.api.Assertions.assertThat(userActivityRepository.count()).isEqualTo(1);
  }
}
