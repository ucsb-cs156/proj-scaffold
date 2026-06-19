package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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
    private String userid;

    @Lob
    @Column(name = "starred_ids", nullable = false)
    private String starredIds = "[]";

    @Lob
    @Column(name = "detail_cards", nullable = false)
    private String detailCards = "[]";

    @Lob
    @Column(name = "mastered_subconcepts", nullable = false)
    private String masteredSubconcepts = "[]";
}
