import { useState } from "react";
import { Alert, Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";

// 403 messages the backend sends for problems the user can fix themselves;
// these are shown on the page rather than in a toast.
const EXPECTED_ERRORS = [
  "must set up Github PAT first",
  "No access to repo via Github PAT token",
  "Read/write access to repo via Github PAT is required",
];

export default function PLTabComponent({ courseId, testIdPrefix }) {
  const [errorMessage, setErrorMessage] = useState(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const mutation = useBackendMutation(
    (data) => ({
      url: "/api/courses/updateGithubRepo",
      method: "PUT",
      params: { courseId, repoName: data.repoName },
    }),
    {
      onSuccess: () => {
        setErrorMessage(null);
        toast("GitHub repo associated with course");
      },
      onError: (error) => {
        const status = error?.response?.status;
        const message = error?.response?.data?.message;
        if (status === 403 && EXPECTED_ERRORS.includes(message)) {
          setErrorMessage(message);
        } else {
          setErrorMessage(null);
          toast.error(message ?? "Error associating GitHub repo");
        }
      },
    },
    [`/api/courses/${courseId}`],
  );

  const onSubmit = (data) => mutation.mutate(data);

  return (
    <div className="tabComponent" data-testid={`${testIdPrefix}-plTab`}>
      <h2>PrairieLearn</h2>

      <h3>GitHub Repo</h3>
      <p>
        Enter the name of the GitHub repo associated with this course (the part
        after <code>https://github.com/</code>, e.g.{" "}
        <code>ucsb-cs156/pl-cs156</code>). Your GitHub PAT (see your profile
        page) must have read/write access to the repo.
      </p>

      {errorMessage && (
        <Alert
          variant="danger"
          data-testid={`${testIdPrefix}-plTab-repo-error`}
        >
          {errorMessage}
        </Alert>
      )}

      <Form onSubmit={handleSubmit(onSubmit)}>
        <Form.Group className="mb-2" style={{ maxWidth: "400px" }}>
          <Form.Label htmlFor={`${testIdPrefix}-plTab-repoName`}>
            Repo name
          </Form.Label>
          <Form.Control
            data-testid={`${testIdPrefix}-plTab-repoName`}
            id={`${testIdPrefix}-plTab-repoName`}
            type="text"
            placeholder="owner/repo"
            isInvalid={Boolean(errors.repoName)}
            {...register("repoName", { required: true })}
          />
          <Form.Control.Feedback type="invalid">
            {errors.repoName && "A repo name is required."}
          </Form.Control.Feedback>
        </Form.Group>
        <Button type="submit" data-testid={`${testIdPrefix}-plTab-repo-submit`}>
          Update Repo
        </Button>
      </Form>
    </div>
  );
}
