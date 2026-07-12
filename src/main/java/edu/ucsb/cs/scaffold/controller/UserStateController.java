package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.UserState;
import edu.ucsb.cs.scaffold.repository.UserStateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User State")
@RestController
@RequiredArgsConstructor
public class UserStateController {

  private final UserStateRepository userStateRepository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Get saved user state by user ID and course ID")
  @GetMapping("/api/user-state")
  public ResponseEntity<UserStateResponse> getUserState(
      @Parameter(description = "User ID (from users table)") @RequestParam Long userid,
      @Parameter(description = "Course ID (from course table)") @RequestParam Long courseId) {
    // A user with no saved state yet gets empty defaults rather than a 404, so the
    // frontend's useBackend query treats "brand new user" as data, not an error.
    return ResponseEntity.ok(
        userStateRepository
            .findByUseridAndCourseId(userid, courseId)
            .map(
                state ->
                    new UserStateResponse(
                        parseStringList(state.getStarredIds()),
                        parseJsonNode(state.getDetailCards()),
                        parseStringList(state.getMasteredSubconcepts()),
                        parseJsonNode(state.getTopLevelPositions())))
            .orElseGet(
                () ->
                    new UserStateResponse(
                        List.of(),
                        objectMapper.createArrayNode(),
                        List.of(),
                        objectMapper.createObjectNode())));
  }

  @Operation(summary = "Upsert saved user state by user ID and course ID")
  @PostMapping("/api/user-state")
  public ResponseEntity<Void> upsertUserState(@RequestBody UserStateRequest body) {
    if (body.userid() == null || body.courseId() == null) {
      return ResponseEntity.badRequest().build();
    }

    try {
      upsert(body);
    } catch (DataIntegrityViolationException e) {
      // Two near-simultaneous saves for a brand new (userid, courseId) pair (e.g. React
      // StrictMode double-invoking a state updater in dev) can both find no existing row and
      // both attempt an insert; the second violates the unique constraint. Retry once as an
      // update against the row the other request just inserted.
      upsert(body);
    }

    return ResponseEntity.noContent().build();
  }

  private void upsert(UserStateRequest body) {
    UserState state =
        userStateRepository
            .findByUseridAndCourseId(body.userid(), body.courseId())
            .orElseGet(UserState::new);
    state.setUserid(body.userid());
    state.setCourseId(body.courseId());
    state.setStarredIds(writeJson(body.starredIds() == null ? List.of() : body.starredIds()));
    state.setDetailCards(
        writeJson(
            body.detailCards() == null ? objectMapper.createArrayNode() : body.detailCards()));
    state.setMasteredSubconcepts(
        writeJson(body.masteredSubconcepts() == null ? List.of() : body.masteredSubconcepts()));
    state.setTopLevelPositions(
        writeJson(
            body.topLevelPositions() == null
                ? objectMapper.createObjectNode()
                : body.topLevelPositions()));
    userStateRepository.save(state);
  }

  private List<String> parseStringList(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to parse stored string list JSON", e);
    }
  }

  private JsonNode parseJsonNode(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to parse stored JSON payload", e);
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize JSON payload", e);
    }
  }

  public record UserStateRequest(
      Long userid,
      @JsonProperty("courseId") Long courseId,
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts,
      @JsonProperty("top_level_positions") JsonNode topLevelPositions) {}

  public record UserStateResponse(
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts,
      @JsonProperty("top_level_positions") JsonNode topLevelPositions) {}
}
