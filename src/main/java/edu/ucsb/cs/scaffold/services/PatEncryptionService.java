package edu.ucsb.cs.scaffold.services;

import edu.ucsb.cs.scaffold.errors.NoSuchKeyVersionException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

/**
 * Encrypts and decrypts users' GitHub personal access tokens (PATs) with AES-256-GCM so the
 * database only ever sees ciphertext.
 *
 * <p>Keys are supplied via environment variables in the form {@code <version>:<base64 of 32
 * bytes>}; see docs/PAT_ENCRYPTION_KEY_instructions.md. {@code PAT_ENCRYPTION_KEY} is the current
 * key, used for all encryption. During a key rotation, {@code PREVIOUS_PAT_ENCRYPTION_KEY}
 * additionally holds the key being retired, so credentials encrypted under it can still be
 * decrypted until the rotate-keys job re-encrypts them under the current key.
 *
 * <p>When neither variable is set the service is "not configured": the app still boots (so
 * deployments that don't use the PAT feature are unaffected), but encryption requests fail with
 * {@link IllegalStateException}. A malformed key value, by contrast, fails at startup — a typo in
 * key configuration should be loud, not silently treated as "feature off".
 */
@Service
public class PatEncryptionService {

  /** Result of encrypting a PAT: the base64 ciphertext and the key version that produced it. */
  public record EncryptedPat(String ciphertext, int keyVersion) {}

  private record VersionedKey(int version, SecretKey key) {}

  // 0 = no key configured; real versions are always >= 1
  private final int currentKeyVersion;

  private final Map<Integer, BytesEncryptor> encryptorsByVersion = new HashMap<>();

  public PatEncryptionService(
      @Value("${app.pat.encryption.key:}") String currentKey,
      @Value("${app.pat.encryption.previous-key:}") String previousKey) {
    if (currentKey.isBlank()) {
      if (!previousKey.isBlank()) {
        throw new IllegalArgumentException(
            "PREVIOUS_PAT_ENCRYPTION_KEY is set but PAT_ENCRYPTION_KEY is not");
      }
      this.currentKeyVersion = 0;
      return;
    }
    VersionedKey current = parseVersionedKey(currentKey, "PAT_ENCRYPTION_KEY");
    this.currentKeyVersion = current.version();
    encryptorsByVersion.put(current.version(), encryptorFor(current.key()));
    if (!previousKey.isBlank()) {
      VersionedKey previous = parseVersionedKey(previousKey, "PREVIOUS_PAT_ENCRYPTION_KEY");
      if (previous.version() == current.version()) {
        throw new IllegalArgumentException(
            "PAT_ENCRYPTION_KEY and PREVIOUS_PAT_ENCRYPTION_KEY have the same key version (%d)"
                .formatted(current.version()));
      }
      encryptorsByVersion.put(previous.version(), encryptorFor(previous.key()));
    }
  }

  /** Whether a current encryption key is configured. */
  public boolean isConfigured() {
    return currentKeyVersion != 0;
  }

  /** The key version new encryptions are performed under (the rotate-keys job's target). */
  public int currentKeyVersion() {
    requireConfigured();
    return currentKeyVersion;
  }

  /** Encrypts a PAT under the current key. */
  public EncryptedPat encrypt(String plaintext) {
    requireConfigured();
    byte[] ciphertext =
        encryptorsByVersion
            .get(currentKeyVersion)
            .encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    return new EncryptedPat(Base64.getEncoder().encodeToString(ciphertext), currentKeyVersion);
  }

  /**
   * Decrypts a stored PAT using the key that encrypted it, identified by the credential's stored
   * key version.
   *
   * @throws NoSuchKeyVersionException if no key is configured for that version
   */
  public String decrypt(String ciphertext, int keyVersion) {
    BytesEncryptor encryptor = encryptorsByVersion.get(keyVersion);
    if (encryptor == null) {
      throw new NoSuchKeyVersionException(keyVersion);
    }
    byte[] plaintext = encryptor.decrypt(Base64.getDecoder().decode(ciphertext));
    return new String(plaintext, StandardCharsets.UTF_8);
  }

  private void requireConfigured() {
    if (currentKeyVersion == 0) {
      throw new IllegalStateException(
          "PAT encryption is not configured on this server; set PAT_ENCRYPTION_KEY (see docs/PAT_ENCRYPTION_KEY_instructions.md)");
    }
  }

  static VersionedKey parseVersionedKey(String value, String name) {
    int colon = value.indexOf(':');
    if (colon < 0) {
      throw new IllegalArgumentException(
          name
              + " must have the form <version>:<base64-key>; see docs/PAT_ENCRYPTION_KEY_instructions.md");
    }
    int version;
    try {
      version = Integer.parseInt(value.substring(0, colon));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " version prefix must be an integer");
    }
    if (version < 1) {
      throw new IllegalArgumentException(name + " version must be >= 1, got " + version);
    }
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(value.substring(colon + 1));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(name + " key material is not valid base64");
    }
    if (keyBytes.length != 32) {
      throw new IllegalArgumentException(
          name
              + " key material must be exactly 32 bytes (256 bits) after base64 decoding, got "
              + keyBytes.length);
    }
    return new VersionedKey(version, new SecretKeySpec(keyBytes, "AES"));
  }

  private static BytesEncryptor encryptorFor(SecretKey key) {
    return new AesBytesEncryptor(key, KeyGenerators.secureRandom(16), CipherAlgorithm.GCM);
  }
}
