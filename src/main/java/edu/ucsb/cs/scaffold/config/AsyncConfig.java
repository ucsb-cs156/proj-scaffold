package edu.ucsb.cs.scaffold.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Makes background jobs actually run in the background, one at a time, in submission order.
 *
 * <p>{@code @EnableAsync} is what makes Spring honor the {@code @Async} annotation on {@code
 * JobService.runJobAsync}. Without it, the annotation is silently ignored and every "async" job
 * runs synchronously inside the HTTP request thread — a long job (e.g. SyncPlRepoJob walking a
 * large repo) then blocks its launch request until the reverse proxy times out, while the work
 * keeps running server-side. (This bug is shared by every app using this jobs infrastructure; check
 * sibling apps for a missing {@code @EnableAsync}.)
 *
 * <p>The {@code jobExecutor} bean is deliberately a single thread with an unbounded FIFO queue:
 * jobs execute strictly one at a time, in the order they were submitted. That prevents two
 * concurrent jobs from racing each other (e.g. double-launching a sync inserting the same rows).
 * Known limitation: the queue is in-memory, so jobs still queued when the app shuts down are lost
 * and their Job rows remain in "running" status.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "jobExecutor")
  public ThreadPoolTaskExecutor jobExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setThreadNamePrefix("job-");
    return executor;
  }
}
