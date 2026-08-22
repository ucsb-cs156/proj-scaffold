package edu.ucsb.cs.scaffold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ScaffoldApplication {

  /*
   * A plain `new RestTemplate()` has NO timeout at all: a hung GitHub/PrairieLearn call (network
   * blip, rate-limit weirdness, etc.) blocks the calling thread forever. That's a real incident
   * this app hit: a job stuck this way permanently occupied the single-threaded jobsExecutor,
   * wedging every job queued behind it, with no way to recover short of restarting the app (see
   * lib-jobs DESIGN.md 9 -- cooperative job cancellation only helps a job that reaches another
   * checkpoint, which a truly hung thread never will). Generous but finite: long enough to never
   * trip on legitimate slowness (ApiRetryHelper already retries 502s with backoff up to 32s
   * between attempts), short enough to guarantee a job can't hang forever.
   */
  private static final int CONNECT_TIMEOUT_MS = 10_000;

  private static final int READ_TIMEOUT_MS = 60_000;

  public static void main(String[] args) {
    SpringApplication.run(ScaffoldApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
