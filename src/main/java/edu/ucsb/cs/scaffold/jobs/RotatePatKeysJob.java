package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.errors.NoSuchKeyVersionException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import edu.ucsb.cs.scaffold.services.jobs.JobContextConsumer;
import lombok.Builder;

/**
 * Re-encrypts every stored PAT under the current PAT_ENCRYPTION_KEY. Launched by an admin (POST
 * /api/jobs/launch/rotatePatKeys) after the key rotation script has moved the retiring key to
 * PREVIOUS_PAT_ENCRYPTION_KEY and installed a new PAT_ENCRYPTION_KEY; see
 * docs/PAT_ENCRYPTION_KEY_instructions.md.
 *
 * <p>Credentials already on the current key version are skipped, so the job is idempotent and can
 * simply be re-launched after a partial failure. A credential whose key version has no configured
 * key (e.g. PREVIOUS_PAT_ENCRYPTION_KEY was removed too early) is logged and skipped rather than
 * failing the whole job; that user must re-enter their PAT.
 */
@Builder
public class RotatePatKeysJob implements JobContextConsumer {

  private PatEncryptionService patEncryptionService;
  private PatCredentialRepository patCredentialRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    int currentVersion = patEncryptionService.currentKeyVersion();
    ctx.log("Rotating stored PATs to key version %d".formatted(currentVersion));
    int rotated = 0;
    int alreadyCurrent = 0;
    int undecryptable = 0;
    for (PatCredential credential : patCredentialRepository.findAll()) {
      int oldVersion = credential.getKeyVersion();
      if (oldVersion == currentVersion) {
        alreadyCurrent++;
        continue;
      }
      try {
        String plaintext = patEncryptionService.decrypt(credential.getCiphertext(), oldVersion);
        PatEncryptionService.EncryptedPat encrypted = patEncryptionService.encrypt(plaintext);
        credential.setCiphertext(encrypted.ciphertext());
        credential.setKeyVersion(encrypted.keyVersion());
        patCredentialRepository.save(credential);
        rotated++;
        ctx.log(
            "Rotated credential id %d from key version %d to %d"
                .formatted(credential.getId(), oldVersion, currentVersion));
      } catch (NoSuchKeyVersionException e) {
        undecryptable++;
        ctx.log("Cannot rotate credential id %d: %s".formatted(credential.getId(), e.getMessage()));
      }
    }
    ctx.log(
        "Done: %d rotated, %d already on the current key version, %d could not be decrypted"
            .formatted(rotated, alreadyCurrent, undecryptable));
  }
}
