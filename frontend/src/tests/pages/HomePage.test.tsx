import { BrowserRouter } from "react-router";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import HomePage from "main/pages/HomePage";

import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const axiosMock = new AxiosMockAdapter(axios);

const queryClient = new QueryClient();

describe("HomePage tests", () => {
  beforeEach(() => {
    queryClient.clear();
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders the navbar chrome with an empty placeholder body", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <HomePage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId("AppNavbar")).toBeInTheDocument();
  });
});
