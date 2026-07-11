package edu.ucsb.cs.scaffold.services;

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
 * Thin wrapper around the PrairieLearn REST API (see https://docs.prairielearn.com/api/),
 * authenticated per call with a user's PrairieLearn personal access token. Callers handle
 * PrairieLearn's HTTP errors (401 = bad token, 403/404 = unknown or inaccessible course instance),
 * which RestTemplate surfaces as HttpClientErrorException subclasses.
 */
@Service
public class PrairieLearnService {

  /**
   * The parts of a course instance response this app uses; the numeric id is the crucial one, since
   * it cannot be obtained from the GitHub repo.
   */
  public record CourseInstanceInfo(Long courseInstanceId, String longName, String shortName) {}

  private final RestTemplate restTemplate;
  private final String plApiBase;

  public PrairieLearnService(
      RestTemplate restTemplate,
      @Value("${pl.api.base:https://us.prairielearn.com/pl/api/v1}") String plApiBase) {
    this.restTemplate = restTemplate;
    this.plApiBase = plApiBase;
  }

  /**
   * Fetches one course instance by its numeric id, e.g. {@code /course_instances/213133}. Returns
   * null when the response has no usable body.
   *
   * @param instanceId PrairieLearn's numeric course instance id
   * @param token the user's PrairieLearn PAT (plaintext)
   */
  public CourseInstanceInfo getCourseInstance(long instanceId, String token) {
    String url =
        UriComponentsBuilder.fromUriString(plApiBase)
            .pathSegment("course_instances", String.valueOf(instanceId))
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Private-Token", token);
    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});

    Map<String, Object> body = response.getBody();
    if (body == null || body.get("course_instance_id") == null) {
      return null;
    }
    return new CourseInstanceInfo(
        // PrairieLearn returns ids as JSON strings, e.g. "213133".
        Long.valueOf(String.valueOf(body.get("course_instance_id"))),
        (String) body.get("course_instance_long_name"),
        (String) body.get("course_instance_short_name"));
  }
}
