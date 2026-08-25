import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";
import * as useBackendModule from "main/utils/useBackend";
import AdminUsersPage from "main/pages/Admin/AdminUsersPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");

const users = Array.from({ length: 12 }, (_, index) => ({
  id: index + 1,
  givenName: `First${index + 1}`,
  familyName: `Last${index + 1}`,
  email: `user${index + 1}@example.com`,
  admin: index === 0,
  instructor: index === 1,
}));

describe("AdminUsersPage tests", () => {
  let queryClient;

  const setupAdminUser = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock.onGet("/api/courses/list").reply(200, []);
  };

  beforeEach(() => {
    queryClient = new QueryClient();
  });

  afterEach(() => {
    useBackendSpy.mockClear();
    queryClient.clear();
  });

  test("renders first page with page size selector", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/users").reply(200, users);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("UsersTable-cell-row-0-col-email"),
      ).toBeInTheDocument();
    });

    expect(
      screen.getByTestId("UsersTable-cell-row-0-col-email"),
    ).toHaveTextContent("user1@example.com");
    expect(
      screen.getByTestId("UsersTable-cell-row-9-col-email"),
    ).toHaveTextContent("user10@example.com");
    expect(screen.queryByText("user11@example.com")).not.toBeInTheDocument();

    const pageSize = screen.getByTestId("AdminUsersPage-page-size");
    expect(pageSize).toHaveValue("10");
    expect(pageSize).toHaveTextContent("10");
    expect(pageSize).toHaveTextContent("25");
    expect(pageSize).toHaveTextContent("50");
    expect(pageSize).toHaveTextContent("100");
    expect(pageSize).toHaveTextContent("500");
  });

  test("navigates to later pages", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/users").reply(200, users);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("AdminUsersPage-page-2")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("AdminUsersPage-next"));

    await waitFor(() => {
      expect(screen.getByText("user11@example.com")).toBeInTheDocument();
    });
    expect(screen.getByText("user12@example.com")).toBeInTheDocument();
    expect(screen.queryByText("user1@example.com")).not.toBeInTheDocument();
  });

  test("changing page size resets pagination", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/users").reply(200, users);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("AdminUsersPage-page-2")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("AdminUsersPage-next"));
    await waitFor(() => {
      expect(screen.getByText("user11@example.com")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId("AdminUsersPage-page-size"), {
      target: { value: "25" },
    });

    await waitFor(() => {
      expect(screen.getByText("user1@example.com")).toBeInTheDocument();
    });
    expect(screen.getByText("user12@example.com")).toBeInTheDocument();
    expect(
      screen.queryByTestId("AdminUsersPage-page-2"),
    ).not.toBeInTheDocument();
  });

  test("useBackend is called with correct cache query key", () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/users").reply(200, users);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/admin/users"],
      { method: "GET", url: "/api/admin/users" },
      [],
    );
  });
});
