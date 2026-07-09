import { render, screen } from "@testing-library/react";
import AboutScaffold from "main/pages/Help/AboutScaffold";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("AboutScaffold tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  axiosMock
    .onGet("/api/currentUser")
    .reply(200, apiCurrentUserFixtures.userOnly);
  axiosMock
    .onGet("/api/systemInfo")
    .reply(200, systemInfoFixtures.showingNeither);

  const queryClient = new QueryClient();
  test("renders expected content", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AboutScaffold />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "About Scaffold" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { level: 2, name: "What Scaffold is for" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Where did Scaffold come from",
      }),
    ).toBeInTheDocument();

    expect(screen.getByRole("link", { name: "PrairieLearn" })).toHaveAttribute(
      "href",
      "https://www.prairielearn.com/",
    );
    expect(
      screen.getByRole("link", {
        name: "https://github.com/ucsb-cs156/proj-scaffold",
      }),
    ).toHaveAttribute("href", "https://github.com/ucsb-cs156/proj-scaffold");
    expect(screen.getByRole("link", { name: "CMPSC 156" })).toHaveAttribute(
      "href",
      "https://ucsb-cs156.github.io",
    );
  });

  test("every link on the page is a live link", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AboutScaffold />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByRole("heading", { level: 1, name: "About Scaffold" });

    const hrefs = [
      ...new Set(
        screen
          .getAllByRole("link")
          .map((link) => link.getAttribute("href"))
          .filter((href) => href && href.startsWith("http")),
      ),
    ];

    expect(hrefs.length).toBeGreaterThan(0);

    const failures = await Promise.all(
      hrefs.map(async (href) => {
        try {
          const response = await fetch(href, { redirect: "follow" });
          if (response.status >= 400) {
            return `${href} returned HTTP status ${response.status}`;
          }
        } catch (error) {
          return `${href} request failed: ${error.cause ?? error}`;
        }
        return null;
      }),
    );

    expect(
      failures.filter((failure) => failure !== null),
      "Expected every link on the page to be a live link",
    ).toEqual([]);
  }, 30000);
});
