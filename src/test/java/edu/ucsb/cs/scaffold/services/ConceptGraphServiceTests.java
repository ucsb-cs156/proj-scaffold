package edu.ucsb.cs.scaffold.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ucsb.cs.scaffold.entity.Concept;
import edu.ucsb.cs.scaffold.entity.ConceptEdge;
import edu.ucsb.cs.scaffold.entity.Course;
import edu.ucsb.cs.scaffold.services.ConceptGraphService.Position;
import edu.ucsb.cs.scaffold.services.ConceptGraphService.ResetResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ConceptGraphServiceTests {

  private final ConceptGraphService service = new ConceptGraphService();
  private final Course course = Course.builder().id(1L).build();

  private Concept concept(long id, int x) {
    return Concept.builder().id(id).course(course).x(x).y(0).build();
  }

  private ConceptEdge edge(long id, Concept source, Concept target) {
    return ConceptEdge.builder().id(id).course(course).source(source).target(target).build();
  }

  // colorForLevel

  @Test
  public void colorForLevel_returns_the_palette_color_for_levels_one_through_five() {
    assertEquals("#c99ffe", service.colorForLevel(1));
    assertEquals("#feaef2", service.colorForLevel(2));
    assertEquals("#93ebff", service.colorForLevel(3));
    assertEquals("#fe9a71", service.colorForLevel(4));
    assertEquals("#2bcd9c", service.colorForLevel(5));
  }

  @Test
  public void colorForLevel_reuses_the_level_five_color_beyond_the_palette() {
    assertEquals("#2bcd9c", service.colorForLevel(6));
    assertEquals("#2bcd9c", service.colorForLevel(100));
  }

  @Test
  public void colorForLevel_clamps_a_non_positive_level_to_level_one() {
    assertEquals("#c99ffe", service.colorForLevel(0));
    assertEquals("#c99ffe", service.colorForLevel(-5));
  }

  // wouldCreateCycle

  @Test
  public void wouldCreateCycle_is_false_when_there_are_no_existing_edges() {
    assertFalse(service.wouldCreateCycle(List.of(), 1L, 2L));
  }

  @Test
  public void wouldCreateCycle_is_false_when_the_target_cannot_reach_the_source() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    // a -> b (unrelated to a hypothetical b -> c edge's cycle risk)
    List<ConceptEdge> existing = List.of(edge(10, a, b));
    assertFalse(service.wouldCreateCycle(existing, b.getId(), c.getId()));
  }

  @Test
  public void wouldCreateCycle_is_true_when_the_target_can_directly_reach_the_source() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    List<ConceptEdge> existing = List.of(edge(10, b, a)); // b -> a already exists
    assertTrue(service.wouldCreateCycle(existing, a.getId(), b.getId())); // proposed a -> b
  }

  @Test
  public void wouldCreateCycle_is_true_when_the_target_can_reach_the_source_transitively() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    List<ConceptEdge> existing = List.of(edge(10, b, c), edge(11, c, a)); // b -> c -> a
    assertTrue(service.wouldCreateCycle(existing, a.getId(), b.getId())); // proposed a -> b
  }

  @Test
  public void wouldCreateCycle_handles_a_node_reachable_by_more_than_one_path() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    Concept d = concept(4, 0);
    // b reaches d via both b->c->d and a direct b->d edge; d does not reach a either way.
    List<ConceptEdge> existing = List.of(edge(10, b, c), edge(11, c, d), edge(12, b, d));
    assertFalse(service.wouldCreateCycle(existing, a.getId(), b.getId()));
  }

  // reset: acyclic structure, levels, and layout

  @Test
  public void reset_ranks_a_linear_chain_by_longest_path_and_stacks_levels_vertically() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    List<ConceptEdge> edges = List.of(edge(10, a, b), edge(11, b, c));

    ResetResult result = service.reset(List.of(a, b, c), edges);

    assertEquals(Map.of(a.getId(), 1, b.getId(), 2, c.getId(), 3), result.levelByConceptId());
    assertTrue(result.cycleEdgeIds().isEmpty());
    assertTrue(result.removedEdgeIds().isEmpty());
    assertEquals(new Position(0, 0), result.positionByConceptId().get(a.getId()));
    assertEquals(new Position(0, -300), result.positionByConceptId().get(b.getId()));
    assertEquals(new Position(0, -600), result.positionByConceptId().get(c.getId()));
  }

  @Test
  public void reset_gives_a_diamond_the_correct_levels_with_no_redundant_edges() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    Concept d = concept(4, 0);
    // a -> b, a -> c, b -> d, c -> d: no edge here is transitively redundant.
    List<ConceptEdge> edges =
        List.of(edge(10, a, b), edge(11, a, c), edge(12, b, d), edge(13, c, d));

    ResetResult result = service.reset(List.of(a, b, c, d), edges);

    assertTrue(result.removedEdgeIds().isEmpty());
    assertEquals(1, result.levelByConceptId().get(a.getId()));
    assertEquals(2, result.levelByConceptId().get(b.getId()));
    assertEquals(2, result.levelByConceptId().get(c.getId()));
    assertEquals(3, result.levelByConceptId().get(d.getId()));
  }

  @Test
  public void reset_removes_a_direct_edge_made_redundant_by_a_longer_path() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    // a -> c is redundant: a already reaches c via a -> b -> c.
    ConceptEdge shortcut = edge(10, a, c);
    List<ConceptEdge> edges = List.of(shortcut, edge(11, a, b), edge(12, b, c));

    ResetResult result = service.reset(List.of(a, b, c), edges);

    assertEquals(Set.of(shortcut.getId()), result.removedEdgeIds());
    // The longest path to c is still 3 (a -> b -> c), unaffected by removing the shortcut.
    assertEquals(3, result.levelByConceptId().get(c.getId()));
  }

  @Test
  public void reset_leaves_an_isolated_concept_with_no_edges_at_level_one() {
    Concept a = concept(1, 0);
    Concept isolated = concept(2, 100);

    ResetResult result = service.reset(List.of(a, isolated), List.of());

    assertEquals(1, result.levelByConceptId().get(a.getId()));
    assertEquals(1, result.levelByConceptId().get(isolated.getId()));
  }

  // reset: cycle detection

  @Test
  public void reset_colors_both_edges_of_a_two_node_cycle_and_excludes_them_from_leveling() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    ConceptEdge ab = edge(10, a, b);
    ConceptEdge ba = edge(11, b, a);

    ResetResult result = service.reset(List.of(a, b), List.of(ab, ba));

    assertEquals(Set.of(ab.getId(), ba.getId()), result.cycleEdgeIds());
    assertTrue(result.removedEdgeIds().isEmpty());
    // Both nodes' only edges were excluded as cyclic, so both fall back to level 1.
    assertEquals(1, result.levelByConceptId().get(a.getId()));
    assertEquals(1, result.levelByConceptId().get(b.getId()));
  }

  @Test
  public void reset_flags_every_edge_in_a_three_node_cycle() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    ConceptEdge ab = edge(10, a, b);
    ConceptEdge bc = edge(11, b, c);
    ConceptEdge ca = edge(12, c, a);

    ResetResult result = service.reset(List.of(a, b, c), List.of(ab, bc, ca));

    assertEquals(Set.of(ab.getId(), bc.getId(), ca.getId()), result.cycleEdgeIds());
  }

  @Test
  public void reset_flags_edges_in_two_separate_disjoint_cycles_independently() {
    // Two unconnected 2-node cycles: whichever one Tarjan's algorithm assigns a nonzero
    // component id to must still have its own size looked up correctly, not conflated with
    // the other cycle's (or any other component's) size.
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    Concept d = concept(4, 0);
    ConceptEdge ab = edge(10, a, b);
    ConceptEdge ba = edge(11, b, a);
    ConceptEdge cd = edge(12, c, d);
    ConceptEdge dc = edge(13, d, c);

    ResetResult result = service.reset(List.of(a, b, c, d), List.of(ab, ba, cd, dc));

    assertEquals(Set.of(ab.getId(), ba.getId(), cd.getId(), dc.getId()), result.cycleEdgeIds());
  }

  @Test
  public void reset_does_not_treat_a_self_loop_as_a_cycle() {
    // Self-loops are already rejected at the API layer (an edge cannot connect a concept to
    // itself), but the pure algorithm should still handle one defensively: a lone node's SCC
    // has size 1 even with a self-loop, so it is not flagged as a cycle edge.
    Concept a = concept(1, 0);
    ConceptEdge selfLoop = edge(10, a, a);

    ResetResult result = service.reset(List.of(a), List.of(selfLoop));

    assertTrue(result.cycleEdgeIds().isEmpty());
  }

  @Test
  public void reset_does_not_flag_a_bridge_edge_leading_into_a_cycle() {
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);
    Concept d = concept(4, 0);
    // b <-> c is a 2-cycle; a -> b is a bridge into it, not itself part of the cycle.
    ConceptEdge ab = edge(10, a, b);
    ConceptEdge bc = edge(11, b, c);
    ConceptEdge cb = edge(12, c, b);

    ResetResult result = service.reset(List.of(a, b, c, d), List.of(ab, bc, cb));

    assertEquals(Set.of(bc.getId(), cb.getId()), result.cycleEdgeIds());
    // a -> b is acyclic and still contributes to b's level.
    assertEquals(1, result.levelByConceptId().get(a.getId()));
    assertEquals(2, result.levelByConceptId().get(b.getId()));
  }

  // reset: layout

  @Test
  public void reset_centers_a_single_concept_at_a_level_on_x_zero() {
    Concept a = concept(1, 500);
    ResetResult result = service.reset(List.of(a), List.of());
    assertEquals(new Position(0, 0), result.positionByConceptId().get(a.getId()));
  }

  @Test
  public void reset_lays_out_multiple_concepts_at_the_same_level_centered_and_separated() {
    // Three isolated (unconnected) concepts all land at level 1.
    Concept a = concept(1, 0);
    Concept b = concept(2, 0);
    Concept c = concept(3, 0);

    ResetResult result = service.reset(List.of(a, b, c), List.of());

    assertEquals(new Position(-350, 0), result.positionByConceptId().get(a.getId()));
    assertEquals(new Position(0, 0), result.positionByConceptId().get(b.getId()));
    assertEquals(new Position(350, 0), result.positionByConceptId().get(c.getId()));
  }

  @Test
  public void reset_orders_concepts_within_a_level_by_prior_x_then_by_id_to_break_ties() {
    // b and c share the same prior x; id must decide their left-to-right order.
    Concept a = concept(3, 200); // rightmost by prior x
    Concept b = concept(1, 0); // tied prior x with c, lower id -> goes first (left)
    Concept c = concept(2, 0); // tied prior x with b, higher id -> goes second

    ResetResult result = service.reset(List.of(a, b, c), List.of());

    assertEquals(new Position(-350, 0), result.positionByConceptId().get(b.getId()));
    assertEquals(new Position(0, 0), result.positionByConceptId().get(c.getId()));
    assertEquals(new Position(350, 0), result.positionByConceptId().get(a.getId()));
  }
}
