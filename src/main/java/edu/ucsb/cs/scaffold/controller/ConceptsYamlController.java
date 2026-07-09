package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.services.ConceptYamlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Download and upload a course's entire concept-graph content (concepts, subconcepts, prerequisite
 * edges, and practice problems) as a YAML file. See docs/yaml-format.md for the format.
 */
@Tag(name = "Concepts YAML")
@RestController
@RequiredArgsConstructor
public class ConceptsYamlController extends ApiController {

  private final ConceptYamlService conceptYamlService;

  @Operation(
      summary = "Download the course's concept-graph content as a YAML file",
      description =
          """
          Produces the course's concepts, subconcepts, prerequisite edges, and practice
          problems as a human-editable YAML document (see docs/yaml-format.md). The file
          can be edited and uploaded to this or another course.
          """)
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/api/concepts/yaml/download")
  public ResponseEntity<String> downloadConceptsYaml(
      @Parameter(name = "courseId") @RequestParam Long courseId) throws EntityNotFoundException {
    String yaml = conceptYamlService.createYAML(courseId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/x-yaml"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"concepts-course-%d.yaml\"".formatted(courseId))
        .body(yaml);
  }

  @Operation(
      summary = "Replace the course's concept-graph content from an uploaded YAML file",
      description =
          """
          Replaces ALL of the course's concepts, subconcepts, prerequisite edges, and
          practice problems with the uploaded document (see docs/yaml-format.md), and
          deletes every user's saved per-course scaffold state, which would be stale.
          All-or-nothing: an invalid file changes nothing and responds 400 with every
          problem found listed under errors.
          """)
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(
      value = "/api/concepts/yaml/upload",
      consumes = {"multipart/form-data"})
  public ResponseEntity<Map<String, Object>> uploadConceptsYaml(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws EntityNotFoundException, IOException {
    Map<String, Object> report =
        conceptYamlService.replaceFromYAML(courseId, file.getInputStream());
    HttpStatus status =
        Boolean.TRUE.equals(report.get("success")) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status).body(report);
  }
}
