import { prereqEdgeData } from "../data/conceptGraph";

export const normalize = (s: string) => s.replace(/\\n/g, "\n");

export function toPastel(hex: string, strength: number = 0.1): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgb(${Math.round(r * strength + 255 * (1 - strength))}, ${Math.round(g * strength + 255 * (1 - strength))}, ${Math.round(b * strength + 255 * (1 - strength))})`;
}

// Walk the prereq graph upward from tagged concepts to include all ancestors
export function computeSubgraph(taggedIds: string[]): Set<string> {
  const result = new Set<string>(taggedIds);
  let changed = true;
  while (changed) {
    changed = false;
    for (const { source, target } of prereqEdgeData) {
      if (result.has(target) && !result.has(source)) {
        result.add(source);
        changed = true;
      }
    }
  }
  return result;
}
