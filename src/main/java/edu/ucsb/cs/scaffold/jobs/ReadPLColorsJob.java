package edu.ucsb.cs.scaffold.jobs;

import edu.ucsb.cs.scaffold.entity.PatCredential;
import edu.ucsb.cs.scaffold.entity.PlColor;
import edu.ucsb.cs.scaffold.enums.PatPlatform;
import edu.ucsb.cs.scaffold.repository.PatCredentialRepository;
import edu.ucsb.cs.scaffold.repository.PlColorRepository;
import edu.ucsb.cs.scaffold.services.GithubService;
import edu.ucsb.cs.scaffold.services.PatEncryptionService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Reads PrairieLearn's badge color map from GitHub (issue #96) and upserts the {@code pl_color}
 * table with any colors that are new or whose hex code has changed. Launched by an admin (POST
 * /api/jobs/launch/readPLColors), using that admin's own stored GitHub PAT.
 *
 * <p>PrairieLearn does not expose these colors through an API, so this job reads the raw {@code
 * colors.scss} stylesheet from the public PrairieLearn repo and locates the {@code $custom-colors:
 * ( 'name': #hex, ... )} Sass map with a regular expression, rather than a full SCSS parser.
 */
@Builder
public class ReadPLColorsJob implements JobContextConsumer {

  static final String PL_REPO_NAME = "PrairieLearn/PrairieLearn";
  static final String COLORS_SCSS_PATH = "apps/prairielearn/public/stylesheets/colors.scss";

  // Matches the $custom-colors: ( ... ); Sass map as a whole, non-greedily, so that only the
  // color map's own entries (and not any later parentheses in the file) are captured.
  static final Pattern COLOR_MAP_PATTERN =
      Pattern.compile("\\$custom-colors\\s*:\\s*\\((.*?)\\)\\s*;", Pattern.DOTALL);

  // Matches a single 'name': #hex entry within the color map.
  static final Pattern COLOR_ENTRY_PATTERN =
      Pattern.compile("['\"]([a-zA-Z0-9_-]+)['\"]\\s*:\\s*#([0-9a-fA-F]{3,8})");

  private long userId;
  private PatCredentialRepository patCredentialRepository;
  private PatEncryptionService patEncryptionService;
  private GithubService githubService;
  private PlColorRepository plColorRepository;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Reading PrairieLearn colors from %s".formatted(COLORS_SCSS_PATH));

    Optional<PatCredential> githubCredential =
        patCredentialRepository.findByUserIdAndPlatform(userId, PatPlatform.GITHUB);
    if (githubCredential.isEmpty()) {
      String message =
          "No GitHub PAT is set up for this user; set one up on the /profile page before running"
              + " this job";
      ctx.log(message);
      throw new Exception(message);
    }
    String githubToken =
        patEncryptionService.decrypt(
            githubCredential.get().getCiphertext(), githubCredential.get().getKeyVersion());

    String scss;
    try {
      scss = githubService.getFileContent(PL_REPO_NAME, COLORS_SCSS_PATH, githubToken);
    } catch (HttpClientErrorException e) {
      String message =
          "Could not read %s from %s (HTTP %d); the file may not exist or may not be accessible"
              .formatted(COLORS_SCSS_PATH, PL_REPO_NAME, e.getStatusCode().value());
      ctx.log(message);
      throw new Exception(message);
    }

    if (scss.isBlank()) {
      String message =
          "%s in %s is empty or could not be read".formatted(COLORS_SCSS_PATH, PL_REPO_NAME);
      ctx.log(message);
      throw new Exception(message);
    }

    Matcher mapMatcher = COLOR_MAP_PATTERN.matcher(scss);
    if (!mapMatcher.find()) {
      String message =
          "%s does not contain the expected $custom-colors color mapping"
              .formatted(COLORS_SCSS_PATH);
      ctx.log(message);
      throw new Exception(message);
    }

    String colorMapBody = mapMatcher.group(1);
    Matcher entryMatcher = COLOR_ENTRY_PATTERN.matcher(colorMapBody);

    int added = 0;
    int updated = 0;
    int unchanged = 0;
    int seen = 0;
    while (entryMatcher.find()) {
      seen++;
      String colorName = entryMatcher.group(1);
      String hexCode = "#" + entryMatcher.group(2).toLowerCase();

      Optional<PlColor> existing = plColorRepository.findById(colorName);
      if (existing.isEmpty()) {
        plColorRepository.save(PlColor.builder().colorName(colorName).hexCode(hexCode).build());
        added++;
        ctx.log("Added color %s: %s".formatted(colorName, hexCode));
      } else if (!existing.get().getHexCode().equalsIgnoreCase(hexCode)) {
        PlColor color = existing.get();
        String oldHexCode = color.getHexCode();
        color.setHexCode(hexCode);
        plColorRepository.save(color);
        updated++;
        ctx.log("Updated color %s: %s -> %s".formatted(colorName, oldHexCode, hexCode));
      } else {
        unchanged++;
      }
    }

    if (seen == 0) {
      String message =
          "%s did not contain any 'name': #hex color entries".formatted(COLORS_SCSS_PATH);
      ctx.log(message);
      throw new Exception(message);
    }

    ctx.log("Done: %d added, %d updated, %d unchanged".formatted(added, updated, unchanged));
  }
}
