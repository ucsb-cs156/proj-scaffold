# Issue #116 (branch name says "118" — see note below): Associate PlAssessmentQuestions with Concepts

Status as of this commit: **backend done and passing tests; frontend not started.**

## Branch/naming note

The user originally asked to start a branch for "issue 118," but the repo's highest issue
number is #116 — the user confirmed mid-conversation that they meant **issue #116**
("Add database table to associate plAssessmentQuestions with Concepts",
https://github.com/ucsb-cs156/proj-scaffold/issues/116). The branch is named
`pc-Claude-issue116`. This doc is `docs/issue118.md` only because that's the name that was
in flight when the user had to stop for a trip — feel free to rename to `issue116.md` when
picking this back up.

## Full plan

The complete, approved implementation plan (research findings + design decisions) is at
`/Users/pconrad/.claude/plans/idempotent-sprouting-anchor.md` on this machine. That file has
the full rationale for every decision below. If that machine/path isn't available, the key
decisions are summarized here.

## What's done (backend, committed)

- **`src/main/java/edu/ucsb/cs/scaffold/entity/PlAssessmentQuestionConcept.java`** — new join
  entity, table `pl_assessment_question_concept`: raw `Long plAssessmentQuestionId` (matches the
  `pl_*` family's raw-FK convention) + `@ManyToOne Concept concept` (a real JPA relation, so
  `concept.getCourse()` is available for validation).
- **`src/main/java/edu/ucsb/cs/scaffold/repository/PlAssessmentQuestionConceptRepository.java`**
  — `findByPlAssessmentQuestionId`, `findByPlAssessmentQuestionIdAndConceptId`.
- **`src/main/resources/db/migration/changes/042-create-pl-assessment-question-concept-table.json`**
  — creates the table with FKs to `pl_assessment_question(id)` and `concepts(id)`, **both with
  `"onDelete": "CASCADE"`**. This is a deliberate deviation from this codebase's usual "manually
  delete children before parent in Java" convention (see `ConceptsController
  .deleteConceptArtifacts`, `SyncCourseWithPlRepoJob`, `PLRepoController.deletePlRepo`) —
  reasoning is in the plan file. If picking this back up, it's worth a final sanity check that
  cascade delete actually fires correctly in a real Postgres run (only unit-tested so far, not
  integration-tested against a live DB).
- **`src/main/java/edu/ucsb/cs/scaffold/controller/PLAssessmentQuestionController.java`** — new
  controller, `@RequestMapping("/api/plAssessmentQuestion")`:
  - `GET /{plAssessmentQuestionId}/concepts` → `List<TaggedConceptDTO>` (no `@PreAuthorize`,
    same as sibling read endpoints).
  - `POST /addConcept?plAssessmentQuestionId=&conceptId=` — validates
    `concept.getCourse().getPlInstanceId()` matches the assessment's `getPlInstanceId()`
    (resolved via `plAssessmentQuestionRepository` → `plAssessmentRepository`), throws
    `IllegalArgumentException` on mismatch.
  - `DELETE /deleteConcept?plAssessmentQuestionId=&conceptId=`.
  - Both mutating endpoints use `@PreAuthorize
    ("@CourseSecurity.hasConceptManagementPermissions(#root, #conceptId)")` — reused as-is, no
    new `CourseSecurity` method or `TestCourseSecurity` stub needed.
  - **422 handling**: the controller declares its own local
    `@ExceptionHandler(IllegalArgumentException.class)` → `HttpStatus.UNPROCESSABLE_ENTITY`,
    overriding `ApiController`'s inherited 400 mapping *for this controller only* (per the
    issue's explicit ask). Every other controller is untouched and still maps
    `IllegalArgumentException` → 400.
  - `TaggedConceptDTO` reuses the frontend's existing `QuestionConcept` JSON shape (`id`,
    `question_id`, `concept_id`, `subconcept_label`) so the frontend type doesn't need to change
    for the read side — `question_id` is populated with the `plAssessmentQuestionId` (field name
    is legacy/unused by the frontend's `concept_id`/`subconcept_label`-only consumption),
    `subconcept_label` is always `null` (this issue only tags top-level concepts, no subconcept
    granularity).
- **`AssessmentController.java`** — `QuestionDTO` gained a `pl_assessment_question_id` field
  (the join row's own id, `join.getId()`), so the frontend can get a question's
  `plAssessmentQuestionId` without an extra round trip. `AssessmentControllerTests.java`'s
  `getQuestions_returns_questions_in_ordinal_order_and_skips_orphaned_join_rows` test was updated
  to expect the new field.
- **Removed** `QuestionController.java` and `QuestionControllerTests.java` — this was the
  intentional stub (`GET /api/questions/{questionId}/concepts`, always returned `[]`) that this
  issue makes obsolete; confirmed via grep it had no other consumers
  (`LegacyQuestionController` is separate/untouched).
- **New** `PLAssessmentQuestionControllerTests.java` — covers GET (empty/populated), POST
  (success + verify saved fields, 404 concept, 404 plAssessmentQuestion, 422 instance mismatch,
  403 anonymous/no-permissions), DELETE (success + verify delete, 404 untagged pair, 403
  anonymous/no-permissions).

**Verified**: `mvn compile` and `mvn test-compile` both pass. `mvn test
-Dtest=PLAssessmentQuestionControllerTests,AssessmentControllerTests` passes (exit code 0, no
failures). **The full backend test suite has NOT been run** (ran out of time) — do that first
before continuing, in case something elsewhere references the removed `QuestionController` or
the changed `QuestionDTO` shape in a way that wasn't caught by grep.

## What's NOT done (frontend — not started at all)

All of this is designed in the plan file but no frontend code has been written yet:

1. **`frontend/src/main/types/conceptGraph.ts`** — add `pl_assessment_question_id: string` to
   the `Question` interface.
2. **`ConceptGraphPage.tsx`** — repoint the `questionConcepts` query from
   `/api/questions/${selectedQuestionId}/concepts` to
   `/api/plAssessmentQuestion/${plAssessmentQuestionId}/concepts`, where
   `plAssessmentQuestionId` comes from `questions.find(q => q.id === selectedQuestionId)
   ?.pl_assessment_question_id`. The existing `useEffect` that turns `questionConcepts` into
   `highlightedIds` via `computeScaffoldSubgraph` (around line 789-811 pre-changes) stays as-is —
   it's the reused "grey out all + ancestors" mechanism.
3. **New component** `frontend/src/main/components/Scaffold/AssignConceptsToggle.tsx` — plain
   `<button>` (matches `AssessmentSelect`/`QuestionSearch` styling, not react-bootstrap), props
   `active`, `disabled`, `onClick`. Needs its own test file and Storybook story per this repo's
   convention (see memory: new components always need dedicated test + story files).
4. **`ScaffoldTopBar.tsx`** — render the new toggle immediately after `QuestionSearch`, gated on
   `enableEditing` (from `useStaffTools()`, already imported there).
5. **`ConceptGraphPage.tsx` wiring**:
   - New state `assignConceptsMode`, reset to `false` alongside the existing
     `selectedAssessmentId`/`selectedQuestionId` reset effects and in `handleReset`.
   - Two `useBackendMutation`s: POST `/api/plAssessmentQuestion/addConcept` and DELETE
     `/api/plAssessmentQuestion/deleteConcept`, each invalidating the
     `["/api/plAssessmentQuestion", plAssessmentQuestionId, "concepts"]` query key on success.
   - `assignedConceptIds = new Set(questionConcepts.map(c => c.concept_id))` — the *directly*
     tagged set, distinct from the ancestor-expanded `highlightedIds`, so clicking an
     ancestor-only-highlighted node doesn't mistakenly fire a DELETE.
   - `handleConceptClick` branches at the top: if `assignConceptsMode && plAssessmentQuestionId`,
     POST or DELETE based on `assignedConceptIds.has(id)` and `return` early (skip the normal
     `setSelectedConceptId` detail-toolbar behavior — assign mode is a distinct interaction mode
     per the issue text).
6. **Tests**: extend `ConceptGraphPage.test.tsx` (toggle + tag/untag flows) and
   `ScaffoldTopBar.test.tsx` (toggle presence/absence).
7. **Manual verification**: run the app, editing mode on, select assessment+question, toggle
   "Assign Concepts," click nodes to tag/untag, confirm graph greying and Network tab
   POST/DELETE calls, confirm toggle hidden without editing/without a selected question.

## Files touched so far (for a quick `git diff` orientation)

```
new file:   docs/issue118.md
new file:   src/main/java/edu/ucsb/cs/scaffold/controller/PLAssessmentQuestionController.java
modified:   src/main/java/edu/ucsb/cs/scaffold/controller/AssessmentController.java
deleted:    src/main/java/edu/ucsb/cs/scaffold/controller/QuestionController.java
new file:   src/main/java/edu/ucsb/cs/scaffold/entity/PlAssessmentQuestionConcept.java
new file:   src/main/java/edu/ucsb/cs/scaffold/repository/PlAssessmentQuestionConceptRepository.java
new file:   src/main/resources/db/migration/changes/042-create-pl-assessment-question-concept-table.json
modified:   src/test/java/edu/ucsb/cs/scaffold/controller/AssessmentControllerTests.java
new file:   src/test/java/edu/ucsb/cs/scaffold/controller/PLAssessmentQuestionControllerTests.java
deleted:    src/test/java/edu/ucsb/cs/scaffold/controller/QuestionControllerTests.java
```

## To resume

1. `git checkout pc-Claude-issue116` (or check out the draft PR's branch).
2. Read this file and the plan file (`~/.claude/plans/idempotent-sprouting-anchor.md`, if still
   present).
3. Run the full backend test suite (`mvn test`) to confirm nothing broke.
4. Pick up the frontend work from item 1 in the "not done" list above.
