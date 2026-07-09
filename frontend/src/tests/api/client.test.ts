import { describe, test, expect, vi, beforeEach, afterEach } from "vitest";
import {
  fetchAssessments,
  fetchQuestions,
  fetchQuestionConcepts,
  fetchUserState,
  saveUserState,
  logUserActivity,
  fetchConceptGraph,
  fetchConceptContent,
  fetchConceptPositions,
  fetchConceptEdges,
  fetchUserStateV2,
  saveUserStateV2,
  logUserActivityV2,
  reorderSubconcepts,
} from "main/api/client";

function mockFetchOnce(
  body: unknown,
  init: { status?: number; ok?: boolean } = {},
) {
  const { status = 200, ok = status >= 200 && status < 300 } = init;
  const response = {
    ok,
    status,
    json: () => Promise.resolve(body),
  } as Response;
  (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
    response,
  );
  return response;
}

describe("api/client", () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("fetchAssessments", () => {
    test("fetches from the assessments endpoint and returns json", async () => {
      const assessments = [{ id: "1", pl_assessment_id: "pl1", name: "HW1" }];
      mockFetchOnce(assessments);

      const result = await fetchAssessments();

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/assessments");
      expect(result).toEqual(assessments);
    });
  });

  describe("fetchQuestions", () => {
    test("fetches from the questions endpoint for the given assessment", async () => {
      const questions = [
        {
          id: "1",
          assessment_id: "1",
          pl_question_uuid: "uuid-1",
          title: "Q1",
        },
      ];
      mockFetchOnce(questions);

      const result = await fetchQuestions("1");

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/assessments/1/questions",
      );
      expect(result).toEqual(questions);
    });
  });

  describe("fetchQuestionConcepts", () => {
    test("fetches from the concepts endpoint for the given question", async () => {
      const concepts = [
        {
          id: "1",
          question_id: "1",
          concept_id: "c1",
          subconcept_label: null,
        },
      ];
      mockFetchOnce(concepts);

      const result = await fetchQuestionConcepts("1");

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/questions/1/concepts",
      );
      expect(result).toEqual(concepts);
    });
  });

  describe("fetchUserState", () => {
    test("returns the parsed user state on success", async () => {
      const userState = {
        starred_ids: ["1"],
        detail_cards: [],
        mastered_subconcepts: [],
      };
      mockFetchOnce(userState);

      const result = await fetchUserState(1);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/user-state/1");
      expect(result).toEqual(userState);
    });

    test("returns null when the response is a 404", async () => {
      mockFetchOnce(null, { status: 404, ok: false });

      const result = await fetchUserState(1);

      expect(result).toBeNull();
    });

    test("throws when the response is not ok and not a 404", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchUserState(1)).rejects.toThrow(
        "Failed to fetch user state for userid 1",
      );
    });
  });

  describe("saveUserState", () => {
    const body = {
      userid: 1,
      starred_ids: ["1"],
      detail_cards: [],
      mastered_subconcepts: [],
    };

    test("posts the body as json on success", async () => {
      mockFetchOnce(null, { status: 200 });

      await saveUserState(body);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/user-state", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(saveUserState(body)).rejects.toThrow(
        "Failed to save user state for userid 1",
      );
    });
  });

  describe("logUserActivity", () => {
    const body = {
      userid: 1,
      event_type: "click",
      payload: { foo: "bar" },
    };

    test("posts the body as json on success", async () => {
      mockFetchOnce(null, { status: 200 });

      await logUserActivity(body);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/user-activity", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(logUserActivity(body)).rejects.toThrow(
        "Failed to log user activity for userid 1",
      );
    });
  });

  describe("fetchConceptGraph", () => {
    test("fetches from the concepts graph endpoint for the given course", async () => {
      const graph = [
        { name: "a", label: "A", color: "#111111", subconcepts: [] },
      ];
      mockFetchOnce(graph);

      const result = await fetchConceptGraph(1);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/concepts/graph?courseId=1",
      );
      expect(result).toEqual(graph);
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchConceptGraph(1)).rejects.toThrow(
        "Failed to fetch concept graph for course 1",
      );
    });
  });

  describe("fetchConceptContent", () => {
    test("fetches from the concepts content endpoint for the given course", async () => {
      const content = {
        a: { description: "d", example: "e", practiceUrl: null },
      };
      mockFetchOnce(content);

      const result = await fetchConceptContent(1);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/concepts/content?courseId=1",
      );
      expect(result).toEqual(content);
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchConceptContent(1)).rejects.toThrow(
        "Failed to fetch concept content for course 1",
      );
    });
  });

  describe("fetchConceptPositions", () => {
    test("fetches from the concepts positions endpoint for the given course", async () => {
      const positions = { a: { x: 1, y: 2 } };
      mockFetchOnce(positions);

      const result = await fetchConceptPositions(1);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/concepts/positions?courseId=1",
      );
      expect(result).toEqual(positions);
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchConceptPositions(1)).rejects.toThrow(
        "Failed to fetch concept positions for course 1",
      );
    });
  });

  describe("fetchConceptEdges", () => {
    test("fetches from the concepts edges endpoint for the given course", async () => {
      const edges = [{ source: "a", target: "b" }];
      mockFetchOnce(edges);

      const result = await fetchConceptEdges(1);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/concepts/edges?courseId=1",
      );
      expect(result).toEqual(edges);
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchConceptEdges(1)).rejects.toThrow(
        "Failed to fetch concept edges for course 1",
      );
    });
  });

  describe("fetchUserStateV2", () => {
    test("returns the parsed user state on success", async () => {
      const userState = {
        starred_ids: ["1"],
        detail_cards: [],
        mastered_subconcepts: [],
      };
      mockFetchOnce(userState);

      const result = await fetchUserStateV2(1, 2);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/user-state-v2?userid=1&courseId=2",
      );
      expect(result).toEqual(userState);
    });

    test("returns null when the response is a 404", async () => {
      mockFetchOnce(null, { status: 404, ok: false });

      const result = await fetchUserStateV2(1, 2);

      expect(result).toBeNull();
    });

    test("throws when the response is not ok and not a 404", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(fetchUserStateV2(1, 2)).rejects.toThrow(
        "Failed to fetch user state for userid 1 in course 2",
      );
    });
  });

  describe("saveUserStateV2", () => {
    const body = {
      userid: 1,
      courseId: 2,
      starred_ids: ["1"],
      detail_cards: [],
      mastered_subconcepts: [],
      top_level_positions: {},
    };

    test("posts the body as json on success", async () => {
      mockFetchOnce(null, { status: 200 });

      await saveUserStateV2(body);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/user-state-v2", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(saveUserStateV2(body)).rejects.toThrow(
        "Failed to save user state for userid 1 in course 2",
      );
    });
  });

  describe("reorderSubconcepts", () => {
    test("puts the complete ordered id list and returns the reordered DTOs", async () => {
      const reordered = [
        { id: 5, parentId: 1, labelHtml: "C" },
        { id: 2, parentId: 1, labelHtml: "A" },
      ];
      mockFetchOnce(reordered);

      const result = await reorderSubconcepts(1, [5, 2]);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/concepts/subconcepts/reorder?parentConceptId=1",
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify([5, 2]),
        },
      );
      expect(result).toEqual(reordered);
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 400, ok: false });

      await expect(reorderSubconcepts(1, [5, 2])).rejects.toThrow(
        "Failed to reorder subconcepts of concept 1",
      );
    });
  });

  describe("logUserActivityV2", () => {
    const body = {
      userid: 1,
      courseId: 2,
      event_type: "click",
      payload: { foo: "bar" },
    };

    test("posts the body as json on success", async () => {
      mockFetchOnce(null, { status: 200 });

      await logUserActivityV2(body);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/user-activity-v2", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
    });

    test("throws when the response is not ok", async () => {
      mockFetchOnce(null, { status: 500, ok: false });

      await expect(logUserActivityV2(body)).rejects.toThrow(
        "Failed to log user activity for userid 1 in course 2",
      );
    });
  });
});
