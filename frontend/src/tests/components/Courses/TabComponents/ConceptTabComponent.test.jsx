import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ConceptTabComponent from "main/components/Courses/TabComponents/ConceptTabComponent";
import conceptsFixtures from "fixtures/conceptsFixtures";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import {
  afterAll,
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  test,
  vi,
} from "vitest";

const mockToast = vi.fn();
const postSpy = vi.fn();

vi.mock("react-toastify", async (importOriginal) => ({
  ...(await importOriginal()),
  toast: (message) => mockToast(message),
}));

const server = setupServer(
  http.get("/api/concepts/course", () =>
    HttpResponse.json(conceptsFixtures.severalConcepts),
  ),
  http.post("/api/concept", async ({ request }) => {
    const body = await request.json();
    postSpy(body);
    return HttpResponse.json({ id: 99, ...body });
  }),
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  mockToast.mockClear();
  postSpy.mockClear();
});
afterAll(() => server.close());

describe("ConceptTabComponent tests", () => {
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
        <ConceptTabComponent courseId={7} testIdPrefix="test" {...props} />
      </QueryClientProvider>,
    );

  test("renders fetched concepts and correct data-testid", async () => {
    renderComponent();

    expect(screen.getByText("Concepts")).toBeInTheDocument();
    expect(screen.getByTestId("test-conceptTab")).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByTestId("test-ConceptTable-cell-row-0-col-label"),
      ).toHaveTextContent("Variables");
    });

    expect(
      screen.queryByTestId("test-ConceptTable-cell-row-0-col-Edit-button"),
    ).not.toBeInTheDocument();
  });

  test("submits a new concept using the current course id", async () => {
    renderComponent();

    fireEvent.click(screen.getByTestId("test-post-button"));

    fireEvent.change(screen.getByLabelText("Label"), {
      target: { value: "Recursion" },
    });
    fireEvent.change(screen.getByLabelText("Description"), {
      target: { value: "Functions that call themselves." },
    });
    fireEvent.change(screen.getByLabelText("Example"), {
      target: { value: "factorial(n - 1)" },
    });
    fireEvent.click(screen.getByTestId("ConceptModal-submit"));

    await waitFor(() => {
      expect(postSpy).toHaveBeenCalledWith({
        courseId: 7,
        label: "Recursion",
        description: "Functions that call themselves.",
        example: "factorial(n - 1)",
        x: 0,
        y: 0,
      });
    });

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Concept Recursion created"),
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId("ConceptModal-submit"),
      ).not.toBeInTheDocument(),
    );
  });

  test("renders with custom testIdPrefix", async () => {
    renderComponent({ testIdPrefix: "InstructorCourseShowPage" });

    expect(
      screen.getByTestId("InstructorCourseShowPage-conceptTab"),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "InstructorCourseShowPage-ConceptTable-cell-row-0-col-id",
        ),
      ).toBeInTheDocument();
    });
  });
});
