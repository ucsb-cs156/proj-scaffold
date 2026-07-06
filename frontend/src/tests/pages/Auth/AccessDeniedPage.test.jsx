import { BrowserRouter } from "react-router";
import { render, screen } from "@testing-library/react";
import AccessDeniedPage from "main/pages/Auth/AccessDeniedPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const axiosMock = new AxiosMockAdapter(axios);

const queryClient = new QueryClient();

describe("AccessDeniedPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("Access Denied Page static checks", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AccessDeniedPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await screen.findByText(/You do not have access to this page/);
    expect(screen.getByText("Return")).toBeInTheDocument();
  });
});
