import React, { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import axios from "axios";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import CopyConceptGraphModal from "main/components/Courses/CopyConceptGraphModal";

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

  const onSuccessScaffoldReset = () => {
    toast("Scaffold reset successfully completed.");
  };

  const scaffoldResetAxiosParams = () => ({
    url: "/api/course/scaffold/reset",
    method: "POST",
    params: {
      courseId,
    },
  });

  const resetScaffoldMutation = useBackendMutation(scaffoldResetAxiosParams, {
    onSuccess: onSuccessScaffoldReset,
  });

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

  const [fromCourseId, setFromCourseId] = useState("");
  const [showCopyModal, setShowCopyModal] = useState(false);

  const { data: courses } = useBackend(
    ["/api/courses/list"],
    { method: "GET", url: "/api/courses/list" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
    true,
  );

  const courseList = (courses ?? []).filter(
    (course) => String(course.id) !== String(courseId),
  );
  const instructorCourses = courseList.filter((c) => c.instructorAccess);
  const staffCourses = courseList.filter(
    (c) => c.staffAccess && !c.instructorAccess,
  );
  const studentCourses = courseList.filter(
    (c) => c.studentAccess && !c.instructorAccess && !c.staffAccess,
  );
  const isAdmin = courseList.some((c) => c.adminAccess);
  const adminCourses = isAdmin ? courseList : [];

  const courseOptionLabel = (course) =>
    `${course.courseName} ${course.term}, ${course.school?.displayName ?? course.school}, ${course.instructorEmail}, ${course.id}`;

  const objectToAxiosParamsCopyConceptGraph = () => ({
    url: "/api/jobs/launch/copyConceptGraph",
    method: "POST",
    params: { fromCourseId, toCourseId: courseId },
  });

  const copyConceptGraphMutation = useBackendMutation(
    objectToAxiosParamsCopyConceptGraph,
    {
      onSuccess: (job) => {
        toast(
          `Copy Concept Graph job (id ${job.id}) launched. You can monitor its progress on the Jobs tab.`,
        );
      },
      onError: (error) => {
        toast.error(
          `Error launching Copy Concept Graph job: ${error.response?.data?.message ?? error.message}`,
        );
      },
    },
  );

  const confirmCopyConceptGraph = () => {
    setShowCopyModal(false);
    copyConceptGraphMutation.mutate();
  };

  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-scaffoldTab`}>
      <h2>Scaffold</h2>
      <Button
        onClick={resetScaffoldMutation.mutate}
        data-testid={`${testIdPrefix}-reset-button`}
      >
        Reset Scaffold
      </Button>
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
      <hr />
      <h4>Copy Concept Graph from Another Course</h4>
      <p>
        Replace this course&apos;s entire concept graph with a copy of another
        course&apos;s concept graph. Select the course to copy from below.
      </p>
      <Form.Group className="mb-2">
        <Form.Label htmlFor={`${testIdPrefix}-copy-concept-graph-from-course`}>
          From Course
        </Form.Label>
        <Form.Select
          id={`${testIdPrefix}-copy-concept-graph-from-course`}
          data-testid={`${testIdPrefix}-copy-concept-graph-from-course-select`}
          value={fromCourseId}
          onChange={(e) => setFromCourseId(e.target.value)}
        >
          <option value="">Select a course...</option>
          {instructorCourses.length > 0 && (
            <optgroup label="Instructor">
              {instructorCourses.map((c) => (
                <option key={c.id} value={c.id}>
                  {courseOptionLabel(c)}
                </option>
              ))}
            </optgroup>
          )}
          {staffCourses.length > 0 && (
            <optgroup label="Staff">
              {staffCourses.map((c) => (
                <option key={c.id} value={c.id}>
                  {courseOptionLabel(c)}
                </option>
              ))}
            </optgroup>
          )}
          {studentCourses.length > 0 && (
            <optgroup label="Student">
              {studentCourses.map((c) => (
                <option key={c.id} value={c.id}>
                  {courseOptionLabel(c)}
                </option>
              ))}
            </optgroup>
          )}
          {adminCourses.length > 0 && (
            <optgroup label="Admin">
              {adminCourses.map((c) => (
                <option key={c.id} value={c.id}>
                  {courseOptionLabel(c)}
                </option>
              ))}
            </optgroup>
          )}
        </Form.Select>
      </Form.Group>
      <Button
        variant="danger"
        disabled={!fromCourseId}
        onClick={() => setShowCopyModal(true)}
        data-testid={`${testIdPrefix}-copy-concept-graph-button`}
      >
        Copy Concept Graph
      </Button>
      <CopyConceptGraphModal
        showModal={showCopyModal}
        toggleShowModal={setShowCopyModal}
        onConfirm={confirmCopyConceptGraph}
        testId={`${testIdPrefix}-copyConceptGraphModal`}
      />
    </div>
  );
}
