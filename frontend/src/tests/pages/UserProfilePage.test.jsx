import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

import UserProfilePage from "main/pages/UserProfilePage";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

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

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <UserProfilePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("UserProfilePage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders name, email, and roles for a regular user, without a PAT section", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "cgaucho@ucsb.edu", fullName: "Chris Gaucho" },
      roles: [{ authority: "ROLE_USER" }],
    });

    renderPage();

    expect(
      await screen.findByTestId("UserProfilePage-title"),
    ).toHaveTextContent("User Profile");
    expect(screen.getByTestId("UserProfilePage-name")).toHaveTextContent(
      "Chris Gaucho",
    );
    expect(screen.getByTestId("UserProfilePage-email")).toHaveTextContent(
      "cgaucho@ucsb.edu",
    );
    expect(screen.getByTestId("UserProfilePage-roles")).toHaveTextContent(
      "user",
    );
    expect(
      screen.queryByTestId("UserProfilePage-pat-section"),
    ).not.toBeInTheDocument();
  });

  test("shows 'Not specified' when the user has no fullName", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "cgaucho@ucsb.edu" },
      roles: [{ authority: "ROLE_USER" }],
    });

    renderPage();

    expect(await screen.findByTestId("UserProfilePage-name")).toHaveTextContent(
      "Not specified",
    );
  });

  test("shows the GitHub PAT section with 'No PAT set' for admin/instructor users without a PAT", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "admin@ucsb.edu", fullName: "Admin User" },
      roles: [
        { authority: "ROLE_USER" },
        { authority: "ROLE_ADMIN" },
        { authority: "ROLE_INSTRUCTOR" },
      ],
    });
    axiosMock.onGet("/api/pat").reply(404, {
      type: "EntityNotFoundException",
      message: "PatCredential with id 1 not found",
    });

    renderPage();

    expect(
      await screen.findByTestId("UserProfilePage-roles"),
    ).toHaveTextContent("user, admin, instructor");
    expect(
      screen.getByTestId("UserProfilePage-pat-section"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByTestId("UserProfilePage-pat-status"),
      ).toHaveTextContent("No PAT set"),
    );
  });

  test("shows the masked PAT and expiration date when a PAT is set", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "diba@ucsb.edu", fullName: "Diba Instructor" },
      roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_INSTRUCTOR" }],
    });
    axiosMock.onGet("/api/pat").reply(200, {
      id: 7,
      userId: 1,
      lastFour: "3f2a",
      expiresAt: "2026-12-31",
    });

    renderPage();

    await waitFor(() =>
      expect(
        screen.getByTestId("UserProfilePage-pat-status"),
      ).toHaveTextContent("PAT: ******3f2a, expires: 2026-12-31"),
    );
  });

  test("shows 'not specified' for the expiration date when it is null", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "diba@ucsb.edu", fullName: "Diba Instructor" },
      roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_INSTRUCTOR" }],
    });
    axiosMock.onGet("/api/pat").reply(200, {
      id: 7,
      userId: 1,
      lastFour: "3f2a",
      expiresAt: null,
    });

    renderPage();

    await waitFor(() =>
      expect(
        screen.getByTestId("UserProfilePage-pat-status"),
      ).toHaveTextContent("PAT: ******3f2a, expires: not specified"),
    );
  });

  test("submits the PAT form and shows a success toast", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "diba@ucsb.edu", fullName: "Diba Instructor" },
      roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_INSTRUCTOR" }],
    });
    axiosMock.onGet("/api/pat").reply(404, {});
    axiosMock.onPost("/api/pat").reply(200, {
      id: 7,
      userId: 1,
      lastFour: "6789",
      expiresAt: null,
    });

    renderPage();

    await screen.findByTestId("UserProfilePage-pat-submit");

    fireEvent.change(screen.getByTestId("UserProfilePage-pat-token"), {
      target: { value: "******" },
    });
    fireEvent.click(screen.getByTestId("UserProfilePage-pat-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      token: "******",
      expiresAt: undefined,
    });
    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("GitHub PAT saved"),
    );
  });

  test("requires a token before submitting the PAT form", async () => {
    axiosMock.onGet("/api/currentUser").reply(200, {
      user: { email: "diba@ucsb.edu", fullName: "Diba Instructor" },
      roles: [{ authority: "ROLE_USER" }, { authority: "ROLE_INSTRUCTOR" }],
    });
    axiosMock.onGet("/api/pat").reply(404, {});

    renderPage();

    await screen.findByTestId("UserProfilePage-pat-submit");
    fireEvent.click(screen.getByTestId("UserProfilePage-pat-submit"));

    await waitFor(() =>
      expect(screen.getByText("A GitHub PAT is required.")).toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
  });
});
