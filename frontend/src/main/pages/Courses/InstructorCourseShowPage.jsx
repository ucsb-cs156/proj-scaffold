import React, { useEffect, useState } from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useCurrentUser } from "main/utils/currentUser";
import { useNavigate, useParams } from "react-router";

import Modal from "react-bootstrap/Modal";
import { Button, Tab, Tabs, OverlayTrigger, Tooltip } from "react-bootstrap";
import EnrollmentTabComponent from "main/components/Courses/TabComponent/EnrollmentTabComponent";
import StaffTabComponent from "main/components/Courses/TabComponent/StaffTabComponent";
import SettingsTabComponent from "main/components/Courses/TabComponent/SettingsTabComponent";
import JobTabComponent from "main/components/Courses/TabComponent/JobTabComponent";
import { hasRole } from "main/utils/currentUser";
import DownloadsTabComponent from "main/components/Courses/TabComponent/DownloadsTabComponent";
import LinkToScaffold from "main/components/Courses/LinkToScaffold";

export default function InstructorCourseShowPage({
  testId = "InstructorCourseShowPage",
  showSettingsTab = true,
  staffTabIsInstructor = true,
  canEditStudents = undefined,
  canManageTeams = undefined,
}) {
  const currentUser = useCurrentUser();
  const courseId = useParams().id;
  const [showErrorModal, setShowErrorModal] = useState(false);

  const {
    data: course,
    error: _errorCourse,
    status: _statusCourse,
    failureCount: courseBackendFailureCount,
  } = useBackend(
    [`/api/courses/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    null,
    true,
  );

  // Stryker disable OptionalChaining -- course?.instructorEmail is more readable than course && course.instructorEmail
  const getCourseFailed = courseBackendFailureCount > 0;
  const canEditCourseOptions =
    hasRole(currentUser, "ROLE_ADMIN") ||
    currentUser?.root?.user?.email === course?.instructorEmail;
  // Stryker enable OptionalChaining

  const navigate = useNavigate();
  useEffect(() => {
    if (getCourseFailed) {
      setShowErrorModal(true);
      const timer = setTimeout(() => {
        navigate("/", { replace: true });
      }, 3000);
      // Stryker disable next-line BlockStatement
      return () => {
        clearTimeout(timer);
      };
    }
  }, [getCourseFailed, navigate]);

  return (
    <BasicLayout>
      <Modal show={showErrorModal}>
        <Modal.Header>
          <Modal.Title>Course Not Found</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Course not found. You will be returned to the course list in 3
          seconds.
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={() => setShowErrorModal(false)} variant={"primary"}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
      {!course ? (
        <div data-testid={`${testId}-loading`}>Course: Loading...</div>
      ) : (
        <div className="border rounded-3 p-4 mb-4 tabsGroup">
          <div className="border rounded-3 p-4 mb-4">
            <div className="d-flex align-items-center gap-3">
              {course.orgName && (
                <img
                  src={`https://github.com/${course.orgName}.png?size=64`}
                  alt={course.orgName}
                  data-testid={`${testId}-github-org-image`}
                  className="rounded-circle border"
                  style={{ width: 48, height: 48 }}
                />
              )}
              <div>
                <div className="d-flex align-items-center gap-2">
                  <h1
                    data-testid={`${testId}-title`}
                    className="h3 mb-0 fw-semibold"
                  >
                    <LinkToScaffold
                      courseName={course.courseName}
                      courseId={course.id}
                      testId={`${testId}-title-linkToScaffold`}
                    />
                  </h1>
                  <span className="badge bg-primary-subtle text-primary-emphasis rounded-pill">
                    {course.term}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <Tabs defaultActiveKey={"students"}>
            <Tab eventKey={"students"} title={"Students"} className="pt-2">
              <EnrollmentTabComponent
                courseId={courseId}
                testIdPrefix={testId}
                currentUser={currentUser}
                canEditStudents={canEditStudents}
              />
            </Tab>
            <Tab eventKey={"staff"} title={"Staff"} className="pt-2">
              <StaffTabComponent
                courseId={courseId}
                testIdPrefix={testId}
                currentUser={currentUser}
                isInstructor={staffTabIsInstructor}
              />
            </Tab>
          </Tabs>
        </div>
      )}
    </BasicLayout>
  );
}
