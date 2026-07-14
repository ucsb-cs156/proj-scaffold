import { describe, test, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import PrairieLearnAssessment from "main/components/Scaffold/PrairieLearnAssessment";

describe("PrairieLearnAssessment", () => {
  test("renders the title with no badge when set abbreviation/number are missing", () => {
    const { container } = render(
      <PrairieLearnAssessment
        assessment={{ name: "Homework 1", pl_assessment_set_color: "#000000" }}
      />,
    );
    expect(screen.getByText("Homework 1")).toBeInTheDocument();
    expect(container.querySelectorAll("span")).toHaveLength(2);
  });

  test("uses white text on a dark (low luminance) background", () => {
    render(
      <PrairieLearnAssessment
        assessment={{
          name: "Homework 1",
          pl_assessment_set_abbreviation: "HW",
          pl_assessment_number: "1",
          pl_assessment_set_color: "#000000",
        }}
      />,
    );
    const badge = screen.getByText("HW1");
    expect(badge).toHaveStyle({ backgroundColor: "#000000", color: "#ffffff" });
  });

  test("uses black text on a light (high luminance) background", () => {
    render(
      <PrairieLearnAssessment
        assessment={{
          name: "Homework 2",
          pl_assessment_set_abbreviation: "HW",
          pl_assessment_number: "2",
          pl_assessment_set_color: "#FFFFFF",
        }}
      />,
    );
    const badge = screen.getByText("HW2");
    expect(badge).toHaveStyle({ backgroundColor: "#FFFFFF", color: "#000000" });
  });

  test("defaults to black text when the background color is not a valid 6-digit hex", () => {
    render(
      <PrairieLearnAssessment
        assessment={{
          name: "Quiz 1",
          pl_assessment_set_abbreviation: "Q",
          pl_assessment_number: "1",
          pl_assessment_set_color: "red",
        }}
      />,
    );
    const badge = screen.getByText("Q1");
    expect(badge).toHaveStyle({ color: "#000000" });
  });

  test("defaults to black text when the background hex contains non-hex characters", () => {
    render(
      <PrairieLearnAssessment
        assessment={{
          name: "Quiz 2",
          pl_assessment_set_abbreviation: "Q",
          pl_assessment_number: "2",
          pl_assessment_set_color: "#zzzzzz",
        }}
      />,
    );
    const badge = screen.getByText("Q2");
    expect(badge).toHaveStyle({ color: "#000000" });
  });

  test("falls back to a default background color when pl_assessment_set_color is missing", () => {
    render(
      <PrairieLearnAssessment
        assessment={{
          name: "Quiz 3",
          pl_assessment_set_abbreviation: "Q",
          pl_assessment_number: "3",
        }}
      />,
    );
    const badge = screen.getByText("Q3");
    expect(badge).toHaveStyle({ backgroundColor: "#94A3B8" });
  });
});
