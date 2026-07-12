package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.UserActivity;
import edu.ucsb.cs.scaffold.repository.UserActivityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Activity")
@RestController
@RequiredArgsConstructor
public class UserActivityController {

  private final UserActivityRepository userActivityRepository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Insert a user activity event with a course ID")
  @PostMapping("/api/user-activity")
  public ResponseEntity<Void> insertUserActivity(@RequestBody UserActivityRequest body) {
    if (body.userid() == null
        || body.courseId() == null
        || body.eventType() == null
        || body.eventType().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    UserActivity activity = new UserActivity();
    activity.setUserid(body.userid());
    activity.setCourseId(body.courseId());
    activity.setEventType(body.eventType());
    activity.setPayload(
        writeJson(body.payload() == null ? objectMapper.createObjectNode() : body.payload()));
    userActivityRepository.save(activity);
    return ResponseEntity.noContent().build();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize JSON payload", e);
    }
  }

  public record UserActivityRequest(
      Long userid,
      @JsonProperty("courseId") Long courseId,
      @JsonProperty("event_type") String eventType,
      JsonNode payload) {}
}
