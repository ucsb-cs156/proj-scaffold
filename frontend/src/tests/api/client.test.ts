import { describe, test, expect, vi, beforeEach, afterEach } from "vitest";
import {
  fetchAssessments,
  fetchQuestions,
  fetchQuestionConcepts,
  fetchUserState,
  saveUserState,
  logUserActivity,
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
});
