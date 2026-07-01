import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import BasicLayout from "./main/layouts/BasicLayout/BasicLayout";
import AdminDeveloperPage from "./main/pages/Admin/AdminDeveloperPage";
import HomePage from "./main/pages/HomePage";

export default function App() {
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
            <BasicLayout>
              <AdminDeveloperPage />
            </BasicLayout>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
