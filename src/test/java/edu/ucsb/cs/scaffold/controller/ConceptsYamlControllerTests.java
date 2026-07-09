package edu.ucsb.cs.scaffold.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs.scaffold.ControllerTestCase;
import edu.ucsb.cs.scaffold.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.services.ConceptYamlService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ConceptsYamlController.class)
public class ConceptsYamlControllerTests extends ControllerTestCase {

  @MockitoBean private ConceptYamlService conceptYamlService;

  private static final String SAMPLE_YAML =
      """
      format: 1
      concepts:
        - id: 1
          label: Recursion
      edges: []
      """;

  // ------------------------------------------------------------------ download

  @Test
  public void download_logged_out_users_cannot_access() throws Exception {
    mockMvc.perform(get("/api/concepts/yaml/download?courseId=42")).andExpect(status().is(403));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void download_users_without_course_permissions_cannot_access() throws Exception {
    mockMvc.perform(get("/api/concepts/yaml/download?courseId=42")).andExpect(status().is(403));
  }

  @Test
  @WithInstructorCoursePermissions
  public void download_returns_yaml_as_an_attachment() throws Exception {
    when(conceptYamlService.createYAML(42L)).thenReturn(SAMPLE_YAML);

    MvcResult response =
        mockMvc
            .perform(get("/api/concepts/yaml/download?courseId=42"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/x-yaml"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition", "attachment; filename=\"concepts-course-42.yaml\""))
            .andReturn();

    assertEquals(SAMPLE_YAML, response.getResponse().getContentAsString());
  }

  @Test
  @WithInstructorCoursePermissions
  public void download_returns_404_when_course_not_found() throws Exception {
    when(conceptYamlService.createYAML(7L))
        .thenThrow(new EntityNotFoundException(Course.class, 7L));

    MvcResult response =
        mockMvc
            .perform(get("/api/concepts/yaml/download?courseId=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 7 not found", json.get("message"));
  }

  // -------------------------------------------------------------------- upload

  @Test
  public void upload_logged_out_users_cannot_access() throws Exception {
    mockMvc
        .perform(multipart("/api/concepts/yaml/upload?courseId=42").file(sampleFile()).with(csrf()))
        .andExpect(status().is(403));
    verify(conceptYamlService, never()).replaceFromYAML(eq(42L), any());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void upload_users_without_course_permissions_cannot_access() throws Exception {
    mockMvc
        .perform(multipart("/api/concepts/yaml/upload?courseId=42").file(sampleFile()).with(csrf()))
        .andExpect(status().is(403));
    verify(conceptYamlService, never()).replaceFromYAML(eq(42L), any());
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_replaces_content_and_returns_the_report() throws Exception {
    Map<String, Object> report =
        Map.of(
            "success", true,
            "errors", List.of(),
            "conceptsCreated", 1,
            "subconceptsCreated", 0,
            "edgesCreated", 0,
            "practiceProblemsCreated", 0,
            "userStatesCleared", 3);
    when(conceptYamlService.replaceFromYAML(eq(42L), any())).thenReturn(report);

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/concepts/yaml/upload?courseId=42").file(sampleFile()).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // The uploaded file's bytes are what reach the service.
    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(conceptYamlService).replaceFromYAML(eq(42L), streamCaptor.capture());
    assertEquals(
        SAMPLE_YAML, new String(streamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8));

    Map<String, Object> json = responseToJson(response);
    assertEquals(true, json.get("success"));
    assertEquals(1, json.get("conceptsCreated"));
    assertEquals(3, json.get("userStatesCleared"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_400_with_the_report_when_the_file_is_invalid() throws Exception {
    Map<String, Object> report =
        Map.of(
            "success", false,
            "errors", List.of("concepts[0]: id is required"),
            "conceptsCreated", 0,
            "subconceptsCreated", 0,
            "edgesCreated", 0,
            "practiceProblemsCreated", 0,
            "userStatesCleared", 0);
    when(conceptYamlService.replaceFromYAML(eq(42L), any())).thenReturn(report);

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/concepts/yaml/upload?courseId=42").file(sampleFile()).with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals(false, json.get("success"));
    assertEquals(List.of("concepts[0]: id is required"), json.get("errors"));
  }

  @Test
  @WithInstructorCoursePermissions
  public void upload_returns_404_when_course_not_found() throws Exception {
    when(conceptYamlService.replaceFromYAML(eq(7L), any()))
        .thenThrow(new EntityNotFoundException(Course.class, 7L));

    MvcResult response =
        mockMvc
            .perform(
                multipart("/api/concepts/yaml/upload?courseId=7").file(sampleFile()).with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Course with id 7 not found", json.get("message"));
  }

  private MockMultipartFile sampleFile() {
    return new MockMultipartFile(
        "file",
        "concepts.yaml",
        "application/x-yaml",
        SAMPLE_YAML.getBytes(StandardCharsets.UTF_8));
  }
}
