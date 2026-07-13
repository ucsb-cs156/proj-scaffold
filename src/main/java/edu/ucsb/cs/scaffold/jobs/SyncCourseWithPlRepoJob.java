package edu.ucsb.cs.scaffold.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlAssessment;
import edu.ucsb.cs.scaffold.entity.PlAssessmentQuestion;
import edu.ucsb.cs.scaffold.entity.PlAssessmentSet;
import edu.ucsb.cs.scaffold.entity.PlInstance;
import edu.ucsb.cs.scaffold.entity.PlQuestion;
import edu.ucsb.cs.scaffold.entity.PlRepo;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentSetRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.GithubService.DirectoryEntry;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Syncs one course's PrairieLearn state (issue #69), replacing the repo-wide SyncPlRepoJob. Takes a
 * course (rather than a PlRepo id) and uses the launching user's GitHub and PrairieLearn PATs.
 *
 * <p>Before doing any work, the job verifies that the user has both PATs, that the course is
 * associated with a GitHub repo and a PrairieLearn course instance, and that the PATs can actually
 * reach both — read/write access to the repo, and access to the course instance via the
 * PrairieLearn API. Any failure terminates the job with a log message telling the user where to fix
 * it (the /profile page for PATs, the PrairieLearn tab of the course settings for the
 * associations).
 *
 * <p>Unlike the old job, the PlInstance table is not repopulated from the repo's courseInstances
 * directory: the course's instance already exists, and its shortName/longName are just
 * sanity-checked (and corrected) against what PrairieLearn reports. Assessments are synced only for
 * the course's own instance. Questions are still traversed for the whole repo, exactly as before:
 * the {@code questions} directory is walked recursively; a directory containing an {@code
 * info.json} is a question whose questionId is its path relative to {@code questions}; {@code
 * __drafts__} directories are skipped; question directories are not traversed further; stale
 * PlQuestion rows are deleted (cascading to their PlScaffoldAssessments and join rows).
 *
 * <p>Assessment sets (issue #93): the {@code assessmentSets} array of {@code
 * courseInstances/<instance>/infoCourseInstance.json} becomes the PlAssessmentSet rows of the
 * instance, matched by abbreviation. Stale rows are deleted.
 *
 * <p>Assessments: each subdirectory of {@code courseInstances/<instance>/assessments} containing an
 * {@code infoAssessment.json} becomes a PlAssessment; the {@code zones} key is walked recursively
 * and every {@code "id"} entry links the assessment to a PlQuestion of the repo (the
 * PlAssessmentQuestion join table, in zone order, rewritten on change). Stale rows are deleted.
 *
 * <p>Finally the job asks the PrairieLearn API for the instance's assessments and copies the fields
 * from issue #71 (numeric id, number, order, title, and the assessment-set
 * abbreviation/number/heading/color) onto the matching PlAssessment rows, matched by name.
 *
 * <p>If the {@code questions} or per-instance {@code assessments} directory is missing (HTTP 404),
 * that step is skipped entirely — including deletions — because a 404 cannot distinguish "directory
 * removed" from "token cannot see the repo", and mass-deleting rows over a token problem would be
 * wrong.
 */
@Builder
public class SyncCourseWithPlRepoJob implements JobContextConsumer {

  static final String COURSE_INSTANCES_PATH = "courseInstances";
  static final String QUESTIONS_PATH = "questions";
  static final String ASSESSMENTS_DIRECTORY = "assessments";
  static final String DRAFTS_DIRECTORY = "__drafts__";
  static final String INFO_JSON = "info.json";
  static final String INFO_ASSESSMENT_JSON = "infoAssessment.json";
  static final String INFO_COURSE_INSTANCE_JSON = "infoCourseInstance.json";
  static final String ASSESSMENT_SETS_KEY = "assessmentSets";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private long userId;
  private Course course;
  private PatCredentialRepository patCredentialRepository;
  private PatEncryptionService patEncryptionService;
  private PlRepoRepository plRepoRepository;
  private PlInstanceRepository plInstanceRepository;
  private PlQuestionRepository plQuestionRepository;
  private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;
  private PlAssessmentRepository plAssessmentRepository;
  private PlAssessmentQuestionRepository plAssessmentQuestionRepository;
  private PlAssessmentSetRepository plAssessmentSetRepository;
  private GithubService githubService;
  private PrairieLearnService prairieLearnService;

  /** The uuid and title of a question, as read from its info.json. */
  record QuestionInfo(UUID uuid, String title) {}

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return course.getId();
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log(
        "Syncing course %d (%s) with PrairieLearn"
            .formatted(course.getId(), course.getCourseName()));

    // (1) Both PATs must be configured before anything else.
    Optional<PatCredential> githubCredential =
        patCredentialRepository.findByUserIdAndPlatform(userId, PatPlatform.GITHUB);
    Optional<PatCredential> plCredential =
        patCredentialRepository.findByUserIdAndPlatform(userId, PatPlatform.PRAIRIELEARN);
    if (githubCredential.isEmpty() || plCredential.isEmpty()) {
      List<String> missing = new ArrayList<>();
      if (githubCredential.isEmpty()) {
        missing.add("GitHub PAT");
      }
      if (plCredential.isEmpty()) {
        missing.add("PrairieLearn PAT");
      }
      throw new Exception(
          "Missing %s: set it up on the /profile page before running this job"
              .formatted(String.join(" and ", missing)));
    }

    // (2) The course must be associated with a repo and a course instance.
    if (course.getPlRepoId() == null || course.getPlInstanceId() == null) {
      List<String> missing = new ArrayList<>();
      if (course.getPlRepoId() == null) {
        missing.add("a GitHub repo");
      }
      if (course.getPlInstanceId() == null) {
        missing.add("a PrairieLearn course instance");
      }
      throw new Exception(
          "This course is not associated with %s yet; set that up on the PrairieLearn tab of the course settings page"
              .formatted(String.join(" or ", missing)));
    }
    PlRepo plRepo =
        plRepoRepository
            .findById(course.getPlRepoId())
            .orElseThrow(() -> new EntityNotFoundException(PlRepo.class, course.getPlRepoId()));
    PlInstance plInstance =
        plInstanceRepository
            .findById(course.getPlInstanceId())
            .orElseThrow(
                () -> new EntityNotFoundException(PlInstance.class, course.getPlInstanceId()));
    if (plInstance.getNumericId() == null) {
      throw new Exception(
          "The course's PrairieLearn instance has no numeric id yet; re-associate it on the PrairieLearn tab of the course settings page");
    }

    String githubToken =
        patEncryptionService.decrypt(
            githubCredential.get().getCiphertext(), githubCredential.get().getKeyVersion());
    String plToken =
        patEncryptionService.decrypt(
            plCredential.get().getCiphertext(), plCredential.get().getKeyVersion());

    // (3) Sanity-check access with both PATs before touching any data.
    boolean canWrite;
    try {
      canWrite = githubService.hasWriteAccess(plRepo.getRepoName(), githubToken);
    } catch (HttpClientErrorException e) {
      throw new Exception(
          "The stored GitHub PAT cannot read repo %s (HTTP %d); check the token on the /profile page and the repo on the PrairieLearn tab"
              .formatted(plRepo.getRepoName(), e.getStatusCode().value()));
    }
    if (!canWrite) {
      throw new Exception(
          "The stored GitHub PAT has read-only access to repo %s; read/write access is required"
              .formatted(plRepo.getRepoName()));
    }
    PrairieLearnService.CourseInstanceInfo instanceInfo;
    try {
      instanceInfo = prairieLearnService.getCourseInstance(plInstance.getNumericId(), plToken);
    } catch (HttpClientErrorException e) {
      throw new Exception(
          "The stored PrairieLearn PAT cannot access course instance %d (HTTP %d); check the token on the /profile page"
              .formatted(plInstance.getNumericId(), e.getStatusCode().value()));
    }
    if (instanceInfo == null) {
      throw new Exception(
          "PrairieLearn returned no data for course instance %d"
              .formatted(plInstance.getNumericId()));
    }
    ctx.log(
        "Access verified: repo %s (read/write) and PrairieLearn instance %d"
            .formatted(plRepo.getRepoName(), plInstance.getNumericId()));

    // (4) Sanity-check the instance metadata instead of repopulating the PlInstance table.
    sanityCheckInstance(ctx, plInstance, instanceInfo);

    try {
      syncAssessmentSets(ctx, plRepo, plInstance, githubToken);
      syncQuestions(ctx, plRepo, githubToken);
      syncAssessments(ctx, plRepo, plInstance, githubToken);
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
      throw new Exception(
          "GitHub rejected the stored PAT (HTTP %d). The token may be expired, revoked, or not approved for this repo; enter a new one (see docs/Github_PAT.md)"
              .formatted(e.getStatusCode().value()));
    }

    // (5) Copy the PrairieLearn-side assessment fields onto the matching rows.
    enrichAssessmentsFromPrairieLearn(ctx, plRepo, plInstance, plToken);
  }

  /**
   * Keeps the stored shortName/longName in line with what PrairieLearn reports for the instance's
   * numeric id — the association was verified when it was created, so a difference here means the
   * instance was renamed on the PrairieLearn side.
   */
  private void sanityCheckInstance(
      JobContext ctx, PlInstance plInstance, PrairieLearnService.CourseInstanceInfo info) {
    boolean changed = false;
    if (!Objects.equals(info.shortName(), plInstance.getShortName())) {
      ctx.log(
          "Instance shortName changed on PrairieLearn: %s -> %s"
              .formatted(plInstance.getShortName(), info.shortName()));
      plInstance.setShortName(info.shortName());
      changed = true;
    }
    if (!Objects.equals(info.longName(), plInstance.getLongName())) {
      ctx.log(
          "Instance longName changed on PrairieLearn: %s -> %s"
              .formatted(plInstance.getLongName(), info.longName()));
      plInstance.setLongName(info.longName());
      changed = true;
    }
    if (changed) {
      plInstanceRepository.save(plInstance);
    } else {
      ctx.log("Instance %s metadata verified".formatted(plInstance.getShortName()));
    }
  }

  /**
   * Syncs the course instance's assessment sets (issue #93) from the {@code assessmentSets} array
   * of {@code courseInstances/<instance>/infoCourseInstance.json}. Each entry becomes a
   * PlAssessmentSet row, matched by abbreviation; stale rows (abbreviations no longer present) are
   * deleted. If the file is missing (HTTP 404) or cannot be parsed, the sync is skipped entirely so
   * a token/access problem cannot mass-delete rows.
   */
  private void syncAssessmentSets(
      JobContext ctx, PlRepo plRepo, PlInstance instance, String token) {
    String path =
        "%s/%s/%s"
            .formatted(COURSE_INSTANCES_PATH, instance.getShortName(), INFO_COURSE_INSTANCE_JSON);
    String content;
    try {
      content = githubService.getFileContent(plRepo.getRepoName(), path, token);
    } catch (HttpClientErrorException.NotFound e) {
      ctx.log(
          "Instance %s has no %s; skipping assessment set sync"
              .formatted(instance.getShortName(), INFO_COURSE_INSTANCE_JSON));
      return;
    }

    JsonNode root;
    try {
      root = MAPPER.readTree(content);
    } catch (Exception e) {
      ctx.log(
          "Skipping assessment set sync for instance %s: could not parse %s"
              .formatted(instance.getShortName(), INFO_COURSE_INSTANCE_JSON));
      return;
    }

    JsonNode assessmentSets = root.get(ASSESSMENT_SETS_KEY);
    if (assessmentSets == null || !assessmentSets.isArray()) {
      ctx.log(
          "Instance %s's %s has no %s array; skipping assessment set sync"
              .formatted(instance.getShortName(), INFO_COURSE_INSTANCE_JSON, ASSESSMENT_SETS_KEY));
      return;
    }

    Map<String, PlAssessmentSet> existingByAbbreviation = new LinkedHashMap<>();
    for (PlAssessmentSet set : plAssessmentSetRepository.findByPlInstanceId(instance.getId())) {
      existingByAbbreviation.put(set.getAbbreviation(), set);
    }

    int added = 0;
    int updated = 0;
    int unchanged = 0;
    Set<String> foundAbbreviations = new LinkedHashSet<>();
    for (JsonNode entry : assessmentSets) {
      String abbreviation = entry.path("abbreviation").asText(null);
      if (abbreviation == null) {
        continue;
      }
      String name = entry.path("name").asText(null);
      String heading = entry.path("heading").asText(null);
      String color = entry.path("color").asText(null);
      foundAbbreviations.add(abbreviation);

      PlAssessmentSet set = existingByAbbreviation.get(abbreviation);
      if (set == null) {
        plAssessmentSetRepository.save(
            PlAssessmentSet.builder()
                .plInstanceId(instance.getId())
                .abbreviation(abbreviation)
                .name(name)
                .heading(heading)
                .color(color)
                .build());
        added++;
      } else if (!Objects.equals(set.getName(), name)
          || !Objects.equals(set.getHeading(), heading)
          || !Objects.equals(set.getColor(), color)) {
        set.setName(name);
        set.setHeading(heading);
        set.setColor(color);
        plAssessmentSetRepository.save(set);
        updated++;
      } else {
        unchanged++;
      }
    }

    int deleted = 0;
    for (String abbreviation : existingByAbbreviation.keySet()) {
      if (!foundAbbreviations.contains(abbreviation)) {
        plAssessmentSetRepository.delete(existingByAbbreviation.get(abbreviation));
        deleted++;
      }
    }

    ctx.log(
        "Assessment sets: %d added, %d updated, %d deleted, %d unchanged"
            .formatted(added, updated, deleted, unchanged));
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
    for (PlQuestion question : plQuestionRepository.findByPlRepoId(plRepo.getId())) {
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
                .plRepoId(plRepo.getId())
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
      plAssessmentQuestionRepository.deleteByPlQuestionId(stale.getId());
      plQuestionRepository.delete(stale);
      deleted++;
      ctx.log("Deleted question %s (no longer on GitHub)".formatted(questionId));
    }

    ctx.log(
        "Questions: %d added, %d updated, %d deleted, %d unchanged"
            .formatted(added, updated, deleted, unchanged));
  }

  /** Syncs assessments from GitHub for the course's own instance only. */
  private void syncAssessments(JobContext ctx, PlRepo plRepo, PlInstance instance, String token) {
    Map<String, PlQuestion> questionsByQuestionId = new LinkedHashMap<>();
    for (PlQuestion question : plQuestionRepository.findByPlRepoId(plRepo.getId())) {
      questionsByQuestionId.put(question.getQuestionId(), question);
    }

    int added = 0;
    int deleted = 0;
    int unchanged = 0;
    String assessmentsPath =
        "%s/%s/%s".formatted(COURSE_INSTANCES_PATH, instance.getShortName(), ASSESSMENTS_DIRECTORY);
    List<DirectoryEntry> entries;
    try {
      entries = githubService.listDirectory(plRepo.getRepoName(), assessmentsPath, token);
    } catch (HttpClientErrorException.NotFound e) {
      ctx.log(
          "Instance %s has no %s directory; skipping assessment sync"
              .formatted(instance.getShortName(), ASSESSMENTS_DIRECTORY));
      return;
    }

    Map<String, PlAssessment> existingByName = new LinkedHashMap<>();
    for (PlAssessment assessment :
        plAssessmentRepository.findByPlRepoIdAndPlInstanceId(plRepo.getId(), instance.getId())) {
      existingByName.put(assessment.getName(), assessment);
    }

    Set<String> foundNames = new LinkedHashSet<>();
    for (DirectoryEntry entry : entries) {
      if (!"dir".equals(entry.type())) {
        continue;
      }
      String assessmentPath = assessmentsPath + "/" + entry.name();
      boolean hasInfoAssessment =
          githubService.listDirectory(plRepo.getRepoName(), assessmentPath, token).stream()
              .anyMatch(e -> INFO_ASSESSMENT_JSON.equals(e.name()) && "file".equals(e.type()));
      if (!hasInfoAssessment) {
        continue;
      }
      String content =
          githubService.getFileContent(
              plRepo.getRepoName(), assessmentPath + "/" + INFO_ASSESSMENT_JSON, token);
      JsonNode root;
      try {
        root = MAPPER.readTree(content);
      } catch (Exception e) {
        ctx.log(
            "Skipping assessment %s (instance %s): could not parse %s"
                .formatted(entry.name(), instance.getShortName(), INFO_ASSESSMENT_JSON));
        continue;
      }
      foundNames.add(entry.name());

      PlAssessment assessment = existingByName.get(entry.name());
      if (assessment == null) {
        assessment =
            plAssessmentRepository.save(
                PlAssessment.builder()
                    .plRepoId(plRepo.getId())
                    .plInstanceId(instance.getId())
                    .name(entry.name())
                    .build());
        added++;
        ctx.log(
            "Added assessment %s (instance %s)".formatted(entry.name(), instance.getShortName()));
      } else {
        unchanged++;
      }

      // get("zones") is null when the key is absent; collectQuestionIds treats that as no links
      syncAssessmentQuestions(
          ctx, plRepo, instance, assessment, root.get("zones"), questionsByQuestionId);
    }

    List<String> staleNames =
        existingByName.keySet().stream()
            .filter(name -> !foundNames.contains(name))
            .sorted()
            .toList();
    for (String name : staleNames) {
      PlAssessment stale = existingByName.get(name);
      plAssessmentQuestionRepository.deleteByPlAssessmentId(stale.getId());
      plAssessmentRepository.delete(stale);
      deleted++;
      ctx.log(
          "Deleted assessment %s (instance %s) (no longer on GitHub)"
              .formatted(name, instance.getShortName()));
    }

    ctx.log("Assessments: %d added, %d deleted, %d unchanged".formatted(added, deleted, unchanged));
  }

  /**
   * Copies the PrairieLearn-side fields (issue #71) onto the PlAssessment rows of this instance,
   * matched by assessment name. A PrairieLearn assessment with no matching repo row (or the
   * reverse) is logged and left alone; the GitHub sync is the source of truth for which rows exist.
   */
  private void enrichAssessmentsFromPrairieLearn(
      JobContext ctx, PlRepo plRepo, PlInstance plInstance, String plToken) {
    List<PrairieLearnService.AssessmentInfo> plAssessments;
    try {
      plAssessments = prairieLearnService.getAssessments(plInstance.getNumericId(), plToken);
    } catch (HttpClientErrorException e) {
      ctx.log(
          "Could not list assessments from PrairieLearn (HTTP %d); skipping assessment field updates"
              .formatted(e.getStatusCode().value()));
      return;
    }

    Map<String, PlAssessment> existingByName = new LinkedHashMap<>();
    for (PlAssessment assessment :
        plAssessmentRepository.findByPlRepoIdAndPlInstanceId(plRepo.getId(), plInstance.getId())) {
      existingByName.put(assessment.getName(), assessment);
    }

    int updated = 0;
    int unmatched = 0;
    for (PrairieLearnService.AssessmentInfo info : plAssessments) {
      PlAssessment assessment = existingByName.get(info.assessmentName());
      if (assessment == null) {
        unmatched++;
        ctx.log(
            "PrairieLearn assessment %s has no matching assessment directory in the repo; skipping"
                .formatted(info.assessmentName()));
        continue;
      }
      assessment.setPlAssessmentId(info.assessmentId());
      assessment.setPlAssessmentNumber(info.assessmentNumber());
      assessment.setPlAssessmentOrder(info.assessmentOrderBy());
      assessment.setPlAssessmentTitle(info.title());
      assessment.setPlAssessmentSetAbbreviation(info.assessmentSetAbbreviation());
      assessment.setPlAssessmentSetNumber(info.assessmentSetNumber());
      assessment.setPlAssessmentSetHeading(info.assessmentSetHeading());
      assessment.setPlAssessmentSetColor(info.assessmentSetColor());
      plAssessmentRepository.save(assessment);
      updated++;
      ctx.log("Updated PrairieLearn fields for assessment %s".formatted(info.assessmentName()));
    }
    ctx.log(
        "PrairieLearn assessment fields: %d updated, %d without a matching repo assessment"
            .formatted(updated, unmatched));
  }

  /**
   * Rewrites the assessment's question list (join rows, in zone order) to match the ids referenced
   * by the zones node of its infoAssessment.json. Ids that don't match any PlQuestion of this repo
   * are logged and skipped. If the list already matches, the rows are left untouched.
   */
  private void syncAssessmentQuestions(
      JobContext ctx,
      PlRepo plRepo,
      PlInstance instance,
      PlAssessment assessment,
      JsonNode zones,
      Map<String, PlQuestion> questionsByQuestionId) {
    Set<String> referencedIds = new LinkedHashSet<>();
    collectQuestionIds(zones, referencedIds);

    List<Long> desiredQuestionRowIds = new ArrayList<>();
    for (String questionId : referencedIds) {
      PlQuestion question = questionsByQuestionId.get(questionId);
      if (question == null) {
        ctx.log(
            "Assessment %s (instance %s) references unknown question id %s; skipping that link"
                .formatted(assessment.getName(), instance.getShortName(), questionId));
        continue;
      }
      desiredQuestionRowIds.add(question.getId());
    }

    List<Long> currentQuestionRowIds =
        plAssessmentQuestionRepository
            .findByPlAssessmentIdOrderByOrdinalAsc(assessment.getId())
            .stream()
            .map(PlAssessmentQuestion::getPlQuestionId)
            .toList();
    if (currentQuestionRowIds.equals(desiredQuestionRowIds)) {
      return;
    }

    plAssessmentQuestionRepository.deleteByPlAssessmentId(assessment.getId());
    // flush so the deletes hit the database before the re-inserts; Hibernate otherwise orders
    // inserts first within the transaction, violating the (assessment, question) unique constraint
    plAssessmentQuestionRepository.flush();
    for (int i = 0; i < desiredQuestionRowIds.size(); i++) {
      plAssessmentQuestionRepository.save(
          PlAssessmentQuestion.builder()
              .plRepoId(plRepo.getId())
              .plAssessmentId(assessment.getId())
              .plQuestionId(desiredQuestionRowIds.get(i))
              .ordinal(i)
              .build());
    }
    ctx.log(
        "Linked %d question(s) to assessment %s (instance %s)"
            .formatted(
                desiredQuestionRowIds.size(), assessment.getName(), instance.getShortName()));
  }

  /**
   * Recursively collects the value of every {@code "id"} key in the JSON tree under the zones node
   * of an infoAssessment.json — each one references a question — preserving document order and
   * dropping duplicates.
   */
  static void collectQuestionIds(JsonNode node, Set<String> ids) {
    if (node == null) {
      return;
    }
    if (node.isObject() && node.hasNonNull("id")) {
      ids.add(node.get("id").asText());
    }
    for (JsonNode child : node) {
      collectQuestionIds(child, ids);
    }
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
