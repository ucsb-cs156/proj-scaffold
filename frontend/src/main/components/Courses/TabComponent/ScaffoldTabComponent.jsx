import React, { useState } from "react";
import { Alert, Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import axios from "axios";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

/**
 * Sanitizes a value for use in a downloaded filename: trims surrounding
 * whitespace, replaces any run of characters that are not letters or digits
 * with a single dash, and strips any leading/trailing dashes left behind.
 * Returns "" for null/undefined input.
 *
 * Examples: "CMPSC 8" -> "CMPSC-8"; " Fall 2026! " -> "Fall-2026"; null -> "".
 *
 * @param {*} value the value to sanitize (coerced to a string)
 * @returns {string} the sanitized, filename-safe string
 */
const sanitizeForFilename = (value) =>
  String(value ?? "")
    .trim()
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

export default function ScaffoldTabComponent({
  courseId,
  courseName,
  term,
  school,
  testIdPrefix,
}) {
  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm();

  // Current course, used to show the current xSpacing/ySpacing. Refetched after a
  // successful spacing update (see the invalidated query key on spacingMutation
  // below) so the fields reflect the value just saved.
  const { data: course } = useBackend(
    [`/api/courses/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    null,
    true,
  );

  const [spacingError, setSpacingError] = useState(null);
  const [spacingUpdated, setSpacingUpdated] = useState(false);

  const {
    register: registerSpacing,
    handleSubmit: handleSubmitSpacing,
    formState: { errors: spacingFormErrors },
  } = useForm({
    values: course
      ? { xSpacing: course.xSpacing, ySpacing: course.ySpacing }
      : undefined,
  });

  const spacingMutation = useBackendMutation(
    (data) => ({
      url: "/api/course/scaffold/spacing",
      method: "PUT",
      params: {
        courseId,
        xSpacing: data.xSpacing,
        ySpacing: data.ySpacing,
      },
    }),
    {
      onSuccess: () => {
        setSpacingError(null);
        setSpacingUpdated(true);
      },
      onError: (error) => {
        setSpacingUpdated(false);
        setSpacingError(
          error?.response?.data?.message ?? "Error updating scaffold spacing",
        );
      },
    },
    [`/api/courses/${courseId}`],
  );

  const onSpacingSubmit = (data) => {
    setSpacingUpdated(false);
    spacingMutation.mutate(data);
  };

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
      const schoolKey = school?.key ?? school;
      const filename = [
        "Scaffold",
        sanitizeForFilename(courseName),
        sanitizeForFilename(term),
        sanitizeForFilename(schoolKey),
        courseId,
      ]
        .filter((part) => part !== "" && part !== undefined && part !== null)
        .join("-");
      link.setAttribute("download", `${filename}.yml`);
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
    <div className="tabComponent" data-testid={`${testIdPrefix}-scaffoldTab`}>
      <h2>Scaffold</h2>
      <p>
        Set the horizontal (xSpacing) and vertical (ySpacing) pixel spacing used
        to lay out top-level concepts the next time the scaffold is reset.
      </p>
      {spacingError && (
        <Alert
          variant="danger"
          className="mt-2"
          data-testid={`${testIdPrefix}-spacing-error`}
        >
          {spacingError}
        </Alert>
      )}
      <Form className="my-2" onSubmit={handleSubmitSpacing(onSpacingSubmit)}>
        <Form.Group className="mb-2" style={{ maxWidth: "200px" }}>
          <Form.Label htmlFor={`${testIdPrefix}-xSpacing`}>
            X Spacing
          </Form.Label>
          {spacingUpdated && (
            <span
              className="text-success ms-2"
              data-testid={`${testIdPrefix}-spacing-check`}
            >
              ✓ updated
            </span>
          )}
          <Form.Control
            data-testid={`${testIdPrefix}-xSpacing`}
            id={`${testIdPrefix}-xSpacing`}
            type="number"
            isInvalid={Boolean(spacingFormErrors.xSpacing)}
            {...registerSpacing("xSpacing", {
              required: true,
              valueAsNumber: true,
              min: 1,
            })}
          />
          <Form.Control.Feedback type="invalid">
            {spacingFormErrors.xSpacing &&
              "X Spacing is required and must be a positive number."}
          </Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-2" style={{ maxWidth: "200px" }}>
          <Form.Label htmlFor={`${testIdPrefix}-ySpacing`}>
            Y Spacing
          </Form.Label>
          <Form.Control
            data-testid={`${testIdPrefix}-ySpacing`}
            id={`${testIdPrefix}-ySpacing`}
            type="number"
            isInvalid={Boolean(spacingFormErrors.ySpacing)}
            {...registerSpacing("ySpacing", {
              required: true,
              valueAsNumber: true,
              min: 1,
            })}
          />
          <Form.Control.Feedback type="invalid">
            {spacingFormErrors.ySpacing &&
              "Y Spacing is required and must be a positive number."}
          </Form.Control.Feedback>
        </Form.Group>
        <Button type="submit" data-testid={`${testIdPrefix}-spacing-submit`}>
          Update Spacing
        </Button>
      </Form>
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
            {errors.upload && "Concepts YAML file is required."}
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
