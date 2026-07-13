package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.LegacyUserActivity;
import edu.ucsb.cs.scaffold.repository.LegacyUserActivityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Serves only LegacyHomePage.tsx (via legacyClient.ts). Events here have no course
// scoping; the course-scoped counterpart is UserActivityController at /api/user-activity.
@Tag(name = "Legacy User Activity")
@RestController
@RequiredArgsConstructor
public class LegacyUserActivityController {

  private final LegacyUserActivityRepository legacyUserActivityRepository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Insert a legacy user activity event")
  @PostMapping("/api/legacy/user-activity")
  public ResponseEntity<Void> insertUserActivity(@RequestBody LegacyUserActivityRequest body) {
    if (body.userid() == null || body.eventType() == null || body.eventType().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    LegacyUserActivity activity = new LegacyUserActivity();
    activity.setUserid(body.userid());
    activity.setEventType(body.eventType());
    activity.setPayload(
        writeJson(body.payload() == null ? objectMapper.createObjectNode() : body.payload()));
    legacyUserActivityRepository.save(activity);
    return ResponseEntity.noContent().build();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize JSON payload", e);
    }
  }

  public record LegacyUserActivityRequest(
      Long userid, @JsonProperty("event_type") String eventType, JsonNode payload) {}
}
