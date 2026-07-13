package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class SyncCourseWithPlRepoJobFactoryTests {

  @Test
  public void create_wires_every_dependency_and_the_user_and_course_into_the_job() {
    SyncCourseWithPlRepoJobFactory factory = new SyncCourseWithPlRepoJobFactory();
    PatCredentialRepository patCredentialRepository = mock(PatCredentialRepository.class);
    PatEncryptionService patEncryptionService = mock(PatEncryptionService.class);
    PlRepoRepository plRepoRepository = mock(PlRepoRepository.class);
    PlInstanceRepository plInstanceRepository = mock(PlInstanceRepository.class);
    PlQuestionRepository plQuestionRepository = mock(PlQuestionRepository.class);
    PlScaffoldAssessmentRepository plScaffoldAssessmentRepository =
        mock(PlScaffoldAssessmentRepository.class);
    PlAssessmentRepository plAssessmentRepository = mock(PlAssessmentRepository.class);
    PlAssessmentQuestionRepository plAssessmentQuestionRepository =
        mock(PlAssessmentQuestionRepository.class);
    GithubService githubService = mock(GithubService.class);
    PrairieLearnService prairieLearnService = mock(PrairieLearnService.class);

    ReflectionTestUtils.setField(factory, "patCredentialRepository", patCredentialRepository);
    ReflectionTestUtils.setField(factory, "patEncryptionService", patEncryptionService);
    ReflectionTestUtils.setField(factory, "plRepoRepository", plRepoRepository);
    ReflectionTestUtils.setField(factory, "plInstanceRepository", plInstanceRepository);
    ReflectionTestUtils.setField(factory, "plQuestionRepository", plQuestionRepository);
    ReflectionTestUtils.setField(
        factory, "plScaffoldAssessmentRepository", plScaffoldAssessmentRepository);
    ReflectionTestUtils.setField(factory, "plAssessmentRepository", plAssessmentRepository);
    ReflectionTestUtils.setField(
        factory, "plAssessmentQuestionRepository", plAssessmentQuestionRepository);
    ReflectionTestUtils.setField(factory, "githubService", githubService);
    ReflectionTestUtils.setField(factory, "prairieLearnService", prairieLearnService);

    Course course = Course.builder().id(20L).courseName("CS156").build();
    SyncCourseWithPlRepoJob job = factory.create(7L, course);

    assertEquals(7L, ReflectionTestUtils.getField(job, "userId"));
    assertSame(course, ReflectionTestUtils.getField(job, "course"));
    assertEquals(course.getId(), job.getScopeId());
    assertSame(
        patCredentialRepository, ReflectionTestUtils.getField(job, "patCredentialRepository"));
    assertSame(patEncryptionService, ReflectionTestUtils.getField(job, "patEncryptionService"));
    assertSame(plRepoRepository, ReflectionTestUtils.getField(job, "plRepoRepository"));
    assertSame(plInstanceRepository, ReflectionTestUtils.getField(job, "plInstanceRepository"));
    assertSame(plQuestionRepository, ReflectionTestUtils.getField(job, "plQuestionRepository"));
    assertSame(
        plScaffoldAssessmentRepository,
        ReflectionTestUtils.getField(job, "plScaffoldAssessmentRepository"));
    assertSame(plAssessmentRepository, ReflectionTestUtils.getField(job, "plAssessmentRepository"));
    assertSame(
        plAssessmentQuestionRepository,
        ReflectionTestUtils.getField(job, "plAssessmentQuestionRepository"));
    assertSame(githubService, ReflectionTestUtils.getField(job, "githubService"));
    assertSame(prairieLearnService, ReflectionTestUtils.getField(job, "prairieLearnService"));
  }
}
