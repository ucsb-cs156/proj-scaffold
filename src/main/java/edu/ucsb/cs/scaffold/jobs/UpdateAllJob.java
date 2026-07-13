package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import lombok.Builder;

@Builder
public class UpdateAllJob implements JobContextConsumer {

  private final UpdateUserService updateUserService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    updateUserService.attachRosterStudentsAllUsers();
    ctx.log("Done");
  }
}
