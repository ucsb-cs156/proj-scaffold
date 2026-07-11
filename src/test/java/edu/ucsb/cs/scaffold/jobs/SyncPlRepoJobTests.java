package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.GithubService.DirectoryEntry;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class SyncPlRepoJobTests {

  PatCredentialRepository patCredentialRepository = mock(PatCredentialRepository.class);
  PatEncryptionService patEncryptionService = mock(PatEncryptionService.class);
  PlRepoRepository plRepoRepository = mock(PlRepoRepository.class);
  PlInstanceRepository plInstanceRepository = mock(PlInstanceRepository.class);
  PlQuestionRepository plQuestionRepository = mock(PlQuestionRepository.class);
  PlScaffoldAssessmentRepository plScaffoldAssessmentRepository =
      mock(PlScaffoldAssessmentRepository.class);
  PlAssessmentRepository plAssessmentRepository = mock(PlAssessmentRepository.class);
  GithubService githubService = mock(GithubService.class);

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  PlRepo plRepo = PlRepo.builder().id(3L).repoName("ucsb-cs156/pl-demo").build();
  PatCredential credential =
      PatCredential.builder().id(9L).userId(7L).ciphertext("CIPHER").keyVersion(2).build();

  static final String REPO = "ucsb-cs156/pl-demo";
  static final String TOKEN = "ghp_secret";
  static final UUID UUID_1 = UUID.fromString("fafd1202-4acc-4bbf-a867-7be5223dc2be");
  static final UUID UUID_2 = UUID.fromString("11111111-2222-3333-4444-555555555555");

  private SyncPlRepoJob job() {
    return SyncPlRepoJob.builder()
        .userId(7L)
        .plRepoId(3L)
        .patCredentialRepository(patCredentialRepository)
        .patEncryptionService(patEncryptionService)
        .plRepoRepository(plRepoRepository)
        .plInstanceRepository(plInstanceRepository)
        .plQuestionRepository(plQuestionRepository)
        .plScaffoldAssessmentRepository(plScaffoldAssessmentRepository)
        .plAssessmentRepository(plAssessmentRepository)
        .githubService(githubService)
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
    when(plRepoRepository.findById(eq(3L))).thenReturn(Optional.of(plRepo));
    when(patCredentialRepository.findByUserId(eq(7L))).thenReturn(Optional.of(credential));
    when(patEncryptionService.decrypt(eq("CIPHER"), eq(2))).thenReturn(TOKEN);
    // Individual tests override these as needed; by default both directories are missing.
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));
  }

  private static final String SKIP_INSTANCES_LINE =
      "Repo ucsb-cs156/pl-demo has no courseInstances directory (or the token cannot see the repo); skipping course instance sync";
  private static final String SKIP_QUESTIONS_LINE =
      "Repo ucsb-cs156/pl-demo has no questions directory (or the token cannot see the repo); skipping question sync";

  // Course instance sync

  @Test
  public void adds_new_instances_and_counts_unchanged_ones() throws Exception {
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenReturn(List.of("Fall2025", "Winter2026"));
    when(plInstanceRepository.findByPlRepoId(eq(3L)))
        .thenReturn(List.of(PlInstance.builder().id(1L).plRepoId(3L).name("Fall2025").build()));

    job().accept(ctx);

    verify(plInstanceRepository)
        .save(eq(PlInstance.builder().plRepoId(3L).name("Winter2026").build()));
    verify(plInstanceRepository, never()).delete(any());

    String expected =
        """
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        Added course instance Winter2026
        Course instances: 1 added, 0 deleted, 1 unchanged
        %s"""
            .formatted(SKIP_QUESTIONS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void deletes_stale_instances_cascading_to_their_assessments() throws Exception {
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenReturn(List.of("Fall2025"));
    PlInstance keep = PlInstance.builder().id(1L).plRepoId(3L).name("Fall2025").build();
    PlInstance stale1 = PlInstance.builder().id(2L).plRepoId(3L).name("Spring2024").build();
    PlInstance stale2 = PlInstance.builder().id(5L).plRepoId(3L).name("Winter2024").build();
    when(plInstanceRepository.findByPlRepoId(eq(3L))).thenReturn(List.of(keep, stale1, stale2));

    job().accept(ctx);

    verify(plScaffoldAssessmentRepository).deleteByPlInstanceId(eq(2L));
    verify(plAssessmentRepository).deleteByPlInstanceId(eq(2L));
    verify(plInstanceRepository).delete(eq(stale1));
    verify(plScaffoldAssessmentRepository).deleteByPlInstanceId(eq(5L));
    verify(plAssessmentRepository).deleteByPlInstanceId(eq(5L));
    verify(plInstanceRepository).delete(eq(stale2));
    verify(plInstanceRepository, never()).delete(eq(keep));
    verify(plInstanceRepository, never()).save(any());

    String expected =
        """
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        Deleted course instance Spring2024 (no longer on GitHub)
        Deleted course instance Winter2024 (no longer on GitHub)
        Course instances: 0 added, 2 deleted, 1 unchanged
        %s"""
            .formatted(SKIP_QUESTIONS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void missing_courseInstances_directory_skips_the_step_including_deletions()
      throws Exception {
    job().accept(ctx);

    verify(plInstanceRepository, never()).findByPlRepoId(any());
    verify(plInstanceRepository, never()).delete(any());
    verify(plInstanceRepository, never()).save(any());

    String expected =
        """
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        %s"""
            .formatted(SKIP_INSTANCES_LINE, SKIP_QUESTIONS_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  // Question sync

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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Skipping directory questions/__drafts__
        Added question foo (2D Array)
        Questions: 1 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Updated question foo (New Title)
        Questions: 0 added, 1 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
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
  public void a_question_whose_info_json_has_uuid_but_no_title_is_skipped() throws Exception {
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
    String expected =
        """
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Skipping question bad: info.json is missing uuid or title
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
    assertEquals(expected, jobStarted.getLog());
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Questions: 0 added, 0 updated, 0 deleted, 1 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
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
    verify(plQuestionRepository).delete(eq(stale));

    String expected =
        """
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Deleted question old (no longer on GitHub)
        Questions: 0 added, 0 updated, 1 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Skipping question bad: could not parse info.json
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void a_question_whose_info_json_lacks_uuid_or_title_is_skipped() throws Exception {
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Skipping question bad: info.json is missing uuid or title
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
    assertEquals(expected, jobStarted.getLog());
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
        Syncing repo ucsb-cs156/pl-demo (PlRepo id 3)
        %s
        Skipping question bad: could not parse info.json
        Questions: 0 added, 0 updated, 0 deleted, 0 unchanged"""
            .formatted(SKIP_INSTANCES_LINE);
    assertEquals(expected, jobStarted.getLog());
  }

  // Failure modes

  @Test
  public void fails_when_the_plrepo_does_not_exist() {
    when(plRepoRepository.findById(eq(3L))).thenReturn(Optional.empty());

    EntityNotFoundException thrown =
        assertThrows(EntityNotFoundException.class, () -> job().accept(ctx));
    assertEquals("PlRepo with id 3 not found", thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_the_user_has_no_stored_pat() {
    when(patCredentialRepository.findByUserId(eq(7L))).thenReturn(Optional.empty());

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "No PAT is stored for user id 7; enter one first (see docs/PAT.md)", thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_401() {
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.UNAUTHORIZED));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 401). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_403_during_question_sync() {
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenReturn(List.of());
    when(plInstanceRepository.findByPlRepoId(eq(3L))).thenReturn(List.of());
    when(githubService.listDirectory(eq(REPO), eq("questions"), eq(TOKEN)))
        .thenThrow(httpError(HttpStatus.FORBIDDEN));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 403). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void other_github_errors_propagate_and_fail_the_job() {
    HttpClientErrorException tooManyRequests = httpError(HttpStatus.TOO_MANY_REQUESTS);
    when(githubService.listSubdirectories(eq(REPO), eq("courseInstances"), eq(TOKEN)))
        .thenThrow(tooManyRequests);

    HttpClientErrorException thrown =
        assertThrows(HttpClientErrorException.class, () -> job().accept(ctx));
    assertEquals(tooManyRequests, thrown);
  }
}
