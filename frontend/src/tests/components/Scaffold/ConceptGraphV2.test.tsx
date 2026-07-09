import { describe, test, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import ConceptGraphV2 from "main/components/Scaffold/ConceptGraphV2";
import { DebugModeContext } from "main/utils/debugModeContext";
import type { ReactElement } from "react";

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

const sampleMajorConcepts = [
  {
    id: 1,
    labelHtml: "Recursion",
    color: "#fe9a71",
    subconcepts: [
      { id: 2, parentId: 1, labelHtml: "Base case" },
      { id: 3, parentId: 1, labelHtml: "State change" },
    ],
  },
  {
    id: 4,
    labelHtml: "Loops",
    color: "#93ebff",
    subconcepts: [{ id: 5, parentId: 4, labelHtml: "For loops" }],
  },
];
const samplePositions = {
  "1": { x: 100, y: 100 },
  "4": { x: 300, y: 100 },
};
const samplePrereqEdgeData = [
  { id: 20, sourceId: 4, targetId: 1, color: null },
];

function baseProps() {
  return {
    majorConcepts: sampleMajorConcepts,
    positions: samplePositions,
    conceptContent: {},
    prereqEdgeData: samplePrereqEdgeData,
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

describe("ConceptGraphV2", () => {
  test("renders the major concept nodes from the supplied data", () => {
    render(<ConceptGraphV2 {...baseProps()} />);
    expect(screen.getByText("Recursion")).toBeInTheDocument();
    expect(screen.getByText("Loops")).toBeInTheDocument();
  });

  test("clicking a concept node calls onConceptClick with its id", () => {
    const props = baseProps();
    render(<ConceptGraphV2 {...props} />);

    fireEvent.click(screen.getByText("Recursion"));

    expect(props.onConceptClick).toHaveBeenCalledWith("1");
  });

  test("clicking the reset control calls onReset", () => {
    const props = baseProps();
    render(<ConceptGraphV2 {...props} />);

    fireEvent.click(screen.getByText("Click to reset graph"));

    expect(props.onReset).toHaveBeenCalled();
  });

  test("clicking the pane calls onPaneClick", () => {
    const props = baseProps();
    const onPaneClick = vi.fn();
    const { container } = render(
      <ConceptGraphV2 {...props} onPaneClick={onPaneClick} />,
    );

    const pane = container.querySelector(".react-flow__pane");
    expect(pane).not.toBeNull();
    fireEvent.click(pane as Element);

    expect(onPaneClick).toHaveBeenCalled();
  });

  test("renders restored detail cards", () => {
    const props = baseProps();
    render(
      <ConceptGraphV2
        {...props}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "1",
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
    render(<ConceptGraphV2 {...props} />);

    fireEvent.click(screen.getByTestId("star-button-1"));

    expect(props.onStarClick).toHaveBeenCalledWith("1");
    expect(props.onConceptClick).not.toHaveBeenCalled();
  });

  test("clicking a subconcept checkbox calls onSubconceptMastered with its label", () => {
    const props = baseProps();
    render(<ConceptGraphV2 {...props} />);

    // The "Recursion" node (id 1) has "Base case" as its first subconcept.
    fireEvent.click(screen.getByTestId("subconcept-checkbox-1-0"));

    expect(props.onSubconceptMastered).toHaveBeenCalledWith("Base case");
  });

  test("dropping a dragged card onto the canvas calls onDetailAdded and renders the new card", async () => {
    const props = baseProps();
    const onDetailAdded = vi.fn();
    const { container } = render(
      <ConceptGraphV2 {...props} onDetailAdded={onDetailAdded} />,
    );

    // ReactFlow's onInit (which populates the instance used for
    // screenToFlowPosition) fires via a setTimeout, so the instance isn't
    // ready synchronously after render.
    await new Promise((resolve) => setTimeout(resolve, 10));

    const flowWrapper = container.querySelector(".react-flow") as Element;
    const payload = {
      cardType: "Example",
      itemLabel: "Recursion",
      conceptId: "1",
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
        conceptId: "1",
        conceptColor: "#fe9a71",
      }),
    );
    expect(screen.getByText("Example")).toBeInTheDocument();
  });

  test("deleting a detail card calls onDetailDeleted and removes the card", () => {
    const props = baseProps();
    const onDetailDeleted = vi.fn();
    render(
      <ConceptGraphV2
        {...props}
        onDetailDeleted={onDetailDeleted}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "1",
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

  test("greys out non-highlighted concepts and highlights selected ones, and highlights matching subconcepts", () => {
    const props = baseProps();
    render(
      <ConceptGraphV2
        {...props}
        highlightedIds={new Set(["1"])}
        highlightedSubconcepts={new Map([["1", new Set(["Base case"])]])}
      />,
    );

    const recursionCard = screen.getByTestId("major-node-1");
    const loopsCard = screen.getByTestId("major-node-4");

    expect(recursionCard.style.opacity).toBe("1");
    expect(loopsCard.style.opacity).toBe("0.25");

    const highlightedSubconcept = screen.getByTestId("subconcept-row-1-0");
    expect(highlightedSubconcept.style.background).not.toBe("");
  });

  test("greys out a detail card when its concept is not in the highlighted set", () => {
    const props = baseProps();
    const { container } = render(
      <ConceptGraphV2
        {...props}
        highlightedIds={new Set(["4"])}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "1",
            conceptColor: "#fe9a71",
            posX: 100,
            posY: 100,
          },
        ]}
      />,
    );

    const detailCard = container.querySelector(
      '[data-id^="detail-restored-"] > div',
    ) as HTMLElement;
    expect(detailCard.style.background).toBe("rgb(241, 245, 249)");
  });

  test("keeps a detail card at full color when its concept is highlighted", () => {
    const props = baseProps();
    const { container } = render(
      <ConceptGraphV2
        {...props}
        highlightedIds={new Set(["1"])}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "1",
            conceptColor: "#fe9a71",
            posX: 100,
            posY: 100,
          },
        ]}
      />,
    );

    const detailCard = container.querySelector(
      '[data-id^="detail-restored-"] > div',
    ) as HTMLElement;
    expect(detailCard.style.background).not.toBe("rgb(241, 245, 249)");
  });

  test("dragging over the canvas allows a drop", () => {
    const props = baseProps();
    const { container } = render(<ConceptGraphV2 {...props} />);
    const flowWrapper = container.querySelector(".react-flow") as Element;

    const event = new Event("dragover", { bubbles: true, cancelable: true });
    Object.defineProperty(event, "dataTransfer", {
      value: { dropEffect: "" },
    });
    flowWrapper.dispatchEvent(event);

    expect((event as DragEvent).dataTransfer!.dropEffect).toBe("move");
    expect(event.defaultPrevented).toBe(true);
  });

  test("dropping with no card payload does not add a detail card", () => {
    const props = baseProps();
    const onDetailAdded = vi.fn();
    const { container } = render(
      <ConceptGraphV2 {...props} onDetailAdded={onDetailAdded} />,
    );
    const flowWrapper = container.querySelector(".react-flow") as Element;

    fireEvent.drop(flowWrapper, {
      clientX: 400,
      clientY: 300,
      dataTransfer: { getData: () => "" },
    });

    expect(onDetailAdded).not.toHaveBeenCalled();
  });

  test("dropping before the flow instance is ready does not add a detail card", () => {
    const props = baseProps();
    const onDetailAdded = vi.fn();
    const { container } = render(
      <ConceptGraphV2 {...props} onDetailAdded={onDetailAdded} />,
    );
    const flowWrapper = container.querySelector(".react-flow") as Element;
    const payload = {
      cardType: "Example",
      itemLabel: "Recursion",
      conceptId: "1",
      conceptColor: "#fe9a71",
      cardContent: "def factorial(n): ...",
    };

    // No await here: dropping immediately after render, before ReactFlow's
    // onInit (fired via setTimeout) has populated the flow instance.
    fireEvent.drop(flowWrapper, {
      clientX: 400,
      clientY: 300,
      dataTransfer: { getData: () => JSON.stringify(payload) },
    });

    expect(onDetailAdded).not.toHaveBeenCalled();
  });

  test("clicking a detail card does not call onConceptClick", () => {
    const props = baseProps();
    render(
      <ConceptGraphV2
        {...props}
        restoredDetailCards={[
          {
            cardType: "Description",
            itemLabel: "Unique Detail Label",
            conceptId: "1",
            conceptColor: "#fe9a71",
            posX: 100,
            posY: 100,
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByText("Unique Detail Label"));

    expect(props.onConceptClick).not.toHaveBeenCalled();
  });
});

describe("ConceptGraphV2 debug mode tooltips", () => {
  function renderWithDebugMode(ui: ReactElement, debugMode: boolean) {
    return render(
      <DebugModeContext.Provider
        value={{ debugMode, setDebugMode: vi.fn(), canUseDebugMode: true }}
      >
        {ui}
      </DebugModeContext.Provider>,
    );
  }

  test("nodes have no title tooltip when debug mode is off", () => {
    renderWithDebugMode(<ConceptGraphV2 {...baseProps()} />, false);
    expect(screen.getByTestId("major-node-1")).not.toHaveAttribute("title");
  });

  test("nodes show the full concept JSON as a tooltip when debug mode is on", () => {
    const props = baseProps();
    props.conceptContent = {
      "1": {
        id: 1,
        parentId: null,
        descriptionHtml: "<p>desc</p>",
        exampleHtml: null,
        practiceUrl: null,
      },
    };
    renderWithDebugMode(<ConceptGraphV2 {...props} />, true);

    const title = screen.getByTestId("major-node-1").getAttribute("title");
    expect(title).not.toBeNull();
    expect(JSON.parse(title!)).toEqual({
      id: "1",
      label: "Recursion",
      color: "#fe9a71",
      subconcepts: [
        { id: 2, parentId: 1, labelHtml: "Base case" },
        { id: 3, parentId: 1, labelHtml: "State change" },
      ],
      conceptContent: {
        id: 1,
        parentId: null,
        descriptionHtml: "<p>desc</p>",
        exampleHtml: null,
        practiceUrl: null,
      },
    });
    // Pretty-printed (multi-line), not a single-line blob.
    expect(title).toContain("\n");
  });
});
