package edu.ucsb.cs.scaffold.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import org.junit.jupiter.api.Test;

public class TestJobTests {

  @Test
  void test_TestJob_succeeds() throws Exception {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    TestJob job = TestJob.builder().fail(false).sleepMs(0).build();
    job.accept(ctx);

    String expected = """
        Hello World! from test job!
        Goodbye from test job!""";
    assertEquals(expected, jobStarted.getLog());
  }

  @Test
  void test_TestJob_fails() {
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    TestJob job = TestJob.builder().fail(true).sleepMs(0).build();

    Exception thrown = assertThrows(Exception.class, () -> job.accept(ctx));
    assertEquals("Fail!", thrown.getMessage());
    assertEquals("Hello World! from test job!", jobStarted.getLog());
  }
}
