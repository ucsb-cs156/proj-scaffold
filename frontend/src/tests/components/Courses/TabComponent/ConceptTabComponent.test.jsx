import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConceptTabComponent from "main/components/Courses/TabComponent/ConceptTabComponent";
import conceptsFixtures from "fixtures/conceptsFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
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

// This file uses two request-mocking layers:
//  - MSW (setupServer below) handles CRUD endpoints (/api/concept, /api/concepts/course)
//    used by the ConceptModal/ConceptTable parts of the component.
//  - axios-mock-adapter (axiosMock) handles YAML endpoints (/api/concepts/yaml/*),
//    which are called with raw axios and need per-test reply control.
// onNoMatch:"passthrough" lets unmatched axios requests fall through to MSW.
const axiosMock = new AxiosMockAdapter(axios, { onNoMatch: "passthrough" });

const mockToast = vi.fn();
const mockToastError = vi.fn();

vi.mock("react-toastify", async (importOriginal) => {
  const originalModule = await importOriginal();
  const toast = (x) => mockToast(x);
  toast.error = (x) => mockToastError(x);
  return {
    ...originalModule,
    toast,
  };
});

const SAMPLE_YAML =
  "format: 1\nconcepts:\n  - id: 1\n    label: Recursion\nedges: []\n";

const successReport = {
  success: true,
  errors: [],
  conceptsCreated: 3,
  subconceptsCreated: 2,
  edgesCreated: 1,
  practiceProblemsCreated: 4,
  userStatesCleared: 17,
};

const postSpy = vi.fn();

// MSW server handles CRUD-related requests
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
  axiosMock.reset();
  mockToast.mockClear();
  mockToastError.mockClear();
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

  // renderComponent uses courseId 7 (for CRUD-focused tests)
  const renderComponent = (props = {}) =>
    render(
      <QueryClientProvider client={queryClient}>
        <ConceptTabComponent courseId={7} testIdPrefix="test" {...props} />
      </QueryClientProvider>,
    );

  // renderConceptTabComponent uses courseId 42 (for YAML-focused tests)
  const renderConceptTabComponent = (props = {}) => {
    render(
      <QueryClientProvider client={queryClient}>
        <ConceptTabComponent courseId={42} testIdPrefix="test" {...props} />
      </QueryClientProvider>,
    );
  };

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

  test("renders button labels and the replace-all warning", () => {
    renderConceptTabComponent();
    expect(screen.getByTestId("test-download-yaml-button")).toHaveTextContent(
      "Download Concepts YAML",
    );
    expect(screen.getByTestId("test-upload-yaml-button")).toHaveTextContent(
      "Upload",
    );
    expect(screen.getByText("Upload Concepts YAML")).toBeInTheDocument();
    expect(
      screen.getByText(/Warning: uploading replaces ALL concepts/),
    ).toBeInTheDocument();
  });

  test("download button fetches the YAML and saves it as a file", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(200, SAMPLE_YAML);

    const createObjectURL = vi.fn(() => "blob:fake-url");
    const revokeObjectURL = vi.fn();
    window.URL.createObjectURL = createObjectURL;
    window.URL.revokeObjectURL = revokeObjectURL;
    let hrefDuringClick = null;
    let connectedDuringClick = null;
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(function () {
        hrefDuringClick = this.getAttribute("href");
        connectedDuringClick = this.isConnected;
      });

    renderConceptTabComponent();
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    const yamlGets = () =>
      axiosMock.history.get.filter(
        (r) => r.url === "/api/concepts/yaml/download",
      );
    await waitFor(() => expect(yamlGets().length).toBe(1));
    expect(yamlGets()[0].params).toEqual({ courseId: 42 });

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    const blob = createObjectURL.mock.calls[0][0];
    expect(blob.type).toBe("application/x-yaml");
    expect(await blob.text()).toBe(SAMPLE_YAML);
    // The temporary anchor points at the blob, is attached to the document when
    // clicked, and is cleaned up (removed, object URL revoked) afterward.
    expect(hrefDuringClick).toBe("blob:fake-url");
    expect(connectedDuringClick).toBe(true);
    expect(document.querySelector("a[download]")).toBeNull();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:fake-url");
    expect(mockToastError).not.toHaveBeenCalled();

    click.mockRestore();
  });

  test("download names the file after the course", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(200, SAMPLE_YAML);

    window.URL.createObjectURL = vi.fn(() => "blob:fake-url");
    window.URL.revokeObjectURL = vi.fn();
    let downloadAttribute = null;
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(function () {
        downloadAttribute = this.getAttribute("download");
      });

    renderConceptTabComponent({ courseId: 7 });
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(downloadAttribute).toBe("concepts-course-7.yaml");

    click.mockRestore();
  });

  test("download shows an error toast when the request fails", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(403);

    renderConceptTabComponent();
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        "Error downloading concepts YAML: Request failed with status code 403",
      ),
    );
  });

  test("upload posts the file and reports what was created", async () => {
    axiosMock.onPost("/api/concepts/yaml/upload").reply(200, successReport);

    renderConceptTabComponent();

    const file = new File([SAMPLE_YAML], "concepts.yaml", {
      type: "application/x-yaml",
    });
    await userEvent.upload(screen.getByTestId("test-upload-yaml-input"), file);
    fireEvent.click(screen.getByTestId("test-upload-yaml-button"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/concepts/yaml/upload");
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 42 });
    const formData = axiosMock.history.post[0].data;
    expect(formData.get("file")).toBe(file);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Concepts replaced: 3 concepts, 2 subconcepts, 1 edges, " +
          "4 practice problems. Saved scaffold state was cleared for 17 user(s).",
      ),
    );
  });

  test("upload shows the backend's error report when the file is invalid", async () => {
    const failureReport = {
      success: false,
      errors: ["concepts[0]: id is required"],
      conceptsCreated: 0,
      subconceptsCreated: 0,
      edgesCreated: 0,
      practiceProblemsCreated: 0,
      userStatesCleared: 0,
    };
    axiosMock.onPost("/api/concepts/yaml/upload").reply(400, failureReport);

    renderConceptTabComponent();

    const file = new File(["format: 2"], "concepts.yaml", {
      type: "application/x-yaml",
    });
    await userEvent.upload(screen.getByTestId("test-upload-yaml-input"), file);
    fireEvent.click(screen.getByTestId("test-upload-yaml-button"));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        `Error uploading concepts YAML: ${JSON.stringify(failureReport, null, 2)}`,
      ),
    );
    expect(mockToast).not.toHaveBeenCalled();
  });

  test("upload requires a file", async () => {
    renderConceptTabComponent();

    fireEvent.click(screen.getByTestId("test-upload-yaml-button"));

    await waitFor(() =>
      expect(
        screen.getByText("Concepts YAML file is required."),
      ).toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
  });
});
