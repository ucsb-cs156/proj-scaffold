import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import SubConceptTabComponent from "main/components/Courses/TabComponent/SubConceptTabComponent";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import subConceptsFixtures from "fixtures/subConceptsFixtures";
import conceptsFixtures from "fixtures/conceptsFixtures";

const axiosMock = new AxiosMockAdapter(axios);

const renderSubConceptTabComponent = (props = {}) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <SubConceptTabComponent courseId={1} testIdPrefix="test" {...props} />
    </QueryClientProvider>,
  );
};

describe("SubConceptTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock
      .onGet("/api/concepts/subconcepts")
      .reply(200, subConceptsFixtures.severalSubConcepts);
    axiosMock
      .onGet("/api/concepts/top-level")
      .reply(200, conceptsFixtures.severalConcepts);
  });

  test("renders the SubConcepts heading", async () => {
    renderSubConceptTabComponent();
    expect(screen.getByText("SubConcepts")).toBeInTheDocument();
  });

  test("renders with correct data-testid", async () => {
    renderSubConceptTabComponent();
    expect(screen.getByTestId("test-subConceptTab")).toBeInTheDocument();
  });

  test("renders with custom testIdPrefix", async () => {
    renderSubConceptTabComponent({
      testIdPrefix: "InstructorCourseShowPage",
    });
    expect(
      screen.getByTestId("InstructorCourseShowPage-subConceptTab"),
    ).toBeInTheDocument();
  });

  test("renders subconcept table with fetched data", async () => {
    renderSubConceptTabComponent();
    expect(await screen.findByText("Declaring variables")).toBeInTheDocument();
    expect(await screen.findByText("Updating variables")).toBeInTheDocument();
  });

  test("renders concept selector", async () => {
    renderSubConceptTabComponent();
    expect(screen.getByTestId("test-conceptSelector")).toBeInTheDocument();
  });

  test("renders concept options in the selector", async () => {
    renderSubConceptTabComponent();
    expect(await screen.findByText("Variables")).toBeInTheDocument();
    expect(await screen.findByText("Loops")).toBeInTheDocument();
  });

  test("Create SubConcept button is disabled when no concept is selected", async () => {
    renderSubConceptTabComponent();
    const button = screen.getByTestId("test-createSubConceptButton");
    expect(button).toBeDisabled();
  });

  test("Create SubConcept button is enabled after selecting a concept", async () => {
    renderSubConceptTabComponent();

    const selector = screen.getByTestId("test-conceptSelector");
    await waitFor(() =>
      expect(screen.queryByText("Variables")).toBeInTheDocument(),
    );

    fireEvent.change(selector, { target: { value: "1" } });

    await waitFor(() => {
      expect(
        screen.getByTestId("test-createSubConceptButton"),
      ).not.toBeDisabled();
    });
  });

  test("Create SubConcept button becomes disabled when selector reset to empty", async () => {
    renderSubConceptTabComponent();

    const selector = screen.getByTestId("test-conceptSelector");
    await waitFor(() =>
      expect(screen.queryByText("Variables")).toBeInTheDocument(),
    );

    fireEvent.change(selector, { target: { value: "1" } });
    await waitFor(() => {
      expect(
        screen.getByTestId("test-createSubConceptButton"),
      ).not.toBeDisabled();
    });

    fireEvent.change(selector, { target: { value: "" } });
    await waitFor(() => {
      expect(screen.getByTestId("test-createSubConceptButton")).toBeDisabled();
    });
  });

  test("clicking Create SubConcept button opens the SubConceptModal", async () => {
    renderSubConceptTabComponent();

    const selector = screen.getByTestId("test-conceptSelector");
    await waitFor(() =>
      expect(screen.queryByText("Variables")).toBeInTheDocument(),
    );
    fireEvent.change(selector, { target: { value: "1" } });

    await waitFor(() => {
      expect(
        screen.getByTestId("test-createSubConceptButton"),
      ).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId("test-createSubConceptButton"));

    expect(
      await screen.findByTestId("SubConceptModal-base"),
    ).toBeInTheDocument();
  });
});
