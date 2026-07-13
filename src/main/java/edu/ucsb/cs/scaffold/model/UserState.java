package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_state",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_USER_STATE_USER_COURSE",
          columnNames = {"userid", "course_id"})
    })
@Getter
@Setter
@NoArgsConstructor
public class UserState {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private Long userid;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  // length is set well above the default 255 so that Hibernate's schema
  // validation matches the VARCHAR(1048576) column defined in the migration.
  @Column(name = "starred_ids", nullable = false, length = 1048576)
  private String starredIds = "[]";

  @Column(name = "detail_cards", nullable = false, length = 1048576)
  private String detailCards = "[]";

  @Column(name = "mastered_subconcepts", nullable = false, length = 1048576)
  private String masteredSubconcepts = "[]";

  // Per-user, per-course override of top-level concept x,y positions, keyed by concept name:
  // {"recursion": {"x": 100, "y": 200}, ...}. Cleared course-wide whenever an instructor runs
  // POST /api/course/scaffold/reset, since a structural reset can move a concept to a
  // different level and make a stale override render in the wrong row.
  @Column(name = "top_level_positions", nullable = false, length = 1048576)
  private String topLevelPositions = "{}";
}
