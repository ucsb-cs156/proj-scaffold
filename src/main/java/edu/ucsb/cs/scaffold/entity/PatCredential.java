package edu.ucsb.cs.scaffold.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A user's encrypted personal access token (PAT) for one platform (GitHub or PrairieLearn). One
 * credential per user per platform. The token itself is write-only: only the AES-256-GCM ciphertext
 * is stored, and it is never serialized to JSON — clients see only metadata (last four characters,
 * expiration date).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "pat_credential",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "platform"})})
public class PatCredential {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform", nullable = false, length = 20)
  private PatPlatform platform;

  // Base64 of the AES-256-GCM ciphertext; excluded from JSON and toString so it can never leak
  // into API responses or logs.
  @JsonIgnore
  @ToString.Exclude
  @Column(name = "ciphertext", nullable = false, length = 512)
  private String ciphertext;

  // Which PAT_ENCRYPTION_KEY version encrypted the ciphertext; only that key can decrypt it.
  @JsonIgnore
  @Column(name = "key_version", nullable = false)
  private int keyVersion;

  @Column(name = "last_four", nullable = false, length = 4)
  private String lastFour;

  @Column(name = "expires_at")
  private LocalDate expiresAt;
}
