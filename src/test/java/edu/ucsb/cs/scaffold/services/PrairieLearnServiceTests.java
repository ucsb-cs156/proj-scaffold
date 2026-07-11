package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs.scaffold.services.PrairieLearnService.CourseInstanceInfo;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class PrairieLearnServiceTests {

  RestTemplate restTemplate = mock(RestTemplate.class);

  PrairieLearnService prairieLearnService =
      new PrairieLearnService(restTemplate, "https://pl.example/pl/api/v1");

  @SuppressWarnings("unchecked")
  private void mockResponse(Map<String, Object> body) {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
  }

  @Test
  public void parses_the_course_instance_fields_from_the_response() {
    mockResponse(
        Map.of(
            "course_instance_id", "213133",
            "course_instance_long_name", "Spring 2026",
            "course_instance_short_name", "S26",
            "course_title", "Introduction to Data Science 2"));

    CourseInstanceInfo info = prairieLearnService.getCourseInstance(213133L, "pl-token");

    assertEquals(Long.valueOf(213133L), info.courseInstanceId());
    assertEquals("Spring 2026", info.longName());
    assertEquals("S26", info.shortName());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void builds_the_url_and_private_token_header_from_its_arguments() {
    mockResponse(Map.of("course_instance_id", "213133"));

    prairieLearnService.getCourseInstance(213133L, "pl-token");

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            any(ParameterizedTypeReference.class));
    assertEquals("https://pl.example/pl/api/v1/course_instances/213133", urlCaptor.getValue());
    assertEquals("pl-token", entityCaptor.getValue().getHeaders().getFirst("Private-Token"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void returns_null_when_the_body_is_null() {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertNull(prairieLearnService.getCourseInstance(213133L, "pl-token"));
  }

  @Test
  public void returns_null_when_the_body_has_no_course_instance_id() {
    mockResponse(Map.of("course_instance_long_name", "Spring 2026"));

    assertNull(prairieLearnService.getCourseInstance(213133L, "pl-token"));
  }

  @Test
  public void parses_a_numeric_course_instance_id_as_well_as_a_string_one() {
    Map<String, Object> body = new HashMap<>();
    body.put("course_instance_id", 213133);
    body.put("course_instance_long_name", "Spring 2026");
    body.put("course_instance_short_name", "S26");
    mockResponse(body);

    CourseInstanceInfo info = prairieLearnService.getCourseInstance(213133L, "pl-token");

    assertEquals(Long.valueOf(213133L), info.courseInstanceId());
  }
}
