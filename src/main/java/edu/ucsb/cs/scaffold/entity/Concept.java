package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "concepts",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_CONCEPTS_COURSE_CONCEPT",
          columnNames = {"course_id", "name"})
    })
public class Concept {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  // Human-readable slug (lowercase letters, digits, hyphens). Non-null exactly for
  // top-level concepts; subconcepts are identified by (parent, label) instead.
  private String name;

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

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @ToString.Exclude
  private Concept parent;

  public boolean isSubconcept() {
    return parent != null;
  }
}
