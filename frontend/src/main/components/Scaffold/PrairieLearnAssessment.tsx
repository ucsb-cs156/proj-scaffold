// Minimal shape needed to render an assessment's title with its colored "set" badge.
// Both Assessment and AssessmentManagementDTO satisfy this structurally.
export interface PrairieLearnAssessmentInfo {
  name: string;
  pl_assessment_set_abbreviation?: string | null;
  pl_assessment_number?: string | null;
  pl_assessment_set_color?: string | null;
}

// Pill-shaped badge showing the assessment's "set" abbreviation + number (e.g. "HW2"),
// colored with pl_assessment_set_color. Renders nothing if the PL-API sync (issue #71)
// hasn't populated these fields yet.
function AssessmentBadge({
  assessment,
}: {
  assessment: PrairieLearnAssessmentInfo;
}) {
  const label =
    (assessment.pl_assessment_set_abbreviation ?? "") +
    (assessment.pl_assessment_number ?? "");
  if (!label) {
    return null;
  }
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        minWidth: 36,
        padding: "2px 8px",
        borderRadius: 999,
        background: assessment.pl_assessment_set_color ?? "#94A3B8",
        color: "#000000",
        fontSize: 11,
        fontWeight: 600,
        lineHeight: 1.4,
        whiteSpace: "nowrap",
      }}
    >
      {label}
    </span>
  );
}

// Shared rendering of an assessment's badge + title, both flush left, matching the
// PrairieLearn styling. Used by both AssessmentSelect (dropdown) and
// UnlockAssessmentsModal (instructor lock/unlock list).
export default function PrairieLearnAssessment({
  assessment,
}: {
  assessment: PrairieLearnAssessmentInfo;
}) {
  return (
    <span
      style={{ display: "flex", alignItems: "center", flex: 1, minWidth: 0 }}
    >
      <AssessmentBadge assessment={assessment} />
      <span
        style={{
          marginLeft: 8,
          textAlign: "left",
          overflow: "hidden",
          textOverflow: "ellipsis",
          whiteSpace: "nowrap",
        }}
      >
        {assessment.name}
      </span>
    </span>
  );
}
