import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

import PLTabComponent from "main/components/Courses/TabComponent/PLTabComponent";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  const originalModule = await importOriginal();
  const toast = (x) => mockToast(x);
  toast.error = (x) => mockToast(x);
  return {
    ...originalModule,
    toast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);

function renderTab() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <PLTabComponent courseId={7} testIdPrefix="test" />
    </QueryClientProvider>,
  );
}

async function submitRepoName(repoName) {
  fireEvent.change(screen.getByTestId("test-plTab-repoName"), {
    target: { value: repoName },
  });
  fireEvent.click(screen.getByTestId("test-plTab-repo-submit"));
  await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
}

describe("PLTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
  });

  test("renders the headings and repo form with correct testids", () => {
    renderTab();
    expect(screen.getByText("PrairieLearn")).toBeInTheDocument();
    expect(screen.getByText("GitHub Repo")).toBeInTheDocument();
    expect(screen.getByTestId("test-plTab")).toBeInTheDocument();
    expect(screen.getByTestId("test-plTab-repoName")).toBeInTheDocument();
    expect(screen.getByTestId("test-plTab-repo-submit")).toBeInTheDocument();
    expect(
      screen.queryByTestId("test-plTab-repo-error"),
    ).not.toBeInTheDocument();
  });

  test("submits the repo name to updateGithubRepo and shows a success toast", async () => {
    axiosMock.onPut("/api/courses/updateGithubRepo").reply(200, {
      id: 7,
      plRepoId: 9,
    });

    renderTab();
    await submitRepoName("ucsb-cs156/pl-demo");

    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 7,
      repoName: "ucsb-cs156/pl-demo",
    });
    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "GitHub repo associated with course",
      ),
    );
    expect(
      screen.queryByTestId("test-plTab-repo-error"),
    ).not.toBeInTheDocument();
  });

  test.each([
    ["must set up Github PAT first"],
    ["No access to repo via Github PAT token"],
    ["Read/write access to repo via Github PAT is required"],
  ])(
    "shows the expected 403 error '%s' on the page, not in a toast",
    async (message) => {
      axiosMock.onPut("/api/courses/updateGithubRepo").reply(403, {
        type: "ForbiddenException",
        message,
      });

      renderTab();
      await submitRepoName("ucsb-cs156/pl-demo");

      expect(
        await screen.findByTestId("test-plTab-repo-error"),
      ).toHaveTextContent(message);
      expect(mockToast).not.toHaveBeenCalled();
    },
  );

  test("uses a toast for an unexpected error status", async () => {
    axiosMock.onPut("/api/courses/updateGithubRepo").reply(404, {
      type: "EntityNotFoundException",
      message: "Course with id 7 not found",
    });

    renderTab();
    await submitRepoName("ucsb-cs156/pl-demo");

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Course with id 7 not found"),
    );
    expect(
      screen.queryByTestId("test-plTab-repo-error"),
    ).not.toBeInTheDocument();
  });

  test("uses a toast for a 403 with an unexpected message", async () => {
    axiosMock.onPut("/api/courses/updateGithubRepo").reply(403, {
      message: "Access Denied",
    });

    renderTab();
    await submitRepoName("ucsb-cs156/pl-demo");

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Access Denied"),
    );
    expect(
      screen.queryByTestId("test-plTab-repo-error"),
    ).not.toBeInTheDocument();
  });

  test("uses a generic toast when the failure has no message at all", async () => {
    axiosMock.onPut("/api/courses/updateGithubRepo").networkError();

    renderTab();
    await submitRepoName("ucsb-cs156/pl-demo");

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Error associating GitHub repo"),
    );
  });

  test("clears a previous on-page error after a later successful submit", async () => {
    axiosMock.onPut("/api/courses/updateGithubRepo").replyOnce(403, {
      message: "must set up Github PAT first",
    });

    renderTab();
    await submitRepoName("ucsb-cs156/pl-demo");
    expect(
      await screen.findByTestId("test-plTab-repo-error"),
    ).toBeInTheDocument();

    axiosMock.onPut("/api/courses/updateGithubRepo").reply(200, { id: 7 });
    fireEvent.click(screen.getByTestId("test-plTab-repo-submit"));

    await waitFor(() =>
      expect(
        screen.queryByTestId("test-plTab-repo-error"),
      ).not.toBeInTheDocument(),
    );
  });

  test("requires a repo name before submitting", async () => {
    renderTab();

    fireEvent.click(screen.getByTestId("test-plTab-repo-submit"));

    await waitFor(() =>
      expect(screen.getByText("A repo name is required.")).toBeInTheDocument(),
    );
    expect(axiosMock.history.put.length).toBe(0);
  });
});
