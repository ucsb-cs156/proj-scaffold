import { render, screen } from "@testing-library/react";
import EdgeConceptTabComponent from "main/components/Courses/TabComponents/EdgeConceptTabComponent";

describe("EdgeConceptTabComponent tests", () => {
  test("renders the Edges heading", () => {
    render(<EdgeConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByText("Edges")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    render(<EdgeConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByTestId("test-edgeConceptTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    render(<EdgeConceptTabComponent testIdPrefix="InstructorCourseShowPage" />);
    expect(
      screen.getByTestId("InstructorCourseShowPage-edgeConceptTab"),
    ).toBeInTheDocument();
  });
});
