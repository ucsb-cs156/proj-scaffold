package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlColor;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlColorRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class ReadPLColorsJobTests {

  static final long USER_ID = 42L;

  PatCredentialRepository patCredentialRepository = mock(PatCredentialRepository.class);
  PatEncryptionService patEncryptionService = mock(PatEncryptionService.class);
  GithubService githubService = mock(GithubService.class);
  PlColorRepository plColorRepository = mock(PlColorRepository.class);

  private ReadPLColorsJob job() {
    return ReadPLColorsJob.builder()
        .userId(USER_ID)
        .patCredentialRepository(patCredentialRepository)
        .patEncryptionService(patEncryptionService)
        .githubService(githubService)
        .plColorRepository(plColorRepository)
        .build();
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(
        status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
  }

  private void stubGithubCredential() {
    PatCredential credential =
        PatCredential.builder().id(1L).userId(USER_ID).ciphertext("CIPHER").keyVersion(1).build();
    when(patCredentialRepository.findByUserIdAndPlatform(USER_ID, PatPlatform.GITHUB))
        .thenReturn(Optional.of(credential));
    when(patEncryptionService.decrypt(eq("CIPHER"), eq(1))).thenReturn("ghp_plaintext");
  }

  @Test
  public void fails_when_no_github_pat_is_configured() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    when(patCredentialRepository.findByUserIdAndPlatform(USER_ID, PatPlatform.GITHUB))
        .thenReturn(Optional.empty());

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));

    assertEquals(
        "No GitHub PAT is set up for this user; set one up on the /profile page before running this"
            + " job",
        thrown.getMessage());
    verify(plColorRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void fails_when_the_colors_scss_file_cannot_be_read() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    stubGithubCredential();
    when(githubService.getFileContent(
            eq("PrairieLearn/PrairieLearn"),
            eq("apps/prairielearn/public/stylesheets/colors.scss"),
            eq("ghp_plaintext")))
        .thenThrow(httpError(HttpStatus.NOT_FOUND));

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));

    assertEquals(
        "Could not read apps/prairielearn/public/stylesheets/colors.scss from"
            + " PrairieLearn/PrairieLearn (HTTP 404); the file may not exist or may not be"
            + " accessible",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_file_does_not_contain_the_color_mapping() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    stubGithubCredential();
    when(githubService.getFileContent(
            eq("PrairieLearn/PrairieLearn"),
            eq("apps/prairielearn/public/stylesheets/colors.scss"),
            eq("ghp_plaintext")))
        .thenReturn("$other-map: (\n  'red1': #ffccbc,\n);\n");

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));

    assertEquals(
        "apps/prairielearn/public/stylesheets/colors.scss does not contain the expected"
            + " $custom-colors color mapping",
        thrown.getMessage());
    verify(plColorRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void fails_when_the_file_content_is_blank() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    stubGithubCredential();
    when(githubService.getFileContent(
            eq("PrairieLearn/PrairieLearn"),
            eq("apps/prairielearn/public/stylesheets/colors.scss"),
            eq("ghp_plaintext")))
        .thenReturn("");

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));

    assertEquals(
        "apps/prairielearn/public/stylesheets/colors.scss in PrairieLearn/PrairieLearn is empty"
            + " or could not be read",
        thrown.getMessage());
  }

  @Test
  public void fails_when_the_color_map_is_found_but_has_no_entries() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    stubGithubCredential();
    when(githubService.getFileContent(
            eq("PrairieLearn/PrairieLearn"),
            eq("apps/prairielearn/public/stylesheets/colors.scss"),
            eq("ghp_plaintext")))
        .thenReturn("$custom-colors: (\n);\n");

    Exception thrown = assertThrows(Exception.class, () -> job().accept(ctx));

    assertEquals(
        "apps/prairielearn/public/stylesheets/colors.scss did not contain any 'name': #hex color"
            + " entries",
        thrown.getMessage());
    verify(plColorRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void adds_new_colors_and_updates_changed_hex_codes_and_leaves_unchanged_ones_alone()
      throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);
    stubGithubCredential();
    String scss =
        """
        $custom-colors: (
          'red1': #ffccbc,
          'red2': #ff0000,
          'blue1': #39d5ff,
        );
        """;
    when(githubService.getFileContent(
            eq("PrairieLearn/PrairieLearn"),
            eq("apps/prairielearn/public/stylesheets/colors.scss"),
            eq("ghp_plaintext")))
        .thenReturn(scss);

    // red1 already exists with the same hex code (unchanged)
    when(plColorRepository.findById("red1"))
        .thenReturn(Optional.of(PlColor.builder().colorName("red1").hexCode("#ffccbc").build()));
    // red2 already exists but with a different (stale) hex code (updated)
    when(plColorRepository.findById("red2"))
        .thenReturn(Optional.of(PlColor.builder().colorName("red2").hexCode("#c72c1c").build()));
    // blue1 does not exist yet (added)
    when(plColorRepository.findById("blue1")).thenReturn(Optional.empty());

    job().accept(ctx);

    verify(plColorRepository, never())
        .save(PlColor.builder().colorName("red1").hexCode("#ffccbc").build());
    verify(plColorRepository, times(1))
        .save(PlColor.builder().colorName("red2").hexCode("#ff0000").build());
    verify(plColorRepository, times(1))
        .save(PlColor.builder().colorName("blue1").hexCode("#39d5ff").build());

    String log = jobStarted.getLog();
    org.junit.jupiter.api.Assertions.assertTrue(log.contains("Added color blue1: #39d5ff"));
    org.junit.jupiter.api.Assertions.assertTrue(
        log.contains("Updated color red2: #c72c1c -> #ff0000"));
    org.junit.jupiter.api.Assertions.assertTrue(
        log.contains("Done: 1 added, 1 updated, 1 unchanged"));
  }
}
