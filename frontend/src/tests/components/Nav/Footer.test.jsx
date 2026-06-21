import { render, screen } from "@testing-library/react";
import Footer from "main/components/Nav/Footer";

describe("Footer tests", () => {
  test("renders correctly", () => {
    render(<Footer />);
    expect(
      screen.getByText(/UCSB Computer Science project/),
    ).toBeInTheDocument();
  });
});
