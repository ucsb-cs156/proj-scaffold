package edu.ucsb.cs.scaffold.services.jobs;

import edu.ucsb.cs.scaffold.entity.Course;

@FunctionalInterface
public interface JobContextConsumer {
  void accept(JobContext c) throws Exception;

  default Course getCourse() {
    return null;
  }
}
