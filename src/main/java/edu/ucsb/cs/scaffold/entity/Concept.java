package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "concepts")
public class Concept {

  // Longest a top-level concept's label may be once its Markdown is rendered to plain text;
  // enforced everywhere labels enter the system (single-concept endpoints and YAML upload).
  // Top-level concepts are drawn as circular nodes on the concept graph, so their labels need
  // to stay short.
  public static final int MAX_RENDERED_LABEL_LENGTH = 32;

  // Longest a subconcept's label may be once its Markdown is rendered to plain text. Subconcepts
  // are listed as rows inside their parent's node (which wrap rather than needing to fit a
  // circle), so they can afford to be longer than top-level concept labels.
  public static final int MAX_RENDERED_SUBCONCEPT_LABEL_LENGTH = 60;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  @Column(nullable = false)
  private String label;

  // length is set well above the default 255 so that Hibernate's schema
  // validation matches the VARCHAR(1048576) column defined in the migration.
  @Column(length = 1048576)
  private String description;

  @Column(length = 1048576)
  private String example;

  private String color;

  private Integer x;
  private Integer y;

  // Longest-path rank from a root (no-prerequisite) concept, 1-based. Top-level only,
  // recomputed by ConceptGraphService.reset(); see /api/course/scaffold/reset.
  private Integer level;

  // Author-chosen position of a subconcept within its parent's list, 0-based. Subconcepts
  // only (null for top-level concepts). Not unique: a concurrent create can leave two
  // subconcepts with the same value, which is harmless because display order always breaks
  // ties by id, and the next reorder rewrites the whole list to 0..n-1.
  private Integer sortOrder;

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @ToString.Exclude
  private Concept parent;

  public boolean isSubconcept() {
    return parent != null;
  }
}
