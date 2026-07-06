import "bootstrap/dist/css/bootstrap.css";

import { BrowserRouter, Route, Routes } from "react-router";
import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";
import AdminsCreatePage from "main/pages/Admin/AdminsCreatePage";
import InstructorsIndexPage from "main/pages/Admin/InstructorsIndexPage";
import InstructorsCreatePage from "main/pages/Admin/InstructorsCreatePage";
import ProtectedPage from "main/pages/Auth/ProtectedPage";
import HomePage from "main/pages/HomePage";
import NotFoundPage from "main/pages/Auth/NotFoundPage";
import SignInPage from "main/pages/Auth/SignInPage";
import SignInSuccessPage from "main/pages/Auth/SignInSuccessPage";

import { useCurrentUser } from "main/utils/currentUser";
import AdminDeveloperPage from "main/pages/Admin/AdminDeveloperPage";
import InstructorCoursesIndexPage from "main/pages/Courses/InstructorCoursesIndexPage";
import AdminCoursesIndexPage from "main/pages/Admin/AdminCoursesIndexPage";

export default function App() {
  const currentUser = useCurrentUser();

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<SignInPage />} />
        <Route path="/login/success" element={<SignInSuccessPage />} />
        <Route
          path="/admin/admins"
          element={
            <ProtectedPage
              component={<AdminsIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/instructors"
          element={
            <ProtectedPage
              component={<InstructorsIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/admins/create"
          element={
            <ProtectedPage
              component={<AdminsCreatePage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/instructors/create"
          element={
            <ProtectedPage
              component={<InstructorsCreatePage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/developer"
          element={
            <ProtectedPage
              component={<AdminDeveloperPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/courses"
          element={
            <ProtectedPage
              component={<AdminCoursesIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/courses"
          element={
            <ProtectedPage
              component={<InstructorCoursesIndexPage />}
              enforceRole={"ROLE_INSTRUCTOR"}
              currentUser={currentUser}
            />
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
