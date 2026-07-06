import {
  render,
  screen,
  waitFor,
  fireEvent,
  within,
} from "@testing-library/react";
import coursesFixtures from "fixtures/coursesFixtures";
import CoursesTable from "main/components/Courses/CoursesTable";
import { BrowserRouter } from "react-router";
import { vi } from "vitest";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";

import { schoolList } from "fixtures/schoolFixtures";

const joinCallback = vi.fn();
const isLoading = vi.fn(() => false);
const axiosMock = new AxiosMockAdapter(axios);

describe("CoursesTable tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    axiosMock.reset();
  });

  test.only("Has the expected column headers and content", () => {
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);
    render(
      <BrowserRouter>
        <CoursesTable
          courses={coursesFixtures.severalCourses}
          testId={"CoursesTable"}
          isLoading={isLoading}
        />
      </BrowserRouter>,
    );

    const expectedHeaders = ["id", "Course Name", "Term", "School"];
    const expectedFields = ["id", "courseName", "term", "school"];
    const testId = "CoursesTable";

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-courseName`),
    ).toHaveTextContent("CMPSC 8");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-term`),
    ).toHaveTextContent("S26");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-school`),
    ).toHaveTextContent("UCSB");
  });
});
