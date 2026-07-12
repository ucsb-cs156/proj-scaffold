package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlAssessmentRepository;
import edu.ucsb.cs.scaffold.repository.PlInstanceRepository;
import edu.ucsb.cs.scaffold.repository.PlQuestionRepository;
import edu.ucsb.cs.scaffold.repository.PlRepoRepository;
import edu.ucsb.cs.scaffold.repository.PlScaffoldAssessmentRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs.scaffold.services.PrairieLearnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembles a {@link SyncCourseWithPlRepoJob} with its many repository/service dependencies, so the
 * two launch sites (the JobsController endpoint and the automatic launch after a successful PUT
 * /api/courses/updatePLInstance) don't each duplicate the wiring.
 */
@Component
public class SyncCourseWithPlRepoJobFactory {

  @Autowired private PatCredentialRepository patCredentialRepository;

  @Autowired private PatEncryptionService patEncryptionService;

  @Autowired private PlRepoRepository plRepoRepository;

  @Autowired private PlInstanceRepository plInstanceRepository;

  @Autowired private PlQuestionRepository plQuestionRepository;

  @Autowired private PlScaffoldAssessmentRepository plScaffoldAssessmentRepository;

  @Autowired private PlAssessmentRepository plAssessmentRepository;

  @Autowired private PlAssessmentQuestionRepository plAssessmentQuestionRepository;

  @Autowired private GithubService githubService;

  @Autowired private PrairieLearnService prairieLearnService;

  public SyncCourseWithPlRepoJob create(long userId, Course course) {
    return SyncCourseWithPlRepoJob.builder()
        .userId(userId)
        .course(course)
        .patCredentialRepository(patCredentialRepository)
        .patEncryptionService(patEncryptionService)
        .plRepoRepository(plRepoRepository)
        .plInstanceRepository(plInstanceRepository)
        .plQuestionRepository(plQuestionRepository)
        .plScaffoldAssessmentRepository(plScaffoldAssessmentRepository)
        .plAssessmentRepository(plAssessmentRepository)
        .plAssessmentQuestionRepository(plAssessmentQuestionRepository)
        .githubService(githubService)
        .prairieLearnService(prairieLearnService)
        .build();
  }
}
