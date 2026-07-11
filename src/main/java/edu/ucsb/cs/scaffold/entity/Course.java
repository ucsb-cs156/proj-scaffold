package edu.ucsb.cs.scaffold.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs.scaffold.enums.School;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String instructorEmail;

  private String courseName;

  private String term;

  @Enumerated(EnumType.STRING)
  private School school;

  @JsonIgnore private String canvasApiToken;

  private String canvasCourseId;

  // Id of the PlRepo (GitHub repo) associated with this course; null until an
  // instructor associates one via PUT /api/courses/updateGithubRepo.
  @Column(name = "pl_repo_id")
  private Long plRepoId;

  // Id of the PlInstance (PrairieLearn course instance) associated with this
  // course; null until an instructor associates one via
  // PUT /api/courses/updatePLInstance.
  @Column(name = "pl_instance_id")
  private Long plInstanceId;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<CourseStaff> courseStaff;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<RosterStudent> rosterStudents;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<Section> sections;
}
