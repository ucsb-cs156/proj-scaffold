import { type Node, type Edge, MarkerType, Position } from "@xyflow/react";
import { majorConcepts, prereqEdgeData } from "../data/conceptGraph";

export function buildGraphElements(
  positions: Record<string, { x: number; y: number }>,
): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = majorConcepts.map((concept) => ({
    id: concept.name,
    type: "major",
    position: positions[concept.name] ?? { x: 0, y: 0 },
    data: {
      label: concept.label,
      color: concept.color,
      subconcepts: concept.subconcepts,
    },
    sourcePosition: Position.Top,
    targetPosition: Position.Bottom,
  }));

  const edges: Edge[] = prereqEdgeData.map((e) => ({
    id: `prereq-${e.source}-${e.target}`,
    source: e.source,
    target: e.target,
    sourceHandle: "top",
    targetHandle: "bottom",
    type: "smooth",
    style: {
      stroke: majorConcepts.find((c) => c.name === e.source)?.color,
      strokeWidth: 4,
    },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: majorConcepts.find((c) => c.name === e.source)?.color,
    },
  }));

  return { nodes, edges };
}
