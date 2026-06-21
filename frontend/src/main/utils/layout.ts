import { type Node, type Edge, MarkerType, Position } from "@xyflow/react";
import { majorConcepts, prereqEdgeData } from "../data/conceptGraph";

const positions: Record<string, { x: number; y: number }> = {
  // Row 5
  "nested-loops": { x: 645, y: 0 },
  "input-output": { x: 955, y: 0 },
  files: { x: 1265, y: 0 },
  modules: { x: 1575, y: 0 },
  "string-methods": { x: 1885, y: 0 },
  // Row 4
  "main-fn": { x: 180, y: 300 },
  loops: { x: 490, y: 300 },
  recursion: { x: 800, y: 300 },
  "nested-lists": { x: 1110, y: 300 },
  "built-in-fns": { x: 1420, y: 300 },
  methods: { x: 1730, y: 300 },
  mutability: { x: 2040, y: 300 },
  testing: { x: 2350, y: 300 },
  // Row 3
  conditionals: { x: 180, y: 650 },
  "errors-debugging": { x: 490, y: 650 },
  lists: { x: 800, y: 650 },
  functions: { x: 1110, y: 650 },
  "string-ops": { x: 1420, y: 650 },
  dictionaries: { x: 1730, y: 650 },
  tuples: { x: 2040, y: 650 },
  sets: { x: 2350, y: 650 },
  // Row 2
  "arithmetic-ops": { x: 800, y: 1000 },
  "boolean-expr": { x: 1110, y: 1000 },
  variables: { x: 1420, y: 1000 },
  "data-rep": { x: 1730, y: 1000 },
  // Row 1
  "data-types": { x: 1265, y: 1350 },
};

export function buildGraphElements(): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = majorConcepts.map((concept) => ({
    id: concept.id,
    type: "major",
    position: positions[concept.id] ?? { x: 0, y: 0 },
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
      stroke: majorConcepts.find((c) => c.id === e.source)?.color,
      strokeWidth: 4,
    },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: majorConcepts.find((c) => c.id === e.source)?.color,
    },
  }));

  return { nodes, edges };
}
