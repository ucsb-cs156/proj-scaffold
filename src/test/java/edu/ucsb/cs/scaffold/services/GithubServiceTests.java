package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class GithubServiceTests {

  RestTemplate restTemplate = mock(RestTemplate.class);

  GithubService githubService = new GithubService(restTemplate, "https://api.github.example");

  @SuppressWarnings("unchecked")
  private void mockResponse(List<Map<String, Object>> body) {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
  }

  @Test
  public void returns_only_directory_names_ignoring_files() {
    mockResponse(
        List.of(
            Map.of("name", "Fall2025", "type", "dir"),
            Map.of("name", "README.md", "type", "file"),
            Map.of("name", "Winter2026", "type", "dir"),
            Map.of("name", "notes.txt", "type", "file")));

    List<String> names =
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "ghp_x");

    assertEquals(List.of("Fall2025", "Winter2026"), names);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void builds_the_contents_url_and_authorization_header_from_its_arguments() {
    mockResponse(List.of());

    githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "ghp_x");

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("rawtypes")
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            any(ParameterizedTypeReference.class));

    assertEquals(
        "https://api.github.example/repos/ucsb-cs156/pl-demo/contents/courseInstances",
        urlCaptor.getValue());
    assertEquals("Bearer ghp_x", entityCaptor.getValue().getHeaders().getFirst("Authorization"));
    assertEquals(
        "application/vnd.github+json", entityCaptor.getValue().getHeaders().getFirst("Accept"));
    assertEquals(
        "2022-11-28", entityCaptor.getValue().getHeaders().getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void returns_an_empty_list_when_github_returns_no_body() {
    mockResponse(null);

    List<String> names =
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "ghp_x");

    assertEquals(List.of(), names);
  }

  @Test
  public void returns_an_empty_list_when_the_directory_contains_no_subdirectories() {
    mockResponse(List.of(Map.of("name", "README.md", "type", "file")));

    List<String> names =
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "ghp_x");

    assertEquals(List.of(), names);
  }

  @Test
  public void listDirectory_returns_all_entries_with_their_types() {
    mockResponse(
        List.of(
            Map.of("name", "info.json", "type", "file"), Map.of("name", "tests", "type", "dir")));

    List<GithubService.DirectoryEntry> entries =
        githubService.listDirectory("ucsb-cs156/pl-demo", "questions/foo", "ghp_x");

    assertEquals(
        List.of(
            new GithubService.DirectoryEntry("info.json", "file"),
            new GithubService.DirectoryEntry("tests", "dir")),
        entries);
  }

  @Test
  public void listDirectory_returns_an_empty_list_when_github_returns_no_body() {
    mockResponse(null);

    assertEquals(
        List.of(), githubService.listDirectory("ucsb-cs156/pl-demo", "questions", "ghp_x"));
  }

  @SuppressWarnings("unchecked")
  private void mockFileResponse(Map<String, Object> body) {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getFileContent_decodes_the_base64_body_github_returns() {
    // GitHub returns base64 with embedded newlines; the MIME decoder must accept them
    String base64WithNewlines =
        Base64.getEncoder()
            .encodeToString("{ \"uuid\": \"abc\" }".getBytes(StandardCharsets.UTF_8))
            .replaceAll("(.{8})", "$1\n");
    mockFileResponse(Map.of("content", base64WithNewlines, "encoding", "base64"));

    String content =
        githubService.getFileContent("ucsb-cs156/pl-demo", "questions/foo/info.json", "ghp_x");

    assertEquals("{ \"uuid\": \"abc\" }", content);

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(restTemplate)
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class));
    assertEquals(
        "https://api.github.example/repos/ucsb-cs156/pl-demo/contents/questions/foo/info.json",
        urlCaptor.getValue());
  }

  @Test
  public void getFileContent_returns_empty_string_when_github_returns_no_body() {
    mockFileResponse(null);

    assertEquals(
        "", githubService.getFileContent("ucsb-cs156/pl-demo", "questions/foo/info.json", "ghp_x"));
  }

  @Test
  public void getFileContent_returns_empty_string_when_the_body_has_no_content_field() {
    mockFileResponse(Map.of("encoding", "base64"));

    assertEquals(
        "", githubService.getFileContent("ucsb-cs156/pl-demo", "questions/foo/info.json", "ghp_x"));
  }

  // ────────────────────────── hasWriteAccess ──────────────────────────

  @SuppressWarnings("unchecked")
  private void mockRepoResponse(Map<String, Object> body) {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
  }

  @Test
  public void hasWriteAccess_is_true_when_the_permissions_block_reports_push() {
    mockRepoResponse(
        Map.of(
            "full_name", "ucsb-cs156/pl-demo", "permissions", Map.of("push", true, "pull", true)));

    assertTrue(githubService.hasWriteAccess("ucsb-cs156/pl-demo", "ghp_x"));
  }

  @Test
  public void hasWriteAccess_is_false_when_the_permissions_block_reports_no_push() {
    mockRepoResponse(
        Map.of(
            "full_name", "ucsb-cs156/pl-demo", "permissions", Map.of("push", false, "pull", true)));

    assertFalse(githubService.hasWriteAccess("ucsb-cs156/pl-demo", "ghp_x"));
  }

  @Test
  public void hasWriteAccess_is_false_when_the_permissions_block_is_missing() {
    mockRepoResponse(Map.of("full_name", "ucsb-cs156/pl-demo"));

    assertFalse(githubService.hasWriteAccess("ucsb-cs156/pl-demo", "ghp_x"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hasWriteAccess_is_false_when_the_body_is_null() {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertFalse(githubService.hasWriteAccess("ucsb-cs156/pl-demo", "ghp_x"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hasWriteAccess_builds_the_repo_url_and_authorization_header_from_its_arguments() {
    mockRepoResponse(Map.of("permissions", Map.of("push", true)));

    githubService.hasWriteAccess("ucsb-cs156/pl-demo", "ghp_x");

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            any(ParameterizedTypeReference.class));
    assertEquals("https://api.github.example/repos/ucsb-cs156/pl-demo", urlCaptor.getValue());
    assertEquals("Bearer ghp_x", entityCaptor.getValue().getHeaders().getFirst("Authorization"));
  }
}
