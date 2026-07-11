package edu.ucsb.cs.scaffold.services;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Thin wrapper around the GitHub REST API, authenticated per call with a user's fine-grained
 * personal access token. Callers handle GitHub's HTTP errors (401/403 = bad or unapproved token,
 * 404 = missing repo or path), which RestTemplate surfaces as HttpClientErrorException subclasses.
 */
@Service
public class GithubService {

  private final RestTemplate restTemplate;
  private final String githubApiBase;

  public GithubService(
      RestTemplate restTemplate,
      @Value("${github.api.base:https://api.github.com}") String githubApiBase) {
    this.restTemplate = restTemplate;
    this.githubApiBase = githubApiBase;
  }

  /**
   * Lists the names of the subdirectories of {@code path} in the given repository, using the <a
   * href="https://docs.github.com/en/rest/repos/contents">Contents API</a>. Files at that path are
   * ignored.
   *
   * @param repoName the repository in {@code owner/repo} form
   * @param path the directory path within the repository, e.g. {@code courseInstances}
   * @param token the user's fine-grained PAT (plaintext)
   * @return the subdirectory names, in the order GitHub returns them (alphabetical)
   */
  public List<String> listSubdirectories(String repoName, String path, String token) {
    String url =
        UriComponentsBuilder.fromUriString(githubApiBase)
            .pathSegment("repos")
            .path("/" + repoName)
            .pathSegment("contents", path)
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.set("Accept", "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

    List<Map<String, Object>> items = response.getBody();
    if (items == null) {
      return List.of();
    }
    return items.stream()
        .filter(item -> "dir".equals(item.get("type")))
        .map(item -> (String) item.get("name"))
        .toList();
  }
}
