import { describe, test, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import ConceptGraph from "main/components/ConceptGraph";

// @xyflow/react measures its container via ResizeObserver and
// getBoundingClientRect, neither of which jsdom implements with real
// dimensions. Stub both so the graph can mount and compute a viewport.
class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeEach(() => {
  vi.stubGlobal("ResizeObserver", MockResizeObserver);
  Element.prototype.getBoundingClientRect = () =>
    ({
      width: 1000,
      height: 1000,
      top: 0,
      left: 0,
      right: 1000,
      bottom: 1000,
      x: 0,
      y: 0,
      toJSON() {},
    }) as DOMRect;
});

function baseProps() {
  return {
    highlightedIds: new Set<string>(),
    highlightedSubconcepts: new Map<string, Set<string>>(),
    onConceptClick: vi.fn(),
    starredIds: new Set<string>(),
    onStarClick: vi.fn(),
    onReset: vi.fn(),
    masteredSubconcepts: new Set<string>(),
    onSubconceptMastered: vi.fn(),
  };
}

describe("ConceptGraph", () => {
  test("renders the major concept nodes", () => {
    render(<ConceptGraph {...baseProps()} />);
    expect(screen.getByText("Recursion")).toBeInTheDocument();
    expect(screen.getByText("Loops")).toBeInTheDocument();
  });

  test("clicking a concept node calls onConceptClick with its id", () => {
    const props = baseProps();
    render(<ConceptGraph {...props} />);

    fireEvent.click(screen.getByText("Recursion"));

    expect(props.onConceptClick).toHaveBeenCalledWith("recursion");
  });

  test("clicking the reset control calls onReset", () => {
    const props = baseProps();
    render(<ConceptGraph {...props} />);

    fireEvent.click(screen.getByText("Click to reset graph"));

    expect(props.onReset).toHaveBeenCalled();
  });

  test("clicking the pane calls onPaneClick", () => {
    const props = baseProps();
    const onPaneClick = vi.fn();
    const { container } = render(
      <ConceptGraph {...props} onPaneClick={onPaneClick} />,
    );

    const pane = container.querySelector(".react-flow__pane");
    expect(pane).not.toBeNull();
    fireEvent.click(pane as Element);

    expect(onPaneClick).toHaveBeenCalled();
  });

  test("renders restored detail cards", () => {
    const props = baseProps();
    render(
      <ConceptGraph
        {...props}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "recursion",
            conceptColor: "#fe9a71",
            posX: 100,
            posY: 100,
          },
        ]}
      />,
    );

    expect(screen.getByText("Description")).toBeInTheDocument();
    // The concept name appears twice: once for the graph node, once for the detail card.
    expect(screen.getAllByText("Recursion").length).toBeGreaterThanOrEqual(2);
  });

  test("clicking a node's star button calls onStarClick with its id, not onConceptClick", () => {
    const props = baseProps();
    render(<ConceptGraph {...props} />);

    fireEvent.click(screen.getByTestId("star-button-recursion"));

    expect(props.onStarClick).toHaveBeenCalledWith("recursion");
    expect(props.onConceptClick).not.toHaveBeenCalled();
  });

  test("clicking a subconcept checkbox calls onSubconceptMastered with its label", () => {
    const props = baseProps();
    render(<ConceptGraph {...props} />);

    // "recursion" node's first subconcept is "Base case".
    fireEvent.click(screen.getByTestId("subconcept-checkbox-recursion-0"));

    expect(props.onSubconceptMastered).toHaveBeenCalledWith("Base case");
  });

  test("dropping a dragged card onto the canvas calls onDetailAdded and renders the new card", async () => {
    const props = baseProps();
    const onDetailAdded = vi.fn();
    const { container } = render(
      <ConceptGraph {...props} onDetailAdded={onDetailAdded} />,
    );

    // ReactFlow's onInit (which populates the instance used for
    // screenToFlowPosition) fires via a setTimeout, so the instance isn't
    // ready synchronously after render.
    await new Promise((resolve) => setTimeout(resolve, 10));

    const flowWrapper = container.querySelector(".react-flow") as Element;
    const payload = {
      cardType: "Example",
      itemLabel: "Recursion",
      conceptId: "recursion",
      conceptColor: "#fe9a71",
      cardContent: "def factorial(n): ...",
    };
    fireEvent.drop(flowWrapper, {
      clientX: 400,
      clientY: 300,
      dataTransfer: {
        getData: () => JSON.stringify(payload),
      },
    });

    expect(onDetailAdded).toHaveBeenCalledWith(
      expect.objectContaining({
        cardType: "Example",
        itemLabel: "Recursion",
        conceptId: "recursion",
        conceptColor: "#fe9a71",
      }),
    );
    expect(screen.getByText("Example")).toBeInTheDocument();
  });

  test("deleting a detail card calls onDetailDeleted and removes the card", () => {
    const props = baseProps();
    const onDetailDeleted = vi.fn();
    render(
      <ConceptGraph
        {...props}
        onDetailDeleted={onDetailDeleted}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "recursion",
            conceptColor: "#fe9a71",
            posX: 100,
            posY: 100,
          },
        ]}
      />,
    );

    expect(screen.getByText("Description")).toBeInTheDocument();

    fireEvent.click(
      screen.getByTestId(
        "detail-delete-detail-restored-0-Description-Recursion",
      ),
    );

    expect(onDetailDeleted).toHaveBeenCalledWith("Description", "Recursion");
    expect(screen.queryByText("Description")).not.toBeInTheDocument();
  });
});
