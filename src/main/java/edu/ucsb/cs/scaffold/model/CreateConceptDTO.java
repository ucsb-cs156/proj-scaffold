package edu.ucsb.cs.scaffold.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /api/concept (creates a top-level concept). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConceptDTO {

  @Schema(
      description = "ID of the course this concept belongs to",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long courseId;

  @Schema(
      description = "Short Markdown label shown on the concept node",
      example = "Arrays",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String label;

  @Schema(description = "Markdown description of the concept")
  private String description;

  @Schema(description = "Markdown example (code blocks encouraged)")
  private String example;

  @Schema(
      description = "X coordinate on the concept map",
      example = "0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer x;

  @Schema(
      description = "Y coordinate on the concept map",
      example = "0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer y;
}
