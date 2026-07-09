import { render, screen } from "@testing-library/react";
import SubConceptTabComponent from "main/components/Courses/TabComponents/SubConceptTabComponent";

describe("SubConceptTabComponent tests", () => {
  test("renders the SubConcepts heading", () => {
    render(<SubConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByText("SubConcepts")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    render(<SubConceptTabComponent testIdPrefix="test" />);
    expect(screen.getByTestId("test-subConceptTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    render(<SubConceptTabComponent testIdPrefix="InstructorCourseShowPage" />);
    expect(
      screen.getByTestId("InstructorCourseShowPage-subConceptTab"),
    ).toBeInTheDocument();
  });
});
