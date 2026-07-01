import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import AppNavbar from "main/components/Nav/AppNavbar";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

function renderNavbar(currentUser, systemInfo) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  qc.setQueryData(["current user"], currentUser);
  qc.setQueryData(["systemInfo"], systemInfo);
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <AppNavbar />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AppNavbar tests", () => {
  test("renders Log In button when not logged in", () => {
    renderNavbar(
      currentUserFixtures.notLoggedIn,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.getByText("Log In")).toBeInTheDocument();
    expect(screen.queryByText("Log Out")).not.toBeInTheDocument();
  });

  test("renders user email and Log Out button when logged in", () => {
    renderNavbar(
      currentUserFixtures.userOnly,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.getByText("Log Out")).toBeInTheDocument();
    expect(screen.queryByText("Log In")).not.toBeInTheDocument();
  });

  test("renders Scaffold brand link", () => {
    renderNavbar(
      currentUserFixtures.notLoggedIn,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.getByText("Scaffold")).toBeInTheDocument();
  });

  test("renders UCSB CS concept graph subtitle", () => {
    renderNavbar(
      currentUserFixtures.notLoggedIn,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.getByText("UCSB CS concept graph")).toBeInTheDocument();
  });

  test("renders Admin menu with Developer Info for admins", () => {
    renderNavbar(
      currentUserFixtures.adminUser,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.getByText("Admin")).toBeInTheDocument();
    expect(screen.getByText("Developer Info")).toBeInTheDocument();
  });

  test("does not render Admin menu for non-admin users", () => {
    renderNavbar(
      currentUserFixtures.userOnly,
      systemInfoFixtures.showingNeither,
    );
    expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    expect(screen.queryByText("Developer Info")).not.toBeInTheDocument();
  });
});
