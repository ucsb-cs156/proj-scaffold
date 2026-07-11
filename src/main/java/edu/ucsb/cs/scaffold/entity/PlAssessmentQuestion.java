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

/**
 * Join table: which PlQuestions appear in a PlAssessment, in the order they appear in the
 * assessment's zones (infoAssessment.json). Populated by the SyncPlRepoJob; a question can appear
 * in many assessments, and an assessment has many questions. Both sides are scoped to the same
 * PlRepo — questionIds can repeat across repos but refer to different PlQuestion rows — and the
 * denormalized pl_repo_id column makes repo-wide cascade deletes simple.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "pl_assessment_question",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"pl_assessment_id", "pl_question_id"})})
public class PlAssessmentQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_repo_id", nullable = false)
  private Long plRepoId;

  @Column(name = "pl_assessment_id", nullable = false)
  private Long plAssessmentId;

  @Column(name = "pl_question_id", nullable = false)
  private Long plQuestionId;

  // Position of the question within the assessment's zones (0-based, document order)
  @Column(name = "ordinal", nullable = false)
  private int ordinal;
}
