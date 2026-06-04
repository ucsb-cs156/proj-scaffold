import { useState, useEffect, useRef, useCallback } from 'react';
import ConceptGraph from './components/ConceptGraph';
import { fetchAssessments, fetchQuestions, fetchQuestionConcepts, fetchUserState, logUserActivity, saveUserState } from './api/client';
import type { Assessment, Question } from './api/client';
import { majorConcepts, prereqEdgeData } from './data/conceptGraph';
import ConsentScreen from './components/ConsentScreen';
import QuestionSearch from './components/QuestionSearch';
import AssessmentSelect from './components/AssessmentSelect';
import { conceptContent, type ConceptContent} from './data/conceptContent';

const normalize = (s: string) => s.replace(/\\n/g, '\n');

function toPastel(hex: string, strength: number = 0.1): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgb(${Math.round(r * strength + 255 * (1 - strength))}, ${Math.round(g * strength + 255 * (1 - strength))}, ${Math.round(b * strength + 255 * (1 - strength))})`;
}

// Walk the prereq graph upward from tagged concepts to include all ancestors
function computeSubgraph(taggedIds: string[]): Set<string> {
  const result = new Set<string>(taggedIds);
  let changed = true;
  while (changed) {
    changed = false;
    for (const { source, target } of prereqEdgeData) {
      if (result.has(target) && !result.has(source)) {
        result.add(source);
        changed = true;
      }
    }
  }
  return result;
}

export default function App() {
  interface SavedDetailCard {
    cardType: string; itemLabel: string;
    conceptId: string; conceptColor: string;
    posX: number; posY: number;
  }

  const [assessments, setAssessments]                       = useState<Assessment[]>([]);
  const [questions, setQuestions]                           = useState<Question[]>([]);
  const [selectedAssessmentId, setSelectedAssessmentId]     = useState('');
  const [selectedQuestionId, setSelectedQuestionId]         = useState('');
  const [highlightedIds, setHighlightedIds]                 = useState<Set<string>>(new Set());
  const [selectedConceptId, setSelectedConceptId]           = useState<string | null>(null);
  const [highlightedSubconcepts, setHighlightedSubconcepts] = useState<Map<string, Set<string>>>(new Map());
  const [selectedItem, setSelectedItem]                     = useState<string | null>(null);
  const [studentPin, setStudentPin]                         = useState<string | null>(null);
  const [_isTracked, setIsTracked]                          = useState(false);
  const [starredIds, setStarredIds]                         = useState<Set<string>>(new Set());
  const [closeHovered, setCloseHovered]                     = useState(false);
  const [savedDetailCards, setSavedDetailCards]             = useState<SavedDetailCard[]>([]);
  const [initialDetailCards, setInitialDetailCards]         = useState<SavedDetailCard[]>([]);
  const [addedDetailKeys, setAddedDetailKeys]               = useState<Set<string>>(new Set());
  const [masteredSubconcepts, setMasteredSubconcepts] = useState<Set<string>>(new Set());
  
  const masteredSubconceptsRef = useRef<Set<string>>(masteredSubconcepts);
  useEffect(() => { masteredSubconceptsRef.current = masteredSubconcepts; }, [masteredSubconcepts]);

  const starredIdsRef = useRef<Set<string>>(starredIds);
  useEffect(() => { starredIdsRef.current = starredIds; }, [starredIds]);

  const savedDetailCardsRef = useRef<SavedDetailCard[]>(savedDetailCards);
  useEffect(() => { savedDetailCardsRef.current = savedDetailCards; }, [savedDetailCards]);

  const persistState = async (pin: string, stars: Set<string>, cards: SavedDetailCard[], mastered: Set<string> = masteredSubconceptsRef.current,) => {
    await saveUserState({
      pin,
      starred_ids: Array.from(stars),
      detail_cards: cards,
      mastered_subconcepts: Array.from(mastered),  
    });
  };

  const handleSubconceptMastered = (sub: string) => {
    setMasteredSubconcepts(prev => {
      const next = new Set(prev);
      next.has(sub) ? next.delete(sub) : next.add(sub);
      persistState(studentPin!, starredIdsRef.current, savedDetailCardsRef.current, next);
      return next;
    });
  };

  const logActivity = useCallback(async (eventType: string, payload: object) => {
    if (!studentPin) return;
    await logUserActivity({
      pin: studentPin,
      event_type: eventType,
      payload,
    });
  }, [studentPin]);

  const handlePaneClick = () => {
    if (!selectedQuestionId) {
      setSelectedConceptId(null);
      setSelectedItem(null);
      setHighlightedIds(new Set());
    }
  };

  useEffect(() => { fetchAssessments().then(setAssessments); }, []);

  useEffect(() => {
    if (!selectedAssessmentId) { setQuestions([]); return; }
    fetchQuestions(selectedAssessmentId).then(setQuestions);
    setSelectedQuestionId('');
    setHighlightedIds(new Set());
    setSelectedConceptId(null);
    setHighlightedSubconcepts(new Map());
  }, [selectedAssessmentId]);

  useEffect(() => {
    if (!selectedQuestionId) {
      setHighlightedIds(new Set());
      setHighlightedSubconcepts(new Map());
      return;
    }
    fetchQuestionConcepts(selectedQuestionId).then(concepts => {
      setHighlightedIds(computeSubgraph(concepts.map(c => c.concept_id)));
      const subMap = new Map<string, Set<string>>();
      concepts.forEach(c => {
        if (c.subconcept_label) {
          if (!subMap.has(c.concept_id)) subMap.set(c.concept_id, new Set());
          subMap.get(c.concept_id)!.add(normalize(c.subconcept_label));
        }
      });
      setHighlightedSubconcepts(subMap);
    });
    setSelectedConceptId(null);
    logActivity('question_viewed', { questionId: selectedQuestionId }); // ← #1
  }, [selectedQuestionId]);

  useEffect(() => {
    setSelectedItem(null);
  }, [selectedConceptId]);

  useEffect(() => { // ← #3
    if (!selectedItem || !selectedConceptId) return;
    logActivity('detail_visited', {
      conceptId: selectedConceptId,
      itemLabel: selectedItem,
    });
  }, [selectedItem]);

  const selectedConcept = selectedConceptId
    ? majorConcepts.find(c => c.id === selectedConceptId)
    : null;

  const handleConsentComplete = async (pin: string, consented: boolean) => {
    setStudentPin(pin);
    setIsTracked(consented);
    
    await logUserActivity({
      pin,
      event_type: 'login',
      payload: {},
    });

    const data = await fetchUserState(pin);

    if (data) {
      setStarredIds(new Set(data.starred_ids as string[]));
      const cards = (data.detail_cards as SavedDetailCard[]) ?? [];
      setSavedDetailCards(cards);
      setAddedDetailKeys(new Set(cards.map(c => `${c.cardType}:${c.itemLabel}`)));
      setInitialDetailCards(cards);
      setMasteredSubconcepts(new Set(data.mastered_subconcepts as string[] ?? []));
    }
  };

  const handleConceptClick = (id: string) => {
    setSelectedConceptId(id);
    if (!selectedQuestionId) {
      setHighlightedIds(computeSubgraph([id]));
    }
    logActivity('concept_clicked', { conceptId: id });
  };

  const handleStarClick = (id: string) => {
    setStarredIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      persistState(studentPin!, next, savedDetailCardsRef.current);
      return next;
    });
  };

  const handleReset = () => {
    setStarredIds(new Set());
    setHighlightedIds(new Set());
    setHighlightedSubconcepts(new Map());
    setSelectedConceptId(null);
    setSelectedItem(null);
    setSelectedQuestionId('');
    setSavedDetailCards([]);
    setAddedDetailKeys(new Set());
    setMasteredSubconcepts(new Set());
    persistState(studentPin!, new Set(), [], new Set());
  };

  const handleDetailAdded = (card: SavedDetailCard) => {
    const key = `${card.cardType}:${card.itemLabel}`;
    setAddedDetailKeys(prev => new Set([...prev, key]));
    setSavedDetailCards(prev => {
      const next = [...prev, card];
      persistState(studentPin!, starredIdsRef.current, next);
      return next;
    });
    logActivity('detail_added_to_graph', { // ← #4
      cardType:  card.cardType,
      itemLabel: card.itemLabel,
      conceptId: card.conceptId,
    });
  };

  const handleDetailDeleted = (cardType: string, itemLabel: string) => {
    const key = `${cardType}:${itemLabel}`;
    setAddedDetailKeys(prev => { const s = new Set(prev); s.delete(key); return s; });
    setSavedDetailCards(prev => {
      const next = prev.filter(c => !(c.cardType === cardType && c.itemLabel === itemLabel));
      persistState(studentPin!, starredIdsRef.current, next);
      return next;
    });
  };

  const handleDetailMoved = (cardType: string, itemLabel: string, posX: number, posY: number) => {
    setSavedDetailCards(prev => {
      const next = prev.map(c =>
        c.cardType === cardType && c.itemLabel === itemLabel
          ? { ...c, posX, posY }
          : c
      );
      persistState(studentPin!, starredIdsRef.current, next);
      return next;
    });
  };

  if (!studentPin) {
    return <ConsentScreen onComplete={handleConsentComplete} />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', width: '100%', height: '100%', color: '#f7ede1' }}>

      {/* ── Top bar ── */}
      <div style={{
        height: 60, flexShrink: 0,
        background: '#f4e87b',
        display: 'flex', alignItems: 'center', gap: 12, padding: '0 20px',
      }}>
        <div style={{
          fontFamily: 'Helvetica, Arial, sans-serif',
          fontWeight: 800,
          fontSize: 20,
          color: '#1E293B',
          background: '#d9f9ff',
          padding: '4px 12px',
          borderRadius: 8,
          borderTop:    '1.5px solid #1E293B',
          borderLeft:   '1.5px solid #1E293B',
          borderRight:  '4px solid #1E293B',
          borderBottom: '4px solid #1E293B',
        }}>
          Scaffold
        </div>
        <AssessmentSelect
          assessments={assessments}
          selectedAssessmentId={selectedAssessmentId}
          onSelect={id => {
            setSelectedAssessmentId(id);
            setSelectedQuestionId('');
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
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginLeft: 'auto',
          paddingRight: 0,
        }}>
          <div style={{
            width: 28,
            height: 28,
            borderRadius: 6,
            borderTop:    '1.5px solid #1E293B',
            borderLeft:   '1.5px solid #1E293B',
            borderRight:  '4px solid #1E293B',
            borderBottom: '4px solid #1E293B',
            background: '#FACC15',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24"
              fill="#1E293B" stroke="#1E293B" strokeWidth="2"
              strokeLinecap="round" strokeLinejoin="round"
            >
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </div>
          <span style={{
            fontFamily: 'Helvetica, Arial, sans-serif',
            fontSize: 'clamp(11px, 2vw, 16px)',
            fontWeight: 700,
            color: '#1E293B',
            letterSpacing: '0.03em',
            whiteSpace: 'nowrap',
          }}>
            {starredIds.size} / 26
          </span>
        </div>
      </div>

      {/* ── Graph ── */}
      <div style={{ flex: 1, minHeight: 0, background: '#ffffff' }}>
        <ConceptGraph
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
          masteredSubconcepts={masteredSubconcepts}
          onSubconceptMastered={handleSubconceptMastered}
          onPaneClick={handlePaneClick}
        />
      </div>

      {/* ── Bottom toolbar ── */}
      <div style={{ position: 'relative', zIndex: 20, flexShrink: 0, background: '#f4e87b' }}>

        {/* Main toolbar row */}
        <div style={{
          padding: '12px 20px 6px',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          minHeight: 60,
        }}>
          {selectedConcept ? (
            <>
              {/* Main concept button */}
              <button
                onClick={() => setSelectedItem(
                  selectedItem === selectedConcept.id ? null : selectedConcept.id
                )}
                style={{
                  background: selectedItem === selectedConcept.id
                    ? selectedConcept.color : '#ffffff',
                  color: '#000000',
                  borderTop:    '1.5px solid #1E293B',
                  borderLeft:   '1.5px solid #1E293B',
                  borderRight:  '4px solid #1E293B',
                  borderBottom: '4px solid #1E293B',
                  //border: '1px solid #000000',
                  borderRadius: 6,
                  padding: '5px 14px',
                  fontSize: 15, fontWeight: 700,
                  fontFamily: 'Helvetica, Arial, sans-serif',
                  cursor: 'pointer',
                  flexShrink: 0,
                  whiteSpace: 'nowrap',
                }}
              >
                {selectedConcept.label.replace(/\n/g, ' ')}
              </button>

              {/* Divider */}
              <div style={{
                width: 1, alignSelf: 'stretch',
                background: '#00000033', flexShrink: 0,
              }} />

              {/* Subconcept buttons */}
              <div style={{
                flex: 1,
                display: 'flex', flexWrap: 'wrap', gap: 8,
                alignItems: 'flex-start',
              }}>
                {selectedConcept.subconcepts.map(sub => (
                  <button
                    key={sub}
                    onClick={() => setSelectedItem(selectedItem === sub ? null : sub)}
                    style={{
                      background: selectedItem === sub ? selectedConcept.color : '#ffffff',
                      color: '#000000',
                      border: '1px solid #000000',
                      borderRadius: 6,
                      padding: '5px 14px',
                      fontSize: 14, fontWeight: 500,
                      fontFamily: 'Helvetica, Arial, sans-serif',
                      cursor: 'pointer',
                    }}
                  >
                    {sub.replace(/\n/g, ' ')}
                  </button>
                ))}
              </div>

              {/* Close button */}
              <div
                onClick={() => { setSelectedConceptId(null); setSelectedItem(null); setCloseHovered(false); if (!selectedQuestionId) setHighlightedIds(new Set());}}
                onMouseEnter={() => setCloseHovered(true)}
                onMouseLeave={() => setCloseHovered(false)}
                style={{
                  width: 28, height: 28, borderRadius: 6,
                  borderTop:    '1.5px solid #1E293B',
                  borderLeft:   '1.5px solid #1E293B',
                  borderRight:  '4px solid #1E293B',
                  borderBottom: '4px solid #1E293B',
                  background: closeHovered ? '#ef4444' : '#ffffff',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  cursor: 'pointer', flexShrink: 0,
                  transition: 'background 0.15s',
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                  stroke={closeHovered ? '#ffffff' : '#1E293B'}
                  strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </div>
            </>
          ) : (
            <div style={{ color: '#000000', fontSize: 13, lineHeight: '36px' }}>
              {selectedQuestionId
                ? 'Click a concept card to explore it.'
                : 'Select a question, or click any concept card to explore it.'}
            </div>
          )}
        </div>

        {/* Expanded cards — only shown when a button is clicked */}
        {selectedConcept && selectedItem !== null && (() => {
          const selectedItemLabel = selectedItem === selectedConcept.id
            ? selectedConcept.label.replace(/\n/g, ' ')
            : selectedItem ?? '';
          
          const contentKey = selectedItem === selectedConcept.id
            ? selectedConcept.id
            : `${selectedConcept.id}:${selectedItem}`;
          const content = conceptContent[contentKey];
          const isMajorConcept = selectedItem === selectedConcept.id;

          return (
            <div style={{ padding: '0 20px 20px', display: 'flex', gap: 12 }}>
              {[
                { label: 'Description',           key: 'description' },
                { label: 'Example',               key: 'example'     },
              ].map(card => {
                const isAdded = addedDetailKeys.has(`${card.label}:${selectedItemLabel}`);
                return (
                  <div
                    key={card.key}
                    draggable={!isAdded}
                    onDragStart={isAdded ? undefined : e => {
                      e.dataTransfer.setData('application/scaffold-card', JSON.stringify({
                        cardType:     card.label,
                        itemLabel:    selectedItemLabel,
                        conceptId:    selectedConcept.id,
                        conceptColor: selectedConcept.color,
                        cardContent:  content?.[card.key as keyof ConceptContent] ?? '',
                      }));
                      e.dataTransfer.effectAllowed = 'move';
                    }}
                    style={{
                      flex: 1,
                      background: toPastel(selectedConcept.color),
                      borderRadius: 8,
                      border: '1px solid #000000',
                      padding: '10px 14px',
                      textAlign: 'left',
                      cursor: isAdded ? 'default' : 'grab',
                      opacity: isAdded ? 0.8 : 1,
                    }}
                  >
                    <span style={{
                      background: selectedConcept.color,
                      borderRadius: 100,
                      padding: '2px 10px',
                      border: '1px solid #000000',
                      fontFamily: 'Helvetica, Arial, sans-serif',
                      fontSize: 12, fontWeight: 700, color: '#000000',
                      whiteSpace: 'nowrap',
                      width: 'fit-content',
                      display: 'block',
                      marginBottom: 8,
                    }}>
                      {card.label}
                    </span>
                    <div style={{
                      fontFamily: 'Helvetica, Arial, sans-serif',
                      fontSize: 15, color: '#1E293B',
                      lineHeight: 1.6,
                      //paddingLeft: 4,
                    }}>
                      {card.key === 'example' ? (
                        <pre style={{
                          fontFamily: 'monospace',
                          border: '1px solid #000000',
                          fontSize: 13,
                          background: '#e2e8f0',
                          borderRadius: 6,
                          padding: '8px 12px',
                          margin: 0,
                          whiteSpace: 'pre-wrap',
                          color: '#1E293B',
                        }}>
                          {content?.[card.key as keyof ConceptContent]
                            ?? `Example for "${selectedItemLabel}" will appear here.`}
                        </pre>
                      ) : (
                        content?.[card.key as keyof ConceptContent]
                          ?? `${card.label} for "${selectedItemLabel}" will appear here.`
                      )}
                    </div>
                  </div>
                );
              })}

              {isMajorConcept && (
              <a
                href={content?.practiceUrl ?? '#'}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  flex: '0 0 auto',
                  alignSelf: 'center',
                  background: selectedConcept.color,
                  borderRadius: 8,
                  borderTop:    '1.5px solid #1E293B',
                  borderLeft:   '1.5px solid #1E293B',
                  borderRight:  '4px solid #1E293B',
                  borderBottom: '4px solid #1E293B',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  textAlign: 'center',
                  fontFamily: 'Helvetica, Arial, sans-serif',
                  fontSize: 15, fontWeight: 700, color: '#1E293B',
                  cursor: content?.practiceUrl ? 'pointer' : 'default',
                  textDecoration: 'none',
                  padding: '10px 14px',
                  opacity: content?.practiceUrl ? 1 : 0.4,
                }}
              >
                Practice with a <br />PrairieLearn <br /> question
              </a>
            )}

            </div>
          );
        })()}
      </div>

    </div>
  );
}