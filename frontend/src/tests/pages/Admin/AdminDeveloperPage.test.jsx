import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AdminDeveloperPage from "main/pages/Admin/AdminDeveloperPage";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { MemoryRouter } from "react-router";

function renderPage(systemInfo) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  qc.setQueryData(["systemInfo"], systemInfo);
  return render(
    <MemoryRouter>
      <QueryClientProvider client={qc}>
        <AdminDeveloperPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe("AdminDeveloperPage tests", () => {
  test("renders developer information with backend links", () => {
    renderPage(systemInfoFixtures.showingNeither);

    expect(screen.getByText("Developer Information")).toBeInTheDocument();
    expect(screen.getByText("Current Deployed Branch")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Swagger" })).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "H2 Console (only on localhost)" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: systemInfoFixtures.showingBoth.sourceRepo,
      }),
    ).toBeInTheDocument();
  });
});
