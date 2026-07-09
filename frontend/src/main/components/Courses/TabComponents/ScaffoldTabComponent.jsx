import React from "react";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";

export default function ScaffoldTabComponent({ courseId, testIdPrefix }) {
  const onSuccessScaffoldReset = () => {
    toast("Scaffold reset successfully completed.");
  };

  const objectToAxiosParams = () => ({
    url: "/api/course/scaffold/reset",
    method: "POST",
    params: {
      courseId,
    },
  });

  const resetScaffoldMutation = useBackendMutation(objectToAxiosParams, {
    onSuccess: onSuccessScaffoldReset,
  });

  const handleResetScaffold = () => {
    resetScaffoldMutation.mutate();
  };

  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-scaffoldTab`}>
      <h2>Scaffold</h2>
      <Button
        onClick={handleResetScaffold}
        data-testid={`${testIdPrefix}-reset-button`}
      >
        Reset Scaffold
      </Button>
    </div>
  );
}
