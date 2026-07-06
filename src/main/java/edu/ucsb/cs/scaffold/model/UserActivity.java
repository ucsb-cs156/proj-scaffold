package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_activity")
@Getter
@Setter
@NoArgsConstructor
public class UserActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private Long userid;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(nullable = false)
  private String payload;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
