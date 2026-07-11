package edu.ucsb.cs.scaffold.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.GithubService.DirectoryEntry;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import edu.ucsb.cs.scaffold.services.jobs.JobContextConsumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Syncs the local state for one PlRepo from GitHub (issues #45 and #47), using the PAT of the user
 * who launched the job ({@code userId}) since the app has no credential of its own that can reach
 * every repo.
 *
 * <p>Course instances: each subdirectory of the repo's {@code courseInstances} directory becomes a
 * PlInstance; PlInstance rows no longer present on GitHub are deleted (cascading to their
 * PlScaffoldAssessments and PlAssessments).
 *
 * <p>Questions: the {@code questions} directory is traversed recursively. A directory containing an
 * {@code info.json} file is a question whose questionId is its path relative to {@code questions}
 * (e.g. {@code questions/foo/bar/info.json} → questionId {@code foo/bar}); uuid and title come from
 * info.json. Directories named {@code __drafts__} are skipped at any depth, and question
 * directories are not traversed further (per PrairieLearn convention their subdirectories, like
 * {@code tests/}, belong to the question). PlQuestion rows no longer present on GitHub are deleted
 * (cascading to their PlScaffoldAssessments).
 *
 * <p>If the {@code courseInstances} or {@code questions} directory is missing (HTTP 404), that step
 * is skipped entirely — including deletions — because a 404 cannot distinguish "directory removed"
 * from "token cannot see the repo", and mass-deleting rows over a token problem would be wrong.
 */
@Builder
public class SyncPlRepoJob implements JobContextConsumer {

  static final String COURSE_INSTANCES_PATH = "courseInstances";
  static final String QUESTIONS_PATH = "questions";
  static final String DRAFTS_DIRECTORY = "__drafts__";
  static final String INFO_JSON = "info.json";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private long userId;
  private long plRepoId;
  private PatCredentialRepository patCredentialRepository;
  private PatEncryptionService patEncryptionService;
  private PlRepoRepository plRepoRepository;
  private PlInstanceRepository plInstanceRepository;
  private PlQuestionRepository plQuestionRepository;
  private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;
  private PlAssessmentRepository plAssessmentRepository;
  private GithubService githubService;

  /** The uuid and title of a question, as read from its info.json. */
  record QuestionInfo(UUID uuid, String title) {}

  @Override
  public void accept(JobContext ctx) throws Exception {
    PlRepo plRepo =
        plRepoRepository
            .findById(plRepoId)
            .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, plRepoId));
    ctx.log("Syncing repo %s (PlRepo id %d)".formatted(plRepo.getRepoName(), plRepoId));

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

    try {
      syncCourseInstances(ctx, plRepo, token);
      syncQuestions(ctx, plRepo, token);
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
      throw new Exception(
          "GitHub rejected the stored PAT (HTTP %d). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/PAT.md)"
              .formatted(e.getStatusCode().value()));
    }
  }

  private void syncCourseInstances(JobContext ctx, PlRepo plRepo, String token) {
    List<String> githubNames;
    try {
      githubNames =
          githubService.listSubdirectories(plRepo.getRepoName(), COURSE_INSTANCES_PATH, token);
    } catch (HttpClientErrorException.NotFound e) {
      ctx.log(
          "Repo %s has no %s directory (or the token cannot see the repo); skipping course instance sync"
              .formatted(plRepo.getRepoName(), COURSE_INSTANCES_PATH));
      return;
    }

    Map<String, PlInstance> existingByName = new LinkedHashMap<>();
    for (PlInstance instance : plInstanceRepository.findByPlRepoId(plRepoId)) {
      existingByName.put(instance.getName(), instance);
    }

    int added = 0;
    int unchanged = 0;
    for (String name : githubNames) {
      if (existingByName.containsKey(name)) {
        unchanged++;
        continue;
      }
      plInstanceRepository.save(PlInstance.builder().plRepoId(plRepoId).name(name).build());
      added++;
      ctx.log("Added course instance %s".formatted(name));
    }

    int deleted = 0;
    List<String> staleNames =
        existingByName.keySet().stream()
            .filter(name -> !githubNames.contains(name))
            .sorted()
            .toList();
    for (String name : staleNames) {
      PlInstance stale = existingByName.get(name);
      plScaffoldAssessmentRepository.deleteByPlInstanceId(stale.getId());
      plAssessmentRepository.deleteByPlInstanceId(stale.getId());
      plInstanceRepository.delete(stale);
      deleted++;
      ctx.log("Deleted course instance %s (no longer on GitHub)".formatted(name));
    }

    ctx.log(
        "Course instances: %d added, %d deleted, %d unchanged"
            .formatted(added, deleted, unchanged));
  }

  private void syncQuestions(JobContext ctx, PlRepo plRepo, String token) {
    Map<String, QuestionInfo> foundQuestions = new LinkedHashMap<>();
    try {
      walkQuestionsDirectory(ctx, plRepo, token, QUESTIONS_PATH, "", foundQuestions);
    } catch (HttpClientErrorException.NotFound e) {
      ctx.log(
          "Repo %s has no %s directory (or the token cannot see the repo); skipping question sync"
              .formatted(plRepo.getRepoName(), QUESTIONS_PATH));
      return;
    }

    Map<String, PlQuestion> existingByQuestionId = new LinkedHashMap<>();
    for (PlQuestion question : plQuestionRepository.findByPlRepoId(plRepoId)) {
      existingByQuestionId.put(question.getQuestionId(), question);
    }

    int added = 0;
    int updated = 0;
    int unchanged = 0;
    for (Map.Entry<String, QuestionInfo> entry : foundQuestions.entrySet()) {
      String questionId = entry.getKey();
      QuestionInfo info = entry.getValue();
      PlQuestion existing = existingByQuestionId.get(questionId);
      if (existing == null) {
        plQuestionRepository.save(
            PlQuestion.builder()
                .plRepoId(plRepoId)
                .questionId(questionId)
                .uuid(info.uuid())
                .title(info.title())
                .build());
        added++;
        ctx.log("Added question %s (%s)".formatted(questionId, info.title()));
      } else if (!existing.getUuid().equals(info.uuid())
          || !existing.getTitle().equals(info.title())) {
        existing.setUuid(info.uuid());
        existing.setTitle(info.title());
        plQuestionRepository.save(existing);
        updated++;
        ctx.log("Updated question %s (%s)".formatted(questionId, info.title()));
      } else {
        unchanged++;
      }
    }

    int deleted = 0;
    List<String> staleQuestionIds =
        existingByQuestionId.keySet().stream()
            .filter(questionId -> !foundQuestions.containsKey(questionId))
            .sorted()
            .toList();
    for (String questionId : staleQuestionIds) {
      PlQuestion stale = existingByQuestionId.get(questionId);
      plScaffoldAssessmentRepository.deleteByPlQuestionId(stale.getId());
      plQuestionRepository.delete(stale);
      deleted++;
      ctx.log("Deleted question %s (no longer on GitHub)".formatted(questionId));
    }

    ctx.log(
        "Questions: %d added, %d updated, %d deleted, %d unchanged"
            .formatted(added, updated, deleted, unchanged));
  }

  /**
   * Recursively walks a directory under {@code questions}. {@code questionId} is the path relative
   * to the questions directory ("" for the questions directory itself, which is never a question).
   */
  private void walkQuestionsDirectory(
      JobContext ctx,
      PlRepo plRepo,
      String token,
      String path,
      String questionId,
      Map<String, QuestionInfo> foundQuestions) {
    List<DirectoryEntry> entries = githubService.listDirectory(plRepo.getRepoName(), path, token);

    boolean hasInfoJson =
        entries.stream()
            .anyMatch(entry -> INFO_JSON.equals(entry.name()) && "file".equals(entry.type()));
    if (hasInfoJson && !questionId.isEmpty()) {
      String content =
          githubService.getFileContent(plRepo.getRepoName(), path + "/" + INFO_JSON, token);
      QuestionInfo info = parseInfoJson(ctx, questionId, content);
      if (info != null) {
        foundQuestions.put(questionId, info);
      }
      return; // a question directory's subdirectories belong to the question; don't recurse
    }

    for (DirectoryEntry entry : entries) {
      if (!"dir".equals(entry.type())) {
        continue;
      }
      if (DRAFTS_DIRECTORY.equals(entry.name())) {
        ctx.log("Skipping directory %s/%s".formatted(path, entry.name()));
        continue;
      }
      String childQuestionId =
          questionId.isEmpty() ? entry.name() : questionId + "/" + entry.name();
      walkQuestionsDirectory(
          ctx, plRepo, token, path + "/" + entry.name(), childQuestionId, foundQuestions);
    }
  }

  /** Returns the parsed uuid/title, or null (with a log message) if info.json is unusable. */
  private QuestionInfo parseInfoJson(JobContext ctx, String questionId, String content) {
    try {
      JsonNode node = MAPPER.readTree(content);
      if (!node.hasNonNull("uuid") || !node.hasNonNull("title")) {
        ctx.log("Skipping question %s: info.json is missing uuid or title".formatted(questionId));
        return null;
      }
      return new QuestionInfo(
          UUID.fromString(node.get("uuid").asText()), node.get("title").asText());
    } catch (Exception e) {
      ctx.log("Skipping question %s: could not parse info.json".formatted(questionId));
      return null;
    }
  }
}
