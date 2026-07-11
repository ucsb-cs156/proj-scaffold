package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "pl_instance",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"pl_repo_id", "short_name"})})
public class PlInstance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_repo_id", nullable = false)
  private Long plRepoId;

  // Directory name under courseInstances/ in the repo, e.g. "S26".
  @Column(name = "short_name", nullable = false)
  private String shortName;

  // From infoCourseInstance.json's longName, e.g. "Spring 2026"; null until the
  // instance is verified against PrairieLearn (PUT /api/courses/updatePLInstance).
  @Column(name = "long_name")
  private String longName;

  // PrairieLearn's numeric course instance id; null until verified against the
  // PrairieLearn API, which is the only place this id can come from.
  @Column(name = "numeric_id")
  private Long numericId;
}
