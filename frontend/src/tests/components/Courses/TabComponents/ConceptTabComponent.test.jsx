import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ConceptTabComponent from "main/components/Courses/TabComponents/ConceptTabComponent";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
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

const renderConceptTabComponent = (props = {}) => {
  const queryClient = new QueryClient();

  render(
    <QueryClientProvider client={queryClient}>
      <ConceptTabComponent courseId={42} testIdPrefix="test" {...props} />
    </QueryClientProvider>,
  );
};

describe("ConceptTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    mockToast.mockClear();
    mockToastError.mockClear();
  });

  test("renders the Concepts heading", () => {
    renderConceptTabComponent();
    expect(screen.getByText("Concepts")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    renderConceptTabComponent();
    expect(screen.getByTestId("test-conceptTab")).toBeInTheDocument();
    expect(screen.getByTestId("test-download-yaml-button")).toBeInTheDocument();
    expect(screen.getByTestId("test-upload-yaml-input")).toBeInTheDocument();
    expect(screen.getByTestId("test-upload-yaml-button")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    renderConceptTabComponent({ testIdPrefix: "InstructorCourseShowPage" });
    expect(
      screen.getByTestId("InstructorCourseShowPage-conceptTab"),
    ).toBeInTheDocument();
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

    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    expect(axiosMock.history.get[0].url).toBe("/api/concepts/yaml/download");
    expect(axiosMock.history.get[0].params).toEqual({ courseId: 42 });

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
