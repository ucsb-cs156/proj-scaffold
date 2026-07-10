package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.entity.PlScaffoldAssessment;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PlScaffoldAssessment")
@RequestMapping("/api/pScaffoldAssessment")
@RestController
@Slf4j
public class PLScaffoldAssessmentController extends ApiController {

  public static final String STATUS_PENDING = "Pending";

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PlQuestionRepository plQuestionRepository;

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

  @Operation(summary = "Create a new PlScaffoldAssessment")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("")
  public PlScaffoldAssessment postPlScaffoldAssessment(
      @Parameter(name = "plRepoId") @RequestParam Long plRepoId,
      @Parameter(name = "plInstanceId") @RequestParam Long plInstanceId,
      @Parameter(name = "plQuestionId") @RequestParam Long plQuestionId) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
    plInstanceRepository
        .findById(plInstanceId)
        .orElseThrow(() -> new EntityNotFoundException(PlInstance.class, plInstanceId));
    plQuestionRepository
        .findById(plQuestionId)
        .orElseThrow(() -> new EntityNotFoundException(PlQuestion.class, plQuestionId));

    if (plScaffoldAssessmentRepository.existsByPlRepoIdAndPlInstanceIdAndPlQuestionId(
        plRepoId, plInstanceId, plQuestionId)) {
      throw new IllegalArgumentException(
          "PlScaffoldAssessment already exists for plRepoId %s, plInstanceId %s, plQuestionId %s"
              .formatted(plRepoId, plInstanceId, plQuestionId));
    }

    PlScaffoldAssessment plScaffoldAssessment =
        PlScaffoldAssessment.builder()
            .plRepoId(plRepoId)
            .plInstanceId(plInstanceId)
            .plQuestionId(plQuestionId)
            .status(STATUS_PENDING)
            .url(null)
            .build();
    return plScaffoldAssessmentRepository.save(plScaffoldAssessment);
  }

  @Operation(summary = "Delete a PlScaffoldAssessment")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  public Object deletePlScaffoldAssessment(@Parameter(name = "id") @RequestParam Long id) {
    PlScaffoldAssessment plScaffoldAssessment =
        plScaffoldAssessmentRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(PlScaffoldAssessment.class, id));
    plScaffoldAssessmentRepository.delete(plScaffoldAssessment);
    return genericMessage("PlScaffoldAssessment with id %s deleted".formatted(id));
  }
}
