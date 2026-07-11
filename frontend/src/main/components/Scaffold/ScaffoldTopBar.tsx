import type { Assessment, Question, Course } from "main/api/client";
import AssessmentSelect from "main/components/Scaffold/AssessmentSelect";
import QuestionSearch from "main/components/Scaffold/QuestionSearch";
import LinkToSettings from "main/components/Scaffold/LinkToSettings";
import StarStatus from "main/components/Scaffold/StarStatus";

interface ScaffoldTopBarProps {
  // Undefined while the course is still loading; the settings link is
  // omitted until it arrives.
  course?: Course;
  assessments: Assessment[];
  selectedAssessmentId: string;
  onSelectAssessment: (id: string) => void;
  questions: Question[];
  selectedQuestionId: string;
  onSelectQuestion: (id: string) => void;
  numStarredConcepts: number;
  numTotalConcepts: number;
}

export default function ScaffoldTopBar({
  course,
  assessments,
  selectedAssessmentId,
  onSelectAssessment,
  questions,
  selectedQuestionId,
  onSelectQuestion,
  numStarredConcepts,
  numTotalConcepts,
}: ScaffoldTopBarProps) {
  return (
    <div className="scaffold-top-bar" data-testid="ScaffoldTopBar">
      <AssessmentSelect
        assessments={assessments}
        selectedAssessmentId={selectedAssessmentId}
        onSelect={onSelectAssessment}
      />
      <div style={{ width: 300 }}>
        <QuestionSearch
          questions={questions}
          selectedQuestionId={selectedQuestionId}
          onSelect={onSelectQuestion}
          disabled={!selectedAssessmentId || questions.length === 0}
        />
      </div>
      <div
        style={{
          marginLeft: "auto",
          display: "flex",
          alignItems: "center",
          gap: 12,
        }}
      >
        {course && (
          <LinkToSettings
            course={course}
            testId="ScaffoldTopBar-linkToSettings"
          />
        )}
        <StarStatus
          numStarredConcepts={numStarredConcepts}
          numTotalConcepts={numTotalConcepts}
        />
      </div>
    </div>
  );
}
