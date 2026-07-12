import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useParams } from "react-router";
import { Spinner } from "react-bootstrap";
import ScaffoldConceptGraph from "main/components/Scaffold/ScaffoldConceptGraph";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";

import { useQueryClient } from "@tanstack/react-query";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import type {
  Assessment,
  Course,
  Question,
  QuestionConcept,
  MajorConceptDTO,
  SubconceptDTO,
  ConceptContentDTO,
  PositionDTO,
  EdgeDTO,
  UserStateResponse,
} from "main/types/conceptGraph";
import LoginScreen from "main/components/Auth/LoginScreen";
import ScaffoldTopBar from "main/components/Scaffold/ScaffoldTopBar";
import { useCurrentUser } from "main/utils/currentUser";
import { StaffToolsProvider } from "main/utils/staffTools";
import {
  normalize,
  toPastel,
  computeScaffoldSubgraph,
} from "main/utils/conceptGraphUtils";

// Database-driven counterpart to LegacyHomePage.tsx, rendered at /course/{courseId}.
// All backend access goes through useBackend/useBackendMutation (React Query),
// against the concepts/*, user-state, and user-activity endpoints, so this page
// can eventually support any course, not just the one LegacyHomePage.tsx has
// baked in. LegacyHomePage.tsx itself is untouched and talks to the frozen
// /api/legacy/* endpoints via main/api/legacyClient.ts instead.

interface SavedDetailCard {
  cardType: string;
  itemLabel: string;
  conceptId: string;
  conceptColor: string;
  posX: number;
  posY: number;
}

// The staff tools (debug tooltips, subconcept reordering) act on this page's
// concept graph, so their provider is mounted here — not at the app root —
// and the Footer's toggles only appear here.
export default function ConceptGraphPage() {
  return (
    <StaffToolsProvider>
      <ConceptGraphPageContent />
    </StaffToolsProvider>
  );
}

function ConceptGraphPageContent() {
  const { courseId: courseIdParam } = useParams<{ courseId: string }>();
  const courseId = Number(courseIdParam);
  const courseIdIsValid =
    courseIdParam !== undefined && !Number.isNaN(courseId);

  const currentUser = useCurrentUser();
  // Derive the numeric id from the users table; null when not logged in.
  const userId: number | null = currentUser?.loggedIn
    ? (currentUser.root.user?.id ?? null)
    : null;

  const queryClient = useQueryClient();

  const [selectedAssessmentId, setSelectedAssessmentId] = useState("");
  const [selectedQuestionId, setSelectedQuestionId] = useState("");

  const { data: assessments = [] } = useBackend<Assessment[]>(
    ["/api/assessments"],
    { method: "GET", url: "/api/assessments" },
    [],
  );

  // The course endpoint is staff-only; for students the request fails and the
  // UI simply omits staff affordances like the settings link, so the expected
  // error must not toast.
  const { data: course } = useBackend<Course | undefined>(
    ["/api/courses", courseId],
    { method: "GET", url: `/api/courses/${courseId}` },
    undefined,
    true,
    { enabled: courseIdIsValid, retry: false },
  );

  const { data: questions = [] } = useBackend<Question[]>(
    ["/api/assessments", selectedAssessmentId, "questions"],
    {
      method: "GET",
      url: `/api/assessments/${selectedAssessmentId}/questions`,
    },
    [],
    false,
    { enabled: !!selectedAssessmentId },
  );

  const { data: questionConcepts = [] } = useBackend<QuestionConcept[]>(
    ["/api/questions", selectedQuestionId, "concepts"],
    { method: "GET", url: `/api/questions/${selectedQuestionId}/concepts` },
    [],
    false,
    { enabled: !!selectedQuestionId },
  );
  const [highlightedIds, setHighlightedIds] = useState<Set<string>>(new Set());
  const [selectedConceptId, setSelectedConceptId] = useState<string | null>(
    null,
  );
  const [highlightedSubconcepts, setHighlightedSubconcepts] = useState<
    Map<string, Set<string>>
  >(new Map());
  const [selectedItem, setSelectedItem] = useState<string | null>(null);
  const [starredIds, setStarredIds] = useState<Set<string>>(new Set());
  const [savedDetailCards, setSavedDetailCards] = useState<SavedDetailCard[]>(
    [],
  );
  const [initialDetailCards, setInitialDetailCards] = useState<
    SavedDetailCard[]
  >([]);
  const [addedDetailKeys, setAddedDetailKeys] = useState<Set<string>>(
    new Set(),
  );
  const [masteredSubconcepts, setMasteredSubconcepts] = useState<Set<string>>(
    new Set(),
  );

  // Graph data fetched from the backend for this course. These four queries
  // feed state that the user mutates locally (dragging, reordering), so window
  // focus must not silently refetch and clobber the local copy; retry is off so
  // a failure surfaces as the error screen right away, as the old fetch did.
  const graphQueryKey = ["/api/concepts/graph", courseId];
  const graphQuery = useBackend<MajorConceptDTO[]>(
    graphQueryKey,
    { method: "GET", url: "/api/concepts/graph", params: { courseId } },
    [],
    false,
    {
      enabled: courseIdIsValid,
      retry: false,
      refetchOnWindowFocus: false,
      // A refetch must snap the locally-reordered copy below back to the
      // authoritative order even when the server data is unchanged, so the
      // mirror effect needs a fresh data reference on every fetch.
      structuralSharing: false,
    },
  );
  const contentQuery = useBackend<Record<string, ConceptContentDTO>>(
    ["/api/concepts/content", courseId],
    { method: "GET", url: "/api/concepts/content", params: { courseId } },
    {},
    false,
    { enabled: courseIdIsValid, retry: false, refetchOnWindowFocus: false },
  );
  const positionsQuery = useBackend<Record<string, PositionDTO>>(
    ["/api/concepts/positions", courseId],
    { method: "GET", url: "/api/concepts/positions", params: { courseId } },
    {},
    false,
    { enabled: courseIdIsValid, retry: false, refetchOnWindowFocus: false },
  );
  const edgesQuery = useBackend<EdgeDTO[]>(
    ["/api/concepts/edges", courseId],
    { method: "GET", url: "/api/concepts/edges", params: { courseId } },
    [],
    false,
    { enabled: courseIdIsValid, retry: false, refetchOnWindowFocus: false },
  );

  // Shared, instructor-controlled top-level positions (only POST
  // /api/course/scaffold/reset changes them).
  const positions = useMemo(
    () => positionsQuery.data ?? {},
    [positionsQuery.data],
  );
  const conceptContent = useMemo(
    () => contentQuery.data ?? {},
    [contentQuery.data],
  );
  const prereqEdgeData = useMemo(
    () => edgesQuery.data ?? [],
    [edgesQuery.data],
  );

  const graphDataError =
    graphQuery.isError ||
    contentQuery.isError ||
    positionsQuery.isError ||
    edgesQuery.isError
      ? `Failed to load concept graph data for course ${courseId}.`
      : null;
  // initialData makes these queries "successful" from the first render, so
  // gate the page on isFetched (the first real response) instead.
  const graphDataLoaded =
    graphQuery.isFetched &&
    contentQuery.isFetched &&
    positionsQuery.isFetched &&
    edgesQuery.isFetched;

  // Local copy of the fetched graph: subconcept reordering updates it
  // optimistically, and a refetch (e.g. invalidation after a rejected reorder)
  // snaps it back to the authoritative order.
  const [majorConcepts, setMajorConcepts] = useState<MajorConceptDTO[]>([]);
  const graphData = graphQuery.data;
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMajorConcepts(graphData ?? []);
  }, [graphData]);

  // Private, per-user overrides of top-level concept positions (dragged by this
  // user); takes precedence over the shared `positions` above. Cleared
  // course-wide by an instructor's scaffold reset.
  const [topLevelPositions, setTopLevelPositions] = useState<
    Record<string, { x: number; y: number }>
  >({});

  const userStateQuery = useBackend<UserStateResponse | undefined>(
    ["/api/user-state", userId, courseId],
    {
      method: "GET",
      url: "/api/user-state",
      params: { userid: userId, courseId },
    },
    undefined,
    false,
    {
      enabled: !!userId && courseIdIsValid,
      retry: false,
      refetchOnWindowFocus: false,
    },
  );

  const saveUserStateMutation = useBackendMutation<{
    userid: number;
    courseId: number;
    starred_ids: string[];
    detail_cards: unknown[];
    mastered_subconcepts: string[];
    top_level_positions: Record<string, { x: number; y: number }>;
  }>((body) => ({ method: "POST", url: "/api/user-state", data: body }), {});

  const logActivityMutation = useBackendMutation<{
    userid: number;
    courseId: number;
    event_type: string;
    payload: object;
  }>((body) => ({ method: "POST", url: "/api/user-activity", data: body }), {});

  const reorderMutation = useBackendMutation<
    { parentConceptId: number; orderedSubconceptIds: number[] },
    SubconceptDTO[]
  >(
    ({ parentConceptId, orderedSubconceptIds }) => ({
      method: "PUT",
      url: "/api/concepts/subconcepts/reorder",
      params: { parentConceptId },
      data: orderedSubconceptIds,
    }),
    {
      // If the backend rejects the reorder, refetch the graph so the local
      // order snaps back to the authoritative one (replaces the default toast;
      // the snap-back itself is the user-visible signal).
      onError: () => queryClient.invalidateQueries({ queryKey: graphQueryKey }),
    },
  );

  const masteredSubconceptsRef = useRef<Set<string>>(masteredSubconcepts);
  useEffect(() => {
    masteredSubconceptsRef.current = masteredSubconcepts;
  }, [masteredSubconcepts]);

  const starredIdsRef = useRef<Set<string>>(starredIds);
  useEffect(() => {
    starredIdsRef.current = starredIds;
  }, [starredIds]);

  const savedDetailCardsRef = useRef<SavedDetailCard[]>(savedDetailCards);
  useEffect(() => {
    savedDetailCardsRef.current = savedDetailCards;
  }, [savedDetailCards]);

  const topLevelPositionsRef =
    useRef<Record<string, { x: number; y: number }>>(topLevelPositions);
  useEffect(() => {
    topLevelPositionsRef.current = topLevelPositions;
  }, [topLevelPositions]);

  const { mutate: logActivityMutate } = logActivityMutation;
  const logActivity = useCallback(
    (eventType: string, payload: object) => {
      if (!userId || !courseIdIsValid) return;
      logActivityMutate({
        userid: userId,
        courseId,
        event_type: eventType,
        payload,
      });
    },
    [userId, courseId, courseIdIsValid, logActivityMutate],
  );

  // Log the login once per visit, as soon as we know who the user is.
  const loginLoggedRef = useRef(false);
  useEffect(() => {
    if (!userId || !courseIdIsValid || loginLoggedRef.current) return;
    loginLoggedRef.current = true;
    logActivity("login", { consented: true });
  }, [userId, courseIdIsValid, logActivity]);

  // Seed the locally-mutated state from the user's saved state exactly once;
  // later query refetches must not clobber unsaved local changes.
  const userStateSeededRef = useRef(false);
  const savedUserState = userStateQuery.data;
  useEffect(() => {
    if (!savedUserState || userStateSeededRef.current) return;
    userStateSeededRef.current = true;
    setStarredIds(new Set(savedUserState.starred_ids as string[]));
    const cards = (savedUserState.detail_cards as SavedDetailCard[]) ?? [];
    setSavedDetailCards(cards);
    setAddedDetailKeys(
      new Set(cards.map((c) => `${c.cardType}:${c.itemLabel}`)),
    );
    setInitialDetailCards(cards);
    setMasteredSubconcepts(
      new Set((savedUserState.mastered_subconcepts ?? []) as string[]),
    );
    setTopLevelPositions(savedUserState.top_level_positions ?? {});
  }, [savedUserState]);

  const { mutate: saveUserStateMutate } = saveUserStateMutation;
  const persistState = useCallback(
    (
      stars: Set<string>,
      cards: SavedDetailCard[],
      mastered: Set<string> = masteredSubconceptsRef.current,
      topLevelPos: Record<
        string,
        { x: number; y: number }
      > = topLevelPositionsRef.current,
    ) => {
      if (!userId || !courseIdIsValid) return;
      saveUserStateMutate({
        userid: userId,
        courseId,
        starred_ids: Array.from(stars),
        detail_cards: cards,
        mastered_subconcepts: Array.from(mastered),
        top_level_positions: topLevelPos,
      });
    },
    [userId, courseId, courseIdIsValid, saveUserStateMutate],
  );

  // Private overrides take precedence over the shared, instructor-controlled positions.
  const effectivePositions = useMemo(
    () => ({ ...positions, ...topLevelPositions }),
    [positions, topLevelPositions],
  );

  const handleMajorMoved = (name: string, posX: number, posY: number) => {
    setTopLevelPositions((prev) => {
      const next = { ...prev, [name]: { x: posX, y: posY } };
      persistState(
        starredIdsRef.current,
        savedDetailCardsRef.current,
        masteredSubconceptsRef.current,
        next,
      );
      return next;
    });
  };

  const handleSubconceptMastered = (sub: string) => {
    setMasteredSubconcepts((prev) => {
      const next = new Set(prev);
      if (next.has(sub)) {
        next.delete(sub);
      } else {
        next.add(sub);
      }
      persistState(starredIdsRef.current, savedDetailCardsRef.current, next);
      return next;
    });
  };

  // An author (with subconcepts unlocked) drag-and-dropped a card's
  // subconcepts. ScaffoldConceptGraph already updated its own nodes; mirror the new
  // order in our copy of the graph data (so anything rebuilt from it agrees)
  // and persist it via the reorder mutation, whose onError snaps the local
  // order back to the authoritative one.
  const handleSubconceptsReordered = (
    parentConceptId: number,
    orderedSubconceptIds: number[],
  ) => {
    setMajorConcepts((prev) =>
      prev.map((concept) =>
        concept.id === parentConceptId
          ? {
              ...concept,
              subconcepts: orderedSubconceptIds
                .map((subId) =>
                  concept.subconcepts.find((sub) => sub.id === subId),
                )
                .filter((sub): sub is SubconceptDTO => sub !== undefined),
            }
          : concept,
      ),
    );
    reorderMutation.mutate({ parentConceptId, orderedSubconceptIds });
  };

  const handlePaneClick = () => {
    if (!selectedQuestionId) {
      setSelectedConceptId(null);
      setSelectedItem(null);
      setHighlightedIds(new Set());
    }
  };

  // Selecting a different assessment clears the question selection and any
  // question-driven highlighting; the questions query above refetches on its
  // own because its key includes selectedAssessmentId.
  useEffect(() => {
    if (!selectedAssessmentId) return;
    /* eslint-disable react-hooks/set-state-in-effect */
    setSelectedQuestionId("");
    setHighlightedIds(new Set());
    setSelectedConceptId(null);
    setHighlightedSubconcepts(new Map());
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [selectedAssessmentId]);

  useEffect(() => {
    if (!selectedQuestionId) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSelectedConceptId(null);
    logActivity("question_viewed", { questionId: selectedQuestionId });
  }, [logActivity, selectedQuestionId]);

  // Recompute the question-driven highlighting whenever the question's
  // concepts arrive or the edges finish loading, so a question selected before
  // that point still gets its full prerequisite chain highlighted rather than
  // only the tagged concepts.
  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    if (!selectedQuestionId) {
      setHighlightedIds(new Set());
      setHighlightedSubconcepts(new Map());
      return;
    }
    setHighlightedIds(
      computeScaffoldSubgraph(
        questionConcepts.map((c) => c.concept_id),
        prereqEdgeData,
      ),
    );
    const subMap = new Map<string, Set<string>>();
    questionConcepts.forEach((c) => {
      if (c.subconcept_label) {
        if (!subMap.has(c.concept_id)) subMap.set(c.concept_id, new Set());
        subMap.get(c.concept_id)!.add(normalize(c.subconcept_label));
      }
    });
    setHighlightedSubconcepts(subMap);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [selectedQuestionId, questionConcepts, prereqEdgeData]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSelectedItem(null);
  }, [selectedConceptId]);

  useEffect(() => {
    if (!selectedItem || !selectedConceptId) return;
    logActivity("detail_visited", {
      conceptId: selectedConceptId,
      itemLabel: selectedItem,
    });
  }, [logActivity, selectedConceptId, selectedItem]);

  const selectedConcept = selectedConceptId
    ? majorConcepts.find((c) => String(c.id) === selectedConceptId)
    : null;

  const handleConceptClick = (id: string) => {
    setSelectedConceptId(id);
    if (!selectedQuestionId) {
      setHighlightedIds(computeScaffoldSubgraph([id], prereqEdgeData));
    }
    logActivity("concept_clicked", { conceptId: id });
  };

  const handleStarClick = (id: string) => {
    setStarredIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      persistState(next, savedDetailCardsRef.current);
      return next;
    });
  };

  const handleReset = () => {
    setStarredIds(new Set());
    setHighlightedIds(new Set());
    setHighlightedSubconcepts(new Map());
    setSelectedConceptId(null);
    setSelectedItem(null);
    setSelectedQuestionId("");
    setSavedDetailCards([]);
    setAddedDetailKeys(new Set());
    setMasteredSubconcepts(new Set());
    setTopLevelPositions({});
    persistState(new Set(), [], new Set(), {});
  };

  const handleDetailAdded = (card: SavedDetailCard) => {
    const key = `${card.cardType}:${card.itemLabel}`;
    setAddedDetailKeys((prev) => new Set([...prev, key]));
    setSavedDetailCards((prev) => {
      const next = [...prev, card];
      persistState(starredIdsRef.current, next);
      return next;
    });
    logActivity("detail_added_to_graph", {
      cardType: card.cardType,
      itemLabel: card.itemLabel,
      conceptId: card.conceptId,
    });
  };

  const handleDetailDeleted = (cardType: string, itemLabel: string) => {
    const key = `${cardType}:${itemLabel}`;
    setAddedDetailKeys((prev) => {
      const s = new Set(prev);
      s.delete(key);
      return s;
    });
    setSavedDetailCards((prev) => {
      const next = prev.filter(
        (c) => !(c.cardType === cardType && c.itemLabel === itemLabel),
      );
      persistState(starredIdsRef.current, next);
      return next;
    });
  };

  const handleDetailMoved = (
    cardType: string,
    itemLabel: string,
    posX: number,
    posY: number,
  ) => {
    setSavedDetailCards((prev) => {
      const next = prev.map((c) =>
        c.cardType === cardType && c.itemLabel === itemLabel
          ? { ...c, posX, posY }
          : c,
      );
      persistState(starredIdsRef.current, next);
      return next;
    });
  };

  if (!currentUser?.loggedIn) {
    return (
      <BasicLayout>
        <LoginScreen />
      </BasicLayout>
    );
  }

  if (!courseIdIsValid) {
    return (
      <BasicLayout>
        <div style={{ padding: 20 }}>Invalid course id.</div>
      </BasicLayout>
    );
  }

  if (graphDataError) {
    return (
      <BasicLayout>
        <div style={{ padding: 20 }}>{graphDataError}</div>
      </BasicLayout>
    );
  }

  if (!graphDataLoaded) {
    return (
      <BasicLayout>
        <div
          data-testid="concept-graph-page-loading"
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flex: 1,
            minHeight: "50vh",
          }}
        >
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Loading...</span>
          </Spinner>
        </div>
      </BasicLayout>
    );
  }

  return (
    <BasicLayout lockScroll>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          width: "100%",
          flex: 1,
          minHeight: 0,
          color: "#f7ede1",
        }}
      >
        {/* ── Top bar ── */}
        <ScaffoldTopBar
          course={course}
          assessments={assessments}
          selectedAssessmentId={selectedAssessmentId}
          onSelectAssessment={(id) => {
            setSelectedAssessmentId(id);
            setSelectedQuestionId("");
          }}
          questions={questions}
          selectedQuestionId={selectedQuestionId}
          onSelectQuestion={setSelectedQuestionId}
          numStarredConcepts={starredIds.size}
          numTotalConcepts={majorConcepts.length}
        />

        {/* ── Graph ── */}
        <div style={{ flex: 1, minHeight: 0, background: "#ffffff" }}>
          <ScaffoldConceptGraph
            majorConcepts={majorConcepts}
            positions={effectivePositions}
            conceptContent={conceptContent}
            prereqEdgeData={prereqEdgeData}
            highlightedIds={highlightedIds}
            highlightedSubconcepts={highlightedSubconcepts}
            onConceptClick={handleConceptClick}
            starredIds={starredIds}
            onStarClick={handleStarClick}
            onReset={handleReset}
            restoredDetailCards={initialDetailCards}
            onDetailAdded={handleDetailAdded}
            onDetailDeleted={handleDetailDeleted}
            onDetailMoved={handleDetailMoved}
            onMajorMoved={handleMajorMoved}
            masteredSubconcepts={masteredSubconcepts}
            onSubconceptMastered={handleSubconceptMastered}
            onSubconceptsReordered={handleSubconceptsReordered}
            onPaneClick={handlePaneClick}
          />
        </div>

        {/* ── Bottom toolbar ── */}
        <div
          style={{
            position: "relative",
            zIndex: 20,
            flexShrink: 0,
            background: "#f4e87b",
          }}
        >
          {/* Main toolbar row */}
          <div
            style={{
              padding: "12px 20px 6px",
              display: "flex",
              alignItems: "center",
              gap: 12,
              minHeight: 60,
            }}
          >
            {selectedConcept ? (
              <>
                {/* Main concept button */}
                <button
                  onClick={() =>
                    setSelectedItem(
                      selectedItem === String(selectedConcept.id)
                        ? null
                        : String(selectedConcept.id),
                    )
                  }
                  style={{
                    background:
                      selectedItem === String(selectedConcept.id)
                        ? selectedConcept.color
                        : "#ffffff",
                    color: "#000000",
                    borderTop: "1.5px solid #1E293B",
                    borderLeft: "1.5px solid #1E293B",
                    borderRight: "4px solid #1E293B",
                    borderBottom: "4px solid #1E293B",
                    borderRadius: 6,
                    padding: "5px 14px",
                    fontSize: 15,
                    fontWeight: 700,
                    fontFamily: "Helvetica, Arial, sans-serif",
                    cursor: "pointer",
                    flexShrink: 0,
                    whiteSpace: "nowrap",
                  }}
                >
                  <span
                    dangerouslySetInnerHTML={{
                      __html: selectedConcept.labelHtml.replace(/\n/g, " "),
                    }}
                  />
                </button>

                {/* Divider */}
                <div
                  style={{
                    width: 1,
                    alignSelf: "stretch",
                    background: "#00000033",
                    flexShrink: 0,
                  }}
                />

                {/* Subconcept buttons */}
                <div
                  style={{
                    flex: 1,
                    display: "flex",
                    flexWrap: "wrap",
                    gap: 8,
                    alignItems: "flex-start",
                  }}
                >
                  {selectedConcept.subconcepts.map((sub) => (
                    <button
                      key={sub.id}
                      onClick={() =>
                        setSelectedItem(
                          selectedItem === sub.labelHtml ? null : sub.labelHtml,
                        )
                      }
                      style={{
                        background:
                          selectedItem === sub.labelHtml
                            ? selectedConcept.color
                            : "#ffffff",
                        color: "#000000",
                        border: "1px solid #000000",
                        borderRadius: 6,
                        padding: "5px 14px",
                        fontSize: 14,
                        fontWeight: 500,
                        fontFamily: "Helvetica, Arial, sans-serif",
                        cursor: "pointer",
                      }}
                    >
                      <span
                        dangerouslySetInnerHTML={{
                          __html: sub.labelHtml.replace(/\n/g, " "),
                        }}
                      />
                    </button>
                  ))}
                </div>

                {/* Close button */}
                <div
                  className="homepage-toolbar-close"
                  onClick={() => {
                    setSelectedConceptId(null);
                    setSelectedItem(null);
                    if (!selectedQuestionId) setHighlightedIds(new Set());
                  }}
                >
                  <svg
                    className="homepage-toolbar-close-icon"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </div>
              </>
            ) : (
              <div
                style={{ color: "#000000", fontSize: 13, lineHeight: "36px" }}
              >
                {selectedQuestionId
                  ? "Click a concept card to explore it."
                  : "Select a question, or click any concept card to explore it."}
              </div>
            )}
          </div>

          {/* Expanded cards — only shown when a button is clicked */}
          {selectedConcept &&
            selectedItem !== null &&
            (() => {
              // selectedItem holds a subconcept's labelHtml, or the major
              // concept's node id (its numeric id as a string). Content is
              // keyed by each concept's own numeric id.
              const isMajorConcept =
                selectedItem === String(selectedConcept.id);
              const selectedItemLabel = isMajorConcept
                ? selectedConcept.labelHtml.replace(/\n/g, " ")
                : (selectedItem ?? "");
              const subconceptId = selectedConcept.subconcepts.find(
                (s) => s.labelHtml === selectedItem,
              )?.id;
              const contentKey = isMajorConcept
                ? String(selectedConcept.id)
                : String(subconceptId);
              const content = conceptContent[contentKey];

              return (
                <div
                  style={{ padding: "0 20px 20px", display: "flex", gap: 12 }}
                >
                  {(
                    [
                      { label: "Description", key: "descriptionHtml" },
                      { label: "Example", key: "exampleHtml" },
                    ] as {
                      label: string;
                      key: "descriptionHtml" | "exampleHtml";
                    }[]
                  ).map((card) => {
                    const isAdded = addedDetailKeys.has(
                      `${card.label}:${selectedItemLabel}`,
                    );
                    return (
                      <div
                        key={card.key}
                        draggable={!isAdded}
                        onDragStart={
                          isAdded
                            ? undefined
                            : (e) => {
                                e.dataTransfer.setData(
                                  "application/scaffold-card",
                                  JSON.stringify({
                                    cardType: card.label,
                                    itemLabel: selectedItemLabel,
                                    conceptId: String(selectedConcept.id),
                                    conceptColor: selectedConcept.color,
                                    cardContent: content?.[card.key] ?? "",
                                  }),
                                );
                                e.dataTransfer.effectAllowed = "move";
                              }
                        }
                        style={{
                          flex: 1,
                          background: toPastel(selectedConcept.color),
                          borderRadius: 8,
                          border: "1px solid #000000",
                          padding: "10px 14px",
                          textAlign: "left",
                          cursor: isAdded ? "default" : "grab",
                          opacity: isAdded ? 0.8 : 1,
                        }}
                      >
                        <span
                          style={{
                            background: selectedConcept.color,
                            borderRadius: 100,
                            padding: "2px 10px",
                            border: "1px solid #000000",
                            fontFamily: "Helvetica, Arial, sans-serif",
                            fontSize: 12,
                            fontWeight: 700,
                            color: "#000000",
                            whiteSpace: "nowrap",
                            width: "fit-content",
                            display: "block",
                            marginBottom: 8,
                          }}
                        >
                          {card.label}
                        </span>
                        <div
                          style={{
                            fontFamily: "Helvetica, Arial, sans-serif",
                            fontSize: 15,
                            color: "#1E293B",
                            lineHeight: 1.6,
                            whiteSpace: "pre-wrap",
                          }}
                        >
                          {card.key === "exampleHtml" ? (
                            <div
                              className="concept-detail-content"
                              style={{
                                fontFamily: "monospace",
                                border: "1px solid #000000",
                                fontSize: 13,
                                background: "#e2e8f0",
                                borderRadius: 6,
                                padding: "8px 12px",
                                margin: 0,
                                whiteSpace: "pre-wrap",
                                color: "#1E293B",
                              }}
                              dangerouslySetInnerHTML={{
                                __html:
                                  content?.[card.key] ??
                                  `Example for "${selectedItemLabel}" will appear here.`,
                              }}
                            />
                          ) : (
                            <div
                              className="concept-detail-content"
                              dangerouslySetInnerHTML={{
                                __html:
                                  content?.[card.key] ??
                                  `${card.label} for "${selectedItemLabel}" will appear here.`,
                              }}
                            />
                          )}
                        </div>
                      </div>
                    );
                  })}

                  {isMajorConcept && (
                    <a
                      href={content?.practiceUrl ?? "#"}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        flex: "0 0 auto",
                        alignSelf: "center",
                        background: selectedConcept.color,
                        borderRadius: 8,
                        borderTop: "1.5px solid #1E293B",
                        borderLeft: "1.5px solid #1E293B",
                        borderRight: "4px solid #1E293B",
                        borderBottom: "4px solid #1E293B",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        textAlign: "center",
                        fontFamily: "Helvetica, Arial, sans-serif",
                        fontSize: 15,
                        fontWeight: 700,
                        color: "#1E293B",
                        cursor: content?.practiceUrl ? "pointer" : "default",
                        textDecoration: "none",
                        padding: "10px 14px",
                        opacity: content?.practiceUrl ? 1 : 0.4,
                      }}
                    >
                      Practice with a <br />
                      PrairieLearn <br /> question
                    </a>
                  )}
                </div>
              );
            })()}
        </div>
      </div>
    </BasicLayout>
  );
}
