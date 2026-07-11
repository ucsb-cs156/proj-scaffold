package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets each admin or instructor store one personal access token (PAT) per platform: a GitHub PAT at
 * /api/pat/github and a PrairieLearn PAT at /api/pat/pl. Tokens are encrypted before they are saved
 * and are write-only: no endpoint ever returns one, only metadata (last four characters, expiration
 * date). To replace a lost or expired token, the user simply POSTs a new one. See
 * docs/Github_PAT.md and docs/PrairieLearn_PAT.md for how to create suitable tokens.
 */
@Tag(name = "PatCredential")
@RequestMapping("/api/pat")
@RestController
@Slf4j
public class PatCredentialController extends ApiController {

  // A GitHub classic PAT: "ghp_" followed by letters and digits. Only classic tokens work here:
  // a fine-grained token can only be scoped to repos owned by the user's own account or an org
  // the user is a member of, and this app's users are outside collaborators on repos owned by
  // another org — so a fine-grained token can never reach them. See docs/PAT-design.md.
  private static final Pattern CLASSIC_PAT_PATTERN = Pattern.compile("^ghp_[A-Za-z0-9]{20,244}$");

  @Autowired private PatCredentialRepository patCredentialRepository;

  @Autowired private PatEncryptionService patEncryptionService;

  @Operation(
      summary = "Get metadata about the current user's stored GitHub PAT (never the token itself)")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/github")
  public PatCredential getGithubPatCredential() {
    return getPatCredential(PatPlatform.GITHUB);
  }

  @Operation(
      summary =
          "Get metadata about the current user's stored PrairieLearn PAT (never the token itself)")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/pl")
  public PatCredential getPlPatCredential() {
    return getPatCredential(PatPlatform.PRAIRIELEARN);
  }

  @Operation(summary = "Set (create or replace) the current user's GitHub PAT")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/github")
  public PatCredential postGithubPatCredential(
      @Parameter(name = "token", description = "GitHub classic PAT (starts with ghp_)")
          @RequestParam
          String token,
      @Parameter(
              name = "expiresAt",
              description = "Expiration date of the token in ISO format, e.g. 2026-12-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate expiresAt) {
    String trimmedToken = token.strip();
    validateGithubToken(trimmedToken);
    return savePatCredential(PatPlatform.GITHUB, trimmedToken, expiresAt);
  }

  @Operation(summary = "Set (create or replace) the current user's PrairieLearn PAT")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("/pl")
  public PatCredential postPlPatCredential(
      @Parameter(name = "token", description = "PrairieLearn personal access token") @RequestParam
          String token,
      @Parameter(
              name = "expiresAt",
              description = "Expiration date of the token in ISO format, e.g. 2026-12-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate expiresAt) {
    String trimmedToken = token.strip();
    if (trimmedToken.isBlank()) {
      throw new IllegalArgumentException("token is required");
    }
    return savePatCredential(PatPlatform.PRAIRIELEARN, trimmedToken, expiresAt);
  }

  private PatCredential getPatCredential(PatPlatform platform) {
    long userId = getCurrentUser().getUser().getId();
    return patCredentialRepository
        .findByUserIdAndPlatform(userId, platform)
        .orElseThrow(() -> new EntityNotFoundException(PatCredential.class, userId));
  }

  private PatCredential savePatCredential(
      PatPlatform platform, String trimmedToken, LocalDate expiresAt) {
    PatEncryptionService.EncryptedPat encrypted = patEncryptionService.encrypt(trimmedToken);

    long userId = getCurrentUser().getUser().getId();
    PatCredential credential =
        patCredentialRepository
            .findByUserIdAndPlatform(userId, platform)
            .orElseGet(() -> PatCredential.builder().userId(userId).platform(platform).build());
    credential.setCiphertext(encrypted.ciphertext());
    credential.setKeyVersion(encrypted.keyVersion());
    credential.setLastFour(trimmedToken.substring(trimmedToken.length() - 4));
    credential.setExpiresAt(expiresAt);
    return patCredentialRepository.save(credential);
  }

  private void validateGithubToken(String token) {
    if (token.isBlank()) {
      throw new IllegalArgumentException("token is required");
    }
    if (token.startsWith("github_pat_")) {
      throw new IllegalArgumentException(
          "fine-grained tokens (github_pat_...) cannot reach this app's repositories; create a classic token (ghp_...) instead — see docs/Github_PAT.md");
    }
    if (!CLASSIC_PAT_PATTERN.matcher(token).matches()) {
      throw new IllegalArgumentException(
          "token must be a GitHub classic personal access token (starting with ghp_); see docs/Github_PAT.md");
    }
  }

  /**
   * Thrown by PatEncryptionService when no PAT_ENCRYPTION_KEY is configured — a server-side
   * deployment problem, not a client error, so it maps to 503 rather than 400.
   */
  @ExceptionHandler({IllegalStateException.class})
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Object handleIllegalStateException(Throwable e) {
    return Map.of(
        "type", e.getClass().getSimpleName(),
        "message", e.getMessage());
  }
}
