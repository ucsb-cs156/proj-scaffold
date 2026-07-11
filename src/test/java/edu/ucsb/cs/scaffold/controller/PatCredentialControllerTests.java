package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService.EncryptedPat;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = PatCredentialController.class)
public class PatCredentialControllerTests extends ControllerTestCase {

  @MockitoBean PatCredentialRepository patCredentialRepository;

  @MockitoBean PatEncryptionService patEncryptionService;

  // A syntactically valid classic PAT shape (not a real token).
  private static final String VALID_TOKEN = "ghp_FAKEfake0123456789FAKEfake0123456789";

  // A PrairieLearn token is an arbitrary opaque string (PL generates UUIDs).
  private static final String VALID_PL_TOKEN = "b970695b-4f08-471f-b3d2-d7d12933f395";

  // MockCurrentUserServiceImpl returns a user with id 1
  private static final long USER_ID = 1L;

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get() throws Exception {
    mockMvc.perform(get("/api/pat/github")).andExpect(status().is(403));
    mockMvc.perform(get("/api/pat/pl")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get() throws Exception {
    mockMvc.perform(get("/api/pat/github")).andExpect(status().is(403));
    mockMvc.perform(get("/api/pat/pl")).andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/pat/github?token=" + VALID_TOKEN)).andExpect(status().is(403));
    mockMvc.perform(post("/api/pat/pl?token=" + VALID_PL_TOKEN)).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(post("/api/pat/github?token=" + VALID_TOKEN).with(csrf()))
        .andExpect(status().is(403));
    mockMvc
        .perform(post("/api/pat/pl?token=" + VALID_PL_TOKEN).with(csrf()))
        .andExpect(status().is(403));
  }

  // GET functionality

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_own_github_credential_metadata_but_never_the_ciphertext()
      throws Exception {
    PatCredential credential =
        PatCredential.builder()
            .id(7L)
            .userId(USER_ID)
            .platform(PatPlatform.GITHUB)
            .ciphertext("SUPERSECRETCIPHERTEXT")
            .keyVersion(2)
            .lastFour("3f2a")
            .expiresAt(LocalDate.of(2026, 12, 31))
            .build();
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.of(credential));

    MvcResult response =
        mockMvc.perform(get("/api/pat/github")).andExpect(status().isOk()).andReturn();

    verify(patCredentialRepository, times(1))
        .findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB));
    String body = response.getResponse().getContentAsString();
    Map<String, Object> json = responseToJson(response);
    assertEquals(7, json.get("id"));
    assertEquals(1, json.get("userId"));
    assertEquals("GITHUB", json.get("platform"));
    assertEquals("3f2a", json.get("lastFour"));
    assertEquals("2026-12-31", json.get("expiresAt"));
    // The token ciphertext and key version are @JsonIgnore'd and must never appear.
    assertFalse(body.contains("SUPERSECRETCIPHERTEXT"));
    assertFalse(body.contains("ciphertext"));
    assertFalse(body.contains("keyVersion"));
  }

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_own_pl_credential_metadata_but_never_the_ciphertext()
      throws Exception {
    PatCredential credential =
        PatCredential.builder()
            .id(8L)
            .userId(USER_ID)
            .platform(PatPlatform.PRAIRIELEARN)
            .ciphertext("SUPERSECRETCIPHERTEXT")
            .keyVersion(2)
            .lastFour("3395")
            .build();
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.of(credential));

    MvcResult response = mockMvc.perform(get("/api/pat/pl")).andExpect(status().isOk()).andReturn();

    verify(patCredentialRepository, times(1))
        .findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN));
    String body = response.getResponse().getContentAsString();
    Map<String, Object> json = responseToJson(response);
    assertEquals(8, json.get("id"));
    assertEquals("PRAIRIELEARN", json.get("platform"));
    assertEquals("3395", json.get("lastFour"));
    assertNull(json.get("expiresAt"));
    assertFalse(body.contains("SUPERSECRETCIPHERTEXT"));
    assertFalse(body.contains("ciphertext"));
    assertFalse(body.contains("keyVersion"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void get_github_returns_404_when_user_has_no_credential() throws Exception {
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/pat/github")).andExpect(status().isNotFound()).andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("PatCredential with id 1 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void get_pl_returns_404_when_user_has_no_credential() throws Exception {
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/pat/pl")).andExpect(status().isNotFound()).andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("PatCredential with id 1 not found", json.get("message"));
  }

  // POST functionality — GitHub

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_post_a_new_github_credential() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 3));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat/github").param("token", VALID_TOKEN).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(USER_ID, saved.getUserId());
    assertEquals(PatPlatform.GITHUB, saved.getPlatform());
    assertEquals("ENCRYPTED", saved.getCiphertext());
    assertEquals(3, saved.getKeyVersion());
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), saved.getLastFour());
    assertNull(saved.getExpiresAt());

    String body = response.getResponse().getContentAsString();
    assertFalse(body.contains("ENCRYPTED"));
    Map<String, Object> json = responseToJson(response);
    assertEquals(1, json.get("userId"));
    assertEquals("GITHUB", json.get("platform"));
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), json.get("lastFour"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_records_the_expiration_date_when_given() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/pat/github")
                .param("token", VALID_TOKEN)
                .param("expiresAt", "2026-12-31")
                .with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    assertEquals(LocalDate.of(2026, 12, 31), captor.getValue().getExpiresAt());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_replaces_the_existing_credential_for_the_user() throws Exception {
    PatCredential existing =
        PatCredential.builder()
            .id(7L)
            .userId(USER_ID)
            .platform(PatPlatform.GITHUB)
            .ciphertext("OLDCIPHERTEXT")
            .keyVersion(1)
            .lastFour("aaaa")
            .expiresAt(LocalDate.of(2025, 1, 1))
            .build();
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("NEWCIPHERTEXT", 2));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.of(existing));
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat/github").param("token", VALID_TOKEN).with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(Long.valueOf(7L), saved.getId()); // same row, replaced in place
    assertEquals(PatPlatform.GITHUB, saved.getPlatform());
    assertEquals("NEWCIPHERTEXT", saved.getCiphertext());
    assertEquals(2, saved.getKeyVersion());
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), saved.getLastFour());
    assertNull(saved.getExpiresAt()); // stale expiry from the old token is cleared
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_strips_surrounding_whitespace_from_the_token() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.GITHUB)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat/github").param("token", "  " + VALID_TOKEN + "\n").with(csrf()))
        .andExpect(status().isOk());

    verify(patEncryptionService, times(1)).encrypt(eq(VALID_TOKEN));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_rejects_a_blank_token() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/pat/github").param("token", "   ").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("token is required", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_rejects_a_fine_grained_token_with_an_explanation() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/pat/github")
                    .param(
                        "token", "github_pat_11FAKEFAKE0123456789_abcdefghijklmnopqrstuvwxyzABCDEF")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "fine-grained tokens (github_pat_...) cannot reach this app's repositories; create a classic token (ghp_...) instead — see docs/Github_PAT.md",
        json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_rejects_a_token_with_an_unrecognized_prefix() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/pat/github")
                    .param("token", "gho_FAKEfake0123456789FAKEfake0123456789")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "token must be a GitHub classic personal access token (starting with ghp_); see docs/Github_PAT.md",
        json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_rejects_a_token_that_is_too_short() throws Exception {
    mockMvc
        .perform(post("/api/pat/github").param("token", "ghp_tooShort").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_github_returns_503_when_encryption_is_not_configured_on_the_server()
      throws Exception {
    when(patEncryptionService.encrypt(any()))
        .thenThrow(new IllegalStateException("PAT encryption is not configured on this server"));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat/github").param("token", VALID_TOKEN).with(csrf()))
            .andExpect(status().isServiceUnavailable())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("IllegalStateException", json.get("type"));
    assertEquals("PAT encryption is not configured on this server", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  // POST functionality — PrairieLearn

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_post_a_new_pl_credential() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_PL_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 3));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat/pl").param("token", VALID_PL_TOKEN).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(USER_ID, saved.getUserId());
    assertEquals(PatPlatform.PRAIRIELEARN, saved.getPlatform());
    assertEquals("ENCRYPTED", saved.getCiphertext());
    assertEquals(3, saved.getKeyVersion());
    assertEquals(VALID_PL_TOKEN.substring(VALID_PL_TOKEN.length() - 4), saved.getLastFour());
    assertNull(saved.getExpiresAt());

    String body = response.getResponse().getContentAsString();
    assertFalse(body.contains("ENCRYPTED"));
    Map<String, Object> json = responseToJson(response);
    assertEquals(1, json.get("userId"));
    assertEquals("PRAIRIELEARN", json.get("platform"));
    assertEquals(VALID_PL_TOKEN.substring(VALID_PL_TOKEN.length() - 4), json.get("lastFour"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_pl_records_the_expiration_date_when_given() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_PL_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/pat/pl")
                .param("token", VALID_PL_TOKEN)
                .param("expiresAt", "2026-12-31")
                .with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    assertEquals(LocalDate.of(2026, 12, 31), captor.getValue().getExpiresAt());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_pl_replaces_the_existing_credential_for_the_user() throws Exception {
    PatCredential existing =
        PatCredential.builder()
            .id(9L)
            .userId(USER_ID)
            .platform(PatPlatform.PRAIRIELEARN)
            .ciphertext("OLDCIPHERTEXT")
            .keyVersion(1)
            .lastFour("aaaa")
            .build();
    when(patEncryptionService.encrypt(eq(VALID_PL_TOKEN)))
        .thenReturn(new EncryptedPat("NEWCIPHERTEXT", 2));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.of(existing));
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat/pl").param("token", VALID_PL_TOKEN).with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(Long.valueOf(9L), saved.getId()); // same row, replaced in place
    assertEquals(PatPlatform.PRAIRIELEARN, saved.getPlatform());
    assertEquals("NEWCIPHERTEXT", saved.getCiphertext());
    assertEquals(2, saved.getKeyVersion());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_pl_strips_surrounding_whitespace_from_the_token() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_PL_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserIdAndPlatform(eq(USER_ID), eq(PatPlatform.PRAIRIELEARN)))
        .thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat/pl").param("token", "  " + VALID_PL_TOKEN + "\n").with(csrf()))
        .andExpect(status().isOk());

    verify(patEncryptionService, times(1)).encrypt(eq(VALID_PL_TOKEN));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_pl_rejects_a_blank_token() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/pat/pl").param("token", "   ").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("token is required", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_pl_returns_503_when_encryption_is_not_configured_on_the_server()
      throws Exception {
    when(patEncryptionService.encrypt(any()))
        .thenThrow(new IllegalStateException("PAT encryption is not configured on this server"));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat/pl").param("token", VALID_PL_TOKEN).with(csrf()))
            .andExpect(status().isServiceUnavailable())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("IllegalStateException", json.get("type"));
    assertEquals("PAT encryption is not configured on this server", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }
}
