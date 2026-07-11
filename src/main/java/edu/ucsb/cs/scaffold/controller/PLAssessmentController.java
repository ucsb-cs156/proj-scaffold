package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only: PlAssessments are populated by sync jobs mirroring the repo on GitHub — not by
 * POST/DELETE endpoints.
 */
@Tag(name = "PlAssessment")
@RequestMapping("/api/plAssessment")
@RestController
@Slf4j
public class PLAssessmentController extends ApiController {

  @Autowired private PlAssessmentRepository plAssessmentRepository;

  @Operation(summary = "List all PlAssessments for a PlRepo and PlInstance")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public List<PlAssessment> allPlAssessments(
      @Parameter(name = "plRepoId") @RequestParam Long plRepoId,
      @Parameter(name = "plInstance") @RequestParam Long plInstance) {
    return plAssessmentRepository.findByPlRepoIdAndPlInstanceId(plRepoId, plInstance);
  }
}
