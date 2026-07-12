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

  // ────────────────────────── getAssessments ──────────────────────────

  @SuppressWarnings("unchecked")
  private void mockListResponse(java.util.List<Map<String, Object>> body) {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
  }

  @Test
  public void parses_the_assessment_fields_from_the_response() {
    Map<String, Object> item = new HashMap<>();
    item.put("assessment_id", "2690012");
    item.put("assessment_name", "exam-02");
    item.put("assessment_label", "E2");
    item.put("assessment_number", "2");
    item.put("assessment_order_by", 6);
    item.put("title", "Final (in Testing Center)");
    item.put("assessment_set_abbreviation", "E");
    item.put("assessment_set_number", 8);
    item.put("assessment_set_heading", "Exams");
    item.put("assessment_set_color", "pink2");
    mockListResponse(java.util.List.of(item));

    java.util.List<PrairieLearnService.AssessmentInfo> assessments =
        prairieLearnService.getAssessments(213133L, "pl-token");

    assertEquals(1, assessments.size());
    PrairieLearnService.AssessmentInfo info = assessments.get(0);
    assertEquals(Long.valueOf(2690012L), info.assessmentId());
    assertEquals("exam-02", info.assessmentName());
    assertEquals(Long.valueOf(2L), info.assessmentNumber());
    assertEquals(Long.valueOf(6L), info.assessmentOrderBy());
    assertEquals("Final (in Testing Center)", info.title());
    assertEquals("E", info.assessmentSetAbbreviation());
    assertEquals(Integer.valueOf(8), info.assessmentSetNumber());
    assertEquals("Exams", info.assessmentSetHeading());
    assertEquals("pink2", info.assessmentSetColor());
  }

  @Test
  public void missing_optional_fields_come_back_as_null() {
    Map<String, Object> item = new HashMap<>();
    item.put("assessment_id", "2690012");
    item.put("assessment_name", "exam-02");
    mockListResponse(java.util.List.of(item));

    PrairieLearnService.AssessmentInfo info =
        prairieLearnService.getAssessments(213133L, "pl-token").get(0);

    assertNull(info.assessmentNumber());
    assertNull(info.assessmentOrderBy());
    assertNull(info.title());
    assertNull(info.assessmentSetAbbreviation());
    assertNull(info.assessmentSetNumber());
    assertNull(info.assessmentSetHeading());
    assertNull(info.assessmentSetColor());
  }

  @Test
  public void entries_without_an_assessment_id_are_skipped() {
    Map<String, Object> good = new HashMap<>();
    good.put("assessment_id", "1");
    good.put("assessment_name", "hw-01");
    mockListResponse(java.util.List.of(Map.of("assessment_name", "no-id"), good));

    java.util.List<PrairieLearnService.AssessmentInfo> assessments =
        prairieLearnService.getAssessments(213133L, "pl-token");

    assertEquals(1, assessments.size());
    assertEquals("hw-01", assessments.get(0).assessmentName());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void returns_an_empty_list_when_the_body_is_null() {
    when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    assertEquals(java.util.List.of(), prairieLearnService.getAssessments(213133L, "pl-token"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void assessments_url_and_private_token_header_are_built_from_the_arguments() {
    mockListResponse(java.util.List.of());

    prairieLearnService.getAssessments(213133L, "pl-token");

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            entityCaptor.capture(),
            any(ParameterizedTypeReference.class));
    assertEquals(
        "https://pl.example/pl/api/v1/course_instances/213133/assessments", urlCaptor.getValue());
    assertEquals("pl-token", entityCaptor.getValue().getHeaders().getFirst("Private-Token"));
  }
}
