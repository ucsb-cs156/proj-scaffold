package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
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
 * Read-only: PlInstances are created, updated, and deleted by the SyncPlRepo job (issues #45/#47),
 * which mirrors the repo's courseInstances directory on GitHub — not by POST/DELETE endpoints.
 */
@Tag(name = "PlInstance")
@RequestMapping("/api/plinstance")
@RestController
@Slf4j
public class PLInstanceController extends ApiController {

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PlRepoRepository plRepoRepository;

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

  private void ensurePlRepoExists(Long plRepoId) {
    plRepoRepository
        .findById(plRepoId)
        .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
  }
}
