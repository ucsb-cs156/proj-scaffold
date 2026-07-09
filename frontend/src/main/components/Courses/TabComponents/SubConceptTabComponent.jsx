import React, { useState } from "react";
import { Button } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";
import SubConceptTable from "main/components/Concept/SubConceptTable";
import SubConceptModal from "main/components/Concept/SubConceptModal";
import ConceptSelector from "main/components/Concept/ConceptSelector";
import { useBackendMutation } from "main/utils/useBackend";

export default function SubConceptTabComponent({ courseId, testIdPrefix }) {
  const [selectedConcept, setSelectedConcept] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const { data: subConcepts = [] } = useBackend(
    ["/api/concepts/subconcepts", courseId],
    {
      method: "GET",
      url: "/api/concepts/subconcepts",
      params: { courseId },
    },
    [],
  );

  const createSubconceptAxiosParams = (data) => ({
    url: "/api/concept/subconcept",
    method: "POST",
    data: {
      courseId: Number(courseId),
      parentConceptId: Number(data.parentId),
      label: data.label,
      description: data.description,
      example: data.example,
    },
  });

  const createSubconceptMutation = useBackendMutation(
    createSubconceptAxiosParams,
    { onSuccess: () => setShowModal(false) },
    ["/api/concepts/subconcepts", courseId],
  );

  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-subConceptTab`}>
      <h2>SubConcepts</h2>
      <ConceptSelector
        courseId={courseId}
        onSelect={setSelectedConcept}
        testId={`${testIdPrefix}-conceptSelector`}
      />
      <Button
        onClick={() => setShowModal(true)}
        disabled={!selectedConcept}
        data-testid={`${testIdPrefix}-createSubConceptButton`}
        className="mt-2"
      >
        Create SubConcept
      </Button>
      <SubConceptTable subConcepts={subConcepts} />
      <SubConceptModal
        showModal={showModal}
        toggleShowModal={setShowModal}
        initialContents={{
          parentId: selectedConcept?.id,
          parentLabel: selectedConcept?.label,
        }}
        onSubmitAction={createSubconceptMutation.mutate}
      />
    </div>
  );
}
