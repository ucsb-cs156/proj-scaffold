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

  // MockCurrentUserServiceImpl returns a user with id 1
  private static final long USER_ID = 1L;

  // Authorization tests

  @Test
  public void logged_out_users_cannot_get() throws Exception {
    mockMvc.perform(get("/api/pat")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_get() throws Exception {
    mockMvc.perform(get("/api/pat")).andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/pat?token=" + VALID_TOKEN)).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/pat?token=" + VALID_TOKEN).with(csrf())).andExpect(status().is(403));
  }

  // GET functionality

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_get_own_credential_metadata_but_never_the_ciphertext()
      throws Exception {
    PatCredential credential =
        PatCredential.builder()
            .id(7L)
            .userId(USER_ID)
            .ciphertext("SUPERSECRETCIPHERTEXT")
            .keyVersion(2)
            .lastFour("3f2a")
            .expiresAt(LocalDate.of(2026, 12, 31))
            .build();
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.of(credential));

    MvcResult response = mockMvc.perform(get("/api/pat")).andExpect(status().isOk()).andReturn();

    verify(patCredentialRepository, times(1)).findByUserId(eq(USER_ID));
    String body = response.getResponse().getContentAsString();
    Map<String, Object> json = responseToJson(response);
    assertEquals(7, json.get("id"));
    assertEquals(1, json.get("userId"));
    assertEquals("3f2a", json.get("lastFour"));
    assertEquals("2026-12-31", json.get("expiresAt"));
    // The token ciphertext and key version are @JsonIgnore'd and must never appear.
    assertFalse(body.contains("SUPERSECRETCIPHERTEXT"));
    assertFalse(body.contains("ciphertext"));
    assertFalse(body.contains("keyVersion"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void get_returns_404_when_user_has_no_credential() throws Exception {
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc.perform(get("/api/pat")).andExpect(status().isNotFound()).andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("PatCredential with id 1 not found", json.get("message"));
  }

  // POST functionality

  @WithMockUser(roles = {"INSTRUCTOR"})
  @Test
  public void instructor_can_post_a_new_credential() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 3));
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat").param("token", VALID_TOKEN).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(USER_ID, saved.getUserId());
    assertEquals("ENCRYPTED", saved.getCiphertext());
    assertEquals(3, saved.getKeyVersion());
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), saved.getLastFour());
    assertNull(saved.getExpiresAt());

    String body = response.getResponse().getContentAsString();
    assertFalse(body.contains("ENCRYPTED"));
    Map<String, Object> json = responseToJson(response);
    assertEquals(1, json.get("userId"));
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), json.get("lastFour"));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_records_the_expiration_date_when_given() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(
            post("/api/pat")
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
  public void post_replaces_the_existing_credential_for_the_user() throws Exception {
    PatCredential existing =
        PatCredential.builder()
            .id(7L)
            .userId(USER_ID)
            .ciphertext("OLDCIPHERTEXT")
            .keyVersion(1)
            .lastFour("aaaa")
            .expiresAt(LocalDate.of(2025, 1, 1))
            .build();
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("NEWCIPHERTEXT", 2));
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.of(existing));
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat").param("token", VALID_TOKEN).with(csrf()))
        .andExpect(status().isOk());

    ArgumentCaptor<PatCredential> captor = ArgumentCaptor.forClass(PatCredential.class);
    verify(patCredentialRepository, times(1)).save(captor.capture());
    PatCredential saved = captor.getValue();
    assertEquals(Long.valueOf(7L), saved.getId()); // same row, replaced in place
    assertEquals("NEWCIPHERTEXT", saved.getCiphertext());
    assertEquals(2, saved.getKeyVersion());
    assertEquals(VALID_TOKEN.substring(VALID_TOKEN.length() - 4), saved.getLastFour());
    assertNull(saved.getExpiresAt()); // stale expiry from the old token is cleared
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_strips_surrounding_whitespace_from_the_token() throws Exception {
    when(patEncryptionService.encrypt(eq(VALID_TOKEN)))
        .thenReturn(new EncryptedPat("ENCRYPTED", 1));
    when(patCredentialRepository.findByUserId(eq(USER_ID))).thenReturn(Optional.empty());
    when(patCredentialRepository.save(any(PatCredential.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockMvc
        .perform(post("/api/pat").param("token", "  " + VALID_TOKEN + "\n").with(csrf()))
        .andExpect(status().isOk());

    verify(patEncryptionService, times(1)).encrypt(eq(VALID_TOKEN));
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_rejects_a_blank_token() throws Exception {
    MvcResult response =
        mockMvc
            .perform(post("/api/pat").param("token", "   ").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("token is required", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_rejects_a_fine_grained_token_with_an_explanation() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/pat")
                    .param(
                        "token", "github_pat_11FAKEFAKE0123456789_abcdefghijklmnopqrstuvwxyzABCDEF")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "fine-grained tokens (github_pat_...) cannot reach this app's repositories; create a classic token (ghp_...) instead — see docs/PAT.md",
        json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_rejects_a_token_with_an_unrecognized_prefix() throws Exception {
    MvcResult response =
        mockMvc
            .perform(
                post("/api/pat")
                    .param("token", "gho_FAKEfake0123456789FAKEfake0123456789")
                    .with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(
        "token must be a GitHub classic personal access token (starting with ghp_); see docs/PAT.md",
        json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_rejects_a_token_that_is_too_short() throws Exception {
    mockMvc
        .perform(post("/api/pat").param("token", "ghp_tooShort").with(csrf()))
        .andExpect(status().isBadRequest());

    verify(patCredentialRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN"})
  @Test
  public void post_returns_503_when_encryption_is_not_configured_on_the_server() throws Exception {
    when(patEncryptionService.encrypt(any()))
        .thenThrow(new IllegalStateException("PAT encryption is not configured on this server"));

    MvcResult response =
        mockMvc
            .perform(post("/api/pat").param("token", VALID_TOKEN).with(csrf()))
            .andExpect(status().isServiceUnavailable())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("IllegalStateException", json.get("type"));
    assertEquals("PAT encryption is not configured on this server", json.get("message"));
    verify(patCredentialRepository, never()).save(any());
  }
}
