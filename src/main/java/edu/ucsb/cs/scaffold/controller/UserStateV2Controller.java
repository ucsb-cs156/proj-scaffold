package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.UserStateV2;
import edu.ucsb.cs.scaffold.repository.UserStateV2Repository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User State V2")
@RestController
@RequiredArgsConstructor
public class UserStateV2Controller {

  private final UserStateV2Repository userStateV2Repository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Get saved user state by user ID and course ID")
  @GetMapping("/api/user-state-v2")
  public ResponseEntity<UserStateV2Response> getUserState(
      @Parameter(description = "User ID (from users table)") @RequestParam Long userid,
      @Parameter(description = "Course ID (from course table)") @RequestParam Long courseId) {
    return userStateV2Repository
        .findByUseridAndCourseId(userid, courseId)
        .map(
            state ->
                ResponseEntity.ok(
                    new UserStateV2Response(
                        parseStringList(state.getStarredIds()),
                        parseJsonNode(state.getDetailCards()),
                        parseStringList(state.getMasteredSubconcepts()),
                        parseJsonNode(state.getTopLevelPositions()))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Upsert saved user state by user ID and course ID")
  @PostMapping("/api/user-state-v2")
  public ResponseEntity<Void> upsertUserState(@RequestBody UserStateV2Request body) {
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

  private void upsert(UserStateV2Request body) {
    UserStateV2 state =
        userStateV2Repository
            .findByUseridAndCourseId(body.userid(), body.courseId())
            .orElseGet(UserStateV2::new);
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
    userStateV2Repository.save(state);
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

  public record UserStateV2Request(
      Long userid,
      @JsonProperty("courseId") Long courseId,
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts,
      @JsonProperty("top_level_positions") JsonNode topLevelPositions) {}

  public record UserStateV2Response(
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts,
      @JsonProperty("top_level_positions") JsonNode topLevelPositions) {}
}
