package edu.ucsb.cs.scaffold.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
 * Thin wrapper around the GitHub REST API, authenticated per call with a user's personal access
 * token. Callers handle GitHub's HTTP errors (401/403 = bad or unapproved token, 404 = missing repo
 * or path), which RestTemplate surfaces as HttpClientErrorException subclasses.
 */
@Service
public class GithubService {

  /** One entry of a directory listing: {@code type} is {@code "dir"}, {@code "file"}, etc. */
  public record DirectoryEntry(String name, String type) {}

  private final RestTemplate restTemplate;
  private final String githubApiBase;
  private final ApiRetryHelper retryHelper;

  public GithubService(
      RestTemplate restTemplate,
      @Value("${github.api.base:https://api.github.com}") String githubApiBase,
      @Value("${GITHUB_SERVICE_RETRY_INITIAL_SLEEP_SECONDS:8}") long retryInitialSleepSeconds,
      @Value("${GITHUB_SERVICE_RETRY_MAX:3}") int retryMax,
      @Value("${GITHUB_SERVICE_RATE_LIMIT_SLEEP_INITIAL_MS:1000}") long rateLimitSleepInitialMs) {
    this.restTemplate = restTemplate;
    this.githubApiBase = githubApiBase;
    this.retryHelper =
        new ApiRetryHelper(
            "GitHub",
            "GITHUB_SERVICE_RATE_LIMIT_SLEEP_INITIAL_MS",
            retryInitialSleepSeconds,
            retryMax,
            rateLimitSleepInitialMs);
  }

  /**
   * Lists the contents of a directory in the given repository, using the <a
   * href="https://docs.github.com/en/rest/repos/contents">Contents API</a>.
   *
   * @param repoName the repository in {@code owner/repo} form
   * @param path the directory path within the repository, e.g. {@code questions/foo}
   * @param token the user's PAT (plaintext)
   * @return the entries, in the order GitHub returns them (alphabetical)
   */
  public List<DirectoryEntry> listDirectory(String repoName, String path, String token) {
    String url = contentsUrl(repoName, path);
    ResponseEntity<List<Map<String, Object>>> response =
        retryHelper.execute(
            "GET " + url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authenticatedEntity(token),
                    new ParameterizedTypeReference<>() {}));

    List<Map<String, Object>> items = response.getBody();
    if (items == null) {
      return List.of();
    }
    return items.stream()
        .map(item -> new DirectoryEntry((String) item.get("name"), (String) item.get("type")))
        .toList();
  }

  /**
   * Lists the names of the subdirectories of {@code path} in the given repository. Files at that
   * path are ignored.
   */
  public List<String> listSubdirectories(String repoName, String path, String token) {
    return listDirectory(repoName, path, token).stream()
        .filter(entry -> "dir".equals(entry.type()))
        .map(DirectoryEntry::name)
        .toList();
  }

  /**
   * Fetches the content of a file in the given repository as a UTF-8 string, using the Contents API
   * (which returns file bodies base64-encoded).
   *
   * @param repoName the repository in {@code owner/repo} form
   * @param path the file path within the repository, e.g. {@code questions/foo/info.json}
   * @param token the user's PAT (plaintext)
   */
  public String getFileContent(String repoName, String path, String token) {
    String url = contentsUrl(repoName, path);
    ResponseEntity<Map<String, Object>> response =
        retryHelper.execute(
            "GET " + url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authenticatedEntity(token),
                    new ParameterizedTypeReference<>() {}));

    Map<String, Object> body = response.getBody();
    if (body == null || body.get("content") == null) {
      return "";
    }
    // GitHub base64-encodes file content with embedded newlines; the MIME decoder accepts them.
    byte[] decoded = Base64.getMimeDecoder().decode((String) body.get("content"));
    return new String(decoded, StandardCharsets.UTF_8);
  }

  /**
   * Reports whether the token has push (read/write) access to the repository, from the {@code
   * permissions} block of <a href="https://docs.github.com/en/rest/repos/repos">GET
   * /repos/{owner}/{repo}</a>. A successful call already proves read access; {@code push=true}
   * additionally proves write access — nothing is written to check this.
   *
   * @param repoName the repository in {@code owner/repo} form
   * @param token the user's PAT (plaintext)
   * @throws org.springframework.web.client.HttpClientErrorException when the token cannot read the
   *     repo at all (GitHub answers 404 for repos a token cannot see, or 401 for a bad token)
   */
  public boolean hasWriteAccess(String repoName, String token) {
    String url = repoUrl(repoName);
    ResponseEntity<Map<String, Object>> response =
        retryHelper.execute(
            "GET " + url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authenticatedEntity(token),
                    new ParameterizedTypeReference<>() {}));

    Map<String, Object> body = response.getBody();
    if (body == null || !(body.get("permissions") instanceof Map<?, ?> permissions)) {
      return false;
    }
    return Boolean.TRUE.equals(permissions.get("push"));
  }

  private String repoUrl(String repoName) {
    return UriComponentsBuilder.fromUriString(githubApiBase)
        .pathSegment("repos")
        .path("/" + repoName)
        .toUriString();
  }

  private String contentsUrl(String repoName, String path) {
    return UriComponentsBuilder.fromUriString(githubApiBase)
        .pathSegment("repos")
        .path("/" + repoName)
        .pathSegment("contents")
        .path("/" + path)
        .toUriString();
  }

  private HttpEntity<Void> authenticatedEntity(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.set("Accept", "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");
    return new HttpEntity<>(headers);
  }
}
