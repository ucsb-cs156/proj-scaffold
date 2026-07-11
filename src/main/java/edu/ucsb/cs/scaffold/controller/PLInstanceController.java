package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PlInstance")
@RequestMapping("/api/plinstance")
@RestController
@Slf4j
public class PLInstanceController extends ApiController {

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Autowired private PlAssessmentRepository plAssessmentRepository;

  @Operation(summary = "List all PlInstances")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("/all")
  public Iterable<PlInstance> allPlInstances() {
    return plInstanceRepository.findAll();
  }

  @Operation(summary = "List all PlInstances for a PlRepo")
  @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
  @GetMapping("")
  public List<PlInstance> allPlInstancesForRepo(
      @Parameter(name = "plrepoId") @RequestParam Long plrepoId) {
    ensurePlRepoExists(plrepoId);
    return plInstanceRepository.findByPlRepoId(plrepoId);
  }

  @Operation(
      summary =
          "Delete a PlInstance, cascading the delete to its PlScaffoldAssessments and"
              + " PlAssessments")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  @Transactional
  public Object deletePlInstance(@Parameter(name = "id") @RequestParam Long id) {
    PlInstance plInstance =
        plInstanceRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(PlInstance.class, id));

    plScaffoldAssessmentRepository.deleteByPlInstanceId(id);
    plAssessmentRepository.deleteByPlInstanceId(id);
    plInstanceRepository.delete(plInstance);

    return genericMessage("PlInstance with id %s deleted".formatted(id));
  }

  private void ensurePlRepoExists(Long plRepoId) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
  }
}
