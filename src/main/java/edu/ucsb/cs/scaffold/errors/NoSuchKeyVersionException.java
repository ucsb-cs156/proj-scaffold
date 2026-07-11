package edu.ucsb.cs.scaffold.errors;

/**
 * Thrown when a stored PAT was encrypted under a key version for which no key material is currently
 * configured (e.g. PREVIOUS_PAT_ENCRYPTION_KEY was removed before the rotate-keys job re-encrypted
 * every credential). The affected credential cannot be decrypted; the user must re-enter their PAT
 * unless the missing key is restored.
 */
public class NoSuchKeyVersionException extends RuntimeException {
  public NoSuchKeyVersionException(int keyVersion) {
    super(
        "No PAT encryption key is configured for key version %d; the credential cannot be decrypted until that key is restored or the user re-enters their PAT"
            .formatted(keyVersion));
  }
}
