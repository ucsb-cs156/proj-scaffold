package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.services.UpdateUserService;
import edu.ucsb.cs.scaffold.services.jobs.JobContext;
import edu.ucsb.cs.scaffold.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class UpdateAllJob implements JobContextConsumer {

  private final UpdateUserService updateUserService;

  @Override
  public Course getCourse() {
    return null;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    updateUserService.attachRosterStudentsAllUsers();
    ctx.log("Done");
  }
}
