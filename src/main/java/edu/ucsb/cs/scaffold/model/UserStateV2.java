package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_state_v2",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_USER_STATE_V2_USER_COURSE",
          columnNames = {"userid", "course_id"})
    })
@Getter
@Setter
@NoArgsConstructor
public class UserStateV2 {

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
}
