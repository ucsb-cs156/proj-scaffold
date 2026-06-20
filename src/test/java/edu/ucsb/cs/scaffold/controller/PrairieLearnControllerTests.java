package edu.ucsb.cs.scaffold.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "pl.api.token=test-token",
      "pl.api.base=https://us.prairielearn.com/pl/api/v1",
      "pl.course.instance.id=99999"
    })
class PrairieLearnControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RestTemplate restTemplate;

  @Test
  void testPrairieLearnReturnsBodyFromApi() throws Exception {
    List<Map<String, Object>> fakeBody = List.of(Map.of("id", 1, "title", "Homework 1"));
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Object.class)))
        .thenReturn(new ResponseEntity<>(fakeBody, HttpStatus.OK));

    mockMvc
        .perform(get("/api/test-pl"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].title").value("Homework 1"));
  }

  @Test
  void testPrairieLearnBuildsCorrectUrl() throws Exception {
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Object.class)))
        .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

    mockMvc.perform(get("/api/test-pl")).andExpect(status().isOk());

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(), eq(Object.class));

    assertThat(urlCaptor.getValue())
        .isEqualTo("https://us.prairielearn.com/pl/api/v1/course_instances/99999/assessments");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPrairieLearnSetsPrivateTokenHeader() throws Exception {
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Object.class)))
        .thenReturn(new ResponseEntity<>(List.of(), HttpStatus.OK));

    mockMvc.perform(get("/api/test-pl")).andExpect(status().isOk());

    ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(anyString(), eq(HttpMethod.GET), entityCaptor.capture(), eq(Object.class));

    assertThat(entityCaptor.getValue().getHeaders().getFirst("Private-Token"))
        .isEqualTo("test-token");
  }
}
