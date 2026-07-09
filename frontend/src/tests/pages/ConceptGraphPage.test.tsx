import { describe, test, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route } from "react-router";
import ConceptGraphPage from "main/pages/ConceptGraphPage";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import mockConsole from "tests/testutils/mockConsole";
import type {
  Assessment,
  Question,
  QuestionConcept,
  MajorConceptDTO,
  ConceptContentDTO,
  EdgeDTO,
  UserStateV2Response,
} from "main/api/client";
import * as client from "main/api/client";

import axios from "axios";
import axiosMockAdapter from "axios-mock-adapter";

const axiosMock = new axiosMockAdapter(axios);

vi.mock("main/api/client", () => ({
  fetchAssessments: vi.fn(),
  fetchQuestions: vi.fn(),
  fetchQuestionConcepts: vi.fn(),
  fetchConceptGraph: vi.fn(),
  fetchConceptContent: vi.fn(),
  fetchConceptPositions: vi.fn(),
  fetchConceptEdges: vi.fn(),
  fetchUserStateV2: vi.fn(),
  saveUserStateV2: vi.fn(),
  logUserActivityV2: vi.fn(),
}));

vi.mock("main/components/Scaffold/ConceptGraphV2", () => ({
  default: (props: {
    highlightedIds: Set<string>;
    starredIds: Set<string>;
    restoredDetailCards?: unknown[];
    masteredSubconcepts: Set<string>;
    onConceptClick: (id: string) => void;
    onStarClick: (id: string) => void;
    onReset: () => void;
    onDetailAdded?: (card: {
      cardType: string;
      itemLabel: string;
      conceptId: string;
      conceptColor: string;
      posX: number;
      posY: number;
    }) => void;
    onDetailDeleted?: (cardType: string, itemLabel: string) => void;
    onDetailMoved?: (
      cardType: string,
      itemLabel: string,
      posX: number,
      posY: number,
    ) => void;
    onMajorMoved?: (name: string, posX: number, posY: number) => void;
    onSubconceptMastered: (sub: string) => void;
    onPaneClick?: () => void;
  }) => (
    <div data-testid="concept-graph-stub">
      <div data-testid="highlighted-count">{props.highlightedIds.size}</div>
      <div data-testid="restored-count">
        {props.restoredDetailCards?.length ?? 0}
      </div>
      <div data-testid="mastered-count">{props.masteredSubconcepts.size}</div>
      <button onClick={() => props.onConceptClick("1")}>
        trigger-concept-click
      </button>
      <button onClick={() => props.onStarClick("1")}>trigger-star-click</button>
      <button onClick={() => props.onStarClick("4")}>
        trigger-unstar-click
      </button>
      <button onClick={props.onReset}>trigger-reset</button>
      <button
        onClick={() =>
          props.onDetailAdded?.({
            cardType: "Description",
            itemLabel: "Recursion",
            conceptId: "1",
            conceptColor: "#fe9a71",
            posX: 1,
            posY: 2,
          })
        }
      >
        trigger-detail-added
      </button>
      <button onClick={() => props.onDetailDeleted?.("Description", "Loops")}>
        trigger-detail-deleted
      </button>
      <button
        onClick={() => props.onDetailMoved?.("Description", "Loops", 5, 6)}
      >
        trigger-detail-moved
      </button>
      <button onClick={() => props.onMajorMoved?.("1", 30, 40)}>
        trigger-major-moved
      </button>
      <button onClick={() => props.onSubconceptMastered("Base case")}>
        trigger-subconcept-mastered
      </button>
      <button onClick={() => props.onSubconceptMastered("For loops")}>
        trigger-unmaster-subconcept
      </button>
      <button onClick={() => props.onPaneClick?.()}>trigger-pane-click</button>
    </div>
  ),
}));

const mockedClient = vi.mocked(client);

const assessments: Assessment[] = [
  { id: "a1", pl_assessment_id: "pl-a1", name: "HW1" },
];

const questions: Question[] = [
  { id: "q1", assessment_id: "a1", pl_question_uuid: "u1", title: "Q1" },
];

const questionConcepts: QuestionConcept[] = [
  {
    id: "qc1",
    question_id: "q1",
    concept_id: "1",
    subconcept_label: "Base case",
  },
];

const majorConcepts: MajorConceptDTO[] = [
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

const positions: Record<string, { x: number; y: number }> = {
  "1": { x: 100, y: 100 },
  "4": { x: 300, y: 100 },
};

const conceptContent: Record<string, ConceptContentDTO> = {
  "1": {
    id: 1,
    parentId: null,
    descriptionHtml: "<p>Recursion description</p>",
    exampleHtml: "<p>def f(): ...</p>",
    practiceUrl:
      "https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000011/",
  },
};

const prereqEdgeData: EdgeDTO[] = [
  { id: 20, sourceId: 4, targetId: 1, color: null },
];

const userState: UserStateV2Response = {
  starred_ids: ["4"],
  detail_cards: [
    {
      cardType: "Description",
      itemLabel: "Loops",
      conceptId: "4",
      conceptColor: "#fe9a71",
      posX: 10,
      posY: 20,
    },
  ] as unknown as UserStateV2Response["detail_cards"],
  mastered_subconcepts: ["For loops"],
  top_level_positions: {},
};

const loggedInWithId = {
  loggedIn: true as const,
  root: { user: { email: "cgaucho@ucsb.edu", id: 42 } },
};

function renderConceptGraphPage(currentUser: unknown, courseId: string = "1") {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  qc.setQueryData(["current user"], currentUser);
  qc.setQueryData(["systemInfo"], systemInfoFixtures.showingNeither);
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/course/${courseId}`]}>
        <Routes>
          <Route path="/course/:courseId" element={<ConceptGraphPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ConceptGraphPage", () => {
  let restoreConsole: () => void;

  beforeEach(() => {
    vi.clearAllMocks();
    // BasicLayout's AppNavbar always fetches /api/systemInfo, which has no
    // real backend in this test environment; silence its logged error.
    restoreConsole = mockConsole();
    mockedClient.fetchAssessments.mockResolvedValue(assessments);
    mockedClient.fetchQuestions.mockResolvedValue(questions);
    mockedClient.fetchQuestionConcepts.mockResolvedValue(questionConcepts);
    mockedClient.fetchConceptGraph.mockResolvedValue(majorConcepts);
    mockedClient.fetchConceptContent.mockResolvedValue(conceptContent);
    mockedClient.fetchConceptPositions.mockResolvedValue(positions);
    mockedClient.fetchConceptEdges.mockResolvedValue(prereqEdgeData);
    mockedClient.fetchUserStateV2.mockResolvedValue(userState);
    mockedClient.saveUserStateV2.mockResolvedValue(undefined);
    mockedClient.logUserActivityV2.mockResolvedValue(undefined);

    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  afterEach(() => {
    restoreConsole();
  });

  test("shows the login screen when not logged in", () => {
    renderConceptGraphPage(currentUserFixtures.notLoggedIn);

    expect(
      screen.getByRole("button", { name: "Log In with Google" }),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("concept-graph-stub")).not.toBeInTheDocument();
  });

  test("shows an invalid course id message instead of fetching when the param isn't numeric", () => {
    renderConceptGraphPage(currentUserFixtures.userOnly, "not-a-number");

    expect(screen.getByText("Invalid course id.")).toBeInTheDocument();
    expect(mockedClient.fetchConceptGraph).not.toHaveBeenCalled();
  });

  test("shows a loading spinner until the concept graph data resolves", async () => {
    let resolveGraph!: (value: MajorConceptDTO[]) => void;
    mockedClient.fetchConceptGraph.mockReturnValue(
      new Promise((resolve) => {
        resolveGraph = resolve;
      }),
    );

    renderConceptGraphPage(currentUserFixtures.userOnly);

    expect(
      await screen.findByTestId("concept-graph-page-loading"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("concept-graph-stub")).not.toBeInTheDocument();

    resolveGraph(majorConcepts);

    expect(await screen.findByTestId("concept-graph-stub")).toBeInTheDocument();
  });

  test("fetches the concept graph data for the course id in the url", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly, "7");

    await screen.findByTestId("concept-graph-stub");

    expect(mockedClient.fetchConceptGraph).toHaveBeenCalledWith(7);
    expect(mockedClient.fetchConceptContent).toHaveBeenCalledWith(7);
    expect(mockedClient.fetchConceptPositions).toHaveBeenCalledWith(7);
    expect(mockedClient.fetchConceptEdges).toHaveBeenCalledWith(7);
  });

  test("shows an error message when fetching the concept graph data fails", async () => {
    mockedClient.fetchConceptGraph.mockRejectedValue(new Error("boom"));

    renderConceptGraphPage(currentUserFixtures.userOnly);

    expect(
      await screen.findByText(/Failed to load concept graph data/),
    ).toBeInTheDocument();
  });

  test("loads assessments on mount when logged in", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);

    fireEvent.click(await screen.findByText("Select assessment…"));
    expect(await screen.findByText("HW1")).toBeInTheDocument();
    expect(mockedClient.fetchAssessments).toHaveBeenCalled();
  });

  test("does not load user state when the current user has no numeric id", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);

    await screen.findByTestId("concept-graph-stub");
    expect(mockedClient.fetchUserStateV2).not.toHaveBeenCalled();
    expect(mockedClient.logUserActivityV2).not.toHaveBeenCalled();
  });

  test("logs in and restores saved user state when the user has a numeric id", async () => {
    renderConceptGraphPage(loggedInWithId);

    await waitFor(() =>
      expect(mockedClient.fetchUserStateV2).toHaveBeenCalledWith(42, 1),
    );
    expect(mockedClient.logUserActivityV2).toHaveBeenCalledWith({
      userid: 42,
      courseId: 1,
      event_type: "login",
      payload: { consented: true },
    });

    expect(await screen.findByText("1 / 2")).toBeInTheDocument();
    expect(await screen.findByTestId("restored-count")).toHaveTextContent("1");
    expect(await screen.findByTestId("mastered-count")).toHaveTextContent("1");
  });

  test("selecting an assessment loads its questions", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    fireEvent.click(await screen.findByText("Select assessment…"));
    fireEvent.mouseDown(await screen.findByText("HW1"));

    expect(mockedClient.fetchQuestions).toHaveBeenCalledWith("a1");
    fireEvent.focus(await screen.findByPlaceholderText("Search questions…"));
    expect(await screen.findByText("Q1")).toBeInTheDocument();
  });

  test("selecting a question highlights its concepts", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    fireEvent.click(await screen.findByText("Select assessment…"));
    fireEvent.mouseDown(await screen.findByText("HW1"));
    fireEvent.focus(await screen.findByPlaceholderText("Search questions…"));
    fireEvent.mouseDown(await screen.findByText("Q1"));

    expect(mockedClient.fetchQuestionConcepts).toHaveBeenCalledWith("q1");
    await waitFor(() =>
      expect(screen.getByTestId("highlighted-count")).not.toHaveTextContent(
        "0",
      ),
    );
  });

  test("clicking a concept from the graph shows its toolbar button", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");

    fireEvent.click(screen.getByText("trigger-concept-click"));

    expect(
      await screen.findByRole("button", { name: "Recursion" }),
    ).toBeInTheDocument();
  });

  test("clicking the concept toolbar button reveals description, example, and practice link", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");
    fireEvent.click(screen.getByText("trigger-concept-click"));
    fireEvent.click(await screen.findByRole("button", { name: "Recursion" }));

    expect(screen.getByText("Description")).toBeInTheDocument();
    expect(screen.getByText("Example")).toBeInTheDocument();
    const practiceLink = screen.getByRole("link", {
      name: /Practice with a/,
    });
    expect(practiceLink).toHaveAttribute(
      "href",
      "https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000011/",
    );
  });

  test("clicking a subconcept button does not show the practice link", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");
    fireEvent.click(screen.getByText("trigger-concept-click"));
    fireEvent.click(await screen.findByRole("button", { name: "Base case" }));

    expect(screen.getByText("Description")).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /Practice with a/ }),
    ).not.toBeInTheDocument();
  });

  test("closing the concept toolbar clears the selection", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");
    fireEvent.click(screen.getByText("trigger-concept-click"));
    fireEvent.click(await screen.findByRole("button", { name: "Recursion" }));

    const conceptButton = screen.getByRole("button", { name: "Recursion" });
    const toolbarRow = conceptButton.parentElement!;
    const closeButton = toolbarRow.lastElementChild as HTMLElement;
    fireEvent.click(closeButton);

    expect(
      screen.queryByRole("button", { name: "Recursion" }),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("highlighted-count")).toHaveTextContent("0");
  });

  test("dragging the Description card sets the drag payload", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");
    fireEvent.click(screen.getByText("trigger-concept-click"));
    fireEvent.click(await screen.findByRole("button", { name: "Recursion" }));

    const setData = vi.fn();
    fireEvent.dragStart(screen.getByText("Description").parentElement!, {
      dataTransfer: { setData, effectAllowed: "" },
    });

    expect(setData).toHaveBeenCalledWith(
      "application/scaffold-card",
      expect.stringContaining('"conceptId":"1"'),
    );
  });

  test("triggering onStarClick from the graph updates the starred count and persists it", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByText("1 / 2");

    fireEvent.click(screen.getByText("trigger-star-click"));

    expect(await screen.findByText("2 / 2")).toBeInTheDocument();
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({
          userid: 42,
          courseId: 1,
          starred_ids: expect.arrayContaining(["4", "1"]),
        }),
      ),
    );
  });

  test("triggering onStarClick on an already-starred concept unstars it", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByText("1 / 2");

    fireEvent.click(screen.getByText("trigger-unstar-click"));

    expect(await screen.findByText("0 / 2")).toBeInTheDocument();
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({ userid: 42, courseId: 1, starred_ids: [] }),
      ),
    );
  });

  test("triggering onReset from the graph clears state and persists it", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByText("1 / 2");

    fireEvent.click(screen.getByText("trigger-reset"));

    expect(await screen.findByText("0 / 2")).toBeInTheDocument();
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith({
        userid: 42,
        courseId: 1,
        starred_ids: [],
        detail_cards: [],
        mastered_subconcepts: [],
        top_level_positions: {},
      }),
    );
  });

  test("triggering onDetailAdded logs activity and persists the new card", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByTestId("concept-graph-stub");

    fireEvent.click(screen.getByText("trigger-detail-added"));

    await waitFor(() =>
      expect(mockedClient.logUserActivityV2).toHaveBeenCalledWith({
        userid: 42,
        courseId: 1,
        event_type: "detail_added_to_graph",
        payload: {
          cardType: "Description",
          itemLabel: "Recursion",
          conceptId: "1",
        },
      }),
    );
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({
          detail_cards: expect.arrayContaining([
            expect.objectContaining({ itemLabel: "Recursion" }),
          ]),
        }),
      ),
    );
  });

  test("triggering onDetailDeleted removes the persisted card", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByText("1 / 2");

    fireEvent.click(screen.getByText("trigger-detail-deleted"));

    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({ detail_cards: [] }),
      ),
    );
  });

  test("triggering onDetailMoved updates the persisted card position", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByText("1 / 2");

    fireEvent.click(screen.getByText("trigger-detail-moved"));

    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({
          detail_cards: [expect.objectContaining({ posX: 5, posY: 6 })],
        }),
      ),
    );
  });

  test("triggering onMajorMoved persists a private top-level position override", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByTestId("concept-graph-stub");

    fireEvent.click(screen.getByText("trigger-major-moved"));

    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({
          top_level_positions: { "1": { x: 30, y: 40 } },
        }),
      ),
    );
  });

  test("triggering onSubconceptMastered toggles it and persists the change", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByTestId("mastered-count");
    expect(screen.getByTestId("mastered-count")).toHaveTextContent("1");

    fireEvent.click(screen.getByText("trigger-subconcept-mastered"));

    expect(await screen.findByTestId("mastered-count")).toHaveTextContent("2");
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({
          mastered_subconcepts: expect.arrayContaining([
            "For loops",
            "Base case",
          ]),
        }),
      ),
    );
  });

  test("triggering onSubconceptMastered on an already-mastered subconcept unmasters it", async () => {
    renderConceptGraphPage(loggedInWithId);
    await screen.findByTestId("mastered-count");
    expect(screen.getByTestId("mastered-count")).toHaveTextContent("1");

    fireEvent.click(screen.getByText("trigger-unmaster-subconcept"));

    expect(await screen.findByTestId("mastered-count")).toHaveTextContent("0");
    await waitFor(() =>
      expect(mockedClient.saveUserStateV2).toHaveBeenCalledWith(
        expect.objectContaining({ mastered_subconcepts: [] }),
      ),
    );
  });

  test("triggering onPaneClick clears the current concept selection", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");
    fireEvent.click(screen.getByText("trigger-concept-click"));
    await screen.findByRole("button", { name: "Recursion" });

    fireEvent.click(screen.getByText("trigger-pane-click"));

    expect(
      screen.queryByRole("button", { name: "Recursion" }),
    ).not.toBeInTheDocument();
  });

  test("does not persist state changes when the logged-in user has no numeric id", async () => {
    renderConceptGraphPage(currentUserFixtures.userOnly);
    await screen.findByTestId("concept-graph-stub");

    fireEvent.click(screen.getByText("trigger-star-click"));

    expect(await screen.findByText("1 / 2")).toBeInTheDocument();
    expect(mockedClient.saveUserStateV2).not.toHaveBeenCalled();
  });

  test("handles a missing saved user state (e.g. a new user) without crashing", async () => {
    mockedClient.fetchUserStateV2.mockResolvedValue(null);
    renderConceptGraphPage(loggedInWithId);

    await waitFor(() =>
      expect(mockedClient.fetchUserStateV2).toHaveBeenCalledWith(42, 1),
    );
    expect(await screen.findByText("0 / 2")).toBeInTheDocument();
    expect(screen.getByTestId("restored-count")).toHaveTextContent("0");
  });
});
