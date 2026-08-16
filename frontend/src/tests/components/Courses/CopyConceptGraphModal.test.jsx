import { vi } from "vitest";
import CopyConceptGraphModal from "main/components/Courses/CopyConceptGraphModal";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";

const toggleShowModal = vi.fn();
const onConfirm = vi.fn();

describe("CopyConceptGraphModal tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderModal = (props = {}) =>
    render(
      <div
        className="modal show"
        style={{ display: "block", position: "initial" }}
      >
        <CopyConceptGraphModal
          showModal={true}
          toggleShowModal={toggleShowModal}
          onConfirm={onConfirm}
          {...props}
        />
      </div>,
    );

  test("renders the warning message", () => {
    renderModal();
    expect(
      screen.getByText(
        "This will replace ALL content in the Concept Graph, and erase all User State; are you sure?",
      ),
    ).toBeInTheDocument();
  });

  test("clicking Yes calls onConfirm", async () => {
    renderModal();
    fireEvent.click(screen.getByTestId("CopyConceptGraphModal-yes-button"));
    await waitFor(() => expect(onConfirm).toHaveBeenCalledTimes(1));
  });

  test("clicking No hides the modal without calling onConfirm", async () => {
    renderModal();
    fireEvent.click(screen.getByTestId("CopyConceptGraphModal-no-button"));
    await waitFor(() => expect(toggleShowModal).toHaveBeenCalledWith(false));
    expect(onConfirm).not.toHaveBeenCalled();
  });

  test("clicking Close hides the modal", async () => {
    renderModal();
    const closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() => expect(toggleShowModal).toHaveBeenCalledWith(false));
  });

  test("supports a custom testId prefix", () => {
    renderModal({ testId: "custom-modal" });
    expect(screen.getByTestId("custom-modal")).toBeInTheDocument();
    expect(screen.getByTestId("custom-modal-yes-button")).toBeInTheDocument();
    expect(screen.getByTestId("custom-modal-no-button")).toBeInTheDocument();
  });
});
