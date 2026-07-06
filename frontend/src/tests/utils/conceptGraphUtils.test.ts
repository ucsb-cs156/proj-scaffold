import { describe, test, expect } from "vitest";
import {
  normalize,
  toPastel,
  computeSubgraph,
} from "main/utils/conceptGraphUtils";

describe("utils/conceptGraphUtils", () => {
  describe("normalize", () => {
    test("replaces literal backslash-n sequences with real newlines", () => {
      expect(normalize("line one\\nline two")).toBe("line one\nline two");
    });

    test("replaces multiple occurrences", () => {
      expect(normalize("a\\nb\\nc")).toBe("a\nb\nc");
    });

    test("leaves strings with no backslash-n unchanged", () => {
      expect(normalize("no newlines here")).toBe("no newlines here");
    });
  });

  describe("toPastel", () => {
    test("returns the original color unchanged when strength is 1", () => {
      expect(toPastel("#000000", 1)).toBe("rgb(0, 0, 0)");
      expect(toPastel("#ffffff", 1)).toBe("rgb(255, 255, 255)");
    });

    test("returns white when strength is 0, regardless of input color", () => {
      expect(toPastel("#123456", 0)).toBe("rgb(255, 255, 255)");
    });

    test("uses the default strength of 0.1 when not specified", () => {
      // black at strength 0.1 -> channel = 0 * 0.1 + 255 * 0.9 = 229.5 -> rounds to 230
      expect(toPastel("#000000")).toBe("rgb(230, 230, 230)");
    });

    test("blends a mid color at a custom strength", () => {
      // #808080 = (128, 128, 128) at strength 0.5 -> 128*0.5 + 255*0.5 = 191.5 -> 192
      expect(toPastel("#808080", 0.5)).toBe("rgb(192, 192, 192)");
    });
  });

  describe("computeSubgraph", () => {
    test("returns just the tagged ids when they have no prerequisites", () => {
      const result = computeSubgraph(["data-types"]);
      expect(result).toEqual(new Set(["data-types"]));
    });

    test("walks upward through direct prerequisites", () => {
      // variables requires data-types (see prereqEdgeData)
      const result = computeSubgraph(["variables"]);
      expect(result.has("variables")).toBe(true);
      expect(result.has("data-types")).toBe(true);
    });

    test("walks upward transitively through multiple levels", () => {
      // nested-loops <- loops <- boolean-expr / arithmetic-ops <- data-types
      const result = computeSubgraph(["nested-loops"]);
      expect(result.has("nested-loops")).toBe(true);
      expect(result.has("loops")).toBe(true);
      expect(result.has("boolean-expr")).toBe(true);
      expect(result.has("arithmetic-ops")).toBe(true);
      expect(result.has("data-types")).toBe(true);
    });

    test("unions ancestors across multiple tagged ids without duplicates", () => {
      const result = computeSubgraph(["variables", "conditionals"]);
      expect(result.has("variables")).toBe(true);
      expect(result.has("conditionals")).toBe(true);
      expect(result.has("boolean-expr")).toBe(true);
      expect(result.has("data-types")).toBe(true);
      // Set semantics guarantee no duplicate entries for the shared ancestor
      expect([...result].filter((id) => id === "data-types")).toHaveLength(1);
    });

    test("returns an empty set for an empty input", () => {
      expect(computeSubgraph([])).toEqual(new Set());
    });
  });
});
