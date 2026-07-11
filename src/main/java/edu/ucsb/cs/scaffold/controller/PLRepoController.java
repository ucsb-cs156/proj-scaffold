package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.regex.Pattern;
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

@Tag(name = "PlRepo")
@RequestMapping("/api/plrepo")
@RestController
@Slf4j
public class PLRepoController extends ApiController {

  // A GitHub owner/organization name: alphanumeric characters or hyphens, cannot begin or end
  // with a hyphen.
  private static final String OWNER_PATTERN = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?";

  // A GitHub repository name: alphanumeric characters, hyphens, underscores, or periods.
  private static final String REPO_PATTERN = "[A-Za-z0-9._-]{1,100}";

  private static final Pattern REPO_NAME_PATTERN =
      Pattern.compile("^" + OWNER_PATTERN + "/" + REPO_PATTERN + "$");

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PlQuestionRepository plQuestionRepository;

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Autowired private PlAssessmentRepository plAssessmentRepository;

  @Autowired private PlAssessmentQuestionRepository plAssessmentQuestionRepository;

  @Operation(summary = "List all PlRepos")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public Iterable<PlRepo> allPlRepos() {
    return plRepoRepository.findAll();
  }

  @Operation(summary = "Create a new PlRepo")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("")
  public PlRepo postPlRepo(@Parameter(name = "repoName") @RequestParam String repoName) {
    String trimmedRepoName = repoName.strip();
    validateRepoName(trimmedRepoName);

    if (plRepoRepository.existsByRepoName(trimmedRepoName)) {
      throw new IllegalArgumentException(
          "PlRepo with repoName %s already exists".formatted(trimmedRepoName));
    }

    PlRepo plRepo = PlRepo.builder().repoName(trimmedRepoName).build();
    return plRepoRepository.save(plRepo);
  }

  @Operation(
      summary =
          "Delete a PlRepo, cascading the delete to its PlInstances, PlQuestions, "
              + "PlScaffoldAssessments, and PlAssessments")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  @Transactional
  public Object deletePlRepo(@Parameter(name = "id") @RequestParam Long id) {
    PlRepo plRepo =
        plRepoRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, id));

    plAssessmentQuestionRepository.deleteByPlRepoId(id);
    plScaffoldAssessmentRepository.deleteByPlRepoId(id);
    plAssessmentRepository.deleteByPlRepoId(id);
    plInstanceRepository.deleteByPlRepoId(id);
    plQuestionRepository.deleteByPlRepoId(id);
    plRepoRepository.delete(plRepo);

    return genericMessage("PlRepo with id %s deleted".formatted(id));
  }

  private void validateRepoName(String repoName) {
    if (repoName.isBlank()) {
      throw new IllegalArgumentException("repoName is required");
    }
    if (!REPO_NAME_PATTERN.matcher(repoName).matches()) {
      throw new IllegalArgumentException(
          "repoName must be in the format owner/repo, e.g. PrairieLearn/pl-ucsb-cmpsc5b");
    }
  }
}
