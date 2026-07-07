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
          columnNames = {"course_id", "concept_id"})
    })
public class Concept {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  @Column(name = "concept_id", nullable = false)
  private String conceptId;

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

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @ToString.Exclude
  private Concept parent;
}
