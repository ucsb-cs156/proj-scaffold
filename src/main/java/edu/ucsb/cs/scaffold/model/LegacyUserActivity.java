package edu.ucsb.cs.scaffold.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "legacy_user_activity")
@Getter
@Setter
@NoArgsConstructor
public class LegacyUserActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private Long userid;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  // length is set well above the default 255 so that Hibernate's schema
  // validation matches the VARCHAR(1048576) column defined in the migration.
  @Column(nullable = false, length = 1048576)
  private String payload;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
