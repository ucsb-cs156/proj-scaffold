package edu.ucsb.cs.scaffold.services;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Graph algorithms for the prerequisite structure of a course's top-level concepts (subconcepts
 * have no position in this graph). Prerequisite edges only ever connect top-level concepts, so
 * every method here operates on that subgraph.
 *
 * <p>{@link #reset} is the analysis run by {@code POST /api/course/scaffold/reset}: it detects
 * cycles (flagging their edges rather than processing them further), removes edges that are
 * redundant given the graph's transitive structure, ranks concepts by longest path from a root, and
 * lays out each level's x,y position. It is a pure function of its inputs — no repository access —
 * so the controller owns loading input and persisting the result.
 */
@Service
public class ConceptGraphService {

  // Index i holds the color for level i+1; levels beyond the palette reuse the last color.
  public static final List<String> LEVEL_COLORS =
      List.of("#c99ffe", "#feaef2", "#93ebff", "#fe9a71", "#2bcd9c");

  public static final String CYCLE_EDGE_COLOR = "#FF0000";

  // Initial guesses; revisit if the resulting layout looks too cramped or too sparse.
  public static final int MIN_HORIZONTAL_SEPARATION = 350;
  public static final int VERTICAL_LEVEL_SEPARATION = 300;

  /** The color assigned to a top-level concept at the given longest-path level (1-based). */
  public String colorForLevel(int level) {
    int index = Math.min(Math.max(level, 1), LEVEL_COLORS.size()) - 1;
    return LEVEL_COLORS.get(index);
  }

  /**
   * True if adding an edge sourceId -&gt; targetId would create a cycle, i.e. targetId can already
   * reach sourceId via existingEdges. Used to reject new prerequisite edges at creation time,
   * before a cycle can ever be persisted.
   */
  public boolean wouldCreateCycle(List<ConceptEdge> existingEdges, Long sourceId, Long targetId) {
    Map<Long, List<Long>> adjacency = buildAdjacency(existingEdges);
    Set<Long> visited = new HashSet<>();
    Deque<Long> queue = new ArrayDeque<>();
    visited.add(targetId);
    queue.add(targetId);
    while (!queue.isEmpty()) {
      Long current = queue.poll();
      if (current.equals(sourceId)) {
        return true;
      }
      for (Long next : adjacency.getOrDefault(current, List.of())) {
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }
    return false;
  }

  public record Position(int x, int y) {}

  public record ResetResult(
      Set<Long> cycleEdgeIds,
      Set<Long> removedEdgeIds,
      Map<Long, Integer> levelByConceptId,
      Map<Long, Position> positionByConceptId) {}

  /**
   * Runs the full scaffold reset analysis: cycle detection, transitive reduction, longest-path
   * leveling, and layout. Does not mutate concepts or edges or read/write any repository; the
   * caller applies {@link ResetResult} to persistent entities.
   *
   * @param topLevelConcepts every top-level concept in the course (used for their id/x, to sort and
   *     lay out concepts with no edges at all, and as the node set for the graph algorithms)
   * @param edges every prerequisite edge in the course
   */
  public ResetResult reset(List<Concept> topLevelConcepts, List<ConceptEdge> edges) {
    Map<Long, Integer> priorXByConceptId =
        topLevelConcepts.stream().collect(Collectors.toMap(Concept::getId, Concept::getX));
    return reset(topLevelConcepts, edges, priorXByConceptId);
  }

  /**
   * Like {@link #reset(List, List)}, but sorts each level by the given prior x values instead of
   * each concept's own x column. Used by the controller to sort by the requesting user's private,
   * unsaved drag positions where they exist, falling back to the concept's persisted x otherwise —
   * see {@code POST /api/course/scaffold/reset}.
   */
  public ResetResult reset(
      List<Concept> topLevelConcepts,
      List<ConceptEdge> edges,
      Map<Long, Integer> priorXByConceptId) {
    Set<Long> nodeIds = topLevelConcepts.stream().map(Concept::getId).collect(Collectors.toSet());

    Map<Long, Integer> sccId = computeStronglyConnectedComponents(buildAdjacency(edges), nodeIds);
    Map<Integer, Long> sccSize =
        sccId.values().stream().collect(Collectors.groupingBy(id -> id, Collectors.counting()));

    Set<Long> cycleEdgeIds = new HashSet<>();
    List<ConceptEdge> acyclicEdges = new ArrayList<>();
    for (ConceptEdge edge : edges) {
      Long sourceId = edge.getSource().getId();
      Long targetId = edge.getTarget().getId();
      boolean inCycle =
          sccId.get(sourceId).equals(sccId.get(targetId)) && sccSize.get(sccId.get(sourceId)) > 1;
      if (inCycle) {
        cycleEdgeIds.add(edge.getId());
      } else {
        acyclicEdges.add(edge);
      }
    }

    Set<Long> removedEdgeIds = computeTransitiveReductionRemovals(nodeIds, acyclicEdges);
    List<ConceptEdge> keptEdges =
        acyclicEdges.stream().filter(e -> !removedEdgeIds.contains(e.getId())).toList();

    Map<Long, Integer> levelByConceptId = computeLongestPathLevels(nodeIds, keptEdges);
    Map<Long, Position> positionByConceptId =
        computeLayout(topLevelConcepts, levelByConceptId, priorXByConceptId);

    return new ResetResult(cycleEdgeIds, removedEdgeIds, levelByConceptId, positionByConceptId);
  }

  /**
   * Lays each level out left to right, sorted by each concept's prior x (then id to break ties),
   * centered horizontally at x=0. Each level sits {@link #VERTICAL_LEVEL_SEPARATION} above the
   * previous one, with level 1 at y=0.
   */
  private Map<Long, Position> computeLayout(
      List<Concept> topLevelConcepts,
      Map<Long, Integer> levelByConceptId,
      Map<Long, Integer> priorXByConceptId) {
    Map<Integer, List<Concept>> byLevel =
        topLevelConcepts.stream()
            .collect(Collectors.groupingBy(c -> levelByConceptId.get(c.getId())));

    Map<Long, Position> positions = new HashMap<>();
    for (Map.Entry<Integer, List<Concept>> entry : byLevel.entrySet()) {
      int level = entry.getKey();
      List<Concept> sorted =
          entry.getValue().stream()
              .sorted(
                  Comparator.comparing((Concept c) -> priorXByConceptId.get(c.getId()))
                      .thenComparing(Concept::getId))
              .toList();
      int n = sorted.size();
      int y = -(level - 1) * VERTICAL_LEVEL_SEPARATION;
      for (int i = 0; i < n; i++) {
        int x = (int) Math.round((i - (n - 1) / 2.0) * MIN_HORIZONTAL_SEPARATION);
        positions.put(sorted.get(i).getId(), new Position(x, y));
      }
    }
    return positions;
  }

  /**
   * Tarjan's algorithm: maps each node id to an integer id shared by its strongly connected
   * component (a nontrivial cycle iff more than one node shares it).
   */
  private Map<Long, Integer> computeStronglyConnectedComponents(
      Map<Long, List<Long>> adjacency, Set<Long> nodeIds) {
    TarjanState state = new TarjanState(adjacency);
    for (Long node : nodeIds) {
      if (!state.index.containsKey(node)) {
        state.strongConnect(node);
      }
    }
    return state.sccId;
  }

  private static final class TarjanState {
    private final Map<Long, List<Long>> adjacency;
    private final Map<Long, Integer> index = new HashMap<>();
    private final Map<Long, Integer> lowlink = new HashMap<>();
    private final Set<Long> onStack = new HashSet<>();
    private final Deque<Long> stack = new ArrayDeque<>();
    private final Map<Long, Integer> sccId = new HashMap<>();
    private int counter = 0;
    private int sccCounter = 0;

    TarjanState(Map<Long, List<Long>> adjacency) {
      this.adjacency = adjacency;
    }

    void strongConnect(Long v) {
      index.put(v, counter);
      lowlink.put(v, counter);
      counter++;
      stack.push(v);
      onStack.add(v);

      for (Long w : adjacency.getOrDefault(v, List.of())) {
        if (!index.containsKey(w)) {
          strongConnect(w);
          lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
        } else if (onStack.contains(w)) {
          lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
        }
      }

      if (lowlink.get(v).equals(index.get(v))) {
        Long w;
        do {
          w = stack.pop();
          onStack.remove(w);
          sccId.put(w, sccCounter);
        } while (!w.equals(v));
        sccCounter++;
      }
    }
  }

  /**
   * An edge u-&gt;v is redundant if some other direct successor w of u can also reach v; such an
   * edge is a "shortcut" whose removal does not change reachability. Requires an acyclic edge set.
   */
  private Set<Long> computeTransitiveReductionRemovals(
      Set<Long> nodeIds, List<ConceptEdge> acyclicEdges) {
    Map<Long, List<Long>> adjacency = buildAdjacency(acyclicEdges);
    Map<Long, Set<Long>> reachable = new HashMap<>();
    for (Long node : nodeIds) {
      reachable.put(node, bfsReachable(node, adjacency));
    }

    Set<Long> removed = new HashSet<>();
    for (ConceptEdge edge : acyclicEdges) {
      Long u = edge.getSource().getId();
      Long v = edge.getTarget().getId();
      boolean redundant =
          adjacency.getOrDefault(u, List.of()).stream()
              .anyMatch(w -> !w.equals(v) && reachable.getOrDefault(w, Set.of()).contains(v));
      if (redundant) {
        removed.add(edge.getId());
      }
    }
    return removed;
  }

  /**
   * Longest path from any root (no incoming edge) to each node; roots are level 1. Requires an
   * acyclic edge set.
   */
  private Map<Long, Integer> computeLongestPathLevels(Set<Long> nodeIds, List<ConceptEdge> edges) {
    Map<Long, List<Long>> outNeighbors = buildAdjacency(edges);
    Map<Long, Integer> inDegree = new HashMap<>();
    for (Long node : nodeIds) {
      inDegree.put(node, 0);
    }
    for (ConceptEdge edge : edges) {
      inDegree.merge(edge.getTarget().getId(), 1, Integer::sum);
    }

    Map<Long, Integer> level = new HashMap<>();
    Deque<Long> queue = new ArrayDeque<>();
    for (Long node : nodeIds) {
      level.put(node, 1);
      if (inDegree.get(node) == 0) {
        queue.add(node);
      }
    }

    while (!queue.isEmpty()) {
      Long u = queue.poll();
      for (Long v : outNeighbors.getOrDefault(u, List.of())) {
        level.put(v, Math.max(level.get(v), level.get(u) + 1));
        if (inDegree.merge(v, -1, Integer::sum) == 0) {
          queue.add(v);
        }
      }
    }
    return level;
  }

  private Set<Long> bfsReachable(Long start, Map<Long, List<Long>> adjacency) {
    Set<Long> visited = new HashSet<>(adjacency.getOrDefault(start, List.of()));
    Deque<Long> queue = new ArrayDeque<>(visited);
    while (!queue.isEmpty()) {
      Long current = queue.poll();
      for (Long next : adjacency.getOrDefault(current, List.of())) {
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }
    return visited;
  }

  private Map<Long, List<Long>> buildAdjacency(List<ConceptEdge> edges) {
    Map<Long, List<Long>> adjacency = new HashMap<>();
    for (ConceptEdge edge : edges) {
      adjacency
          .computeIfAbsent(edge.getSource().getId(), k -> new ArrayList<>())
          .add(edge.getTarget().getId());
    }
    return adjacency;
  }
}
