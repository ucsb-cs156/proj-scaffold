package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class MarkdownServiceTests {

  private final MarkdownService service = new MarkdownService();

  // clean: null and canonicalization

  @Test
  public void clean_returns_null_for_null_input() {
    assertNull(service.clean(null));
  }

  @Test
  public void clean_reformats_markdown_to_canonical_commonmark() {
    assertEquals("# Hello\n\nSome *text*  here", service.clean("#  Hello\n\nSome *text*  here"));
  }

  @Test
  public void clean_escapes_a_bare_less_than_in_plain_text() {
    // The '<' is escaped in Markdown syntax, but still renders as '<' for the user.
    assertEquals("if a \\< b: pass", service.clean("if a < b: pass"));
  }

  @Test
  public void clean_leaves_code_spans_untouched() {
    assertEquals("use `if a < b:` here", service.clean("use `if a < b:` here"));
  }

  @Test
  public void clean_leaves_fenced_code_blocks_untouched() {
    String fenced = "```python\nif a < b:\n    print('x')\n```";
    assertEquals(fenced, service.clean(fenced));
  }

  // clean: raw HTML sanitization

  @Test
  public void clean_removes_script_blocks_entirely() {
    assertEquals(
        "before\n\nafter", service.clean("before\n\n<script>alert('x')</script>\n\nafter"));
  }

  @Test
  public void clean_removes_inline_script_tags_leaving_only_their_text() {
    assertEquals(
        "hello alert('x') world", service.clean("hello <script>alert('x')</script> world"));
  }

  @Test
  public void clean_keeps_allowed_inline_html_tags() {
    // The sanitizer balances each inline fragment individually, so the opening tag
    // survives (self-closed); the important part is that nothing unsafe remains.
    assertEquals("hello <b></b>bold world", service.clean("hello <b>bold</b> world"));
  }

  @Test
  public void clean_keeps_an_allowed_html_block_with_its_class_attribute() {
    assertEquals("<div class=\"note\">ok</div>", service.clean("<div class=\"note\">ok</div>"));
  }

  @Test
  public void clean_strips_a_disallowed_attribute_from_an_html_block_that_stays_non_blank() {
    // The div element is allowed but onclick is not, so the sanitized literal differs from
    // the original (and is non-blank), which is what exercises htmlBlock.setLiteral(safe).
    assertEquals("<div>ok</div>", service.clean("<div onclick=\"evil()\">ok</div>"));
  }

  // clean: link and image destinations

  @Test
  public void clean_drops_javascript_link_destinations() {
    assertEquals("[click]()", service.clean("[click](javascript:alert(1))"));
  }

  @Test
  public void clean_keeps_https_link_destinations() {
    assertEquals("[site](https://example.com)", service.clean("[site](https://example.com)"));
  }

  @Test
  public void clean_keeps_relative_link_destinations() {
    assertEquals("[rel](/docs/page)", service.clean("[rel](/docs/page)"));
  }

  @Test
  public void clean_drops_javascript_image_destinations() {
    assertEquals("![alt]()", service.clean("![alt](javascript:alert(1))"));
  }

  @Test
  public void clean_sanitizes_a_link_nested_inside_an_images_alt_text() {
    // Exercises visitChildren(image): a Link inside the image's description is only
    // reached, and its javascript: destination only dropped, if the image's children
    // are visited.
    assertEquals(
        "![a [bad]() desc](https://ok.com/img.png)",
        service.clean("![a [bad](javascript:alert(1)) desc](https://ok.com/img.png)"));
  }

  @Test
  public void clean_sanitizes_an_image_nested_inside_a_links_text() {
    // Exercises visitChildren(link): an Image inside the link's text is only reached,
    // and its javascript: destination only dropped, if the link's children are visited.
    assertEquals(
        "[a ![img]() text](https://ok.com/page)",
        service.clean("[a ![img](javascript:alert(1)) text](https://ok.com/page)"));
  }

  // renderedLength

  @Test
  public void renderedLength_is_zero_for_null_input() {
    assertEquals(0, service.renderedLength(null));
  }

  @Test
  public void renderedLength_counts_plain_text() {
    assertEquals(11, service.renderedLength("hello world"));
  }

  @Test
  public void renderedLength_does_not_count_markdown_syntax() {
    // "Numeric (integers, floats)" is 26 characters; the ** markers don't count.
    assertEquals(26, service.renderedLength("**Numeric (integers, floats)**"));
  }

  @Test
  public void renderedLength_collapses_whitespace_runs_to_a_single_space() {
    // "Basic Data Types" = 16 characters, even though the source has a newline and
    // extra spaces (the style used by seed-data labels for line wrapping).
    assertEquals(16, service.renderedLength("Basic \n Data Types"));
  }

  @Test
  public void renderedLength_measures_the_rendered_text_of_structured_markdown() {
    // Renders as "Hello" + "Some text  here" -> collapsed to "Hello Some text here" = 20.
    assertEquals(20, service.renderedLength("#  Hello\n\nSome *text*  here"));
  }

  // safeDestination

  @Test
  public void safeDestination_allows_relative_urls_without_a_scheme() {
    assertEquals("/docs/page", MarkdownService.safeDestination("/docs/page"));
  }

  @Test
  public void safeDestination_allows_http_https_and_mailto() {
    assertEquals("http://a.com", MarkdownService.safeDestination("http://a.com"));
    assertEquals("https://a.com", MarkdownService.safeDestination("https://a.com"));
    assertEquals("mailto:a@b.com", MarkdownService.safeDestination("mailto:a@b.com"));
  }

  @Test
  public void safeDestination_is_case_insensitive_about_the_scheme() {
    assertEquals("HTTPS://A.COM", MarkdownService.safeDestination("HTTPS://A.COM"));
  }

  @Test
  public void safeDestination_drops_other_schemes() {
    assertEquals("", MarkdownService.safeDestination("javascript:alert(1)"));
    assertEquals("", MarkdownService.safeDestination("data:text/html,x"));
  }

  @Test
  public void safeDestination_drops_a_destination_whose_first_character_is_a_colon() {
    // The empty scheme before the colon is not an allowed scheme.
    assertEquals("", MarkdownService.safeDestination(":foo"));
  }
}
