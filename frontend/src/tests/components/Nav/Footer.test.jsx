import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import Footer from "main/components/Nav/Footer";
import { afterEach, expect, beforeEach, vi, describe, test } from "vitest";

// Use doMock and resetModules for isolated mocks.
// The vi.doMock and vi.resetModules calls should be inside the describe blocks.

const queryClient = new QueryClient();

describe("Footer tests", () => {
  describe("SystemInfo returns content", () => {
  
    test("renders correctly with system info content", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <Footer />
        </QueryClientProvider>,
      );
      // Wait for the query to resolve and content to appear.
      const expectedText =
        /Scaffold is a product of Kate Larrick at UC Santa Barbara/;

      await waitFor(() => {
        expect(
          screen.getByText(expectedText)
        ).toBeInTheDocument();
      });
    });
  });
});
