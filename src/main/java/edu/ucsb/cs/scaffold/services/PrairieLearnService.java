package edu.ucsb.cs.scaffold.services;

import java.util.ArrayList;
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
  private final ApiRetryHelper retryHelper;

  public PrairieLearnService(
      RestTemplate restTemplate,
      @Value("${pl.api.base:https://us.prairielearn.com/pl/api/v1}") String plApiBase,
      @Value("${PL_SERVICE_RETRY_INITIAL_SLEEP_SECONDS:8}") long retryInitialSleepSeconds,
      @Value("${PL_SERVICE_RETRY_MAX:3}") int retryMax,
      @Value("${PL_SERVICE_RATE_LIMIT_SLEEP_INITIAL_MS:1000}") long rateLimitSleepInitialMs) {
    this.restTemplate = restTemplate;
    this.plApiBase = plApiBase;
    this.retryHelper =
        new ApiRetryHelper(
            "PrairieLearn",
            "PL_SERVICE_RATE_LIMIT_SLEEP_INITIAL_MS",
            retryInitialSleepSeconds,
            retryMax,
            rateLimitSleepInitialMs);
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
        retryHelper.execute(
            "GET " + url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}));

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

  /**
   * One assessment from the {@code /course_instances/{id}/assessments} response — the fields
   * PlAssessment stores (issue #71). {@code assessmentName} matches the assessment's directory name
   * in the GitHub repo (PlAssessment.name), which is how the two sources are joined.
   */
  public record AssessmentInfo(
      Long assessmentId,
      String assessmentName,
      // alphanumeric, e.g. "2" or "1a"
      String assessmentNumber,
      Long assessmentOrderBy,
      String title,
      String assessmentSetAbbreviation,
      Integer assessmentSetNumber,
      String assessmentSetHeading,
      String assessmentSetColor) {}

  /**
   * Fetches all assessments of a course instance, e.g. {@code
   * /course_instances/213133/assessments}. Entries without an {@code assessment_id} are skipped.
   *
   * @param instanceId PrairieLearn's numeric course instance id
   * @param token the user's PrairieLearn PAT (plaintext)
   */
  public List<AssessmentInfo> getAssessments(long instanceId, String token) {
    String url =
        UriComponentsBuilder.fromUriString(plApiBase)
            .pathSegment("course_instances", String.valueOf(instanceId), "assessments")
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Private-Token", token);
    ResponseEntity<List<Map<String, Object>>> response =
        retryHelper.execute(
            "GET " + url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}));

    List<Map<String, Object>> body = response.getBody();
    if (body == null) {
      return List.of();
    }
    List<AssessmentInfo> assessments = new ArrayList<>();
    for (Map<String, Object> item : body) {
      if (item.get("assessment_id") == null) {
        continue;
      }
      assessments.add(
          new AssessmentInfo(
              asLong(item.get("assessment_id")),
              (String) item.get("assessment_name"),
              asString(item.get("assessment_number")),
              asLong(item.get("assessment_order_by")),
              (String) item.get("title"),
              (String) item.get("assessment_set_abbreviation"),
              asInteger(item.get("assessment_set_number")),
              (String) item.get("assessment_set_heading"),
              (String) item.get("assessment_set_color")));
    }
    return assessments;
  }

  // PrairieLearn is inconsistent about numbers: some come as JSON strings ("2690012"), some as
  // JSON numbers (6). Both parse via their string form; null stays null.
  private static Long asLong(Object value) {
    return value == null ? null : Long.valueOf(String.valueOf(value));
  }

  private static Integer asInteger(Object value) {
    return value == null ? null : Integer.valueOf(String.valueOf(value));
  }

  // assessment_number is alphanumeric ("1a") but other fields may arrive as JSON numbers,
  // so normalize through String.valueOf rather than casting.
  private static String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
