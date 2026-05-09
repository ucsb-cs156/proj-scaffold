import { render, screen } from "@testing-library/react";
import HomePage from "main/pages/HomePage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { expect } from "vitest";

describe("HomePage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  axiosMock
    .onGet("/api/currentUser")
    .reply(200, apiCurrentUserFixtures.userOnly);
  axiosMock
    .onGet("/api/systemInfo")
    .reply(200, systemInfoFixtures.showingNeither);

  const queryClient = new QueryClient();
  test("renders main content correctly", async () => {
    axiosMock.onGet("/api/currentUser").reply(404);
    axiosMock.onGet("/api/user/pin").reply(200, null);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Welcome to Scaffold!/);
    expect(screen.getByText(/Welcome to Scaffold!/)).toBeInTheDocument();
    expect(screen.getByText(/To view your pin, please/)).toBeInTheDocument();
  });

  test("renders main content correctly when logged in", async () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userWithRoles);
    axiosMock.onGet("/api/user/pin").reply(200, "1234");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HomePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await screen.findByText(/Your pin for Scaffold is: 1234/);
    expect(
      screen.getByText(/Your pin for Scaffold is: 1234/),
    ).toBeInTheDocument();
  });
});
