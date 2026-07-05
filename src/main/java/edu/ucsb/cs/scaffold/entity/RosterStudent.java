package edu.ucsb.cs.scaffold.entity;

import edu.ucsb.cs.scaffold.enums.RosterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_STUDENT",
          columnNames = {"course_id", "student_id"}),
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_EMAIL",
          columnNames = {"course_id", "email"})
    })
public class RosterStudent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id")
  private Course course;

  private String studentId;
  private String firstName;
  private String lastName;
  private String email;
  @Builder.Default private String section = "";

  @ManyToOne
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private User user;

  @Enumerated(EnumType.STRING)
  private RosterStatus rosterStatus;
}
