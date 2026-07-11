package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlScaffoldAssessment;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only: PlScaffoldAssessments are populated by sync jobs mirroring the repo on GitHub — not by
 * POST/DELETE endpoints.
 */
@Tag(name = "PlScaffoldAssessment")
@RequestMapping("/api/pScaffoldAssessment")
@RestController
@Slf4j
public class PLScaffoldAssessmentController extends ApiController {

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Operation(summary = "Get the PlScaffoldAssessment for a given PlQuestion and PlInstance")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public PlScaffoldAssessment getPlScaffoldAssessment(
      @Parameter(name = "plQuestion") @RequestParam Long plQuestion,
      @Parameter(name = "plInstance") @RequestParam Long plInstance) {
    return plScaffoldAssessmentRepository
        .findByPlQuestionIdAndPlInstanceId(plQuestion, plInstance)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    PlScaffoldAssessment.class,
                    "plQuestion=%s, plInstance=%s".formatted(plQuestion, plInstance)));
  }
}
