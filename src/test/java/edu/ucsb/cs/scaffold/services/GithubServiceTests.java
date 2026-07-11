package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "github_pat_x");

    assertEquals(List.of("Fall2025", "Winter2026"), names);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void builds_the_contents_url_and_authorization_header_from_its_arguments() {
    mockResponse(List.of());

    githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "github_pat_x");

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
    assertEquals(
        "Bearer github_pat_x", entityCaptor.getValue().getHeaders().getFirst("Authorization"));
    assertEquals(
        "application/vnd.github+json", entityCaptor.getValue().getHeaders().getFirst("Accept"));
    assertEquals(
        "2022-11-28", entityCaptor.getValue().getHeaders().getFirst("X-GitHub-Api-Version"));
  }

  @Test
  public void returns_an_empty_list_when_github_returns_no_body() {
    mockResponse(null);

    List<String> names =
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "github_pat_x");

    assertEquals(List.of(), names);
  }

  @Test
  public void returns_an_empty_list_when_the_directory_contains_no_subdirectories() {
    mockResponse(List.of(Map.of("name", "README.md", "type", "file")));

    List<String> names =
        githubService.listSubdirectories("ucsb-cs156/pl-demo", "courseInstances", "github_pat_x");

    assertEquals(List.of(), names);
  }
}
