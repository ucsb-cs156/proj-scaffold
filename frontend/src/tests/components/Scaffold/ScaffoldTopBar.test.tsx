import { describe, test, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import ScaffoldTopBar from "main/components/Scaffold/ScaffoldTopBar";
import type { Assessment, Course, Question } from "main/types/conceptGraph";

const assessments: Assessment[] = [
  { id: "1", pl_assessment_id: "pl1", name: "HW1" },
  { id: "2", pl_assessment_id: "pl2", name: "HW2" },
];

const questions: Question[] = [
  { id: "q1", assessment_id: "1", pl_question_uuid: "u1", title: "Loops 101" },
];

const course: Course = { id: 7, courseName: "CMPSC 156" };

const defaultProps = {
  course,
  assessments,
  selectedAssessmentId: "",
  onSelectAssessment: () => {},
  questions: [] as Question[],
  selectedQuestionId: "",
  onSelectQuestion: () => {},
  numStarredConcepts: 2,
  numTotalConcepts: 10,
};

function renderTopBar(props: Partial<typeof defaultProps> = {}) {
  return render(
    <MemoryRouter>
      <ScaffoldTopBar {...defaultProps} {...props} />
    </MemoryRouter>,
  );
}

describe("ScaffoldTopBar", () => {
  test("renders the bar with its className and testid", () => {
    renderTopBar();
    const bar = screen.getByTestId("ScaffoldTopBar");
    expect(bar).toHaveClass("scaffold-top-bar");
  });

  test("shows the star status and a disabled question search when no assessment is selected", () => {
    renderTopBar();
    expect(screen.getByText("2 / 10")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("Select assessment to pick question"),
    ).toBeDisabled();
  });

  test("links to the course settings page", () => {
    renderTopBar();
    expect(screen.getByTestId("ScaffoldTopBar-linkToSettings")).toHaveAttribute(
      "href",
      "/course/7/settings",
    );
  });

  test("omits the settings link while the course is still loading", () => {
    renderTopBar({ course: undefined });
    expect(
      screen.queryByTestId("ScaffoldTopBar-linkToSettings"),
    ).not.toBeInTheDocument();
    expect(screen.getByText("2 / 10")).toBeInTheDocument();
  });

  test("selecting an assessment calls onSelectAssessment", () => {
    const onSelectAssessment = vi.fn();
    renderTopBar({ onSelectAssessment });
    fireEvent.click(screen.getByText("Select assessment…"));
    fireEvent.mouseDown(screen.getByText("HW2"));
    expect(onSelectAssessment).toHaveBeenCalledWith("2");
  });

  test("enables the question search once an assessment is selected and questions load", () => {
    const onSelectQuestion = vi.fn();
    renderTopBar({
      selectedAssessmentId: "1",
      questions,
      onSelectQuestion,
    });
    const input = screen.getByPlaceholderText("Search questions…");
    expect(input).toBeEnabled();
    fireEvent.focus(input);
    fireEvent.mouseDown(screen.getByText("Loops 101"));
    expect(onSelectQuestion).toHaveBeenCalledWith("q1");
  });

  test("keeps the question search disabled when the selected assessment has no questions", () => {
    renderTopBar({ selectedAssessmentId: "1" });
    expect(
      screen.getByPlaceholderText("Select assessment to pick question"),
    ).toBeDisabled();
  });
});
