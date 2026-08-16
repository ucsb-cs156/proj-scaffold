import { describe, test, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import AssignConceptsToggle from "main/components/Scaffold/AssignConceptsToggle";

describe("AssignConceptsToggle", () => {
  test("renders the Assign Concepts label", () => {
    render(
      <AssignConceptsToggle
        active={false}
        disabled={false}
        onClick={vi.fn()}
      />,
    );
    expect(screen.getByText("Assign Concepts")).toBeInTheDocument();
  });

  test("calls onClick when clicked and not disabled", () => {
    const onClick = vi.fn();
    render(
      <AssignConceptsToggle
        active={false}
        disabled={false}
        onClick={onClick}
      />,
    );

    fireEvent.click(screen.getByTestId("AssignConceptsToggle"));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  test("does not call onClick when disabled", () => {
    const onClick = vi.fn();
    render(
      <AssignConceptsToggle active={false} disabled={true} onClick={onClick} />,
    );

    fireEvent.click(screen.getByTestId("AssignConceptsToggle"));

    expect(onClick).not.toHaveBeenCalled();
  });

  test("is disabled when disabled is true", () => {
    render(
      <AssignConceptsToggle active={false} disabled={true} onClick={vi.fn()} />,
    );
    expect(screen.getByTestId("AssignConceptsToggle")).toBeDisabled();
  });

  test("shows an active visual state when active is true", () => {
    render(
      <AssignConceptsToggle active={true} disabled={false} onClick={vi.fn()} />,
    );
    const button = screen.getByTestId("AssignConceptsToggle");
    expect(button).toHaveStyle({ background: "rgb(30, 41, 59)" });
  });

  test("shows an inactive visual state when active is false", () => {
    render(
      <AssignConceptsToggle
        active={false}
        disabled={false}
        onClick={vi.fn()}
      />,
    );
    const button = screen.getByTestId("AssignConceptsToggle");
    expect(button).toHaveStyle({ background: "rgb(255, 255, 255)" });
  });
});
