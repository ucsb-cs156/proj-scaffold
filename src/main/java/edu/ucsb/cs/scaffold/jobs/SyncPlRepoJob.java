package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import edu.ucsb.cs.scaffold.services.jobs.JobContextConsumer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Populates the PlInstance table for one PlRepo from GitHub: each subdirectory of the repo's {@code
 * courseInstances} directory becomes a PlInstance (issue #45).
 *
 * <p>The job runs with the PAT of the user who launched it ({@code userId}), since the app has no
 * credential of its own that can reach every repo. Names already in the table are kept as-is; names
 * in the table but no longer on GitHub are reported in the job log but deliberately not deleted
 * (deleting would have to cascade to assessments — out of scope for this proof of concept).
 */
@Builder
public class SyncPlRepoJob implements JobContextConsumer {

  static final String COURSE_INSTANCES_PATH = "courseInstances";

  private long userId;
  private long plRepoId;
  private PatCredentialRepository patCredentialRepository;
  private PatEncryptionService patEncryptionService;
  private PlRepoRepository plRepoRepository;
  private PlInstanceRepository plInstanceRepository;
  private GithubService githubService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    PlRepo plRepo =
        plRepoRepository
            .findById(plRepoId)
            .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
    ctx.log(
        "Syncing course instances for repo %s (PlRepo id %d)"
            .formatted(plRepo.getRepoName(), plRepoId));

    PatCredential credential =
        patCredentialRepository
            .findByUserId(userId)
            .orElseThrow(
                () ->
                    new Exception(
                        "No PAT is stored for user id %d; enter one first (see docs/PAT.md)"
                            .formatted(userId)));
    String token =
        patEncryptionService.decrypt(credential.getCiphertext(), credential.getKeyVersion());

    List<String> githubNames;
    try {
      githubNames =
          githubService.listSubdirectories(plRepo.getRepoName(), COURSE_INSTANCES_PATH, token);
    } catch (HttpClientErrorException.NotFound e) {
      ctx.log(
          "Repo %s has no %s directory (or the token cannot see the repo); nothing to sync"
              .formatted(plRepo.getRepoName(), COURSE_INSTANCES_PATH));
      return;
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
      throw new Exception(
          "GitHub rejected the stored PAT (HTTP %d). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)"
              .formatted(e.getStatusCode().value()));
    }

    Set<String> existingNames =
        plInstanceRepository.findByPlRepoId(plRepoId).stream()
            .map(PlInstance::getName)
            .collect(Collectors.toSet());

    int added = 0;
    int alreadyPresent = 0;
    for (String name : githubNames) {
      if (existingNames.contains(name)) {
        alreadyPresent++;
        continue;
      }
      plInstanceRepository.save(PlInstance.builder().plRepoId(plRepoId).name(name).build());
      added++;
      ctx.log("Added course instance %s".formatted(name));
    }

    List<String> stale =
        existingNames.stream().filter(name -> !githubNames.contains(name)).sorted().toList();
    if (!stale.isEmpty()) {
      ctx.log(
          "Note: %d course instance(s) in the database but not on GitHub (left untouched): %s"
              .formatted(stale.size(), String.join(", ", stale)));
    }

    ctx.log("Done: %d added, %d already present".formatted(added, alreadyPresent));
  }
}
