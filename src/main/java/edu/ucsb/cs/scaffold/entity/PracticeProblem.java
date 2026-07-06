package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "practice_problems",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_PRACTICE_PROBLEMS_COURSE_CONCEPT_URL",
          columnNames = {"course_id", "concept_id", "url"})
    })
public class PracticeProblem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  @ManyToOne
  @JoinColumn(name = "concept_id", nullable = false)
  @ToString.Exclude
  private Concept concept;

  @Column(nullable = false, length = 512)
  private String url;
}
