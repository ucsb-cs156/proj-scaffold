package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity(name = "instructors")
public class Instructor {
  @Id private String email;
}
