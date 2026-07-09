import React, { useState } from "react";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";
import ConceptModal from "main/components/Concept/ConceptModal";
import ConceptTable from "main/components/Concept/ConceptTable";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

export default function ConceptTabComponent({ courseId, testIdPrefix }) {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const conceptsPath = `/api/concepts/course?courseId=${courseId}`;
  const { data: concepts } = useBackend(
    [conceptsPath],
    { method: "GET", url: conceptsPath },
    [],
    true,
  );

  const objectToAxiosParams = (concept) => ({
    url: "/api/concept",
    method: "POST",
    data: {
      courseId,
      label: concept.label,
      description: concept.description,
      example: concept.example,
      x: 0,
      y: 0,
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
    </div>
  );
}
