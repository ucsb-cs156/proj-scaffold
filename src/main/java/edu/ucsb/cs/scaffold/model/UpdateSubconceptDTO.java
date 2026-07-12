package edu.ucsb.cs.scaffold.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for PUT /api/concept/subconcept/put (updates a subconcept). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubconceptDTO {

  @Schema(
      description = "Short Markdown label (unique among the parent's subconcepts)",
      example = "Accessing a value",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String label;

  @Schema(description = "Markdown description of the subconcept")
  private String description;

  @Schema(description = "Markdown example (code blocks encouraged)")
  private String example;
}
