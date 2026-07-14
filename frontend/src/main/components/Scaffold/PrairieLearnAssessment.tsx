// Minimal shape needed to render an assessment's title with its colored "set" badge.
// Both Assessment and AssessmentManagementDTO satisfy this structurally.
export interface PrairieLearnAssessmentInfo {
  name: string;
  pl_assessment_set_abbreviation?: string | null;
  pl_assessment_number?: string | null;
  pl_assessment_set_color?: string | null;
}

// Chooses badge text color for legibility against the given background color, per the
// standard perceived-luminance formula: white text on dark/saturated backgrounds
// (luminance <= 150), black text otherwise.
function badgeTextColor(backgroundHex: string): string {
  const hex = backgroundHex.replace("#", "");
  if (hex.length !== 6) {
    return "#000000";
  }
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  if ([r, g, b].some((c) => Number.isNaN(c))) {
    return "#000000";
  }
  const luminance = r * 0.299 + g * 0.587 + b * 0.114;
  return luminance <= 150 ? "#ffffff" : "#000000";
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
  const background = assessment.pl_assessment_set_color ?? "#94A3B8";
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        minWidth: 111,
        padding: "2px 8px",
        borderRadius: 999,
        background,
        color: badgeTextColor(background),
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
