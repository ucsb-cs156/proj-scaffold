import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useParams } from "react-router";
import { Spinner } from "react-bootstrap";
import ConceptGraphV2 from "main/components/Scaffold/ConceptGraphV2";
import "App.css";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";

import {
  fetchAssessments,
  fetchQuestions,
  fetchQuestionConcepts,
  fetchConceptGraph,
  fetchConceptContent,
  fetchConceptPositions,
  fetchConceptEdges,
  fetchUserStateV2,
  logUserActivityV2,
  saveUserStateV2,
  type MajorConceptDTO,
  type ConceptContentDTO,
  type EdgeDTO,
} from "main/api/client";
import type { Assessment, Question } from "main/api/client";
import LoginScreen from "main/components/Auth/LoginScreen";
import QuestionSearch from "main/components/Scaffold/QuestionSearch";
import AssessmentSelect from "main/components/Scaffold/AssessmentSelect";
import { useCurrentUser } from "main/utils/currentUser";
import {
  normalize,
  toPastel,
  computeSubgraphV2,
} from "main/utils/conceptGraphUtils";

// Database-driven counterpart to LegacyHomePage.tsx, rendered at /course/{courseId}.
// Everything here is fetched from the concepts/* and user-state-v2/
// user-activity-v2 backend endpoints instead of the hardcoded data files, so
// this page can eventually support any course, not just the one LegacyHomePage.tsx
// has baked in. LegacyHomePage.tsx itself is untouched.

interface SavedDetailCard {
  cardType: string;
  itemLabel: string;
  conceptId: string;
  conceptColor: string;
  posX: number;
  posY: number;
}

export default function ConceptGraphPage() {
  const { courseId: courseIdParam } = useParams<{ courseId: string }>();
  const courseId = Number(courseIdParam);
  const courseIdIsValid =
    courseIdParam !== undefined && !Number.isNaN(courseId);

  const currentUser = useCurrentUser();
  // Derive the numeric id from the users table; null when not logged in.
  const userId: number | null = currentUser?.loggedIn
    ? ((currentUser.root as { user?: { id?: number } })?.user?.id ?? null)
    : null;

  const [assessments, setAssessments] = useState<Assessment[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [selectedAssessmentId, setSelectedAssessmentId] = useState("");
  const [selectedQuestionId, setSelectedQuestionId] = useState("");
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

  // Graph data fetched from the backend for this course.
  const [majorConcepts, setMajorConcepts] = useState<MajorConceptDTO[]>([]);
  const [positions, setPositions] = useState<
    Record<string, { x: number; y: number }>
  >({});
  // Private, per-user overrides of top-level concept positions (dragged by this user);
  // takes precedence over the shared `positions` above, which only an instructor's
  // POST /api/course/scaffold/reset can change. Cleared course-wide by that same reset.
  const [topLevelPositions, setTopLevelPositions] = useState<
    Record<string, { x: number; y: number }>
  >({});
  const [conceptContent, setConceptContent] = useState<
    Record<string, ConceptContentDTO>
  >({});
  const [prereqEdgeData, setPrereqEdgeData] = useState<EdgeDTO[]>([]);
  const [graphDataLoaded, setGraphDataLoaded] = useState(false);
  const [graphDataError, setGraphDataError] = useState<string | null>(null);

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

  // Fetch the concept graph data for this course once on mount.
  useEffect(() => {
    if (!courseIdIsValid) return;
    let cancelled = false;

    Promise.all([
      fetchConceptGraph(courseId),
      fetchConceptContent(courseId),
      fetchConceptPositions(courseId),
      fetchConceptEdges(courseId),
    ])
      .then(([graph, content, positionsData, edges]) => {
        if (cancelled) return;
        setMajorConcepts(graph);
        setConceptContent(content);
        setPositions(positionsData);
        setPrereqEdgeData(edges);
        setGraphDataLoaded(true);
      })
      .catch(() => {
        if (cancelled) return;
        setGraphDataError(
          `Failed to load concept graph data for course ${courseId}.`,
        );
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, courseIdIsValid]);

  // Load the user's saved state once on login (and once graph data is ready).
  const userStateLoadedRef = useRef(false);
  useEffect(() => {
    if (!userId || !courseIdIsValid || userStateLoadedRef.current) return;
    userStateLoadedRef.current = true;

    logUserActivityV2({
      userid: userId,
      courseId,
      event_type: "login",
      payload: { consented: true },
    });

    fetchUserStateV2(userId, courseId).then((data) => {
      if (data) {
        setStarredIds(new Set(data.starred_ids as string[]));
        const cards = (data.detail_cards as SavedDetailCard[]) ?? [];
        setSavedDetailCards(cards);
        setAddedDetailKeys(
          new Set(cards.map((c) => `${c.cardType}:${c.itemLabel}`)),
        );
        setInitialDetailCards(cards);
        setMasteredSubconcepts(
          new Set((data.mastered_subconcepts ?? []) as string[]),
        );
        setTopLevelPositions(data.top_level_positions ?? {});
      }
    });
  }, [userId, courseId, courseIdIsValid]);

  const persistState = useCallback(
    async (
      stars: Set<string>,
      cards: SavedDetailCard[],
      mastered: Set<string> = masteredSubconceptsRef.current,
      topLevelPos: Record<
        string,
        { x: number; y: number }
      > = topLevelPositionsRef.current,
    ) => {
      if (!userId || !courseIdIsValid) return;
      await saveUserStateV2({
        userid: userId,
        courseId,
        starred_ids: Array.from(stars),
        detail_cards: cards,
        mastered_subconcepts: Array.from(mastered),
        top_level_positions: topLevelPos,
      });
    },
    [userId, courseId, courseIdIsValid],
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

  const logActivity = useCallback(
    async (eventType: string, payload: object) => {
      if (!userId || !courseIdIsValid) return;
      await logUserActivityV2({
        userid: userId,
        courseId,
        event_type: eventType,
        payload,
      });
    },
    [userId, courseId, courseIdIsValid],
  );

  const handlePaneClick = () => {
    if (!selectedQuestionId) {
      setSelectedConceptId(null);
      setSelectedItem(null);
      setHighlightedIds(new Set());
    }
  };

  useEffect(() => {
    fetchAssessments().then(setAssessments);
  }, []);

  useEffect(() => {
    if (!selectedAssessmentId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setQuestions([]);
      return;
    }
    fetchQuestions(selectedAssessmentId).then(setQuestions);
    setSelectedQuestionId("");
    setHighlightedIds(new Set());
    setSelectedConceptId(null);
    setHighlightedSubconcepts(new Map());
  }, [selectedAssessmentId]);

  useEffect(() => {
    if (!selectedQuestionId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setHighlightedIds(new Set());

      setHighlightedSubconcepts(new Map());
      return;
    }
    fetchQuestionConcepts(selectedQuestionId).then((concepts) => {
      setHighlightedIds(
        computeSubgraphV2(
          concepts.map((c) => c.concept_id),
          prereqEdgeData,
        ),
      );
      const subMap = new Map<string, Set<string>>();
      concepts.forEach((c) => {
        if (c.subconcept_label) {
          if (!subMap.has(c.concept_id)) subMap.set(c.concept_id, new Set());
          subMap.get(c.concept_id)!.add(normalize(c.subconcept_label));
        }
      });
      setHighlightedSubconcepts(subMap);
    });
    setSelectedConceptId(null);
    logActivity("question_viewed", { questionId: selectedQuestionId });
    // Including prereqEdgeData also re-runs this when the edges finish loading,
    // so a question selected before that point still gets its full
    // prerequisite chain highlighted rather than only the tagged concepts.
  }, [logActivity, selectedQuestionId, prereqEdgeData]);

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
      setHighlightedIds(computeSubgraphV2([id], prereqEdgeData));
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
    <BasicLayout>
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
        <div
          style={{
            height: 60,
            flexShrink: 0,
            background: "#f4e87b",
            display: "flex",
            alignItems: "center",
            gap: 12,
            padding: "0 20px",
          }}
        >
          <div
            style={{
              fontFamily: "Helvetica, Arial, sans-serif",
              fontWeight: 800,
              fontSize: 20,
              color: "#1E293B",
              background: "#d9f9ff",
              padding: "4px 12px",
              borderRadius: 8,
              borderTop: "1.5px solid #1E293B",
              borderLeft: "1.5px solid #1E293B",
              borderRight: "4px solid #1E293B",
              borderBottom: "4px solid #1E293B",
            }}
          >
            Scaffold
          </div>
          <AssessmentSelect
            assessments={assessments}
            selectedAssessmentId={selectedAssessmentId}
            onSelect={(id) => {
              setSelectedAssessmentId(id);
              setSelectedQuestionId("");
            }}
          />
          <div style={{ flex: 1, maxWidth: 300 }}>
            <QuestionSearch
              questions={questions}
              selectedQuestionId={selectedQuestionId}
              onSelect={setSelectedQuestionId}
              disabled={!selectedAssessmentId || questions.length === 0}
            />
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              marginLeft: "auto",
              paddingRight: 0,
            }}
          >
            <div
              style={{
                width: 28,
                height: 28,
                borderRadius: 6,
                borderTop: "1.5px solid #1E293B",
                borderLeft: "1.5px solid #1E293B",
                borderRight: "4px solid #1E293B",
                borderBottom: "4px solid #1E293B",
                background: "#FACC15",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                flexShrink: 0,
              }}
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="#1E293B"
                stroke="#1E293B"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <span
              style={{
                fontFamily: "Helvetica, Arial, sans-serif",
                fontSize: "clamp(11px, 2vw, 16px)",
                fontWeight: 700,
                color: "#1E293B",
                letterSpacing: "0.03em",
                whiteSpace: "nowrap",
              }}
            >
              {starredIds.size} / {majorConcepts.length}
            </span>
          </div>
        </div>

        {/* ── Graph ── */}
        <div style={{ flex: 1, minHeight: 0, background: "#ffffff" }}>
          <ConceptGraphV2
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
