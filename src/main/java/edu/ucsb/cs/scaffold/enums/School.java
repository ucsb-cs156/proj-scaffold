package edu.ucsb.cs.scaffold.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum School {
  UCSB(
      "UCSB",
      List.of("UC Santa Barbara", "University of California, Santa Barbara", "SB"),
      "https://ucsb.instructure.com/api/graphql");

  private School(String displayName, List<String> alternateNames, String canvasImplementation) {
    this.displayName = displayName;
    this.alternateNames = alternateNames;
    this.canvasImplementation = canvasImplementation;
  }

  private final List<String> alternateNames;
  private final String canvasImplementation;
  private final String displayName;
  private final String key = this.name();

  /*
   * This is mostly to allow tests to serialize back in objects with school properties.
   */
  @JsonCreator
  public static School fromKey(JsonNode node) {
    if (node == null) {
      return null;
    }
    if (node.isTextual()) {
      return School.valueOf(node.asText().toUpperCase());
    }
    if (node.has("key")) {
      return School.valueOf(node.get("key").asText().toUpperCase());
    }

    throw new IllegalArgumentException("Invalid JSON node for School enum");
  }
}
