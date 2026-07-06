package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "concept_edges",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_CONCEPT_EDGES_SOURCE_TARGET",
          columnNames = {"source_concept_id", "target_concept_id"})
    })
public class ConceptEdge {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  @ManyToOne
  @JoinColumn(name = "source_concept_id", nullable = false)
  @ToString.Exclude
  private Concept source;

  @ManyToOne
  @JoinColumn(name = "target_concept_id", nullable = false)
  @ToString.Exclude
  private Concept target;
}
