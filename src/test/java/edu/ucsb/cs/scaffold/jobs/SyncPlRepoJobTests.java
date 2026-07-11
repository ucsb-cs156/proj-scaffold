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
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
  GithubService githubService = mock(GithubService.class);

  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  PlRepo plRepo = PlRepo.builder().id(3L).repoName("ucsb-cs156/pl-demo").build();
  PatCredential credential =
      PatCredential.builder().id(9L).userId(7L).ciphertext("CIPHER").keyVersion(2).build();

  private SyncPlRepoJob job() {
    return SyncPlRepoJob.builder()
        .userId(7L)
        .plRepoId(3L)
        .patCredentialRepository(patCredentialRepository)
        .patEncryptionService(patEncryptionService)
        .plRepoRepository(plRepoRepository)
        .plInstanceRepository(plInstanceRepository)
        .githubService(githubService)
        .build();
  }

  @BeforeEach
  public void setUp() {
    when(plRepoRepository.findById(eq(3L))).thenReturn(Optional.of(plRepo));
    when(patCredentialRepository.findByUserId(eq(7L))).thenReturn(Optional.of(credential));
    when(patEncryptionService.decrypt(eq("CIPHER"), eq(2))).thenReturn("github_pat_secret");
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(
        status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
  }

  @Test
  public void adds_new_instances_and_skips_existing_ones() throws Exception {
    when(githubService.listSubdirectories(
            eq("ucsb-cs156/pl-demo"), eq("courseInstances"), eq("github_pat_secret")))
        .thenReturn(List.of("Fall2025", "Winter2026"));
    when(plInstanceRepository.findByPlRepoId(eq(3L)))
        .thenReturn(List.of(PlInstance.builder().id(1L).plRepoId(3L).name("Fall2025").build()));

    job().accept(ctx);

    PlInstance expectedNew = PlInstance.builder().plRepoId(3L).name("Winter2026").build();
    verify(plInstanceRepository).save(eq(expectedNew));

    String expected =
        """
        Syncing course instances for repo ucsb-cs156/pl-demo (PlRepo id 3)
        Added course instance Winter2026
        Done: 1 added, 1 already present""";
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void reports_instances_in_the_database_that_are_no_longer_on_github() throws Exception {
    when(githubService.listSubdirectories(
            eq("ucsb-cs156/pl-demo"), eq("courseInstances"), eq("github_pat_secret")))
        .thenReturn(List.of("Fall2025"));
    when(plInstanceRepository.findByPlRepoId(eq(3L)))
        .thenReturn(
            List.of(
                PlInstance.builder().id(1L).plRepoId(3L).name("Fall2025").build(),
                PlInstance.builder().id(2L).plRepoId(3L).name("Spring2024").build(),
                PlInstance.builder().id(3L).plRepoId(3L).name("Winter2024").build()));

    job().accept(ctx);

    verify(plInstanceRepository, never()).save(any());

    String expected =
        """
        Syncing course instances for repo ucsb-cs156/pl-demo (PlRepo id 3)
        Note: 2 course instance(s) in the database but not on GitHub (left untouched): Spring2024, Winter2024
        Done: 0 added, 1 already present""";
    assertEquals(expected, jobStarted.getLog());
  }

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
  public void completes_with_a_log_message_when_the_repo_has_no_courseInstances_directory()
      throws Exception {
    when(githubService.listSubdirectories(any(), any(), any()))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));

    job().accept(ctx);

    verify(plInstanceRepository, never()).save(any());
    String expected =
        """
        Syncing course instances for repo ucsb-cs156/pl-demo (PlRepo id 3)
        Repo ucsb-cs156/pl-demo has no courseInstances directory (or the token cannot see the repo); nothing to sync""";
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_401() {
    when(githubService.listSubdirectories(any(), any(), any()))
        .thenThrow(httpError(HttpStatus.UNAUTHORIZED));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 401). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void fails_with_a_helpful_message_when_github_returns_403() {
    when(githubService.listSubdirectories(any(), any(), any()))
        .thenThrow(httpError(HttpStatus.FORBIDDEN));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));
    assertEquals(
        "GitHub rejected the stored PAT (HTTP 403). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)",
        thrown.getMessage());
  }

  @Test
  public void other_github_errors_propagate_and_fail_the_job() {
    HttpClientErrorException tooManyRequests = httpError(HttpStatus.TOO_MANY_REQUESTS);
    when(githubService.listSubdirectories(any(), any(), any())).thenThrow(tooManyRequests);

    HttpClientErrorException thrown =
        assertThrows(HttpClientErrorException.class, () -> job().accept(ctx));
    assertEquals(tooManyRequests, thrown);
  }
}
