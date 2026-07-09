import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import EdgeConceptTabComponent from "main/components/Courses/TabComponents/EdgeConceptTabComponent";
import conceptsFixtures from "fixtures/conceptsFixtures";
import edgesFixtures from "fixtures/edgesFixtures";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import {
  afterAll,
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from "vitest";

const mockToast = vi.fn();
const postSpy = vi.fn();
const deleteSpy = vi.fn();

vi.mock("react-toastify", async (importOriginal) => ({
  ...(await importOriginal()),
  toast: (message) => mockToast(message),
}));

const server = setupServer(
  http.get("/api/concepts/course", () =>
    HttpResponse.json(conceptsFixtures.severalConcepts),
  ),
  http.get("/api/concepts/edges", () =>
    HttpResponse.json(edgesFixtures.severalEdges),
  ),
  http.post("/api/concepts/edges/post", ({ request }) => {
    const url = new URL(request.url);
    postSpy(Object.fromEntries(url.searchParams));
    return HttpResponse.json({
      id: 99,
      sourceId: Number(url.searchParams.get("sourceConceptId")),
      targetId: Number(url.searchParams.get("targetConceptId")),
      color: null,
    });
  }),
  http.delete("/api/concepts/edges/delete", ({ request }) => {
    const url = new URL(request.url);
    deleteSpy(Object.fromEntries(url.searchParams));
    return HttpResponse.json({
      message: `ConceptEdge with id ${url.searchParams.get("id")} deleted`,
    });
  }),
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  mockToast.mockClear();
  postSpy.mockClear();
  deleteSpy.mockClear();
});
afterAll(() => server.close());

describe("EdgeConceptTabComponent tests", () => {
  let queryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
  });

  const renderComponent = (props = {}) =>
    render(
      <QueryClientProvider client={queryClient}>
        <EdgeConceptTabComponent courseId={7} testIdPrefix="test" {...props} />
      </QueryClientProvider>,
    );

  test("renders the Edges heading and correct data-testid", async () => {
    renderComponent();
    expect(screen.getByText("Edges")).toBeInTheDocument();
    expect(screen.getByTestId("test-edgeConceptTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    renderComponent({ testIdPrefix: "InstructorCourseShowPage" });
    expect(
      screen.getByTestId("InstructorCourseShowPage-edgeConceptTab"),
    ).toBeInTheDocument();
  });

  test("renders fetched edges with concept labels", async () => {
    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByTestId("test-EdgeTable-cell-row-0-col-sourceLabel"),
      ).toHaveTextContent("Variables");
    });
    expect(
      screen.getByTestId("test-EdgeTable-cell-row-0-col-targetLabel"),
    ).toHaveTextContent("Loops");
    expect(
      screen.getByTestId("test-EdgeTable-cell-row-0-col-id"),
    ).toHaveTextContent("10");
  });

  test("falls back to id when a concept label is unknown", async () => {
    server.use(
      http.get("/api/concepts/edges", () =>
        HttpResponse.json([
          { id: 42, sourceId: 1, targetId: 999, color: null },
        ]),
      ),
    );
    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByTestId("test-EdgeTable-cell-row-0-col-targetLabel"),
      ).toHaveTextContent("id 999");
    });
    expect(
      screen.getByTestId("test-EdgeTable-cell-row-0-col-sourceLabel"),
    ).toHaveTextContent("Variables");
  });

  test("populates the source and target dropdowns with concept labels", async () => {
    renderComponent();

    const sourceSelect = await screen.findByTestId("test-source-select");
    const targetSelect = screen.getByTestId("test-target-select");

    await waitFor(() => {
      expect(sourceSelect).toContainHTML("Variables");
    });
    expect(sourceSelect).toContainHTML("Loops");
    expect(targetSelect).toContainHTML("Variables");
    expect(targetSelect).toContainHTML("Loops");
  });

  test("create button is disabled until different from/to concepts are selected", async () => {
    renderComponent();

    const createButton = screen.getByTestId("test-create-edge-button");
    expect(createButton).toBeDisabled();

    const sourceSelect = await screen.findByTestId("test-source-select");
    const targetSelect = screen.getByTestId("test-target-select");

    await waitFor(() => {
      expect(sourceSelect).toContainHTML("Variables");
    });

    fireEvent.change(sourceSelect, { target: { value: "1" } });
    expect(createButton).toBeDisabled();

    fireEvent.change(targetSelect, { target: { value: "1" } });
    expect(createButton).toBeDisabled();

    fireEvent.change(targetSelect, { target: { value: "2" } });
    expect(createButton).toBeEnabled();
  });

  test("submits a new edge with the selected concepts", async () => {
    renderComponent();

    const sourceSelect = await screen.findByTestId("test-source-select");
    const targetSelect = screen.getByTestId("test-target-select");

    await waitFor(() => {
      expect(sourceSelect).toContainHTML("Variables");
    });

    fireEvent.change(sourceSelect, { target: { value: "1" } });
    fireEvent.change(targetSelect, { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("test-create-edge-button"));

    await waitFor(() => {
      expect(postSpy).toHaveBeenCalledWith({
        sourceConceptId: "1",
        targetConceptId: "2",
      });
    });
    await waitFor(() => expect(mockToast).toHaveBeenCalledWith("Edge created"));

    // selections are reset after a successful create
    expect(sourceSelect).toHaveValue("");
    expect(targetSelect).toHaveValue("");
  });

  test("deletes an edge when the Delete button is clicked", async () => {
    renderComponent();

    const deleteButton = await screen.findByTestId(
      "test-EdgeTable-cell-row-0-col-Delete-button",
    );
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith({ id: "10" });
    });
    await waitFor(() => expect(mockToast).toHaveBeenCalledWith("Edge deleted"));
  });
});
