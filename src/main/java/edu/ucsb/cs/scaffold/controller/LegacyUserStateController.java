package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.LegacyUserState;
import edu.ucsb.cs.scaffold.repository.LegacyUserStateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Serves only LegacyHomePage.tsx (via legacyClient.ts). State here is per-user with no
// course scoping; the course-scoped counterpart is UserStateController at /api/user-state.
@Tag(name = "Legacy User State")
@RestController
@RequiredArgsConstructor
public class LegacyUserStateController {

  private final LegacyUserStateRepository legacyUserStateRepository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Get saved legacy user state by user ID")
  @GetMapping("/api/legacy/user-state/{userid}")
  public ResponseEntity<LegacyUserStateResponse> getUserState(
      @Parameter(description = "User ID (from users table)") @PathVariable Long userid) {
    return legacyUserStateRepository
        .findByUserid(userid)
        .map(
            state ->
                ResponseEntity.ok(
                    new LegacyUserStateResponse(
                        parseStringList(state.getStarredIds()),
                        parseJsonNode(state.getDetailCards()),
                        parseStringList(state.getMasteredSubconcepts()))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Upsert saved legacy user state by user ID")
  @PostMapping("/api/legacy/user-state")
  public ResponseEntity<Void> upsertUserState(@RequestBody LegacyUserStateRequest body) {
    if (body.userid() == null) {
      return ResponseEntity.badRequest().build();
    }

    LegacyUserState state =
        legacyUserStateRepository.findByUserid(body.userid()).orElseGet(LegacyUserState::new);
    state.setUserid(body.userid());
    state.setStarredIds(writeJson(body.starredIds() == null ? List.of() : body.starredIds()));
    state.setDetailCards(
        writeJson(
            body.detailCards() == null ? objectMapper.createArrayNode() : body.detailCards()));
    state.setMasteredSubconcepts(
        writeJson(body.masteredSubconcepts() == null ? List.of() : body.masteredSubconcepts()));
    legacyUserStateRepository.save(state);

    return ResponseEntity.noContent().build();
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

  public record LegacyUserStateRequest(
      Long userid,
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts) {}

  public record LegacyUserStateResponse(
      @JsonProperty("starred_ids") List<String> starredIds,
      @JsonProperty("detail_cards") JsonNode detailCards,
      @JsonProperty("mastered_subconcepts") List<String> masteredSubconcepts) {}
}
