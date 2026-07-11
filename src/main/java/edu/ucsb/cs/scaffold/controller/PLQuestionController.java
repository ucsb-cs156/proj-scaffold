package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
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
 * Read-only: PlQuestions are created, updated, and deleted by the SyncPlRepo job (issues #45/#47),
 * which mirrors the repo's questions directory on GitHub — not by POST/DELETE endpoints.
 */
@Tag(name = "PlQuestion")
@RequestMapping("/api/plQuestion")
@RestController
@Slf4j
public class PLQuestionController extends ApiController {

  @Autowired private PlQuestionRepository plQuestionRepository;

  @Autowired private PlRepoRepository plRepoRepository;

  @Operation(summary = "List all PlQuestions for a PlRepo")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public List<PlQuestion> allPlQuestions(
      @Parameter(name = "plrepoId") @RequestParam Long plrepoId) {
    ensurePlRepoExists(plrepoId);
    return plQuestionRepository.findByPlRepoId(plrepoId);
  }

  private void ensurePlRepoExists(Long plRepoId) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
  }
}
