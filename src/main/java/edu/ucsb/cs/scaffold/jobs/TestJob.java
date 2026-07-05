package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import edu.ucsb.cs.scaffold.services.jobs.JobContextConsumer;
import edu.ucsb.cs.scaffold.utilities.Sleep;
import lombok.Builder;

@Builder
public class TestJob implements JobContextConsumer {

  private boolean fail;
  private int sleepMs;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Hello World! from test job!");
    Sleep.sleepQuietly(sleepMs);
    if (fail) {
      throw new Exception("Fail!");
    }
    ctx.log("Goodbye from test job!");
  }
}
