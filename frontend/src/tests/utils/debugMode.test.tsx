import { describe, test, expect, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { type ReactNode } from "react";
import { DebugModeProvider, useDebugMode } from "main/utils/debugMode";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import Footer from "main/components/Nav/Footer";

function makeWrapper(currentUser: object) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  qc.setQueryData(["current user"], currentUser);

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={qc}>
        <DebugModeProvider>{children}</DebugModeProvider>
      </QueryClientProvider>
    );
  };
}

describe("debugMode utility tests", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe("canUseDebugMode", () => {
    test("is false for a regular user", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.userOnly),
      });
      expect(result.current.canUseDebugMode).toBe(false);
    });

    test("is true for an admin user", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      expect(result.current.canUseDebugMode).toBe(true);
    });

    test("is true for an instructor user", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.instructorUser),
      });
      expect(result.current.canUseDebugMode).toBe(true);
    });

    test("is false when not logged in", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.notLoggedIn),
      });
      expect(result.current.canUseDebugMode).toBe(false);
    });
  });

  describe("debugMode toggle", () => {
    test("defaults to false", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      expect(result.current.debugMode).toBe(false);
    });

    test("can be toggled on for admin user", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      act(() => {
        result.current.setDebugMode(true);
      });
      expect(result.current.debugMode).toBe(true);
    });

    test("persists to sessionStorage when toggled on", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      act(() => {
        result.current.setDebugMode(true);
      });
      expect(sessionStorage.getItem("debugMode")).toBe("true");
    });

    test("reads initial value from sessionStorage", () => {
      sessionStorage.setItem("debugMode", "true");
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      expect(result.current.debugMode).toBe(true);
    });

    test("debugMode is false for regular user even if sessionStorage has true", () => {
      sessionStorage.setItem("debugMode", "true");
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.userOnly),
      });
      expect(result.current.debugMode).toBe(false);
    });
  });

  describe("Footer debug toggle rendering", () => {
    test("debug toggle is not shown for a regular user", () => {
      const qc = new QueryClient({
        defaultOptions: { queries: { retry: false } },
      });
      qc.setQueryData(["current user"], currentUserFixtures.userOnly);

      render(
        <QueryClientProvider client={qc}>
          <DebugModeProvider>
            <Footer />
          </DebugModeProvider>
        </QueryClientProvider>,
      );

      expect(screen.queryByTestId("debug-mode-toggle")).not.toBeInTheDocument();
    });

    test("can be toggled off", () => {
      const { result } = renderHook(() => useDebugMode(), {
        wrapper: makeWrapper(currentUserFixtures.adminUser),
      });
      act(() => {
        result.current.setDebugMode(true);
      });
      expect(result.current.debugMode).toBe(true);

      act(() => {
        result.current.setDebugMode(false);
      });
      expect(result.current.debugMode).toBe(false);
      expect(sessionStorage.getItem("debugMode")).toBe("false");
    });
  });

  describe("useDebugMode default context", () => {
    test("returns default values when used outside a provider", () => {
      const { result } = renderHook(() => useDebugMode());
      expect(result.current.debugMode).toBe(false);
      expect(result.current.canUseDebugMode).toBe(false);
      expect(typeof result.current.setDebugMode).toBe("function");
    });
  });
});

describe("Footer debug toggle UI", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  function renderFooterWithUser(currentUser: object) {
    const qc = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    qc.setQueryData(["current user"], currentUser);
    render(
      <QueryClientProvider client={qc}>
        <DebugModeProvider>
          <Footer />
        </DebugModeProvider>
      </QueryClientProvider>,
    );
  }

  test("does not show debug toggle for regular user", () => {
    renderFooterWithUser(currentUserFixtures.userOnly);
    expect(
      screen.queryByTestId("debug-mode-toggle"),
    ).not.toBeInTheDocument();
  });

  test("does not show debug toggle for not-logged-in user", () => {
    renderFooterWithUser(currentUserFixtures.notLoggedIn);
    expect(
      screen.queryByTestId("debug-mode-toggle"),
    ).not.toBeInTheDocument();
  });

  test("shows debug toggle for admin user", () => {
    renderFooterWithUser(currentUserFixtures.adminUser);
    expect(screen.getByTestId("debug-mode-toggle")).toBeInTheDocument();
    expect(screen.getByText("Debug Mode")).toBeInTheDocument();
  });

  test("shows debug toggle for instructor user", () => {
    renderFooterWithUser(currentUserFixtures.instructorUser);
    expect(screen.getByTestId("debug-mode-toggle")).toBeInTheDocument();
    expect(screen.getByText("Debug Mode")).toBeInTheDocument();
  });

  test("debug toggle starts unchecked", () => {
    renderFooterWithUser(currentUserFixtures.adminUser);
    const toggle = screen.getByTestId("debug-mode-toggle") as HTMLInputElement;
    expect(toggle.checked).toBe(false);
  });

  test("clicking debug toggle enables debug mode", () => {
    renderFooterWithUser(currentUserFixtures.adminUser);
    const toggle = screen.getByTestId("debug-mode-toggle") as HTMLInputElement;
    fireEvent.click(toggle);
    expect(toggle.checked).toBe(true);
  });
});
