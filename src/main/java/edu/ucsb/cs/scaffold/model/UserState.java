package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_state")
@Getter
@Setter
@NoArgsConstructor
public class UserState {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private Long userid;

  @Column(name = "starred_ids", nullable = false)
  private String starredIds = "[]";

  @Column(name = "detail_cards", nullable = false)
  private String detailCards = "[]";

  @Column(name = "mastered_subconcepts", nullable = false)
  private String masteredSubconcepts = "[]";
}
