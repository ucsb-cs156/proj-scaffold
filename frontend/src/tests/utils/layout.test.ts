import { describe, test, expect } from "vitest";
import { MarkerType, Position } from "@xyflow/react";
import { buildGraphElements } from "main/utils/layout";
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
      expect(edge?.type).toBe("smooth");
      expect(edge?.style).toEqual({ stroke: sourceColor, strokeWidth: 4 });
      expect(edge?.markerEnd).toEqual({
        type: MarkerType.ArrowClosed,
        color: sourceColor,
      });
    });
  });
});
