import React, { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import axios from "axios";
import ConceptModal from "main/components/Concept/ConceptModal";
import ConceptTable from "main/components/Concept/ConceptTable";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

// Suppress error toasts when the concept list fetch fails (e.g. network error);
// background fetches should not interrupt the user with toast messages.
const suppressFetchToasts = true;
const DEFAULT_NEW_CONCEPT_POSITION = { x: 0, y: 0 };

export default function ConceptTabComponent({ courseId, testIdPrefix }) {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const conceptsPath = `/api/concepts/course?courseId=${courseId}`;
  const { data: concepts } = useBackend(
    [conceptsPath],
    { method: "GET", url: conceptsPath },
    [],
    suppressFetchToasts,
  );

  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm();

  const objectToAxiosParams = (concept) => ({
    url: "/api/concept",
    method: "POST",
    data: {
      courseId,
      label: concept.label,
      description: concept.description,
      example: concept.example,
      ...DEFAULT_NEW_CONCEPT_POSITION,
    },
  });

  const createConceptMutation = useBackendMutation(
    objectToAxiosParams,
    {
      onSuccess: (concept) => {
        toast(`Concept ${concept.label} created`);
        setShowCreateModal(false);
      },
    },
    [conceptsPath],
  );

  const downloadYaml = async () => {
    try {
      const response = await axios({
        url: "/api/concepts/yaml/download",
        method: "GET",
        params: { courseId },
      });
      const blob = new Blob([response.data], { type: "application/x-yaml" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `concepts-course-${courseId}.yaml`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(`Error downloading concepts YAML: ${error.message}`);
    }
  };

  const objectToAxiosParamsUpload = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: "/api/concepts/yaml/upload",
      method: "POST",
      data: file,
      params: {
        courseId,
      },
    };
  };

  const uploadMutation = useBackendMutation(objectToAxiosParamsUpload, {
    onSuccess: (report) => {
      toast(
        `Concepts replaced: ${report.conceptsCreated} concepts, ` +
          `${report.subconceptsCreated} subconcepts, ${report.edgesCreated} edges, ` +
          `${report.practiceProblemsCreated} practice problems. ` +
          `Saved scaffold state was cleared for ${report.userStatesCleared} user(s).`,
      );
      reset();
    },
    onError: (error) => {
      toast.error(
        `Error uploading concepts YAML: ${JSON.stringify(error.response.data, null, 2)}`,
      );
    },
  });

  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-conceptTab`}>
      <h2>Concepts</h2>
      <Button
        onClick={() => setShowCreateModal(true)}
        data-testid={`${testIdPrefix}-post-button`}
        className="mb-3"
      >
        Create Concept
      </Button>
      <ConceptModal
        showModal={showCreateModal}
        toggleShowModal={setShowCreateModal}
        onSubmitAction={(concept) => createConceptMutation.mutate(concept)}
      />
      <ConceptTable
        concepts={concepts}
        showButtons={false}
        testId={`${testIdPrefix}-ConceptTable`}
      />
      <hr />
      <p>
        Download this course&apos;s concepts, subconcepts, prerequisite edges,
        and practice problems as an editable YAML file, or replace them by
        uploading one. See <code>docs/yaml-format.md</code> for the file format.
      </p>
      <Button
        onClick={downloadYaml}
        data-testid={`${testIdPrefix}-download-yaml-button`}
      >
        Download Concepts YAML
      </Button>
      <Form onSubmit={handleSubmit(uploadMutation.mutate)} className="mt-4">
        <Form.Group className="mb-2">
          <Form.Label htmlFor="concepts-yaml-upload">
            Upload Concepts YAML
          </Form.Label>
          <Form.Control
            data-testid={`${testIdPrefix}-upload-yaml-input`}
            id="concepts-yaml-upload"
            type="file"
            accept=".yaml,.yml"
            isInvalid={Boolean(errors.upload)}
            {...register("upload", { required: true })}
          />
          <Form.Control.Feedback type="invalid">
            {errors.upload && "Concepts YAML file is required. "}
          </Form.Control.Feedback>
          <Form.Text muted>
            Warning: uploading replaces ALL concepts, subconcepts, edges, and
            practice problems for this course, and clears every student&apos;s
            saved scaffold state.
          </Form.Text>
        </Form.Group>
        <Button
          type="submit"
          data-testid={`${testIdPrefix}-upload-yaml-button`}
          className="mt-3"
        >
          Upload
        </Button>
      </Form>
    </div>
  );
}
