import { render, screen } from "@testing-library/react";
import ScaffoldTabComponent from "main/components/Courses/TabComponents/ScaffoldTabComponent";

describe("ScaffoldTabComponent tests", () => {
  test("renders the Scaffold heading", () => {
    render(<ScaffoldTabComponent testIdPrefix="test" />);
    expect(screen.getByText("Scaffold")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    render(<ScaffoldTabComponent testIdPrefix="test" />);
    expect(screen.getByTestId("test-scaffoldTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    render(<ScaffoldTabComponent testIdPrefix="InstructorCourseShowPage" />);
    expect(
      screen.getByTestId("InstructorCourseShowPage-scaffoldTab"),
    ).toBeInTheDocument();
  });
});
