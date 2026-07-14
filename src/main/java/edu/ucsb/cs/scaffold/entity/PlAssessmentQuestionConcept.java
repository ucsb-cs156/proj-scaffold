package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Tags a Concept onto a specific PlAssessmentQuestion (a question's occurrence within one
 * assessment), so instructors can associate concepts with the questions students practice. The
 * plAssessmentQuestionId is a raw FK (matching the pl_* family's convention, since it points at a
 * SyncPlRepoJob-populated row) while concept is a real JPA relation, matching Concept/ConceptEdge.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "pl_assessment_question_concept",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_PL_ASSESSMENT_QUESTION_CONCEPT",
          columnNames = {"pl_assessment_question_id", "concept_id"})
    })
public class PlAssessmentQuestionConcept {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_assessment_question_id", nullable = false)
  private Long plAssessmentQuestionId;

  @ManyToOne
  @JoinColumn(name = "concept_id", nullable = false)
  @ToString.Exclude
  private Concept concept;
}
