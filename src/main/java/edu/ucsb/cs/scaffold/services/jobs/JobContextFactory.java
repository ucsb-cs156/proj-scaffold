package edu.ucsb.cs.scaffold.services.jobs;

import edu.ucsb.cs.scaffold.entity.Job;
import edu.ucsb.cs.scaffold.repository.JobsRepository;
import org.springframework.stereotype.Component;

@Component
public class JobContextFactory {
  private final JobsRepository repository;

  public JobContextFactory(JobsRepository repository) {
    this.repository = repository;
  }

  public JobContext createContext(Job job) {
    return new JobContext(repository, job);
  }
}
