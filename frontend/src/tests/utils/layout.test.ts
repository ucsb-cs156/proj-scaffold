import { describe, test, expect } from "vitest";
import { MarkerType, Position } from "@xyflow/react";
import { buildGraphElements, buildGraphElementsV2 } from "main/utils/layout";
import { majorConcepts, prereqEdgeData } from "main/data/conceptGraph";
import { positions } from "main/data/conceptGraphPositions";

describe("utils/layout", () => {
  describe("buildGraphElements", () => {
    test("creates one node per major concept", () => {
      const { nodes } = buildGraphElements(positions);
      expect(nodes).toHaveLength(majorConcepts.length);
    });

    test("each node carries the concept's id, type, and data", () => {
      const { nodes } = buildGraphElements(positions);
      majorConcepts.forEach((concept) => {
        const node = nodes.find((n) => n.id === concept.name);
        expect(node).toBeDefined();
        expect(node?.type).toBe("major");
        expect(node?.sourcePosition).toBe(Position.Top);
        expect(node?.targetPosition).toBe(Position.Bottom);
        expect(node?.data).toEqual({
          label: concept.label,
          color: concept.color,
          subconcepts: concept.subconcepts,
        });
      });
    });

    test("places each node at its position from the provided positions map", () => {
      const { nodes } = buildGraphElements(positions);
      const recursionNode = nodes.find((n) => n.id === "recursion");
      expect(recursionNode?.position).toEqual(positions["recursion"]);
    });

    test("defaults a node's position to the origin when it is missing from the positions map", () => {
      const customPositions: Record<string, { x: number; y: number }> = {
        ...positions,
      };
      delete customPositions["recursion"];

      const { nodes } = buildGraphElements(customPositions);
      const recursionNode = nodes.find((n) => n.id === "recursion");
      expect(recursionNode?.position).toEqual({ x: 0, y: 0 });
    });

    test("creates one edge per prerequisite relationship", () => {
      const { edges } = buildGraphElements(positions);
      expect(edges).toHaveLength(prereqEdgeData.length);
    });

    test("each edge connects the correct source and target with the expected shape", () => {
      const { edges } = buildGraphElements(positions);
      const { source, target } = prereqEdgeData[0];
      const edge = edges.find(
        (e) => e.source === source && e.target === target,
      );
      const sourceColor = majorConcepts.find((c) => c.name === source)?.color;

      expect(edge).toBeDefined();
      expect(edge?.id).toBe(`prereq-${source}-${target}`);
      expect(edge?.sourceHandle).toBe("top");
      expect(edge?.targetHandle).toBe("bottom");
      expect(edge?.type).toBe("smoothstep");
      expect(edge?.style).toEqual({ stroke: sourceColor, strokeWidth: 4 });
      expect(edge?.markerEnd).toEqual({
        type: MarkerType.ArrowClosed,
        color: sourceColor,
      });
    });
  });

  describe("buildGraphElementsV2", () => {
    const sampleConcepts = [
      {
        name: "a",
        labelHtml: "A",
        color: "#111111",
        subconcepts: [{ id: 101, parentId: 1, labelHtml: "sub-a1" }],
      },
      { name: "b", labelHtml: "B", color: "#222222", subconcepts: [] },
    ];
    const sampleEdges = [{ source: "a", target: "b" }];
    const samplePositions = { a: { x: 10, y: 20 }, b: { x: 30, y: 40 } };

    test("creates one node per supplied major concept, using the supplied data (not the hardcoded import)", () => {
      const { nodes } = buildGraphElementsV2(
        samplePositions,
        sampleConcepts,
        sampleEdges,
      );
      expect(nodes).toHaveLength(2);
      expect(nodes.map((n) => n.id).sort()).toEqual(["a", "b"]);
      expect(nodes[0].data).toEqual({
        labelHtml: "A",
        color: "#111111",
        subconcepts: [{ id: 101, parentId: 1, labelHtml: "sub-a1" }],
      });
    });

    test("places each node at its position from the supplied positions map", () => {
      const { nodes } = buildGraphElementsV2(
        samplePositions,
        sampleConcepts,
        sampleEdges,
      );
      expect(nodes.find((n) => n.id === "a")?.position).toEqual({
        x: 10,
        y: 20,
      });
    });

    test("defaults a node's position to the origin when missing from the positions map", () => {
      const { nodes } = buildGraphElementsV2({}, sampleConcepts, sampleEdges);
      expect(nodes.find((n) => n.id === "a")?.position).toEqual({
        x: 0,
        y: 0,
      });
    });

    test("creates one edge per supplied prerequisite relationship, colored from the supplied concepts", () => {
      const { edges } = buildGraphElementsV2(
        samplePositions,
        sampleConcepts,
        sampleEdges,
      );
      expect(edges).toHaveLength(1);
      expect(edges[0]).toMatchObject({
        id: "prereq-a-b",
        source: "a",
        target: "b",
        sourceHandle: "top",
        targetHandle: "bottom",
        type: "smoothstep",
        style: { stroke: "#111111", strokeWidth: 4 },
        markerEnd: { type: MarkerType.ArrowClosed, color: "#111111" },
      });
    });

    test("is independent of the hardcoded conceptGraph.ts data", () => {
      const { nodes, edges } = buildGraphElementsV2(
        samplePositions,
        sampleConcepts,
        sampleEdges,
      );
      expect(nodes).toHaveLength(2);
      expect(edges).toHaveLength(1);
      // sanity check that this is genuinely not the hardcoded 26-concept dataset
      expect(nodes.length).not.toEqual(majorConcepts.length);
    });
  });
});
