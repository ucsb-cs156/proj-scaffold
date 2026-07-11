import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { hasRole, useCurrentUser } from "main/utils/currentUser";
import { useBackendMutation } from "main/utils/useBackend";

const testIdPrefix = "UserProfilePage";

// Shape of the /api/pat response (entity/PatCredential.java). The ciphertext and
// keyVersion fields are @JsonIgnore'd on the backend, so the token itself never
// appears here — only metadata about it.
interface PatCredential {
  id: number;
  userId: number;
  lastFour: string;
  expiresAt: string | null;
}

// PatCredential fetch, hoisted out of the component so a fresh function identity
// isn't created on every render.
async function fetchPatCredential(): Promise<PatCredential | null> {
  try {
    const response = await axios.get<PatCredential>("/api/pat");
    return response.data;
  } catch (e: unknown) {
    const err = e as { status?: number };
    if (err.status === 404) {
      return null;
    }
    throw e;
  }
}

function usePatCredential(enabled: boolean) {
  return useQuery<PatCredential | null>({
    queryKey: ["/api/pat"],
    queryFn: fetchPatCredential,
    enabled,
    retry: false,
  });
}

// Roles come back from the backend as e.g. "ROLE_ADMIN"; display them as "admin".
function friendlyRoleName(role: string): string {
  return role.replace(/^ROLE_/, "").toLowerCase();
}

type PatFormFields = {
  token: string;
  expiresAt?: string;
};

export default function UserProfilePage(): React.JSX.Element {
  const currentUser = useCurrentUser();
  const canSeePat =
    hasRole(currentUser, "ROLE_ADMIN") ||
    hasRole(currentUser, "ROLE_INSTRUCTOR");

  const { data: patCredential } = usePatCredential(canSeePat);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PatFormFields>();

  const mutation = useBackendMutation(
    (data: PatFormFields) => ({
      url: "/api/pat",
      method: "POST",
      params: {
        token: data.token,
        expiresAt: data.expiresAt || undefined,
      },
    }),
    {
      onSuccess: () => {
        toast("GitHub PAT saved");
        reset();
      },
      onError: (error: unknown) => {
        const message =
          (error as { response?: { data?: { message?: string } } }).response
            ?.data?.message ?? "Error saving GitHub PAT";
        toast.error(message);
      },
    },
    ["/api/pat"],
  );

  const onSubmit = (data: PatFormFields) => mutation.mutate(data);

  if (!currentUser.loggedIn) {
    return <BasicLayout>Loading...</BasicLayout>;
  }

  const { user, rolesList } = currentUser.root;

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1 data-testid={`${testIdPrefix}-title`}>User Profile</h1>
        <table className="table table-striped" style={{ maxWidth: "600px" }}>
          <tbody>
            <tr>
              <th>Name</th>
              <td data-testid={`${testIdPrefix}-name`}>
                {user.fullName ?? "Not specified"}
              </td>
            </tr>
            <tr>
              <th>Email</th>
              <td data-testid={`${testIdPrefix}-email`}>{user.email}</td>
            </tr>
            <tr>
              <th>Roles</th>
              <td data-testid={`${testIdPrefix}-roles`}>
                {rolesList.map(friendlyRoleName).join(", ")}
              </td>
            </tr>
          </tbody>
        </table>

        {canSeePat && (
          <div data-testid={`${testIdPrefix}-pat-section`}>
            <h2>GitHub PAT</h2>
            {patCredential ? (
              <p data-testid={`${testIdPrefix}-pat-status`}>
                PAT: ******{patCredential.lastFour}, expires:{" "}
                {patCredential.expiresAt ?? "not specified"}
              </p>
            ) : (
              <p data-testid={`${testIdPrefix}-pat-status`}>No PAT set</p>
            )}

            <Form onSubmit={handleSubmit(onSubmit)}>
              <Form.Group className="mb-2">
                <Form.Label htmlFor="pat-token">GitHub PAT</Form.Label>
                <Form.Control
                  data-testid={`${testIdPrefix}-pat-token`}
                  id="pat-token"
                  type="text"
                  isInvalid={Boolean(errors.token)}
                  {...register("token", { required: true })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.token && "A GitHub PAT is required."}
                </Form.Control.Feedback>
              </Form.Group>
              <Form.Group className="mb-2">
                <Form.Label htmlFor="pat-expiresAt">
                  Expiration Date (optional)
                </Form.Label>
                <Form.Control
                  data-testid={`${testIdPrefix}-pat-expiresAt`}
                  id="pat-expiresAt"
                  type="date"
                  {...register("expiresAt")}
                />
              </Form.Group>
              <Button type="submit" data-testid={`${testIdPrefix}-pat-submit`}>
                Set PAT
              </Button>
            </Form>
          </div>
        )}
      </div>
    </BasicLayout>
  );
}
