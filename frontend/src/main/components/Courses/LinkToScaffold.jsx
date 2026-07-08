import OurTable from "main/components/Common/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Tooltip, OverlayTrigger, Button, Fade } from "react-bootstrap";
import { Link } from "react-router";
import { useState } from "react";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import UpdateInstructorForm from "main/components/Courses/UpdateInstructorForm";
import CourseModal from "main/components/Courses/CourseModal";
import Modal from "react-bootstrap/Modal";
import { useLocation } from "react-router";
import { GraphIcon, UserListIcon } from "main/components/Common/Icons";

export default function LinkToScaffold({
  courseName,
  courseId,
  rowIndex = 0,
  testId = "LinkToScaffold",
}) {
  return (
    <OverlayTrigger
      placement="right"
      overlay={
        <Tooltip id={`tooltip-coursename-${rowIndex}`}>
          View scaffold for {courseName}
        </Tooltip>
      }
    >
      <Link to={`/course/${courseId}`} data-testid={testId}>
        <GraphIcon className="me-2" />
        {courseName}
      </Link>
    </OverlayTrigger>
  );
}
