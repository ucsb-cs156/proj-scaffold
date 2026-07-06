package edu.ucsb.cs.scaffold.startup;

import edu.ucsb.cs.scaffold.entity.Admin;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  @Autowired AdminRepository adminRepository;

  @Autowired CourseRepository courseRepository;

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
    String sql =
        StreamUtils.copyToString(
            new ClassPathResource(SEED_DATA_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
    executeSeedStatements(sql);
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
