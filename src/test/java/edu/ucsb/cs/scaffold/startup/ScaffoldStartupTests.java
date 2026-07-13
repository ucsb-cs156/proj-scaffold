package edu.ucsb.cs.scaffold.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import edu.ucsb.cs.scaffold.entity.Admin;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PlColor;
import edu.ucsb.cs.scaffold.enums.School;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.CourseRepository;
import edu.ucsb.cs.scaffold.repository.PlColorRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class ScaffoldStartupTests {

  @Mock private AdminRepository adminRepository;

  @Mock private CourseRepository courseRepository;

  @Mock private PlColorRepository plColorRepository;

  @Mock private JdbcTemplate jdbcTemplate;

  private ScaffoldStartup scaffoldStartup;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    scaffoldStartup = new ScaffoldStartup();
    scaffoldStartup.adminRepository = adminRepository;
    scaffoldStartup.courseRepository = courseRepository;
    scaffoldStartup.plColorRepository = plColorRepository;
    scaffoldStartup.jdbcTemplate = jdbcTemplate;
    scaffoldStartup.adminEmails = List.of("phtcon@ucsb.edu", "admin2@ucsb.edu");
    when(courseRepository.existsById(ScaffoldStartup.SEED_COURSE_ID)).thenReturn(true);
  }

  @Test
  void alwaysRunOnStartup_saves_all_admin_emails() {
    scaffoldStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("phtcon@ucsb.edu"));
    verify(adminRepository).save(new Admin("admin2@ucsb.edu"));
    verifyNoMoreInteractions(adminRepository);
  }

  @Test
  void alwaysRunOnStartup_handles_repository_exception_gracefully() {
    doThrow(new RuntimeException("DB error")).when(adminRepository).save(any(Admin.class));

    // Should not throw — exception is caught and logged
    scaffoldStartup.alwaysRunOnStartup();

    verify(adminRepository, times(1)).save(any(Admin.class));
  }

  @Test
  void alwaysRunOnStartup_creates_seed_course_when_missing() {
    when(courseRepository.existsById(ScaffoldStartup.SEED_COURSE_ID)).thenReturn(false);

    scaffoldStartup.alwaysRunOnStartup();

    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    Course saved = captor.getValue();
    assertEquals("CMPSC 8", saved.getCourseName());
    assertEquals("phtcon@ucsb.edu", saved.getInstructorEmail());
    assertEquals("S26", saved.getTerm());
    assertEquals(School.UCSB, saved.getSchool());
  }

  @Test
  void alwaysRunOnStartup_skips_seed_course_creation_when_already_present() {
    when(courseRepository.existsById(ScaffoldStartup.SEED_COURSE_ID)).thenReturn(true);

    scaffoldStartup.alwaysRunOnStartup();

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void alwaysRunOnStartup_handles_course_seeding_exception_gracefully() {
    when(courseRepository.existsById(ScaffoldStartup.SEED_COURSE_ID))
        .thenThrow(new RuntimeException("DB error"));

    // Should not throw — exception is caught and logged, and does not prevent the rest of
    // alwaysRunOnStartup from having already run the admin-email seeding above it.
    scaffoldStartup.alwaysRunOnStartup();

    verify(adminRepository, times(2)).save(any(Admin.class));
  }

  @Test
  void alwaysRunOnStartup_loads_the_bundled_seed_data_file_without_error() {
    // Exercises the real classpath resource (db/seed/concepts.sql): splitting, blank-line
    // skipping, comment-line skipping, and successful statement execution.
    scaffoldStartup.alwaysRunOnStartup();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, times(246)).execute(captor.capture());
    List<String> executed = captor.getAllValues();
    assertEquals(105, executed.stream().filter(s -> s.startsWith("INSERT INTO concepts")).count());
    assertEquals(
        105, executed.stream().filter(s -> s.startsWith("INSERT INTO practice_problems")).count());
    assertEquals(
        36, executed.stream().filter(s -> s.startsWith("INSERT INTO concept_edges")).count());
    // Every statement should be a real, complete statement — no leftover comment/blank noise.
    executed.forEach(
        s -> {
          assertEquals(false, s.isBlank());
          assertEquals(false, s.startsWith("--"));
          assertEquals(true, s.endsWith(";"));
        });
    // Spot check: this concept's description contains an interior blank line (a paragraph
    // break) that must survive the leading-comment/blank-line stripping untouched.
    assertEquals(
        1,
        executed.stream()
            .filter(s -> s.contains("'Converting between bases'"))
            .filter(s -> s.contains("starting at 0.\n\nTo convert"))
            .count());
  }

  @Test
  void alwaysRunOnStartup_skips_the_seed_data_file_when_concepts_already_exist() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM concepts WHERE course_id = ?",
            Integer.class,
            ScaffoldStartup.SEED_COURSE_ID))
        .thenReturn(105);

    scaffoldStartup.alwaysRunOnStartup();

    verify(jdbcTemplate, never()).execute(anyString());
  }

  @Test
  void seedConceptsAlreadyPresent_is_false_when_count_is_zero() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM concepts WHERE course_id = ?",
            Integer.class,
            ScaffoldStartup.SEED_COURSE_ID))
        .thenReturn(0);

    assertEquals(false, scaffoldStartup.seedConceptsAlreadyPresent());
  }

  @Test
  void seedConceptsAlreadyPresent_is_false_when_count_is_null() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM concepts WHERE course_id = ?",
            Integer.class,
            ScaffoldStartup.SEED_COURSE_ID))
        .thenReturn(null);

    assertEquals(false, scaffoldStartup.seedConceptsAlreadyPresent());
  }

  @Test
  void seedConceptsAlreadyPresent_is_true_when_concepts_exist() {
    when(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM concepts WHERE course_id = ?",
            Integer.class,
            ScaffoldStartup.SEED_COURSE_ID))
        .thenReturn(1);

    assertEquals(true, scaffoldStartup.seedConceptsAlreadyPresent());
  }

  @Test
  void stripLeadingCommentsAndBlankLines_preserves_interior_blank_lines_in_string_values() {
    String statement = "\n-- header comment\n\nINSERT INTO foo VALUES ('para1\n\npara2');";

    String cleaned = ScaffoldStartup.stripLeadingCommentsAndBlankLines(statement);

    assertEquals("INSERT INTO foo VALUES ('para1\n\npara2');", cleaned);
  }

  @Test
  void stripLeadingCommentsAndBlankLines_returns_empty_when_the_whole_statement_is_a_comment() {
    String statement = "-- pure comment, nothing else in this statement";

    String cleaned = ScaffoldStartup.stripLeadingCommentsAndBlankLines(statement);

    assertEquals("", cleaned);
  }

  @Test
  void executeSeedStatements_skips_a_statement_that_is_entirely_a_comment() {
    String sql = "-- pure comment;\nINSERT INTO foo VALUES (1);";

    scaffoldStartup.executeSeedStatements(sql);

    verify(jdbcTemplate, times(1)).execute(anyString());
    verify(jdbcTemplate).execute("INSERT INTO foo VALUES (1);");
  }

  @Test
  void executeSeedStatements_executes_each_statement() {
    String sql = "INSERT INTO foo VALUES (1);\nINSERT INTO bar VALUES ('it''s here');\n";

    scaffoldStartup.executeSeedStatements(sql);

    verify(jdbcTemplate).execute("INSERT INTO foo VALUES (1);");
    verify(jdbcTemplate).execute("INSERT INTO bar VALUES ('it''s here');");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void executeSeedStatements_skips_blank_and_comment_only_lines() {
    String sql = "\n-- just a comment\n\nINSERT INTO foo VALUES (1);\n";

    scaffoldStartup.executeSeedStatements(sql);

    verify(jdbcTemplate, times(1)).execute(anyString());
    verify(jdbcTemplate).execute("INSERT INTO foo VALUES (1);");
  }

  @Test
  void executeSeedStatements_continues_after_a_duplicate_key_error() {
    String sql = "INSERT INTO foo VALUES (1);\nINSERT INTO bar VALUES (2);\n";
    doThrow(new DataIntegrityViolationException("duplicate key"))
        .when(jdbcTemplate)
        .execute("INSERT INTO foo VALUES (1);");

    // Should not throw, and the second statement should still be attempted.
    scaffoldStartup.executeSeedStatements(sql);

    verify(jdbcTemplate).execute("INSERT INTO foo VALUES (1);");
    verify(jdbcTemplate).execute("INSERT INTO bar VALUES (2);");
  }

  @Test
  void splitSqlStatements_splits_on_top_level_semicolons_only() {
    String sql = "INSERT INTO foo VALUES (1);INSERT INTO bar VALUES (2);";

    List<String> statements = ScaffoldStartup.splitSqlStatements(sql);

    assertEquals(2, statements.size());
    assertEquals("INSERT INTO foo VALUES (1);", statements.get(0));
    assertEquals("INSERT INTO bar VALUES (2);", statements.get(1));
  }

  @Test
  void splitSqlStatements_ignores_semicolons_inside_quoted_strings() {
    String sql = "INSERT INTO foo VALUES ('a;b');INSERT INTO bar VALUES ('it''s; here');";

    List<String> statements = ScaffoldStartup.splitSqlStatements(sql);

    assertEquals(2, statements.size());
    assertEquals("INSERT INTO foo VALUES ('a;b');", statements.get(0));
    assertEquals("INSERT INTO bar VALUES ('it''s; here');", statements.get(1));
  }

  @Test
  void splitSqlStatements_includes_a_trailing_statement_with_no_final_semicolon() {
    String sql = "INSERT INTO foo VALUES (1);\nINSERT INTO bar VALUES (2)";

    List<String> statements = ScaffoldStartup.splitSqlStatements(sql);

    assertEquals(2, statements.size());
    assertEquals("\nINSERT INTO bar VALUES (2)", statements.get(1));
  }

  @Test
  void alwaysRunOnStartup_seeds_all_default_pl_colors_when_table_is_empty() {
    when(plColorRepository.existsById(anyString())).thenReturn(false);

    scaffoldStartup.alwaysRunOnStartup();

    verify(plColorRepository, times(ScaffoldStartup.SEED_PL_COLORS.size())).save(any(PlColor.class));
    verify(plColorRepository).save(PlColor.builder().colorName("red1").hexCode("#ffccbc").build());
    verify(plColorRepository).save(PlColor.builder().colorName("gray3").hexCode("#505050").build());
  }

  @Test
  void alwaysRunOnStartup_skips_pl_colors_that_already_exist() {
    when(plColorRepository.existsById(anyString())).thenReturn(true);

    scaffoldStartup.alwaysRunOnStartup();

    verify(plColorRepository, never()).save(any(PlColor.class));
  }

  @Test
  void alwaysRunOnStartup_handles_pl_color_seeding_exception_gracefully() {
    when(plColorRepository.existsById(anyString())).thenThrow(new RuntimeException("DB error"));

    // Should not throw — exception is caught and logged.
    scaffoldStartup.alwaysRunOnStartup();

    verify(plColorRepository, never()).save(any(PlColor.class));
  }
}
