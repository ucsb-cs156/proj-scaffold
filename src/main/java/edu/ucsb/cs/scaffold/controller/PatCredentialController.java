package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PatCredential;
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
 * Lets each admin or instructor store one GitHub fine-grained personal access token (PAT). The
 * token is encrypted before it is saved and is write-only: no endpoint ever returns it, only
 * metadata (last four characters, expiration date). To replace a lost or expired token, the user
 * simply POSTs a new one. See docs/PAT.md for how to create a suitable token.
 */
@Tag(name = "PatCredential")
@RequestMapping("/api/pat")
@RestController
@Slf4j
public class PatCredentialController extends ApiController {

  // A GitHub fine-grained PAT: "github_pat_" followed by letters, digits, and underscores
  // (currently 82 of them; we allow some slack). Classic tokens (ghp_...) are deliberately
  // rejected because they can't be scoped to specific repositories; see docs/PAT.md.
  private static final Pattern FINE_GRAINED_PAT_PATTERN =
      Pattern.compile("^github_pat_[A-Za-z0-9_]{30,244}$");

  @Autowired private PatCredentialRepository patCredentialRepository;

  @Autowired private PatEncryptionService patEncryptionService;

  @Operation(summary = "Get metadata about the current user's stored PAT (never the token itself)")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public PatCredential getPatCredential() {
    long userId = getCurrentUser().getUser().getId();
    return patCredentialRepository
        .findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException(PatCredential.class, userId));
  }

  @Operation(summary = "Set (create or replace) the current user's GitHub fine-grained PAT")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @PostMapping("")
  public PatCredential postPatCredential(
      @Parameter(name = "token", description = "GitHub fine-grained PAT (starts with github_pat_)")
          @RequestParam
          String token,
      @Parameter(
              name = "expiresAt",
              description = "Expiration date of the token in ISO format, e.g. 2026-12-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate expiresAt) {
    String trimmedToken = token.strip();
    validateToken(trimmedToken);
    PatEncryptionService.EncryptedPat encrypted = patEncryptionService.encrypt(trimmedToken);

    long userId = getCurrentUser().getUser().getId();
    PatCredential credential =
        patCredentialRepository
            .findByUserId(userId)
            .orElseGet(() -> PatCredential.builder().userId(userId).build());
    credential.setCiphertext(encrypted.ciphertext());
    credential.setKeyVersion(encrypted.keyVersion());
    credential.setLastFour(trimmedToken.substring(trimmedToken.length() - 4));
    credential.setExpiresAt(expiresAt);
    return patCredentialRepository.save(credential);
  }

  private void validateToken(String token) {
    if (token.isBlank()) {
      throw new IllegalArgumentException("token is required");
    }
    if (!FINE_GRAINED_PAT_PATTERN.matcher(token).matches()) {
      throw new IllegalArgumentException(
          "token must be a GitHub fine-grained personal access token (starting with github_pat_); see docs/PAT.md");
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
