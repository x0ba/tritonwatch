import { afterEach, describe, expect, it, vi } from "vitest";
import { CATALOG_LOOKUP_BATCH_SIZE, loadWatchlistItems, lookupCatalogCourses } from "./api";

function jsonResponse(body: unknown, status = 200): Promise<Response> {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("lookupCatalogCourses", () => {
  it("keeps the 101st course available by batching lookup requests", async () => {
    const ids = Array.from({ length: CATALOG_LOOKUP_BATCH_SIZE + 1 }, (_, index) => `CSE ${index}`);
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), "http://localhost:8083");
      const requested = url.searchParams.getAll("ids");
      expect(requested.length).toBeLessThanOrEqual(CATALOG_LOOKUP_BATCH_SIZE);
      return jsonResponse({
        term: "FA26",
        count: requested.length,
        courses: requested.map((courseId) => ({
          courseId,
          title: courseId,
          openSeats: courseId === "CSE 100" ? 5 : 0,
          waitlist: 0,
        })),
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    const courses = await lookupCatalogCourses({ term: "FA26", ids });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(courses).toHaveLength(101);
    expect(courses.find((course) => course.courseId === "CSE 100")).toMatchObject({
      openSeats: 5,
    });
  });
});

describe("loadWatchlistItems", () => {
  it("requests watches for the selected term", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/watch-requests") {
        expect(url.searchParams.get("term")).toBe("FA26");
        return jsonResponse({
          watches: [
            {
              id: "watch-1",
              courseId: "CSE 11",
              term: "FA26",
              createdAt: "2026-09-01T00:00:00Z",
            },
          ],
        });
      }

      expect(url.pathname).toBe("/api/v1/catalog/courses/lookup");
      return jsonResponse({
        term: "FA26",
        count: 1,
        courses: [{ courseId: "CSE 11", title: "Introduction to CSE", openSeats: 2, waitlist: 0 }],
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    const items = await loadWatchlistItems("token", { term: "FA26" });

    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({
      courseId: "CSE 11",
      term: "FA26",
      openSeats: 2,
      seatsOpen: true,
    });
  });

  it("keeps watches from every term when no term filter is passed", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/watch-requests") {
        expect(url.searchParams.has("term")).toBe(false);
        return jsonResponse({
          watches: [
            {
              id: "watch-fa",
              courseId: "CSE 11",
              term: "FA26",
              createdAt: "2026-09-01T00:00:00Z",
            },
            {
              id: "watch-sp",
              courseId: "CSE 101",
              term: "SP26",
              createdAt: "2026-09-02T00:00:00Z",
            },
          ],
        });
      }

      const term = url.searchParams.get("term");
      const courseId = term === "SP26" ? "CSE 101" : "CSE 11";
      return jsonResponse({
        term,
        count: 1,
        courses: [{ courseId, title: courseId, openSeats: 4, waitlist: 0 }],
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    const items = await loadWatchlistItems("token");

    expect(items.map((item) => `${item.term}:${item.courseId}`)).toEqual([
      "FA26:CSE 11",
      "SP26:CSE 101",
    ]);
  });
});
