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
    name = "pl_assessment_set",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"pl_instance_id", "abbreviation"})})
public class PlAssessmentSet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_instance_id", nullable = false)
  private Long plInstanceId;

  // e.g. "LEC"
  @Column(name = "abbreviation", nullable = false)
  private String abbreviation;

  // e.g. "Lecture"
  @Column(name = "name", nullable = false)
  private String name;

  // e.g. "Lectures"
  @Column(name = "heading", nullable = false)
  private String heading;

  // e.g. "turquoise2"
  @Column(name = "color", nullable = false)
  private String color;
}
