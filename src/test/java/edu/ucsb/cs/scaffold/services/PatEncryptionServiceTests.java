package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs.scaffold.errors.NoSuchKeyVersionException;
import edu.ucsb.cs.scaffold.services.PatEncryptionService.EncryptedPat;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class PatEncryptionServiceTests {

  private static final String PLAINTEXT = "ghp_FAKEfakeNotARealTokenJustATestValue0";

  /** Deterministic 32-byte key material; a different seed gives a different key. */
  private static String keyMaterial(int seed) {
    byte[] bytes = new byte[32];
    Arrays.fill(bytes, (byte) seed);
    return Base64.getEncoder().encodeToString(bytes);
  }

  private static String versionedKey(int version) {
    return version + ":" + keyMaterial(version);
  }

  // Round trip and basic properties

  @Test
  public void encrypt_then_decrypt_round_trips() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(1), "");

    EncryptedPat encrypted = service.encrypt(PLAINTEXT);

    assertEquals(1, encrypted.keyVersion());
    assertNotEquals(PLAINTEXT, encrypted.ciphertext());
    assertEquals(PLAINTEXT, service.decrypt(encrypted.ciphertext(), encrypted.keyVersion()));
  }

  @Test
  public void encrypting_the_same_plaintext_twice_gives_different_ciphertexts() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(1), "");

    EncryptedPat first = service.encrypt(PLAINTEXT);
    EncryptedPat second = service.encrypt(PLAINTEXT);

    assertNotEquals(first.ciphertext(), second.ciphertext());
    assertEquals(PLAINTEXT, service.decrypt(first.ciphertext(), 1));
    assertEquals(PLAINTEXT, service.decrypt(second.ciphertext(), 1));
  }

  @Test
  public void configured_service_reports_isConfigured_and_current_version() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(3), "");

    assertTrue(service.isConfigured());
    assertEquals(3, service.currentKeyVersion());
  }

  // Previous-key handling (rotation window)

  @Test
  public void decrypts_rows_encrypted_under_the_previous_key_and_encrypts_under_the_current_one() {
    PatEncryptionService oldService = new PatEncryptionService(versionedKey(1), "");
    EncryptedPat encryptedUnderV1 = oldService.encrypt(PLAINTEXT);

    PatEncryptionService rotatingService =
        new PatEncryptionService(versionedKey(2), versionedKey(1));

    assertEquals(2, rotatingService.currentKeyVersion());
    assertEquals(PLAINTEXT, rotatingService.decrypt(encryptedUnderV1.ciphertext(), 1));

    EncryptedPat encryptedUnderV2 = rotatingService.encrypt(PLAINTEXT);
    assertEquals(2, encryptedUnderV2.keyVersion());
    assertEquals(PLAINTEXT, rotatingService.decrypt(encryptedUnderV2.ciphertext(), 2));
  }

  @Test
  public void decrypt_with_an_unknown_key_version_throws_NoSuchKeyVersionException() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(1), "");
    EncryptedPat encrypted = service.encrypt(PLAINTEXT);

    NoSuchKeyVersionException thrown =
        assertThrows(
            NoSuchKeyVersionException.class, () -> service.decrypt(encrypted.ciphertext(), 99));
    assertEquals(
        "No PAT encryption key is configured for key version 99; the credential cannot be decrypted until that key is restored or the user re-enters their PAT",
        thrown.getMessage());
  }

  @Test
  public void tampered_ciphertext_fails_to_decrypt() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(1), "");
    EncryptedPat encrypted = service.encrypt(PLAINTEXT);

    byte[] bytes = Base64.getDecoder().decode(encrypted.ciphertext());
    bytes[bytes.length - 1] ^= 1; // flip one bit of the GCM tag
    String tampered = Base64.getEncoder().encodeToString(bytes);

    assertThrows(IllegalStateException.class, () -> service.decrypt(tampered, 1));
  }

  @Test
  public void decrypting_with_the_wrong_key_version_fails_even_if_the_version_exists() {
    PatEncryptionService service = new PatEncryptionService(versionedKey(2), versionedKey(1));
    EncryptedPat encrypted = service.encrypt(PLAINTEXT); // encrypted under v2

    assertThrows(IllegalStateException.class, () -> service.decrypt(encrypted.ciphertext(), 1));
  }

  // Unconfigured service (no PAT_ENCRYPTION_KEY set)

  @Test
  public void unconfigured_service_boots_but_refuses_to_encrypt() {
    PatEncryptionService service = new PatEncryptionService("", "");

    assertFalse(service.isConfigured());

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> service.encrypt(PLAINTEXT));
    assertEquals(
        "PAT encryption is not configured on this server; set PAT_ENCRYPTION_KEY (see docs/PAT_ENCRYPTION_KEY_instructions.md)",
        thrown.getMessage());

    assertThrows(IllegalStateException.class, () -> service.currentKeyVersion());
    assertThrows(NoSuchKeyVersionException.class, () -> service.decrypt("abcd", 1));
  }

  @Test
  public void previous_key_without_a_current_key_is_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new PatEncryptionService("", versionedKey(1)));
    assertEquals(
        "PREVIOUS_PAT_ENCRYPTION_KEY is set but PAT_ENCRYPTION_KEY is not", thrown.getMessage());
  }

  // Malformed key values fail fast at construction

  @Test
  public void current_and_previous_keys_with_the_same_version_are_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService(versionedKey(2), "2:" + keyMaterial(7)));
    assertEquals(
        "PAT_ENCRYPTION_KEY and PREVIOUS_PAT_ENCRYPTION_KEY have the same key version (2)",
        thrown.getMessage());
  }

  @Test
  public void key_without_a_version_prefix_is_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new PatEncryptionService(keyMaterial(1), ""));
    assertEquals(
        "PAT_ENCRYPTION_KEY must have the form <version>:<base64-key>; see docs/PAT_ENCRYPTION_KEY_instructions.md",
        thrown.getMessage());
  }

  @Test
  public void key_starting_with_a_colon_is_rejected_as_a_bad_version_prefix() {
    // colon at index 0: the form check passes but the (empty) version prefix does not parse
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService(":" + keyMaterial(1), ""));
    assertEquals("PAT_ENCRYPTION_KEY version prefix must be an integer", thrown.getMessage());
  }

  @Test
  public void key_with_a_non_numeric_version_prefix_is_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService("two:" + keyMaterial(1), ""));
    assertEquals("PAT_ENCRYPTION_KEY version prefix must be an integer", thrown.getMessage());
  }

  @Test
  public void key_with_version_zero_is_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService("0:" + keyMaterial(1), ""));
    assertEquals("PAT_ENCRYPTION_KEY version must be >= 1, got 0", thrown.getMessage());
  }

  @Test
  public void key_with_invalid_base64_material_is_rejected() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new PatEncryptionService("1:!!!not-base64", ""));
    assertEquals("PAT_ENCRYPTION_KEY key material is not valid base64", thrown.getMessage());
  }

  @Test
  public void key_material_that_is_not_32_bytes_is_rejected() {
    String sixteenBytes = Base64.getEncoder().encodeToString(new byte[16]);
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService("1:" + sixteenBytes, ""));
    assertEquals(
        "PAT_ENCRYPTION_KEY key material must be exactly 32 bytes (256 bits) after base64 decoding, got 16",
        thrown.getMessage());
  }

  @Test
  public void malformed_previous_key_is_rejected_with_its_own_name_in_the_message() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PatEncryptionService(versionedKey(2), "not-a-versioned-key"));
    assertEquals(
        "PREVIOUS_PAT_ENCRYPTION_KEY must have the form <version>:<base64-key>; see docs/PAT_ENCRYPTION_KEY_instructions.md",
        thrown.getMessage());
  }
}
