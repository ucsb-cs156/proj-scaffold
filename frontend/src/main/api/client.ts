const API_BASE = "/api";

export interface Assessment {
  id: string;
  pl_assessment_id: string;
  name: string;
}

export interface Question {
  id: string;
  assessment_id: string;
  pl_question_uuid: string;
  title: string;
}

export interface QuestionConcept {
  id: string;
  question_id: string;
  concept_id: string;
  subconcept_label: string | null;
}

export async function fetchAssessments(): Promise<Assessment[]> {
  const res = await fetch(`${API_BASE}/assessments`);
  return res.json();
}

export async function fetchQuestions(
  assessmentId: string,
): Promise<Question[]> {
  const res = await fetch(`${API_BASE}/assessments/${assessmentId}/questions`);
  return res.json();
}

export async function fetchQuestionConcepts(
  questionId: string,
): Promise<QuestionConcept[]> {
  const res = await fetch(`${API_BASE}/questions/${questionId}/concepts`);
  return res.json();
}

export interface UserStateResponse {
  starred_ids: string[];
  detail_cards: unknown[];
  mastered_subconcepts: string[];
}

export async function fetchUserState(
  userid: number,
): Promise<UserStateResponse | null> {
  const res = await fetch(`${API_BASE}/user-state/${userid}`);
  if (res.status === 404) return null;
  if (!res.ok)
    throw new Error(`Failed to fetch user state for userid ${userid}`);
  return res.json();
}

export async function saveUserState(body: {
  userid: number;
  starred_ids: string[];
  detail_cards: unknown[];
  mastered_subconcepts: string[];
}): Promise<void> {
  const res = await fetch(`${API_BASE}/user-state`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok)
    throw new Error(`Failed to save user state for userid ${body.userid}`);
}

export async function logUserActivity(body: {
  userid: number;
  event_type: string;
  payload: object;
}): Promise<void> {
  const res = await fetch(`${API_BASE}/user-activity`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok)
    throw new Error(`Failed to log user activity for userid ${body.userid}`);
}

// ── Database-driven concept graph (per-course) ──────────────────────────────

/**
 * An HTML string that has already been rendered from Markdown and sanitized
 * server-side (OWASP HTML sanitizer) — see MarkdownService on the backend.
 * Safe to render via `dangerouslySetInnerHTML`; do not re-escape or otherwise
 * transform it before doing so.
 */
export type SafeHtml = string;

export interface SubconceptDTO {
  id: number;
  parentId: number;
  labelHtml: SafeHtml;
}

export interface MajorConceptDTO {
  id: number;
  name: string;
  labelHtml: SafeHtml;
  color: string;
  subconcepts: SubconceptDTO[];
}

export interface ConceptContentDTO {
  id: number;
  parentId: number | null;
  descriptionHtml: SafeHtml | null;
  exampleHtml: SafeHtml | null;
  practiceUrl: string | null;
}

export interface PositionDTO {
  x: number;
  y: number;
}

export interface EdgeDTO {
  source: string;
  target: string;
}

export async function fetchConceptGraph(
  courseId: number,
): Promise<MajorConceptDTO[]> {
  const res = await fetch(`${API_BASE}/concepts/graph?courseId=${courseId}`);
  if (!res.ok)
    throw new Error(`Failed to fetch concept graph for course ${courseId}`);
  return res.json();
}

export async function fetchConceptContent(
  courseId: number,
): Promise<Record<string, ConceptContentDTO>> {
  const res = await fetch(`${API_BASE}/concepts/content?courseId=${courseId}`);
  if (!res.ok)
    throw new Error(`Failed to fetch concept content for course ${courseId}`);
  return res.json();
}

export async function fetchConceptPositions(
  courseId: number,
): Promise<Record<string, PositionDTO>> {
  const res = await fetch(
    `${API_BASE}/concepts/positions?courseId=${courseId}`,
  );
  if (!res.ok)
    throw new Error(`Failed to fetch concept positions for course ${courseId}`);
  return res.json();
}

export async function fetchConceptEdges(courseId: number): Promise<EdgeDTO[]> {
  const res = await fetch(`${API_BASE}/concepts/edges?courseId=${courseId}`);
  if (!res.ok)
    throw new Error(`Failed to fetch concept edges for course ${courseId}`);
  return res.json();
}

// ── Course-scoped user state / activity (V2) ────────────────────────────────

export interface UserStateV2Response {
  starred_ids: string[];
  detail_cards: unknown[];
  mastered_subconcepts: string[];
  top_level_positions: Record<string, { x: number; y: number }>;
}

export async function fetchUserStateV2(
  userid: number,
  courseId: number,
): Promise<UserStateV2Response | null> {
  const res = await fetch(
    `${API_BASE}/user-state-v2?userid=${userid}&courseId=${courseId}`,
  );
  if (res.status === 404) return null;
  if (!res.ok)
    throw new Error(
      `Failed to fetch user state for userid ${userid} in course ${courseId}`,
    );
  return res.json();
}

export async function saveUserStateV2(body: {
  userid: number;
  courseId: number;
  starred_ids: string[];
  detail_cards: unknown[];
  mastered_subconcepts: string[];
  top_level_positions: Record<string, { x: number; y: number }>;
}): Promise<void> {
  const res = await fetch(`${API_BASE}/user-state-v2`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok)
    throw new Error(
      `Failed to save user state for userid ${body.userid} in course ${body.courseId}`,
    );
}

export async function logUserActivityV2(body: {
  userid: number;
  courseId: number;
  event_type: string;
  payload: object;
}): Promise<void> {
  const res = await fetch(`${API_BASE}/user-activity-v2`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok)
    throw new Error(
      `Failed to log user activity for userid ${body.userid} in course ${body.courseId}`,
    );
}
