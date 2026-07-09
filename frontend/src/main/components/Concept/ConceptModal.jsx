import { useEffect } from "react";
import Modal from "react-bootstrap/Modal";
import { Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

export default function ConceptModal({
  onSubmitAction,
  showModal,
  toggleShowModal,
  initialContents,
  buttonText = "Create",
  modalTitle = "Create Concept",
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm({
    defaultValues: initialContents ?? {},
  });

  useEffect(() => {
    reset(initialContents ?? {});
  }, [initialContents, reset]);

  const closeModal = () => {
    toggleShowModal(false);
  };

  return (
    <Modal
      show={showModal}
      onHide={closeModal}
      centered={true}
      data-testid={"ConceptModal-base"}
    >
      <Modal.Header>
        <Modal.Title>{modalTitle}</Modal.Title>
        <button
          type="button"
          className="btn-close"
          aria-label="Close"
          data-testid={"ConceptModal-closeButton"}
          onClick={closeModal}
        ></button>
      </Modal.Header>
      <Form onSubmit={handleSubmit(onSubmitAction)}>
        <Modal.Body>
          <Form.Group>
            <Form.Label htmlFor="label">Label</Form.Label>
            <Form.Control
              data-testid={"ConceptModal-label"}
              id="label"
              type="text"
              size={40}
              isInvalid={Boolean(errors.label)}
              {...register("label", {
                required: "Label is required.",
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.label?.message}
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mt-3">
            <Form.Label htmlFor="description">Description</Form.Label>
            <Form.Control
              as="textarea"
              data-testid={"ConceptModal-description"}
              id="description"
              rows={8}
              cols={40}
              style={{ width: "40ch" }}
              {...register("description")}
            />
          </Form.Group>
          <Form.Group className="mt-3">
            <Form.Label htmlFor="example">Example</Form.Label>
            <Form.Control
              as="textarea"
              data-testid={"ConceptModal-example"}
              id="example"
              rows={8}
              cols={40}
              style={{ width: "40ch" }}
              {...register("example")}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <button
            type="submit"
            className="btn btn-primary"
            data-testid="ConceptModal-submit"
          >
            {buttonText}
          </button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
