package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PlQuestion")
@RequestMapping("/api/plQuestion")
@RestController
@Slf4j
public class PLQuestionController extends ApiController {

  @Autowired private PlQuestionRepository plQuestionRepository;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Operation(summary = "List all PlQuestions for a PlRepo")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public List<PlQuestion> allPlQuestions(
      @Parameter(name = "plrepoId") @RequestParam Long plrepoId) {
    ensurePlRepoExists(plrepoId);
    return plQuestionRepository.findByPlRepoId(plrepoId);
  }

  @Operation(summary = "Create a new PlQuestion")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("")
  public PlQuestion postPlQuestion(
      @Parameter(name = "plRepoId") @RequestParam Long plRepoId,
      @Parameter(name = "questionId") @RequestParam String questionId,
      @Parameter(name = "uuid") @RequestParam UUID uuid,
      @Parameter(name = "title") @RequestParam String title) {
    ensurePlRepoExists(plRepoId);

    String trimmedQuestionId = questionId.strip();
    if (trimmedQuestionId.isBlank()) {
      throw new IllegalArgumentException("questionId is required");
    }

    if (plQuestionRepository.existsByPlRepoIdAndQuestionId(plRepoId, trimmedQuestionId)) {
      throw new IllegalArgumentException(
          "PlQuestion with questionId %s already exists for plRepoId %s"
              .formatted(trimmedQuestionId, plRepoId));
    }

    PlQuestion plQuestion =
        PlQuestion.builder()
            .plRepoId(plRepoId)
            .questionId(trimmedQuestionId)
            .uuid(uuid)
            .title(title)
            .build();
    return plQuestionRepository.save(plQuestion);
  }

  @Operation(summary = "Delete a PlQuestion, cascading the delete to its PlScaffoldAssessments")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  @Transactional
  public Object deletePlQuestion(@Parameter(name = "id") @RequestParam Long id) {
    PlQuestion plQuestion =
        plQuestionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(PlQuestion.class, id));

    plScaffoldAssessmentRepository.deleteByPlQuestionId(id);
    plQuestionRepository.delete(plQuestion);

    return genericMessage("PlQuestion with id %s deleted".formatted(id));
  }

  private void ensurePlRepoExists(Long plRepoId) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
  }
}
