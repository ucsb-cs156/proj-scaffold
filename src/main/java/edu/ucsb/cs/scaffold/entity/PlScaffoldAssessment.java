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
    name = "pl_scaffold_assessment",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"pl_repo_id", "pl_instance_id", "pl_question_id"})
    })
public class PlScaffoldAssessment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_repo_id", nullable = false)
  private Long plRepoId;

  @Column(name = "pl_instance_id", nullable = false)
  private Long plInstanceId;

  @Column(name = "pl_question_id", nullable = false)
  private Long plQuestionId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "url")
  private String url;
}
