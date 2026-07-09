import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ScaffoldTabComponent from "main/components/Courses/TabComponents/ScaffoldTabComponent";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const renderScaffoldTabComponent = (props = {}) => {
  const queryClient = new QueryClient();

  render(
    <QueryClientProvider client={queryClient}>
      <ScaffoldTabComponent courseId={1} testIdPrefix="test" {...props} />
    </QueryClientProvider>,
  );
};

describe("ScaffoldTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    mockToast.mockClear();
    axiosMock
      .onPost("/api/course/scaffold/reset")
      .reply(200, { message: "Scaffold reset completed." });
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
});
