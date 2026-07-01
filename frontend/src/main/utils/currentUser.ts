import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useNavigate } from "react-router";

export function useCurrentUser() {
  const queryResults = useQuery({
    queryKey: ["current user"],
    queryFn: async () => {
      try {
        const response = await axios.get("/api/currentUser");
        const rolesList: string[] =
          response.data.roles?.map((r: { authority: string }) => r.authority) ??
          [];
        return { loggedIn: true, root: { ...response.data, rolesList } };
      } catch (e: unknown) {
        const err = e as { status?: number };
        if (err.status === 403) {
          return { loggedIn: false, root: {} };
        }
        throw e;
      }
    },
    initialData: { loggedIn: false, root: null },
  });
  return queryResults.data;
}

export function useLogout() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: async () => {
      await axios.post("/logout");
      await queryClient.resetQueries({ queryKey: ["current user"] });
      navigate("/");
    },
  });
}

export function hasRole(
  currentUser: ReturnType<typeof useCurrentUser>,
  role: string,
): boolean {
  if (currentUser == null) return false;
  return (
    (currentUser.root as { rolesList?: string[] })?.rolesList?.includes(role) ??
    false
  );
}
