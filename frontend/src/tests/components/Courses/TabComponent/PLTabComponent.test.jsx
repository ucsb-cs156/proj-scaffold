import { render, screen } from "@testing-library/react";
import PLTabComponent from "main/components/Courses/TabComponent/PLTabComponent";

describe("PLTabComponent tests", () => {
  test("renders the PrairieLearn heading", () => {
    render(<PLTabComponent testIdPrefix="test" />);
    expect(screen.getByText("PrairieLearn")).toBeInTheDocument();
  });

  test("renders with correct data-testid", () => {
    render(<PLTabComponent testIdPrefix="test" />);
    expect(screen.getByTestId("test-plTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", () => {
    render(<PLTabComponent testIdPrefix="InstructorCourseShowPage" />);
    expect(
      screen.getByTestId("InstructorCourseShowPage-plTab"),
    ).toBeInTheDocument();
  });
});
