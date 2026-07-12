package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "legacy_user_state")
@Getter
@Setter
@NoArgsConstructor
public class LegacyUserState {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private Long userid;

  // length is set well above the default 255 so that Hibernate's schema
  // validation matches the VARCHAR(1048576) column defined in the migration.
  @Column(name = "starred_ids", nullable = false, length = 1048576)
  private String starredIds = "[]";

  @Column(name = "detail_cards", nullable = false, length = 1048576)
  private String detailCards = "[]";

  @Column(name = "mastered_subconcepts", nullable = false, length = 1048576)
  private String masteredSubconcepts = "[]";
}
