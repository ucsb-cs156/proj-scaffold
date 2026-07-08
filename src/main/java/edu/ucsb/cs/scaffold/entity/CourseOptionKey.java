package edu.ucsb.cs.scaffold.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseOptionKey implements Serializable {
  private Long courseId;
  private String option;
}
