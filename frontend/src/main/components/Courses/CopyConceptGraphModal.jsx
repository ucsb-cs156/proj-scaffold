import Modal from "react-bootstrap/Modal";
import { Button } from "react-bootstrap";

export default function CopyConceptGraphModal({
  showModal,
  toggleShowModal,
  onConfirm,
  testId = "CopyConceptGraphModal",
}) {
  const hideModal = () => {
    toggleShowModal(false);
  };

  return (
    <Modal
      show={showModal}
      onHide={hideModal}
      centered={true}
      data-testid={testId}
    >
      <Modal.Header closeButton>Copy Concept Graph</Modal.Header>
      <Modal.Body>
        This will replace ALL content in the Concept Graph, and erase all User
        State; are you sure?
      </Modal.Body>
      <Modal.Footer>
        <Button
          variant="secondary"
          onClick={hideModal}
          data-testid={`${testId}-no-button`}
        >
          No, keep current content
        </Button>
        <Button
          variant="danger"
          onClick={onConfirm}
          data-testid={`${testId}-yes-button`}
        >
          Yes, replace all content
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
