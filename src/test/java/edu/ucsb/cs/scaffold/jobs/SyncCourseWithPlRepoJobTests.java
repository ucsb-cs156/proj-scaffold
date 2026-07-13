package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.GithubService.DirectoryEntry;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService.AssessmentInfo;
import edu.ucsb.cs.scaffold.services.PrairieLearnService.CourseInstanceInfo;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class SyncCourseWithPlRepoJobTests {

  PatCredentialRepository patCredentialRepository = mock(PatCredentialRepository.class);
  PatEncryptionService patEncryptionService = mock(PatEncryptionService.class);
  PlRepoRepository plRepoRepository = mock(PlRepoRepository.class);
  PlInstanceRepository plInstanceRepository = mock(PlInstanceRepository.class);
  PlQuestionRepository plQuestionRepository = mock(PlQuestionRepository.class);
  PlScaffoldAssessmentRepository plScaffoldAssessmentRepository =
      mock(PlScaffoldAssessmentRepository.class);
  PlAssessmentRepository plAssessmentRepository = mock(PlAssessmentRepository.class);
  PlAssessmentQuestionRepository plAssessmentQuestionRepository =
      mock(PlAssessmentQuestionRepository.class);
  GithubService githubService = mock(GithubService.class);
  PrairieLearnService prairieLearnService = mock(PrairieLearnService.class);

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  Course course =
      Course.builder().id(20L).courseName("CS156").plRepoId(3L).plInstanceId(10L).build();
  PlRepo plRepo = PlRepo.builder().id(3L).repoName("ucsb-cs156/pl-demo").build();
  PlInstance plInstance =
      PlInstance.builder()
          .id(10L)
          .plRepoId(3L)
          .shortName("Fall2025")
          .longName("Fall 2025")
          .numericId(213133L)
          .build();
  PatCredential githubCredential =
      PatCredential.builder()
          .id(9L)
          .userId(7L)
          .platform(PatPlatform.GITHUB)
          .ciphertext("CIPHER")
          .keyVersion(2)
          .build();
  PatCredential plCredential =
      PatCredential.builder()
          .id(8L)
          .userId(7L)
          .platform(PatPlatform.PRAIRIELEARN)
          .ciphertext("PL_CIPHER")
          .keyVersion(2)
          .build();

  static final String REPO = "ucsb-cs156/pl-demo";
  static final String TOKEN = "ghp_secret";
  static final String PL_TOKEN = "pl_secret";
  static final UUID UUID_1 = UUID.fromString("fafd1202-4acc-4bbf-a867-7be5223dc2be");
  static final UUID UUID_2 = UUID.fromString("11111111-2222-3333-4444-555555555555");

  private SyncCourseWithPlRepoJob job() {
    return SyncCourseWithPlRepoJob.builder()
        .userId(7L)
        .course(course)
        .patCredentialRepository(patCredentialRepository)
        .patEncryptionService(patEncryptionService)
        .plRepoRepository(plRepoRepository)
        .plInstanceRepository(plInstanceRepository)
        .plQuestionRepository(plQuestionRepository)
        .plScaffoldAssessmentRepository(plScaffoldAssessmentRepository)
        .plAssessmentRepository(plAssessmentRepository)
        .plAssessmentQuestionRepository(plAssessmentQuestionRepository)
        .githubService(githubService)
        .prairieLearnService(prairieLearnService)
        .build();
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(
        status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
  }

  private static DirectoryEntry dir(String name) {
    return new DirectoryEntry(name, "dir");
  }

  private static DirectoryEntry file(String name) {
    return new DirectoryEntry(name, "file");
  }

  private static String infoJson(UUID uuid, String title) {
    return """
        { "uuid": "%s", "title": "%s", "topic": "Default", "type": "v3" }"""
        .formatted(uuid, title);
  }

  @BeforeEach
  public void setUp() {
    when(patCredentialRepository.findByUserIdAndPlatform(eq(7L), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.of(githubCredential));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(7L), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.of(plCredential));
    when(patEncryptionService.decrypt(eq("CIPHER"), eq(2))).thenReturn(TOKEN);
    when(patEncryptionService.decrypt(eq("PL_CIPHER"), eq(2))).thenReturn(PL_TOKEN);
    when(plRepoRepository.findById(eq(3L))).thenReturn(Optional.of(plRepo));
    when(plInstanceRepository.findById(eq(10L))).thenReturn(Optional.of(plInstance));
    when(githubService.hasWriteAccess(eq(REPO), eq(TOKEN))).thenReturn(true);
    when(prairieLearnService.getCourseInstance(eq(213133L), eq(PL_TOKEN)))
        .thenReturn(new CourseInstanceInfo(213133L, "Fall 2025", "Fall2025"));
    // Individual tests override these as needed; by default both directories are missing.
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));
  }

  static final String ASSESSMENTS_PATH = "courseInstances/Fall2025/assessments";

  private static final String HEADER = "Syncing course 20 (CS156) with PrairieLearn";
  private static final String ACCESS_LINE =
      "Access verified: repo ucsb-cs156/pl-demo (read/write) and PrairieLearn instance 213133";
  private static final String VERIFIED_LINE = "Instance Fall2025 metadata verified";
  private static final String SKIP_QUESTIONS_LINE =
      "Repo ucsb-cs156/pl-demo has no questions directory (or the token cannot see the repo); skipping question sync";
  private static final String SKIP_ASSESSMENTS_LINE =
      "Instance Fall2025 has no assessments directory; skipping assessment sync";
  private static final String NO_ASSESSMENTS_SUMMARY =
      "Assessments: 0 added, 0 deleted, 0 unchanged";
  private static final String ENRICH_ZERO_SUMMARY =
      "PrairieLearn assessment fields: 0 updated, 0 without a matching repo assessment";

  private static final String PREAMBLE = HEADER + "\n" + ACCESS_LINE + "\n" + VERIFIED_LINE;

  // ────────────────────────── precondition failures ──────────────────────────

  @Test
  public void fails_when_the_github_pat_is_missing() {
    when(patCredentialRepository.findByUserIdAndPlatform(eq(7L), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "Missing GitHub PAT: set it up on the /profile page before running this job",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_prairielearn_pat_is_missing() {
    when(patCredentialRepository.findByUserIdAndPlatform(eq(7L), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "Missing PrairieLearn PAT: set it up on the /profile page before running this job",
        thrown.getMessage());
  }

  @Test
  public void fails_when_both_pats_are_missing() {
    when(patCredentialRepository.findByUserIdAndPlatform(anyLong(), any()))
        .thenReturn(Optional.empty());

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "Missing GitHub PAT and PrairieLearn PAT: set it up on the /profile page before running this job",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_course_has_no_repo_association() {
    course.setPlRepoId(null);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "This course is not associated with a GitHub repo yet; set that up on the PrairieLearn tab of the course settings page",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_course_has_no_instance_association() {
    course.setPlInstanceId(null);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "This course is not associated with a PrairieLearn course instance yet; set that up on the PrairieLearn tab of the course settings page",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_course_has_neither_association() {
    course.setPlRepoId(null);
    course.setPlInstanceId(null);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "This course is not associated with a GitHub repo or a PrairieLearn course instance yet; set that up on the PrairieLearn tab of the course settings page",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_plrepo_row_does_not_exist() {
    when(plRepoRepository.findById(eq(3L))).thenReturn(Optional.empty());

    EntityNotFoundException thrown =
        assertThrows(EntityNotFoundException.class, () -> job().accept(ctx));
    assertEquals("PlRepo with id 3 not found", thrown.getMessage());
  }

  @Test
  public void fails_when_the_plinstance_row_does_not_exist() {
    when(plInstanceRepository.findById(eq(10L))).thenReturn(Optional.empty());

    EntityNotFoundException thrown =
        assertThrows(EntityNotFoundException.class, () -> job().accept(ctx));
    assertEquals("PlInstance with id 10 not found", thrown.getMessage());
  }

  @Test
  public void fails_when_the_instance_has_no_numeric_id() {
    plInstance.setNumericId(null);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "The course's PrairieLearn instance has no numeric id yet; re-associate it on the PrairieLearn tab of the course settings page",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_github_pat_cannot_read_the_repo() {
    when(githubService.hasWriteAccess(eq(REPO), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "The stored GitHub PAT cannot read repo ucsb-cs156/pl-demo (HTTP 404); check the token on the /profile page and the repo on the PrairieLearn tab",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_github_pat_has_read_only_access() {
    when(githubService.hasWriteAccess(eq(REPO), eq(TOKEN))).thenReturn(false);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "The stored GitHub PAT has read-only access to repo ucsb-cs156/pl-demo; read/write access is required",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_prairielearn_pat_cannot_access_the_instance() {
    when(prairieLearnService.getCourseInstance(eq(213133L), eq(PL_TOKEN)))
        .thenThrow(httpError(HttpStatus.FORBIDDEN));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "The stored PrairieLearn PAT cannot access course instance 213133 (HTTP 403); check the token on the /profile page",
        thrown.getMessage());
  }

  @Test
  public void fails_when_prairielearn_returns_no_data_for_the_instance() {
    when(prairieLearnService.getCourseInstance(eq(213133L), eq(PL_TOKEN))).thenReturn(null);

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals("PrairieLearn returned no data for course instance 213133", thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_401_during_question_sync() {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.UNAUTHORIZED));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 401). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/Github_PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_403_during_question_sync() {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.FORBIDDEN));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 403). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/Github_PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void other_github_errors_propagate_and_fail_the_job() {
    HttpClientErrorException tooManyRequests = httpError(HttpStatus.TOO_MANY_REQUESTS);
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(tooManyRequests);

    HttpClientErrorException thrown =
        assertThrows(HttpClientErrorException.class, () -> job().accept(ctx));
    assertEquals(tooManyRequests, thrown);
  }

  // ────────────────────────── instance metadata sanity check ──────────────────────────

  @Test
  public void happy_path_with_nothing_to_sync_verifies_metadata_and_logs_each_step()
      throws Exception {
    job().accept(ctx);

    verify(plInstanceRepository, never()).save(any());
    String expected =
        """
        %s
        %s
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void updates_the_stored_names_when_the_instance_was_renamed_on_prairielearn()
      throws Exception {
    when(prairieLearnService.getCourseInstance(eq(213133L), eq(PL_TOKEN)))
        .thenReturn(new CourseInstanceInfo(213133L, "Winter 2026", "Winter2026"));
    // the renamed instance's assessments directory: empty rather than missing
    when(githubService.listDirectory(
            eq(REPO), eq("courseInstances/Winter2026/assessments"), eq(TOKEN)))
        .thenReturn(List.of());

    job().accept(ctx);

    verify(plInstanceRepository).save(eq(plInstance));
    assertEquals("Winter2026", plInstance.getShortName());
    assertEquals("Winter 2026", plInstance.getLongName());

    String expected =
        """
        %s
        %s
        Instance shortName changed on PrairieLearn: Fall2025 -> Winter2026
        Instance longName changed on PrairieLearn: Fall 2025 -> Winter 2026
        %s
        %s
        %s"""
            .formatted(
                HEADER,
                ACCESS_LINE,
                SKIP_QUESTIONS_LINE,
                NO_ASSESSMENTS_SUMMARY,
                ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  // ────────────────────────── question sync ──────────────────────────

  @Test
  public void adds_top_level_questions_skipping_drafts_files_and_question_subdirectories()
      throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("foo"), dir("__drafts__"), file("README.md")));
    when(githubService.listDirectory(eq(REPO), eq("questions/foo"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json"), dir("tests")));
    when(githubService.getFileContent(eq(REPO), eq("questions/foo/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_1, "2D Array"));
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository)
        .save(
            eq(
                PlQuestion.builder()
                    .plRepoId(3L)
                    .questionId("foo")
                    .uuid(UUID_1)
                    .title("2D Array")
                    .build()));
    verify(githubService, never()).listDirectory(eq(REPO), eq("questions/__drafts__"), eq(TOKEN));
    verify(githubService, never()).listDirectory(eq(REPO), eq("questions/foo/tests"), eq(TOKEN));

    String expected =
        """
        %s
        Skipping directory questions/__drafts__
        Added question foo (2D Array)
        Questions: 1 added, 0 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void nested_questions_get_slash_separated_questionIds() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("a")));
    when(githubService.listDirectory(eq(REPO), eq("questions/a"), eq(TOKEN)))
        .thenReturn(List.of(dir("b")));
    when(githubService.listDirectory(eq(REPO), eq("questions/a/b"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/a/b/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_1, "Nested"));
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository)
        .save(
            eq(
                PlQuestion.builder()
                    .plRepoId(3L)
                    .questionId("a/b")
                    .uuid(UUID_1)
                    .title("Nested")
                    .build()));
  }

  @Test
  public void info_json_at_the_questions_root_is_not_a_question() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json"), dir("foo")));
    when(githubService.listDirectory(eq(REPO), eq("questions/foo"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/foo/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_1, "Real Question"));
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(githubService, never()).getFileContent(eq(REPO), eq("questions/info.json"), eq(TOKEN));
    verify(plQuestionRepository)
        .save(
            eq(
                PlQuestion.builder()
                    .plRepoId(3L)
                    .questionId("foo")
                    .uuid(UUID_1)
                    .title("Real Question")
                    .build()));
  }

  @Test
  public void a_subdirectory_named_info_json_does_not_make_its_parent_a_question()
      throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("weird")));
    when(githubService.listDirectory(eq(REPO), eq("questions/weird"), eq(TOKEN)))
        .thenReturn(List.of(dir("info.json")));
    when(githubService.listDirectory(eq(REPO), eq("questions/weird/info.json"), eq(TOKEN)))
        .thenReturn(List.of());
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
    String expected =
        """
        %s
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void updates_a_question_whose_uuid_or_title_changed() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("foo")));
    when(githubService.listDirectory(eq(REPO), eq("questions/foo"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/foo/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_2, "New Title"));
    PlQuestion existing =
        PlQuestion.builder()
            .id(11L)
            .plRepoId(3L)
            .questionId("foo")
            .uuid(UUID_1)
            .title("Old Title")
            .build();
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of(existing));

    job().accept(ctx);

    verify(plQuestionRepository).save(eq(existing));
    assertEquals(UUID_2, existing.getUuid());
    assertEquals("New Title", existing.getTitle());
    verify(plQuestionRepository, never()).delete(any());

    String expected =
        """
        %s
        Updated question foo (New Title)
        Questions: 0 added, 1 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void updates_a_question_when_only_the_title_changed() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("foo")));
    when(githubService.listDirectory(eq(REPO), eq("questions/foo"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/foo/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_1, "New Title"));
    PlQuestion existing =
        PlQuestion.builder()
            .id(11L)
            .plRepoId(3L)
            .questionId("foo")
            .uuid(UUID_1)
            .title("Old Title")
            .build();
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of(existing));

    job().accept(ctx);

    verify(plQuestionRepository).save(eq(existing));
    assertEquals(UUID_1, existing.getUuid());
    assertEquals("New Title", existing.getTitle());
  }

  @Test
  public void an_unchanged_question_is_not_saved_again() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("foo")));
    when(githubService.listDirectory(eq(REPO), eq("questions/foo"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/foo/info.json"), eq(TOKEN)))
        .thenReturn(infoJson(UUID_1, "Same"));
    when(plQuestionRepository.findByPlRepoId(eq(3L)))
        .thenReturn(
            List.of(
                PlQuestion.builder()
                    .id(11L)
                    .plRepoId(3L)
                    .questionId("foo")
                    .uuid(UUID_1)
                    .title("Same")
                    .build()));

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
    String expected =
        """
        %s
        Questions: 0 added, 0 updated, 0 deleted, 1 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void deletes_stale_questions_cascading_to_their_scaffold_assessments() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN))).thenReturn(List.of());
    PlQuestion stale =
        PlQuestion.builder()
            .id(12L)
            .plRepoId(3L)
            .questionId("old")
            .uuid(UUID_1)
            .title("Old")
            .build();
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of(stale));

    job().accept(ctx);

    verify(plScaffoldAssessmentRepository).deleteByPlQuestionId(eq(12L));
    verify(plAssessmentQuestionRepository).deleteByPlQuestionId(eq(12L));
    verify(plQuestionRepository).delete(eq(stale));

    String expected =
        """
        %s
        Deleted question old (no longer on GitHub)
        Questions: 0 added, 0 updated, 1 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void a_question_with_unparseable_info_json_is_skipped() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("bad")));
    when(githubService.listDirectory(eq(REPO), eq("questions/bad"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/bad/info.json"), eq(TOKEN)))
        .thenReturn("this is not json {");
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
    String expected =
        """
        %s
        Skipping question bad: could not parse info.json
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void a_question_whose_info_json_lacks_uuid_is_skipped() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("bad")));
    when(githubService.listDirectory(eq(REPO), eq("questions/bad"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/bad/info.json"), eq(TOKEN)))
        .thenReturn("""
            { "title": "No uuid here" }""");
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
    String expected =
        """
        %s
        Skipping question bad: info.json is missing uuid or title
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void a_question_whose_info_json_lacks_title_is_skipped() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("bad")));
    when(githubService.listDirectory(eq(REPO), eq("questions/bad"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/bad/info.json"), eq(TOKEN)))
        .thenReturn("""
            { "uuid": "%s" }""".formatted(UUID_1));
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
  }

  @Test
  public void a_question_with_an_invalid_uuid_is_skipped() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenReturn(List.of(dir("bad")));
    when(githubService.listDirectory(eq(REPO), eq("questions/bad"), eq(TOKEN)))
        .thenReturn(List.of(file("info.json")));
    when(githubService.getFileContent(eq(REPO), eq("questions/bad/info.json"), eq(TOKEN)))
        .thenReturn("""
            { "uuid": "not-a-uuid", "title": "Bad UUID" }""");
    when(plQuestionRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());

    job().accept(ctx);

    verify(plQuestionRepository, never()).save(any());
    String expected =
        """
        %s
        Skipping question bad: could not parse info.json
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_ASSESSMENTS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  // ────────────────────────── assessment sync (course's own instance only) ─────────────────────

  private void stubQuestionsInDatabase() {
    when(plQuestionRepository.findByPlRepoId(eq(3L)))
        .thenReturn(
            List.of(
                PlQuestion.builder()
                    .id(11L)
                    .plRepoId(3L)
                    .questionId("foo")
                    .uuid(UUID_1)
                    .title("T1")
                    .build(),
                PlQuestion.builder()
                    .id(12L)
                    .plRepoId(3L)
                    .questionId("bar/baz")
                    .uuid(UUID_2)
                    .title("T2")
                    .build()));
  }

  private static final String EXAM1_INFO_ASSESSMENT =
      """
      {
        "uuid": "22222222-3333-4444-5555-666666666666",
        "title": "Exam 1",
        "zones": [
          {
            "title": "Zone 1",
            "questions": [
              { "id": "foo", "points": 5 },
              { "alternatives": [ { "id": "bar/baz", "points": 3 } ] }
            ]
          }
        ]
      }
      """;

  // The questions listed by stubQuestionsInDatabase appear as "deleted" in the questions phase,
  // since the (stubbed) questions directory listing is empty. These lines keep the expected logs
  // of the assessment tests exact without repeating them in every test.
  private static final String QUESTIONS_DELETED_LINES =
      """
      Deleted question bar/baz (no longer on GitHub)
      Deleted question foo (no longer on GitHub)
      Questions: 0 added, 0 updated, 2 deleted, 0 unchanged""";

  private void stubEmptyQuestionsDirectory() {
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN))).thenReturn(List.of());
  }

  @Test
  public void adds_assessments_and_links_questions_in_zone_order() throws Exception {
    stubQuestionsInDatabase();
    stubEmptyQuestionsDirectory();
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("exam1"), file("README.md")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/exam1"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/exam1/infoAssessment.json"), eq(TOKEN)))
        .thenReturn(EXAM1_INFO_ASSESSMENT);
    when(plAssessmentRepository.save(any(PlAssessment.class)))
        .thenAnswer(
            invocation -> {
              PlAssessment saved = invocation.getArgument(0);
              saved.setId(77L);
              return saved;
            });

    job().accept(ctx);

    verify(plAssessmentRepository)
        .save(
            eq(
                PlAssessment.builder()
                    .id(77L)
                    .plRepoId(3L)
                    .plInstanceId(10L)
                    .name("exam1")
                    .build()));
    verify(plAssessmentQuestionRepository)
        .save(
            eq(
                PlAssessmentQuestion.builder()
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(11L)
                    .ordinal(0)
                    .build()));
    verify(plAssessmentQuestionRepository)
        .save(
            eq(
                PlAssessmentQuestion.builder()
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(12L)
                    .ordinal(1)
                    .build()));

    String expected =
        """
        %s
        %s
        Added assessment exam1 (instance Fall2025)
        Linked 2 question(s) to assessment exam1 (instance Fall2025)
        Assessments: 1 added, 0 deleted, 0 unchanged
        %s"""
            .formatted(PREAMBLE, QUESTIONS_DELETED_LINES, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void unknown_question_ids_in_zones_are_logged_and_skipped() throws Exception {
    stubQuestionsInDatabase();
    stubEmptyQuestionsDirectory();
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("exam1")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/exam1"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/exam1/infoAssessment.json"), eq(TOKEN)))
        .thenReturn(
            """
            { "zones": [ { "questions": [ { "id": "foo" }, { "id": "nope" } ] } ] }""");
    when(plAssessmentRepository.save(any(PlAssessment.class)))
        .thenAnswer(
            invocation -> {
              PlAssessment saved = invocation.getArgument(0);
              saved.setId(77L);
              return saved;
            });

    job().accept(ctx);

    verify(plAssessmentQuestionRepository)
        .save(
            eq(
                PlAssessmentQuestion.builder()
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(11L)
                    .ordinal(0)
                    .build()));
    verify(plAssessmentQuestionRepository, times(1)).save(any());

    String expected =
        """
        %s
        %s
        Added assessment exam1 (instance Fall2025)
        Assessment exam1 (instance Fall2025) references unknown question id nope; skipping that link
        Linked 1 question(s) to assessment exam1 (instance Fall2025)
        Assessments: 1 added, 0 deleted, 0 unchanged
        %s"""
            .formatted(PREAMBLE, QUESTIONS_DELETED_LINES, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void an_assessment_whose_links_already_match_is_left_untouched() throws Exception {
    stubQuestionsInDatabase();
    stubEmptyQuestionsDirectory();
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("exam1")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/exam1"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/exam1/infoAssessment.json"), eq(TOKEN)))
        .thenReturn(EXAM1_INFO_ASSESSMENT);
    PlAssessment existing =
        PlAssessment.builder().id(77L).plRepoId(3L).plInstanceId(10L).name("exam1").build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(eq(3L), eq(10L)))
        .thenReturn(List.of(existing));
    when(plAssessmentQuestionRepository.findByPlAssessmentIdOrderByOrdinalAsc(eq(77L)))
        .thenReturn(
            List.of(
                PlAssessmentQuestion.builder()
                    .id(1L)
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(11L)
                    .ordinal(0)
                    .build(),
                PlAssessmentQuestion.builder()
                    .id(2L)
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(12L)
                    .ordinal(1)
                    .build()));

    job().accept(ctx);

    verify(plAssessmentRepository, never()).save(any());
    verify(plAssessmentQuestionRepository, never()).deleteByPlAssessmentId(any());
    verify(plAssessmentQuestionRepository, never()).save(any());

    String expected =
        """
        %s
        %s
        Assessments: 0 added, 0 deleted, 1 unchanged
        %s"""
            .formatted(PREAMBLE, QUESTIONS_DELETED_LINES, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void changed_links_are_rewritten_with_a_flush_between_delete_and_insert()
      throws Exception {
    stubQuestionsInDatabase();
    stubEmptyQuestionsDirectory();
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("exam1")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/exam1"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/exam1/infoAssessment.json"), eq(TOKEN)))
        .thenReturn("""
            { "zones": [ { "questions": [ { "id": "bar/baz" } ] } ] }""");
    PlAssessment existing =
        PlAssessment.builder().id(77L).plRepoId(3L).plInstanceId(10L).name("exam1").build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(eq(3L), eq(10L)))
        .thenReturn(List.of(existing));
    when(plAssessmentQuestionRepository.findByPlAssessmentIdOrderByOrdinalAsc(eq(77L)))
        .thenReturn(
            List.of(
                PlAssessmentQuestion.builder()
                    .id(1L)
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(11L)
                    .ordinal(0)
                    .build()));

    job().accept(ctx);

    verify(plAssessmentQuestionRepository).deleteByPlAssessmentId(eq(77L));
    verify(plAssessmentQuestionRepository).flush();
    verify(plAssessmentQuestionRepository)
        .save(
            eq(
                PlAssessmentQuestion.builder()
                    .plRepoId(3L)
                    .plAssessmentId(77L)
                    .plQuestionId(12L)
                    .ordinal(0)
                    .build()));

    String expected =
        """
        %s
        %s
        Linked 1 question(s) to assessment exam1 (instance Fall2025)
        Assessments: 0 added, 0 deleted, 1 unchanged
        %s"""
            .formatted(PREAMBLE, QUESTIONS_DELETED_LINES, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void stale_assessments_are_deleted_with_their_join_rows() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of());
    PlAssessment stale =
        PlAssessment.builder().id(88L).plRepoId(3L).plInstanceId(10L).name("oldExam").build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(eq(3L), eq(10L)))
        .thenReturn(List.of(stale));
    when(prairieLearnService.getAssessments(eq(213133L), eq(PL_TOKEN))).thenReturn(List.of());

    job().accept(ctx);

    verify(plAssessmentQuestionRepository).deleteByPlAssessmentId(eq(88L));
    verify(plAssessmentRepository).delete(eq(stale));

    String expected =
        """
        %s
        %s
        Deleted assessment oldExam (instance Fall2025) (no longer on GitHub)
        Assessments: 0 added, 1 deleted, 0 unchanged
        %s"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void an_assessment_without_a_zones_key_gets_no_question_links() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("exam1")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/exam1"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/exam1/infoAssessment.json"), eq(TOKEN)))
        .thenReturn(
            """
            { "uuid": "22222222-3333-4444-5555-666666666666", "title": "No zones" }""");
    when(plAssessmentRepository.save(any(PlAssessment.class)))
        .thenAnswer(
            invocation -> {
              PlAssessment saved = invocation.getArgument(0);
              saved.setId(77L);
              return saved;
            });

    job().accept(ctx);

    verify(plAssessmentQuestionRepository, never()).save(any());

    String expected =
        """
        %s
        %s
        Added assessment exam1 (instance Fall2025)
        Assessments: 1 added, 0 deleted, 0 unchanged
        %s"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void directories_without_infoAssessment_json_are_not_assessments() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("notes"), file("README.md")));
    // a subdirectory *named* infoAssessment.json is not a file, so "notes" is not an assessment
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/notes"), eq(TOKEN)))
        .thenReturn(List.of(dir("infoAssessment.json"), file("other.txt")));

    job().accept(ctx);

    verify(plAssessmentRepository, never()).save(any());
    verify(plAssessmentQuestionRepository, never()).save(any());

    String expected =
        """
        %s
        %s
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, NO_ASSESSMENTS_SUMMARY, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void an_assessment_with_unparseable_infoAssessment_json_is_skipped() throws Exception {
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of(dir("bad")));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH + "/bad"), eq(TOKEN)))
        .thenReturn(List.of(file("infoAssessment.json")));
    when(githubService.getFileContent(
            eq(REPO), eq(ASSESSMENTS_PATH + "/bad/infoAssessment.json"), eq(TOKEN)))
        .thenReturn("this is not json {");

    job().accept(ctx);

    verify(plAssessmentRepository, never()).save(any());

    String expected =
        """
        %s
        %s
        Skipping assessment bad (instance Fall2025): could not parse infoAssessment.json
        %s
        %s"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, NO_ASSESSMENTS_SUMMARY, ENRICH_ZERO_SUMMARY);
    assertEquals(expected, jobStarted.getLog());
  }

  // ────────────────────────── PrairieLearn assessment field enrichment ──────────────────────────

  private static final AssessmentInfo EXAM1_PL_INFO =
      new AssessmentInfo(
          2690012L, "exam1", "1a", 6L, "Final (in Testing Center)", "E", 8, "Exams", "pink2");

  @Test
  public void copies_the_prairielearn_fields_onto_the_matching_assessment() throws Exception {
    PlAssessment existing =
        PlAssessment.builder().id(77L).plRepoId(3L).plInstanceId(10L).name("exam1").build();
    when(plAssessmentRepository.findByPlRepoIdAndPlInstanceId(eq(3L), eq(10L)))
        .thenReturn(List.of(existing));
    when(githubService.listDirectory(eq(REPO), eq(ASSESSMENTS_PATH), eq(TOKEN)))
        .thenReturn(List.of());
    when(prairieLearnService.getAssessments(eq(213133L), eq(PL_TOKEN)))
        .thenReturn(List.of(EXAM1_PL_INFO));

    job().accept(ctx);

    verify(plAssessmentRepository).save(eq(existing));
    assertEquals(Long.valueOf(2690012L), existing.getPlAssessmentId());
    assertEquals("1a", existing.getPlAssessmentNumber());
    assertEquals(Long.valueOf(6L), existing.getPlAssessmentOrder());
    assertEquals("Final (in Testing Center)", existing.getPlAssessmentTitle());
    assertEquals("E", existing.getPlAssessmentSetAbbreviation());
    assertEquals(Integer.valueOf(8), existing.getPlAssessmentSetNumber());
    assertEquals("Exams", existing.getPlAssessmentSetHeading());
    assertEquals("pink2", existing.getPlAssessmentSetColor());

    String expected =
        """
        %s
        %s
        Deleted assessment exam1 (instance Fall2025) (no longer on GitHub)
        Assessments: 0 added, 1 deleted, 0 unchanged
        Updated PrairieLearn fields for assessment exam1
        PrairieLearn assessment fields: 1 updated, 0 without a matching repo assessment"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void prairielearn_assessments_with_no_matching_repo_row_are_logged_and_skipped()
      throws Exception {
    when(prairieLearnService.getAssessments(eq(213133L), eq(PL_TOKEN)))
        .thenReturn(List.of(EXAM1_PL_INFO));

    job().accept(ctx);

    verify(plAssessmentRepository, never()).save(any());

    String expected =
        """
        %s
        %s
        %s
        PrairieLearn assessment exam1 has no matching assessment directory in the repo; skipping
        PrairieLearn assessment fields: 0 updated, 1 without a matching repo assessment"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, SKIP_ASSESSMENTS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void a_prairielearn_error_on_the_assessments_listing_skips_enrichment_only()
      throws Exception {
    when(prairieLearnService.getAssessments(eq(213133L), eq(PL_TOKEN)))
        .thenThrow(httpError(HttpStatus.TOO_MANY_REQUESTS));

    job().accept(ctx);

    String expected =
        """
        %s
        %s
        %s
        Could not list assessments from PrairieLearn (HTTP 429); skipping assessment field updates"""
            .formatted(PREAMBLE, SKIP_QUESTIONS_LINE, SKIP_ASSESSMENTS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void the_job_reports_its_course_scope_for_the_jobs_table() {
    assertEquals("course", job().getScopeType());
    assertEquals(course.getId(), job().getScopeId());
  }
}
