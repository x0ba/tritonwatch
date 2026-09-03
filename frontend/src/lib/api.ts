import type {
  CatalogCourse,
  CreateWatchRequest,
  TermOption,
  UpdateNotificationPreferencesRequest,
  UpdateProfileRequest,
  UserProfile,
  WatchRequest,
  WatchlistItem,
} from "./types";

const userApiBase = () => import.meta.env.VITE_USER_API_BASE_URL ?? "http://localhost:8081";

const watchlistApiBase = () =>
  import.meta.env.VITE_WATCHLIST_API_BASE_URL ?? "http://localhost:8082";

const catalogApiBase = () => import.meta.env.VITE_CATALOG_API_BASE_URL ?? "http://localhost:8083";

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, message: string, body: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function parseJson(response: Response): Promise<unknown> {
  const text = await response.text();
  return text ? (JSON.parse(text) as unknown) : null;
}

function detailFromBody(body: unknown, status: number): string {
  if (body && typeof body === "object" && "detail" in body && typeof body.detail === "string") {
    return body.detail;
  }
  return `Request failed (${status})`;
}

async function request<T>(
  baseUrl: string,
  path: string,
  accessToken: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  headers.set("Authorization", `Bearer ${accessToken}`);
  if (init.body) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const body = await parseJson(response);

  if (!response.ok) {
    throw new ApiError(response.status, detailFromBody(body, response.status), body);
  }

  return body as T;
}

async function publicRequest<T>(baseUrl: string, path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers,
  });

  const body = await parseJson(response);
  if (!response.ok) {
    throw new ApiError(response.status, detailFromBody(body, response.status), body);
  }
  return body as T;
}

export async function getMe(accessToken: string): Promise<UserProfile | null> {
  try {
    return await request<UserProfile>(userApiBase(), "/api/v1/me", accessToken);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function upsertMe(
  accessToken: string,
  body: UpdateProfileRequest,
): Promise<UserProfile> {
  return request<UserProfile>(userApiBase(), "/api/v1/me", accessToken, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function updateNotificationPreferences(
  accessToken: string,
  body: UpdateNotificationPreferencesRequest,
): Promise<UserProfile> {
  return request<UserProfile>(userApiBase(), "/api/v1/me/notification-preferences", accessToken, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function requestEmailVerification(accessToken: string): Promise<void> {
  await request<void>(userApiBase(), "/api/v1/me/email/verification-requests", accessToken, {
    method: "POST",
  });
}

export async function confirmEmailVerification(
  accessToken: string,
  code: string,
): Promise<UserProfile> {
  return request<UserProfile>(userApiBase(), "/api/v1/me/email/verifications", accessToken, {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export async function requestPhoneVerification(accessToken: string): Promise<void> {
  await request<void>(userApiBase(), "/api/v1/me/phone/verification-requests", accessToken, {
    method: "POST",
  });
}

export async function confirmPhoneVerification(
  accessToken: string,
  code: string,
): Promise<UserProfile> {
  return request<UserProfile>(userApiBase(), "/api/v1/me/phone/verifications", accessToken, {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export async function deleteMe(accessToken: string): Promise<void> {
  await request<void>(userApiBase(), "/api/v1/me", accessToken, {
    method: "DELETE",
  });
}

export async function createWatchRequest(
  accessToken: string,
  body: CreateWatchRequest,
): Promise<WatchRequest> {
  return request<WatchRequest>(watchlistApiBase(), "/api/v1/watch-requests", accessToken, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

type WatchRequestListResponse = {
  watches: WatchRequest[];
};

export async function listWatchRequests(
  accessToken: string,
  options: { term?: string; signal?: AbortSignal } = {},
): Promise<WatchRequest[]> {
  const params = new URLSearchParams();
  if (options.term) {
    params.set("term", options.term);
  }
  const query = params.toString();
  const response = await request<WatchRequestListResponse>(
    watchlistApiBase(),
    `/api/v1/watch-requests${query ? `?${query}` : ""}`,
    accessToken,
    { signal: options.signal },
  );
  return response.watches;
}

type CatalogSearchResponse = {
  term: string;
  query: string;
  count: number;
  courses: CatalogCourse[];
};

type CatalogTermsResponse = {
  currentTerm: string;
  terms: TermOption[];
};

export async function searchCatalogCourses(options: {
  term: string;
  query: string;
  limit?: number;
  signal?: AbortSignal;
}): Promise<CatalogCourse[]> {
  const params = new URLSearchParams();
  params.set("term", options.term);
  params.set("q", options.query);
  if (options.limit != null) {
    params.set("limit", String(options.limit));
  }

  const response = await publicRequest<CatalogSearchResponse>(
    catalogApiBase(),
    `/api/v1/catalog/courses?${params.toString()}`,
    { signal: options.signal },
  );
  return response.courses;
}

export async function listCatalogTerms(signal?: AbortSignal): Promise<CatalogTermsResponse> {
  return publicRequest<CatalogTermsResponse>(catalogApiBase(), "/api/v1/catalog/terms", {
    signal,
  });
}

type CatalogLookupResponse = {
  term: string;
  count: number;
  courses: CatalogCourse[];
};

export const CATALOG_LOOKUP_BATCH_SIZE = 100;

export async function lookupCatalogCourses(options: {
  term: string;
  ids: string[];
  signal?: AbortSignal;
}): Promise<CatalogCourse[]> {
  if (options.ids.length === 0) {
    return [];
  }

  const batches: string[][] = [];
  for (let i = 0; i < options.ids.length; i += CATALOG_LOOKUP_BATCH_SIZE) {
    batches.push(options.ids.slice(i, i + CATALOG_LOOKUP_BATCH_SIZE));
  }

  const results = await Promise.all(
    batches.map((ids) =>
      lookupCatalogCourseBatch({
        term: options.term,
        ids,
        signal: options.signal,
      }),
    ),
  );
  return results.flat();
}

async function lookupCatalogCourseBatch(options: {
  term: string;
  ids: string[];
  signal?: AbortSignal;
}): Promise<CatalogCourse[]> {
  const params = new URLSearchParams();
  params.set("term", options.term);
  for (const id of options.ids) {
    params.append("ids", id);
  }

  const response = await publicRequest<CatalogLookupResponse>(
    catalogApiBase(),
    `/api/v1/catalog/courses/lookup?${params.toString()}`,
    { signal: options.signal },
  );
  return response.courses;
}

export function toWatchlistItem(watch: WatchRequest, course?: CatalogCourse): WatchlistItem {
  const openSeats = course?.openSeats ?? 0;
  return {
    id: watch.id,
    courseId: watch.courseId,
    term: watch.term,
    title: course?.title ?? watch.courseId,
    openSeats,
    waitlist: course?.waitlist ?? 0,
    watchingSince: watch.createdAt,
    seatsOpen: openSeats > 0,
  };
}

export async function loadWatchlistItems(
  accessToken: string,
  options: { term?: string; signal?: AbortSignal } = {},
): Promise<WatchlistItem[]> {
  const watches = await listWatchRequests(accessToken, {
    term: options.term,
    signal: options.signal,
  });
  if (watches.length === 0) {
    return [];
  }

  const idsByTerm = new Map<string, string[]>();
  for (const watch of watches) {
    const ids = idsByTerm.get(watch.term) ?? [];
    ids.push(watch.courseId);
    idsByTerm.set(watch.term, ids);
  }

  const courses = await Promise.all(
    [...idsByTerm.entries()].map(async ([term, ids]) => {
      const found = await lookupCatalogCourses({
        term,
        ids,
        signal: options.signal,
      });
      return [term, found] as const;
    }),
  );

  const courseByTermAndId = new Map<string, CatalogCourse>();
  for (const [term, found] of courses) {
    for (const course of found) {
      courseByTermAndId.set(`${term}:${course.courseId}`, course);
    }
  }

  return watches.map((watch) =>
    toWatchlistItem(watch, courseByTermAndId.get(`${watch.term}:${watch.courseId}`)),
  );
}
