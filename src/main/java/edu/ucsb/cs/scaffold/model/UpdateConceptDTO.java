package edu.ucsb.cs.scaffold.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for PUT /api/concept/put (updates a top-level concept). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConceptDTO {

  @Schema(
      description = "Short Markdown label shown on the concept node",
      example = "Arrays",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String label;

  @Schema(description = "Markdown description of the concept")
  private String description;

  @Schema(description = "Markdown example (code blocks encouraged)")
  private String example;
}
