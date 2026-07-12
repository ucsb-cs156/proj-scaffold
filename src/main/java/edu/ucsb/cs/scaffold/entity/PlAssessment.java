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
    name = "pl_assessment",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"pl_repo_id", "pl_instance_id", "name"})})
public class PlAssessment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_repo_id", nullable = false)
  private Long plRepoId;

  @Column(name = "pl_instance_id", nullable = false)
  private Long plInstanceId;

  @Column(name = "name", nullable = false)
  private String name;

  // Nullable: assessments synced from infoAssessment.json have no URL (column kept for future use)
  @Column(name = "url")
  private String url;

  // The fields below come from the PrairieLearn API's assessments response and
  // are all nullable: rows are created by the GitHub sync job, which cannot
  // know them; a future job will fill them in from the PL API (issue #71).

  // PrairieLearn's numeric assessment id ("assessment_id"), from which the
  // student-facing URL is built.
  @Column(name = "pl_assessment_id")
  private Long plAssessmentId;

  // "assessment_number"
  @Column(name = "pl_assessment_number")
  private Long plAssessmentNumber;

  // "assessment_order_by"
  @Column(name = "pl_assessment_order")
  private Long plAssessmentOrder;

  // "title"
  @Column(name = "pl_assessment_title")
  private String plAssessmentTitle;

  // "assessment_set_abbreviation"
  @Column(name = "pl_assessment_set_abbreviation")
  private String plAssessmentSetAbbreviation;

  // "assessment_set_number"
  @Column(name = "pl_assessment_set_number")
  private Integer plAssessmentSetNumber;

  // "assessment_set_heading"
  @Column(name = "pl_assessment_set_heading")
  private String plAssessmentSetHeading;

  // "assessment_set_color"
  @Column(name = "pl_assessment_set_color")
  private String plAssessmentSetColor;
}
