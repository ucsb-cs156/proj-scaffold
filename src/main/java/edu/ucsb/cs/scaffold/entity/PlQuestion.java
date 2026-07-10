package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
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
    name = "pl_question",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"pl_repo_id", "question_id"})})
public class PlQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pl_repo_id", nullable = false)
  private Long plRepoId;

  @Column(name = "question_id", nullable = false)
  private String questionId;

  @Column(name = "uuid", nullable = false)
  private UUID uuid;

  @Column(name = "title", nullable = false)
  private String title;
}
