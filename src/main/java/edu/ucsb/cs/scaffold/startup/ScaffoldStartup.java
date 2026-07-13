package edu.ucsb.cs.scaffold.startup;

import edu.ucsb.cs.scaffold.entity.Admin;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlColor;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PlColorRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Slf4j
@Component
public class ScaffoldStartup {

  static final Long SEED_COURSE_ID = 1L;
  static final String SEED_DATA_RESOURCE = "db/seed/concepts.sql";

  // Default PrairieLearn badge colors (issue #96), taken from the $custom-colors map in
  // https://github.com/PrairieLearn/PrairieLearn/blob/master/apps/prairielearn/public/stylesheets/colors.scss.
  // ReadPLColorsJob keeps these up to date with any changes on the PrairieLearn side; this seed
  // only fills in rows that don't already exist so a restart never clobbers job updates.
  static final Map<String, String> SEED_PL_COLORS = new LinkedHashMap<>();

  static {
    SEED_PL_COLORS.put("red1", "#ffccbc");
    SEED_PL_COLORS.put("red2", "#ff6c5c");
    SEED_PL_COLORS.put("red3", "#c72c1c");
    SEED_PL_COLORS.put("pink1", "#ffbcd8");
    SEED_PL_COLORS.put("pink2", "#fa5c98");
    SEED_PL_COLORS.put("pink3", "#ba1c58");
    SEED_PL_COLORS.put("purple1", "#dcc6e0");
    SEED_PL_COLORS.put("purple2", "#9b59b6");
    SEED_PL_COLORS.put("purple3", "#5e147d");
    SEED_PL_COLORS.put("blue1", "#39d5ff");
    SEED_PL_COLORS.put("blue2", "#1297e0");
    SEED_PL_COLORS.put("blue3", "#0057a0");
    SEED_PL_COLORS.put("turquoise1", "#5efaf7");
    SEED_PL_COLORS.put("turquoise2", "#27cbc0");
    SEED_PL_COLORS.put("turquoise3", "#008b80");
    SEED_PL_COLORS.put("green1", "#8effc1");
    SEED_PL_COLORS.put("green2", "#2ecc71");
    SEED_PL_COLORS.put("green3", "#008c31");
    SEED_PL_COLORS.put("yellow1", "#fdeea5");
    SEED_PL_COLORS.put("yellow2", "#f5ce32");
    SEED_PL_COLORS.put("yellow3", "#d6a100");
    SEED_PL_COLORS.put("orange1", "#ffdcb5");
    SEED_PL_COLORS.put("orange2", "#ff926b");
    SEED_PL_COLORS.put("orange3", "#c3522b");
    SEED_PL_COLORS.put("brown1", "#e6bfa8");
    SEED_PL_COLORS.put("brown2", "#c0957c");
    SEED_PL_COLORS.put("brown3", "#7d5640");
    SEED_PL_COLORS.put("gray1", "#e0e0e0");
    SEED_PL_COLORS.put("gray2", "#909090");
    SEED_PL_COLORS.put("gray3", "#505050");
  }

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  @Autowired AdminRepository adminRepository;

  @Autowired CourseRepository courseRepository;

  @Autowired PlColorRepository plColorRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  public void alwaysRunOnStartup() {
    log.info("ScaffoldStartup.alwaysRunOnStartup called");

    try {
      adminEmails.forEach(
          (email) -> {
            Admin admin = new Admin(email.strip());
            adminRepository.save(admin);
          });
    } catch (Exception e) {
      log.error("Error loading ADMIN_EMAILS into admins table:", e);
    }

    // Temporary demo/test seed data for the concept graph feature. This creates course 1
    // (if it doesn't already exist) and loads db/seed/concepts.sql, ignoring errors for
    // records that already exist. Remove this once the app is past the demo/testing stage.
    try {
      seedCourseOne();
      seedConceptsSqlFile();
    } catch (Exception e) {
      log.error("Error seeding concept graph demo data:", e);
    }

    try {
      seedPlColors();
    } catch (Exception e) {
      log.error("Error seeding pl_color table:", e);
    }
  }

  private void seedPlColors() {
    SEED_PL_COLORS.forEach(
        (colorName, hexCode) -> {
          if (!plColorRepository.existsById(colorName)) {
            plColorRepository.save(PlColor.builder().colorName(colorName).hexCode(hexCode).build());
          }
        });
  }

  private void seedCourseOne() {
    if (courseRepository.existsById(SEED_COURSE_ID)) {
      return;
    }
    Course course =
        Course.builder()
            .courseName("CMPSC 8")
            .instructorEmail("phtcon@ucsb.edu")
            .term("S26")
            .school(School.UCSB)
            .build();
    courseRepository.save(course);
    log.info("Created seed course: CMPSC 8");
  }

  private void seedConceptsSqlFile() throws IOException {
    // Subconcepts have a NULL name, so re-running the seed script would not trip the
    // (course_id, name) unique constraint and would insert duplicates; skip the whole
    // script once any concepts exist for the seed course.
    if (seedConceptsAlreadyPresent()) {
      return;
    }
    String sql =
        StreamUtils.copyToString(
            new ClassPathResource(SEED_DATA_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
    executeSeedStatements(sql);
  }

  boolean seedConceptsAlreadyPresent() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM concepts WHERE course_id = ?", Integer.class, SEED_COURSE_ID);
    return count != null && count > 0;
  }

  void executeSeedStatements(String sql) {
    for (String rawStatement : splitSqlStatements(sql)) {
      String cleaned = stripLeadingCommentsAndBlankLines(rawStatement);
      if (cleaned.isEmpty()) {
        continue;
      }
      try {
        jdbcTemplate.execute(cleaned);
      } catch (DataAccessException e) {
        log.debug("Skipping seed statement (likely already applied): {}", e.getMessage());
      }
    }
  }

  /**
   * Removes leading blank and "--" comment lines from a single SQL statement, since those aren't
   * separated from the following real statement by their own semicolon and would otherwise end up
   * glued to the front of it. Only LEADING lines are removed (stopping at the first line of real
   * content) so that a blank line inside a multi-paragraph string value later in the same statement
   * is left untouched.
   */
  static String stripLeadingCommentsAndBlankLines(String statement) {
    String[] lines = statement.split("\n", -1);
    int start = 0;
    while (start < lines.length) {
      String trimmedLine = lines[start].strip();
      if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
        start++;
      } else {
        break;
      }
    }
    String result = String.join("\n", Arrays.copyOfRange(lines, start, lines.length));
    return result.strip();
  }

  /**
   * Splits a SQL script into individual statements on top-level semicolons, ignoring semicolons
   * that appear inside single-quoted string literals (including '' escaped quotes, since a pair of
   * quote-toggles cancels out and leaves the parser in the correct state).
   */
  static List<String> splitSqlStatements(String sql) {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuotedString = false;
    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);
      current.append(c);
      if (c == '\'') {
        inSingleQuotedString = !inSingleQuotedString;
      } else if (c == ';' && !inSingleQuotedString) {
        statements.add(current.toString());
        current.setLength(0);
      }
    }
    if (!current.toString().isBlank()) {
      statements.add(current.toString());
    }
    return statements;
  }
}
