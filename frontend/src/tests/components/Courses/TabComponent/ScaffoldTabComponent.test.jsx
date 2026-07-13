import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ScaffoldTabComponent from "main/components/Courses/TabComponent/ScaffoldTabComponent";
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

const renderScaffoldTabComponent = (props = {}) => {
  const queryClient = new QueryClient();

  render(
    <QueryClientProvider client={queryClient}>
      <ScaffoldTabComponent
        courseId={1}
        courseName="CMPSC 8"
        term="S26"
        school={{ key: "UCSB", displayName: "UCSB" }}
        testIdPrefix="test"
        {...props}
      />
    </QueryClientProvider>,
  );
};

describe("ScaffoldTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    mockToast.mockClear();
    mockToastError.mockClear();
    axiosMock
      .onPost("/api/course/scaffold/reset")
      .reply(200, { message: "Scaffold reset completed." });
    axiosMock.onGet("/api/courses/list").reply(200, []);
  });

  test("renders the Scaffold heading", () => {
    renderScaffoldTabComponent();
    expect(screen.getByText("Scaffold")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    renderScaffoldTabComponent();
    expect(screen.getByTestId("test-scaffoldTab")).toBeInTheDocument();
    expect(screen.getByTestId("test-reset-button")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    renderScaffoldTabComponent({
      testIdPrefix: "InstructorCourseShowPage",
    });
    expect(
      screen.getByTestId("InstructorCourseShowPage-scaffoldTab"),
    ).toBeInTheDocument();
  });

  test("reset button posts scaffold reset for the course and shows success feedback", async () => {
    renderScaffoldTabComponent({ courseId: 42 });

    fireEvent.click(screen.getByTestId("test-reset-button"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({ courseId: 42 });
    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Scaffold reset successfully completed.",
      ),
    );
  });

  test("renders button labels and the replace-all warning", () => {
    renderScaffoldTabComponent();
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

    renderScaffoldTabComponent({ courseId: 42 });
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

    renderScaffoldTabComponent({
      courseId: 7,
      courseName: "CMPSC 8",
      term: "S26",
      school: { key: "UCSB", displayName: "UCSB" },
    });
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(downloadAttribute).toBe("Scaffold-CMPSC-8-S26-UCSB-7.yml");

    click.mockRestore();
  });

  test("download sanitizes course name and term when building the filename", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(200, SAMPLE_YAML);

    window.URL.createObjectURL = vi.fn(() => "blob:fake-url");
    window.URL.revokeObjectURL = vi.fn();
    let downloadAttribute = null;
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(function () {
        downloadAttribute = this.getAttribute("download");
      });

    renderScaffoldTabComponent({
      courseId: 42,
      courseName: "CMPSC 156: Intro to S/W Eng.",
      term: "Fall 2026",
      school: { key: "OTHER", displayName: "Other" },
    });
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(downloadAttribute).toBe(
      "Scaffold-CMPSC-156-Intro-to-S-W-Eng-Fall-2026-OTHER-42.yml",
    );

    click.mockRestore();
  });

  test("download omits missing course details instead of leaving blank segments", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(200, SAMPLE_YAML);

    window.URL.createObjectURL = vi.fn(() => "blob:fake-url");
    window.URL.revokeObjectURL = vi.fn();
    let downloadAttribute = null;
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(function () {
        downloadAttribute = this.getAttribute("download");
      });

    renderScaffoldTabComponent({
      courseId: 9,
      courseName: undefined,
      term: undefined,
      school: undefined,
    });
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() => expect(click).toHaveBeenCalledTimes(1));
    expect(downloadAttribute).toBe("Scaffold-9.yml");

    click.mockRestore();
  });

  test("download shows an error toast when the request fails", async () => {
    axiosMock.onGet("/api/concepts/yaml/download").reply(403);

    renderScaffoldTabComponent({ courseId: 42 });
    fireEvent.click(screen.getByTestId("test-download-yaml-button"));

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        "Error downloading concepts YAML: Request failed with status code 403",
      ),
    );
  });

  test("upload posts the file and reports what was created", async () => {
    axiosMock.onPost("/api/concepts/yaml/upload").reply(200, successReport);

    renderScaffoldTabComponent({ courseId: 42 });

    const file = new File([SAMPLE_YAML], "concepts.yaml", {
      type: "application/x-yaml",
    });
    await userEvent.upload(screen.getByTestId("test-upload-yaml-input"), file);
    fireEvent.click(screen.getByTestId("test-upload-yaml-button"));

    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(
          (r) => r.url === "/api/concepts/yaml/upload",
        ).length,
      ).toBe(1),
    );
    const uploadRequest = axiosMock.history.post.find(
      (r) => r.url === "/api/concepts/yaml/upload",
    );
    expect(uploadRequest.params).toEqual({ courseId: 42 });
    const formData = uploadRequest.data;
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

    renderScaffoldTabComponent({ courseId: 42 });

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
    renderScaffoldTabComponent({ courseId: 42 });

    fireEvent.click(screen.getByTestId("test-upload-yaml-button"));

    await waitFor(() =>
      expect(
        screen.getByText("Concepts YAML file is required."),
      ).toBeInTheDocument(),
    );
    expect(
      axiosMock.history.post.filter(
        (r) => r.url === "/api/concepts/yaml/upload",
      ).length,
    ).toBe(0);
  });

  const otherCourses = [
    {
      id: 2,
      courseName: "CMPSC 156",
      term: "F25",
      school: { key: "UCSB", displayName: "UCSB" },
      instructorEmail: "instructor@example.org",
      studentAccess: false,
      staffAccess: false,
      instructorAccess: true,
      adminAccess: false,
    },
    {
      id: 3,
      courseName: "CMPSC 24",
      term: "F25",
      school: { key: "UCSB", displayName: "UCSB" },
      instructorEmail: "staffprof@example.org",
      studentAccess: false,
      staffAccess: true,
      instructorAccess: false,
      adminAccess: false,
    },
    {
      id: 1,
      courseName: "CMPSC 8",
      term: "S26",
      school: { key: "UCSB", displayName: "UCSB" },
      instructorEmail: "self@example.org",
      studentAccess: false,
      staffAccess: false,
      instructorAccess: true,
      adminAccess: false,
    },
  ];

  test("renders the Copy Concept Graph section with courses grouped by access, excluding the current course", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, otherCourses);

    renderScaffoldTabComponent({ courseId: 1 });

    expect(
      screen.getByText("Copy Concept Graph from Another Course"),
    ).toBeInTheDocument();

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 156/ }),
      ).toBeInTheDocument(),
    );
    expect(
      screen.getByRole("option", { name: /CMPSC 24/ }),
    ).toBeInTheDocument();
    // The current course (id 1) is not offered as a "from" choice.
    expect(
      screen.queryByRole("option", { name: /CMPSC 8/ }),
    ).not.toBeInTheDocument();
  });

  test("shows an Admin group listing every course when the user has adminAccess", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, [
      {
        id: 5,
        courseName: "CMPSC 130",
        term: "F25",
        school: { key: "UCSB", displayName: "UCSB" },
        instructorEmail: "someone@example.org",
        studentAccess: false,
        staffAccess: false,
        instructorAccess: false,
        adminAccess: true,
      },
    ]);

    renderScaffoldTabComponent({ courseId: 1 });

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 130/ }),
      ).toBeInTheDocument(),
    );
    const select = screen.getByTestId(
      "test-copy-concept-graph-from-course-select",
    );
    expect(select.querySelector("optgroup[label='Admin']")).not.toBeNull();
  });

  test("Copy Concept Graph button is disabled until a from-course is selected", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, otherCourses);

    renderScaffoldTabComponent({ courseId: 1 });

    const copyButton = screen.getByTestId("test-copy-concept-graph-button");
    expect(copyButton).toBeDisabled();

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 156/ }),
      ).toBeInTheDocument(),
    );
    fireEvent.change(
      screen.getByTestId("test-copy-concept-graph-from-course-select"),
      { target: { value: "2" } },
    );

    expect(copyButton).not.toBeDisabled();
  });

  test("clicking Copy Concept Graph opens the confirmation modal, and Yes launches the job", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, otherCourses);
    axiosMock
      .onPost("/api/jobs/launch/copyConceptGraph")
      .reply(200, { id: 42, status: "queued" });

    renderScaffoldTabComponent({ courseId: 1 });

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 156/ }),
      ).toBeInTheDocument(),
    );
    fireEvent.change(
      screen.getByTestId("test-copy-concept-graph-from-course-select"),
      { target: { value: "2" } },
    );
    fireEvent.click(screen.getByTestId("test-copy-concept-graph-button"));

    expect(
      screen.getByText(
        "This will replace ALL content in the Concept Graph, and erase all User State; are you sure?",
      ),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByTestId("test-copyConceptGraphModal-yes-button"),
    );

    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(
          (r) => r.url === "/api/jobs/launch/copyConceptGraph",
        ).length,
      ).toBe(1),
    );
    const request = axiosMock.history.post.find(
      (r) => r.url === "/api/jobs/launch/copyConceptGraph",
    );
    expect(request.params).toEqual({ fromCourseId: "2", toCourseId: 1 });

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Copy Concept Graph job (id 42) launched. You can monitor its progress on the Jobs tab.",
      ),
    );
  });

  test("clicking No on the confirmation modal does not launch the job", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, otherCourses);

    renderScaffoldTabComponent({ courseId: 1 });

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 156/ }),
      ).toBeInTheDocument(),
    );
    fireEvent.change(
      screen.getByTestId("test-copy-concept-graph-from-course-select"),
      { target: { value: "2" } },
    );
    fireEvent.click(screen.getByTestId("test-copy-concept-graph-button"));
    fireEvent.click(screen.getByTestId("test-copyConceptGraphModal-no-button"));

    expect(
      axiosMock.history.post.filter(
        (r) => r.url === "/api/jobs/launch/copyConceptGraph",
      ).length,
    ).toBe(0);
  });

  test("shows an error toast when launching the Copy Concept Graph job fails", async () => {
    axiosMock.onGet("/api/courses/list").reply(200, otherCourses);
    axiosMock
      .onPost("/api/jobs/launch/copyConceptGraph")
      .reply(403, { message: "Forbidden" });

    renderScaffoldTabComponent({ courseId: 1 });

    await waitFor(() =>
      expect(
        screen.getByRole("option", { name: /CMPSC 156/ }),
      ).toBeInTheDocument(),
    );
    fireEvent.change(
      screen.getByTestId("test-copy-concept-graph-from-course-select"),
      { target: { value: "2" } },
    );
    fireEvent.click(screen.getByTestId("test-copy-concept-graph-button"));
    fireEvent.click(
      screen.getByTestId("test-copyConceptGraphModal-yes-button"),
    );

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith(
        "Error launching Copy Concept Graph job: Forbidden",
      ),
    );
  });
});
