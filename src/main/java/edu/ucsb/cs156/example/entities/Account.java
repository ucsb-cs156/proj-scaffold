package edu.ucsb.cs156.example.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This is a JPA entity that represents a url. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "accounts")
public class Account {
  @Id private String email;
  private String pin;
}
