package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.errors.NoSuchKeyVersionException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService.EncryptedPat;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RotatePatKeysJobTests {

  PatEncryptionService patEncryptionService = mock(PatEncryptionService.class);
  PatCredentialRepository patCredentialRepository = mock(PatCredentialRepository.class);

  private RotatePatKeysJob job() {
    return RotatePatKeysJob.builder()
        .patEncryptionService(patEncryptionService)
        .patCredentialRepository(patCredentialRepository)
        .build();
  }

  @Test
  public void rotates_old_rows_and_skips_rows_already_on_the_current_version() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    PatCredential oldRow =
        PatCredential.builder().id(1L).userId(10L).ciphertext("OLD").keyVersion(1).build();
    PatCredential currentRow =
        PatCredential.builder().id(2L).userId(20L).ciphertext("CURRENT").keyVersion(2).build();
    when(patCredentialRepository.findAll()).thenReturn(List.of(oldRow, currentRow));
    when(patEncryptionService.currentKeyVersion()).thenReturn(2);
    when(patEncryptionService.decrypt(eq("OLD"), eq(1))).thenReturn("github_pat_plaintext");
    when(patEncryptionService.encrypt(eq("github_pat_plaintext")))
        .thenReturn(new EncryptedPat("NEW", 2));

    job().accept(ctx);

    verify(patCredentialRepository, times(1)).save(eq(oldRow));
    assertEquals("NEW", oldRow.getCiphertext());
    assertEquals(2, oldRow.getKeyVersion());
    assertEquals("CURRENT", currentRow.getCiphertext());

    String expected =
        """
        Rotating stored PATs to key version 2
        Rotated credential id 1 from key version 1 to 2
        Done: 1 rotated, 1 already on the current key version, 0 could not be decrypted""";
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void logs_and_skips_credentials_whose_key_version_is_no_longer_available()
      throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    PatCredential orphanRow =
        PatCredential.builder().id(5L).userId(10L).ciphertext("ORPHAN").keyVersion(1).build();
    when(patCredentialRepository.findAll()).thenReturn(List.of(orphanRow));
    when(patEncryptionService.currentKeyVersion()).thenReturn(3);
    when(patEncryptionService.decrypt(eq("ORPHAN"), eq(1)))
        .thenThrow(new NoSuchKeyVersionException(1));

    job().accept(ctx);

    verify(patCredentialRepository, never()).save(any());
    assertEquals(1, orphanRow.getKeyVersion()); // left untouched

    String expected =
        """
        Rotating stored PATs to key version 3
        Cannot rotate credential id 5: No PAT encryption key is configured for key version 1; the credential cannot be decrypted until that key is restored or the user re-enters their PAT
        Done: 0 rotated, 0 already on the current key version, 1 could not be decrypted""";
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  public void fails_when_encryption_is_not_configured() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    when(patEncryptionService.currentKeyVersion())
        .thenThrow(new IllegalStateException("PAT encryption is not configured on this server"));

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> job().accept(ctx));
    assertEquals("PAT encryption is not configured on this server", thrown.getMessage());
    verify(patCredentialRepository, never()).save(any());
  }
}
