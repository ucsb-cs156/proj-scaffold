import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AdminDeveloperPage from "main/pages/Admin/AdminDeveloperPage";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

function renderPage(systemInfo) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  qc.setQueryData(["systemInfo"], systemInfo);
  return render(
    <QueryClientProvider client={qc}>
      <AdminDeveloperPage />
    </QueryClientProvider>,
  );
}

describe("AdminDeveloperPage tests", () => {
  test("renders developer information with backend links", () => {
    renderPage(systemInfoFixtures.showingBoth);

    expect(screen.getByText("Developer Information")).toBeInTheDocument();
    expect(screen.getByText("Current Deployed Branch")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Swagger" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "H2 Console" })).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: systemInfoFixtures.showingBoth.sourceRepo,
      }),
    ).toBeInTheDocument();
  });

  test("hides backend links when system info disables them", () => {
    renderPage(systemInfoFixtures.showingNeither);
    expect(
      screen.queryByRole("link", { name: "Swagger" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: "H2 Console" }),
    ).not.toBeInTheDocument();
  });
});
