package edu.ucsb.cs.scaffold.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.model.UserActivityV2;
import edu.ucsb.cs.scaffold.repository.UserActivityV2Repository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Activity V2")
@RestController
@RequiredArgsConstructor
public class UserActivityV2Controller {

  private final UserActivityV2Repository userActivityV2Repository;
  private final ObjectMapper objectMapper;

  @Operation(summary = "Insert a user activity event with a course ID")
  @PostMapping("/api/user-activity-v2")
  public ResponseEntity<Void> insertUserActivity(@RequestBody UserActivityV2Request body) {
    if (body.userid() == null
        || body.courseId() == null
        || body.eventType() == null
        || body.eventType().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    UserActivityV2 activity = new UserActivityV2();
    activity.setUserid(body.userid());
    activity.setCourseId(body.courseId());
    activity.setEventType(body.eventType());
    activity.setPayload(
        writeJson(body.payload() == null ? objectMapper.createObjectNode() : body.payload()));
    userActivityV2Repository.save(activity);
    return ResponseEntity.noContent().build();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize JSON payload", e);
    }
  }

  public record UserActivityV2Request(
      Long userid,
      @JsonProperty("courseId") Long courseId,
      @JsonProperty("event_type") String eventType,
      JsonNode payload) {}
}
