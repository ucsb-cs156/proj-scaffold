package edu.ucsb.cs.scaffold.services;

import java.util.Set;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

/**
 * Cleans user-supplied Markdown before it is stored. Markdown itself has no invalid syntax (every
 * string is a valid CommonMark document), so "linting" takes the form of reformatting to canonical
 * CommonMark. The security-relevant part is sanitizing raw HTML embedded in the Markdown: only the
 * HTML nodes of the parsed document are run through the OWASP sanitizer, so code samples such as
 * <code>if a &lt; b:</code> in text or code blocks are left untouched.
 */
@Service
public class MarkdownService {

  private static final Set<String> ALLOWED_LINK_SCHEMES = Set.of("http", "https", "mailto");

  private static final PolicyFactory HTML_POLICY =
      new HtmlPolicyBuilder()
          .allowElements(
              "p",
              "b",
              "i",
              "em",
              "strong",
              "a",
              "h1",
              "h2",
              "h3",
              "h4",
              "h5",
              "h6",
              "ul",
              "ol",
              "li",
              "code",
              "pre",
              "blockquote",
              "hr",
              "br",
              "table",
              "thead",
              "tbody",
              "tr",
              "th",
              "td",
              "span",
              "div",
              "img")
          .allowAttributes("href")
          .onElements("a")
          .allowAttributes("class")
          .onElements("code", "span", "div")
          .allowAttributes("src", "alt")
          .onElements("img")
          .allowUrlProtocols("http", "https", "mailto")
          .requireRelNofollowOnLinks()
          .toFactory();

  private final Parser parser = Parser.builder().build();
  private final MarkdownRenderer markdownRenderer = MarkdownRenderer.builder().build();
  private final TextContentRenderer textContentRenderer = TextContentRenderer.builder().build();
  private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

  /**
   * Sanitizes raw HTML embedded in the Markdown (dropping anything the OWASP policy rejects,
   * including scripts and javascript: URLs), removes unsafe link/image destinations, and reformats
   * the result to canonical CommonMark.
   *
   * @param markdown the raw Markdown, may be null
   * @return the cleaned Markdown, or null if the input was null
   */
  public String clean(String markdown) {
    if (markdown == null) {
      return null;
    }
    Node document = parser.parse(markdown);
    document.accept(
        new AbstractVisitor() {
          @Override
          public void visit(HtmlBlock htmlBlock) {
            String safe = HTML_POLICY.sanitize(htmlBlock.getLiteral());
            if (safe.isBlank()) {
              htmlBlock.unlink();
            } else {
              htmlBlock.setLiteral(safe);
            }
          }

          @Override
          public void visit(HtmlInline htmlInline) {
            String safe = HTML_POLICY.sanitize(htmlInline.getLiteral());
            if (safe.isBlank()) {
              htmlInline.unlink();
            } else {
              htmlInline.setLiteral(safe);
            }
          }

          @Override
          public void visit(Link link) {
            link.setDestination(safeDestination(link.getDestination()));
            visitChildren(link);
          }

          @Override
          public void visit(Image image) {
            image.setDestination(safeDestination(image.getDestination()));
            visitChildren(image);
          }
        });
    return markdownRenderer.render(document).strip();
  }

  /**
   * Like {@link #clean}, but for short single-line-ish fields such as concept/subconcept labels,
   * which have no legitimate use for embedded raw HTML. Rather than sanitizing (and possibly
   * dropping) HTML-looking fragments, any raw HTML is turned into literal text, so that CS notation
   * such as {@code List<Integer>} or {@code Node<T>} round-trips intact instead of being silently
   * stripped by the HTML sanitizer (which can otherwise reduce a label to nothing and make it fail
   * the "label may not be empty" check). The result is still safe to render with {@link
   * #toInlineHtml}, since that runs its own HTML sanitization pass at display time.
   *
   * @param markdown the raw Markdown, may be null
   * @return the cleaned Markdown, or null if the input was null
   */
  public String cleanLabel(String markdown) {
    if (markdown == null) {
      return null;
    }
    Node document = parser.parse(markdown);
    document.accept(
        new AbstractVisitor() {
          @Override
          public void visit(HtmlBlock htmlBlock) {
            Paragraph replacement = new Paragraph();
            replacement.appendChild(new Text(htmlBlock.getLiteral()));
            htmlBlock.insertAfter(replacement);
            htmlBlock.unlink();
          }

          @Override
          public void visit(HtmlInline htmlInline) {
            htmlInline.insertAfter(new Text(htmlInline.getLiteral()));
            htmlInline.unlink();
          }

          @Override
          public void visit(Link link) {
            link.setDestination(safeDestination(link.getDestination()));
            visitChildren(link);
          }

          @Override
          public void visit(Image image) {
            image.setDestination(safeDestination(image.getDestination()));
            visitChildren(image);
          }
        });
    return markdownRenderer.render(document).strip();
  }

  /**
   * Renders Markdown to HTML for direct display (e.g. via {@code dangerouslySetInnerHTML} on the
   * frontend). The rendered HTML is itself run through the OWASP sanitizer before being returned,
   * so the result is safe to inject even if the stored Markdown was never cleaned (e.g. seed data
   * inserted directly via SQL).
   *
   * @param markdown the raw Markdown, may be null
   * @return sanitized HTML, or null if the input was null
   */
  public String toHtml(String markdown) {
    if (markdown == null) {
      return null;
    }
    String html = htmlRenderer.render(parser.parse(markdown));
    return HTML_POLICY.sanitize(html).strip();
  }

  /**
   * Like {@link #toHtml}, but for short, single-line Markdown meant to be displayed inline (e.g. a
   * concept label shown inside a {@code <span>}): the block-level {@code <p>} wrapper CommonMark
   * would normally add around a single paragraph is stripped, since nesting a block element inside
   * an inline container is invalid HTML.
   *
   * @param markdown the raw Markdown, may be null
   * @return sanitized inline HTML, or null if the input was null
   */
  public String toInlineHtml(String markdown) {
    String html = toHtml(markdown);
    // == -1 rather than < 0: indexOf with a fromIndex of 3 can only return -1 or >= 3, so the
    // two are equivalent, but < 0 leaves an unkillable "boundary" mutant (<= 0) for pitest.
    if (html != null
        && html.startsWith("<p>")
        && html.endsWith("</p>")
        && html.indexOf("<p>", "<p>".length()) == -1) {
      return html.substring("<p>".length(), html.length() - "</p>".length());
    }
    return html;
  }

  /**
   * Returns the length of the plain text this Markdown renders to, with runs of whitespace
   * collapsed to a single space. Used to enforce limits on the <i>rendered</i> length of a field
   * independent of how verbose the Markdown markup is.
   */
  public int renderedLength(String markdown) {
    if (markdown == null) {
      return 0;
    }
    String text = textContentRenderer.render(parser.parse(markdown));
    return text.strip().replaceAll("\\s+", " ").length();
  }

  /** Allows relative destinations and http/https/mailto; anything else is dropped. */
  static String safeDestination(String destination) {
    int colon = destination.indexOf(':');
    if (colon < 0) {
      return destination;
    }
    String scheme = destination.substring(0, colon).toLowerCase();
    return ALLOWED_LINK_SCHEMES.contains(scheme) ? destination : "";
  }
}
