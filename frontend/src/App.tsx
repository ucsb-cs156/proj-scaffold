import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import BasicLayout from "./main/layouts/BasicLayout/BasicLayout";
import AdminDeveloperPage from "./main/pages/Admin/AdminDeveloperPage";
import HomePage from "./main/pages/HomePage";
import { hasRole, useCurrentUser } from "./main/utils/currentUser";

export default function App() {
  const currentUser = useCurrentUser();

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <BasicLayout>
              <HomePage />
            </BasicLayout>
          }
        />
        <Route
          path="/admin/developer"
          element={
            hasRole(currentUser, "ROLE_ADMIN") ? (
              <BasicLayout>
                <AdminDeveloperPage />
              </BasicLayout>
            ) : (
              <Navigate to="/" replace />
            )
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
