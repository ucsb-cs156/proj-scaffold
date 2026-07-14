package edu.ucsb.cs.scaffold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A named color used by PrairieLearn (e.g. {@code red1}), and its hex code, as defined in
 * PrairieLearn's {@code colors.scss}. Seeded on startup and kept up to date by the ReadPLColorsJob
 * (issue #96).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "pl_color")
public class PlColor {
  @Id
  @Column(name = "color_name", nullable = false)
  private String colorName;

  @Column(name = "hex_code", nullable = false)
  private String hexCode;
}
