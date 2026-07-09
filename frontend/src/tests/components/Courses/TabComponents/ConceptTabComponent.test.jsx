import { render, screen } from "@testing-library/react";
import ConceptTabComponent from "main/components/Courses/TabComponents/ConceptTabComponent";

describe("ConceptTabComponent tests", () => {
  test("renders the Concepts heading", () => {
    render(<ConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByText("Concepts")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    render(<ConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByTestId("test-conceptTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    render(<ConceptTabComponent testIdPrefix="InstructorCourseShowPage" />);
    expect(
      screen.getByTestId("InstructorCourseShowPage-conceptTab"),
    ).toBeInTheDocument();
  });
});
