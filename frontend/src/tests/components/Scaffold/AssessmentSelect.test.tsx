import { describe, test, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import AssessmentSelect from "main/components/Scaffold/AssessmentSelect";
import type { Assessment } from "main/types/conceptGraph";

const assessments: Assessment[] = [
  { id: "1", pl_assessment_id: "pl1", name: "HW1" },
  { id: "2", pl_assessment_id: "pl2", name: "HW2" },
];

const assessmentsWithBadges: Assessment[] = [
  {
    id: "1",
    pl_assessment_id: "pl1",
    name: "Homework 1",
    pl_assessment_set_abbreviation: "HW",
    pl_assessment_number: "1",
    pl_assessment_set_color: "#3B82F6",
  },
];

describe("AssessmentSelect", () => {
  test("shows placeholder text when nothing is selected", () => {
    render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId=""
        onSelect={vi.fn()}
      />,
    );
    expect(screen.getByText("Select assessment…")).toBeInTheDocument();
  });

  test("shows the name of the selected assessment", () => {
    render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId="2"
        onSelect={vi.fn()}
      />,
    );
    expect(screen.getByText("HW2")).toBeInTheDocument();
    expect(screen.queryByText("Select assessment…")).not.toBeInTheDocument();
  });

  test("applies the is-selected class only to the currently selected option", () => {
    const { container } = render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId="2"
        onSelect={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByText("HW2"));

    const options = container.querySelectorAll(".dropdown-option");
    const hw1 = [...options].find((el) => el.textContent === "HW1");
    const hw2 = [...options].find((el) => el.textContent === "HW2");

    expect(hw1).not.toHaveClass("is-selected");
    expect(hw2).toHaveClass("is-selected");
  });

  test("dropdown is closed until the control is clicked", () => {
    render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId=""
        onSelect={vi.fn()}
      />,
    );
    expect(screen.queryByText("HW1")).not.toBeInTheDocument();

    fireEvent.click(screen.getByText("Select assessment…"));

    expect(screen.getByText("HW1")).toBeInTheDocument();
    expect(screen.getByText("HW2")).toBeInTheDocument();
  });

  test("selecting an option calls onSelect and closes the dropdown", () => {
    const onSelect = vi.fn();
    render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId=""
        onSelect={onSelect}
      />,
    );

    fireEvent.click(screen.getByText("Select assessment…"));
    fireEvent.mouseDown(screen.getByText("HW1"));

    expect(onSelect).toHaveBeenCalledWith("1");
    expect(screen.queryByText("HW2")).not.toBeInTheDocument();
  });

  test("does not open a dropdown when there are no assessments", () => {
    render(
      <AssessmentSelect
        assessments={[]}
        selectedAssessmentId=""
        onSelect={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByText("Select assessment…"));

    expect(screen.queryByText("HW1")).not.toBeInTheDocument();
  });

  test("clicking outside the component closes the dropdown", () => {
    render(
      <div>
        <div data-testid="outside">outside</div>
        <AssessmentSelect
          assessments={assessments}
          selectedAssessmentId=""
          onSelect={vi.fn()}
        />
      </div>,
    );

    fireEvent.click(screen.getByText("Select assessment…"));
    expect(screen.getByText("HW1")).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByTestId("outside"));

    expect(screen.queryByText("HW1")).not.toBeInTheDocument();
  });

  test("shows a colored badge with the set abbreviation and number", () => {
    render(
      <AssessmentSelect
        assessments={assessmentsWithBadges}
        selectedAssessmentId="1"
        onSelect={vi.fn()}
      />,
    );

    const badge = screen.getByText("HW1");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveStyle({ backgroundColor: "#3B82F6" });
    expect(screen.getByText("Homework 1")).toBeInTheDocument();
  });

  test("does not render a badge when the assessment has no set abbreviation/number", () => {
    render(
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId="1"
        onSelect={vi.fn()}
      />,
    );

    expect(screen.getByText("HW1")).toBeInTheDocument();
  });
});
