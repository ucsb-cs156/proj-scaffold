import { type Node, type Edge, MarkerType, Position } from '@xyflow/react';
import { majorConcepts, prereqEdgeData } from '../data/conceptGraph';

const positions: Record<string, { x: number; y: number }> = {
  // Row 0 — top (most advanced)
  'errors-debugging':  { x: 955, y: 0    },
  'modules':           { x: 1265, y: 0    },
  'files':             { x: 1575,  y: 0    },
  // Row 1
  'nested-loops':      { x: 645,  y: 290  },
  'main-fn':           { x: 955,  y: 290  },
  'input-output':      { x: 1265, y: 290  },
  'string-methods':    { x: 1575, y: 290  },
  'recursion':         { x: 1885, y: 290  },
  // Row 2
  'loops':             { x: 335,  y: 590  },
  'conditionals':      { x: 645,  y: 590  },
  'testing':           { x: 955, y: 590  },
  'built-in-fns':      { x: 1265, y: 590  },
  'nested-lists':      { x: 1575, y: 590  },
  'methods':           { x: 1885, y: 590  },
  'mutability':        { x: 2195, y: 590  },
  // Row 3
  'arithmetic-ops':    { x: 335,  y: 950  },
  'boolean-expr':      { x: 645,  y: 950  },
  'functions':         { x: 955,  y: 950  },
  'lists':             { x: 1265, y: 950  },
  'dictionaries':      { x: 1575, y: 950  },
  'tuples':            { x: 1885, y: 950  },
  'sets':              { x: 2195, y: 950  },
  // Row 4
  'data-rep':          { x: 955,  y: 1270 },
  'variables':         { x: 1265, y: 1270 },
  'string-ops':        { x: 1575, y: 1270 },
  // Row 5 — bottom (most basic)
  'data-types':        { x: 1265, y: 1520 },
};

export function buildGraphElements(): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = majorConcepts.map(concept => ({
    id:   concept.id,
    type: 'major',
    position: positions[concept.id] ?? { x: 0, y: 0 },
    data: {
      label:       concept.label,
      color:       concept.color,
      subconcepts: concept.subconcepts,
    },
    sourcePosition: Position.Top,
    targetPosition: Position.Bottom,
  }));

  const edges: Edge[] = prereqEdgeData.map(e => ({
    id:           `prereq-${e.source}-${e.target}`,
    source:        e.source,
    target:        e.target,
    sourceHandle: 'top',
    targetHandle: 'bottom',
    type:         'smooth',
    style: {
      stroke:          majorConcepts.find(c => c.id === e.source)?.color,
      strokeWidth:     4,
    },
    markerEnd: {
      type:  MarkerType.ArrowClosed,
      color: majorConcepts.find(c => c.id === e.source)?.color,
    },
  }));

  return { nodes, edges };
}