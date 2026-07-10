package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PlAssessment")
@RequestMapping("/api/plAssessment")
@RestController
@Slf4j
public class PLAssessmentController extends ApiController {

  @Autowired private PlAssessmentRepository plAssessmentRepository;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Operation(summary = "List all PlAssessments for a PlRepo and PlInstance")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public List<PlAssessment> allPlAssessments(
      @Parameter(name = "plRepoId") @RequestParam Long plRepoId,
      @Parameter(name = "plInstance") @RequestParam Long plInstance) {
    return plAssessmentRepository.findByPlRepoIdAndPlInstanceId(plRepoId, plInstance);
  }

  @Operation(summary = "Create a new PlAssessment")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("")
  public PlAssessment postPlAssessment(
      @Parameter(name = "plRepoId") @RequestParam Long plRepoId,
      @Parameter(name = "plInstanceId") @RequestParam Long plInstanceId,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "url") @RequestParam String url) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
    plInstanceRepository
        .findById(plInstanceId)
        .orElseThrow(() -> new EntityNotFoundException(PlInstance.class, plInstanceId));

    String trimmedName = name.strip();
    if (trimmedName.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }

    if (plAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndName(
        plRepoId, plInstanceId, trimmedName)) {
      throw new IllegalArgumentException(
          "PlAssessment with name %s already exists for plRepoId %s, plInstanceId %s"
              .formatted(trimmedName, plRepoId, plInstanceId));
    }

    PlAssessment plAssessment =
        PlAssessment.builder()
            .plRepoId(plRepoId)
            .plInstanceId(plInstanceId)
            .name(trimmedName)
            .url(url)
            .build();
    return plAssessmentRepository.save(plAssessment);
  }

  @Operation(summary = "Delete a PlAssessment")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  public Object deletePlAssessment(@Parameter(name = "id") @RequestParam Long id) {
    PlAssessment plAssessment =
        plAssessmentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(PlAssessment.class, id));
    plAssessmentRepository.delete(plAssessment);
    return genericMessage("PlAssessment with id %s deleted".formatted(id));
  }
}
