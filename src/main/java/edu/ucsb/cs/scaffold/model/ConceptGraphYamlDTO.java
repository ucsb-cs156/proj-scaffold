package edu.ucsb.cs.scaffold.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The YAML document exchanged by the concept-graph download/upload endpoints. See
 * docs/yaml-format.md for the format specification; field names here are the YAML keys.
 *
 * <p>Concept {@code id}s are <em>external</em> ids: consecutive integers assigned at export whose
 * only job is to let {@code edges} refer to top-level concepts within the file. They are unrelated
 * to database row ids, which change on every upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"format", "concepts", "edges"})
public class ConceptGraphYamlDTO {

  /** Format version; always 1 for now. */
  private Integer format;

  private List<ConceptNodeDTO> concepts;

  private List<EdgeNodeDTO> edges;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({
    "id",
    "label",
    "color",
    "level",
    "x",
    "y",
    "description",
    "example",
    "practiceProblems",
    "subconcepts"
  })
  public static class ConceptNodeDTO {
    private Long id;
    private String label;
    private String color;
    private Integer level;
    private Integer x;
    private Integer y;
    private String description;
    private String example;
    private List<String> practiceProblems;
    private List<SubconceptNodeDTO> subconcepts;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonPropertyOrder({"label", "description", "example", "practiceProblems"})
  public static class SubconceptNodeDTO {
    private String label;
    private String description;
    private String example;
    private List<String> practiceProblems;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({"from", "to"})
  public static class EdgeNodeDTO {
    private Long from;
    private Long to;
  }
}
