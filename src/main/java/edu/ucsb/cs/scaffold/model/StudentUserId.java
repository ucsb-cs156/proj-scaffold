package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "student_userids")
@Getter
@Setter
@NoArgsConstructor
public class StudentUserId {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String userid;
}
